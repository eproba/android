package com.czaplicki.eproba.ui.manage_exams

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.czaplicki.eproba.EprobaApplication
import com.czaplicki.eproba.R
import com.czaplicki.eproba.api.EprobaService
import com.czaplicki.eproba.db.Exam
import com.czaplicki.eproba.db.ExamDao
import com.czaplicki.eproba.db.Task
import com.czaplicki.eproba.db.User
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Response


class ManagedExamAdapter(
    private val dataSet: MutableList<Exam>,
    private val users: List<User>,
    private val service: EprobaService,
    val examDao: ExamDao
) :
    RecyclerView.Adapter<ManagedExamAdapter.ViewHolder>() {


    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView
        val supervisor: TextView
        val progressPercentage: TextView
        val adFrame: FrameLayout
        val taskList: RecyclerView
        val menuButton: ImageButton

        init {
            // Define click listener for the ViewHolder's View.
            name = view.findViewById(R.id.name)
            supervisor = view.findViewById(R.id.supervisor)
            progressPercentage = view.findViewById(R.id.progressPercentage)
            adFrame = view.findViewById(R.id.ad_frame)
            taskList = view.findViewById(R.id.task_list) as RecyclerView
            menuButton = view.findViewById(R.id.menu_button)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.exam_item, viewGroup, false)
        val viewHolder = ViewHolder(view)

        viewHolder.taskList.layoutManager = LinearLayoutManager(viewHolder.itemView.context)
        val mDividerItemDecoration = MaterialDividerItemDecoration(
            viewHolder.taskList.context,
            LinearLayoutManager(viewHolder.itemView.context).orientation
        )
        viewHolder.taskList.addItemDecoration(mDividerItemDecoration)
        return viewHolder
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val exam = dataSet[position]

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        if (exam.id == -1L && exam.name == "no_exams") {
            viewHolder.name.text = viewHolder.itemView.context.getString(R.string.no_exams)
            viewHolder.supervisor.visibility = View.GONE
            viewHolder.progressPercentage.visibility = View.GONE
            viewHolder.taskList.visibility = View.GONE
            viewHolder.menuButton.visibility = View.GONE
            return
        } else if (exam.id == -1L && exam.name == "ad") {
            viewHolder.name.text = viewHolder.itemView.context.getString(R.string.advertisement)
            viewHolder.supervisor.visibility = View.GONE
            viewHolder.progressPercentage.visibility = View.GONE
            viewHolder.taskList.visibility = View.GONE
            viewHolder.adFrame.visibility = View.VISIBLE
            viewHolder.menuButton.visibility = View.GONE
            val builder = AdLoader.Builder(
                viewHolder.itemView.context,
                "ca-app-pub-7127294792989521/8405155665"
            )
                .forNativeAd { nativeAd ->
                    // Assumes that your ad layout is in a file call native_ad_layout.xml
                    // in the res/layout folder
                    val adView = LayoutInflater.from(viewHolder.adFrame.context)
                        .inflate(R.layout.native_ad_layout, null) as NativeAdView
                    // This method sets the text, images and the native ad, etc into the ad
                    // view.
                    populateNativeAdView(nativeAd, adView)
                    // Assumes you have a placeholder FrameLayout in your View layout
                    // (with id ad_frame) where the ad is to be placed.
                    viewHolder.adFrame.removeAllViews()
                    viewHolder.adFrame.addView(adView)
                }
            builder.build().loadAd(AdRequest.Builder().build())
            return
        }

        viewHolder.name.text =
            exam.name + " - " + users.find { it.id == exam.userId }?.nicknameWithRank
        if (exam.isArchived) {
            viewHolder.name.text = viewHolder.name.text.toString() + " (archiwum)"
        }
        if (exam.supervisor != null) {
            viewHolder.supervisor.visibility = View.VISIBLE
            viewHolder.supervisor.text =
                users.find { it.id == exam.supervisor }?.fullNameWithNickname
        } else {
            viewHolder.supervisor.visibility = View.GONE
        }
        viewHolder.progressPercentage.visibility = View.VISIBLE
        viewHolder.progressPercentage.text =
            if (exam.tasks.size == 0) "" else viewHolder.itemView.context.getString(
                R.string.progress_percentage,
                exam.tasks.filter { it.status == Task.Status.APPROVED }.size * 100 / exam.tasks.size
            )
        viewHolder.taskList.visibility = View.VISIBLE
        viewHolder.taskList.adapter =
            ManagedTaskAdapter(exam, users, viewHolder.progressPercentage, service)
        viewHolder.adFrame.visibility = View.GONE
        viewHolder.menuButton.visibility = View.VISIBLE
        viewHolder.menuButton.setOnClickListener {
            val popup = PopupMenu(viewHolder.itemView.context, viewHolder.menuButton)
            popup.menuInflater.inflate(R.menu.exam_menu, popup.menu)
            popup.menu.findItem(R.id.archive_exam).title =
                if (exam.isArchived) viewHolder.itemView.context.getString(
                    R.string.unarchive_exam
                ) else viewHolder.itemView.context.getString(R.string.archive_exam)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.edit_exam -> {
                        Toast.makeText(
                            viewHolder.itemView.context,
                            viewHolder.itemView.context.getString(
                                R.string.not_implemented_yet,
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }

                    R.id.delete_exam -> {
                        MaterialAlertDialogBuilder(
                            viewHolder.itemView.context,
                            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                        )
                            .setTitle(R.string.delete_exam_title)
                            .setMessage(
                                viewHolder.itemView.context.getString(
                                    R.string.delete_exam_confirmation,
                                    exam.name + " - " + users.find { it.id == exam.userId }?.nicknameWithRank
                                )
                            )
                            .setIcon(R.drawable.delete_forever_48px)
                            .setNeutralButton(R.string.cancel) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setPositiveButton(R.string.delete_exam) { dialog, _ ->
                                GlobalScope.launch {
                                    try {
                                        val response: Response<Void> = if (!exam.isArchived) {
                                            service.deleteExam(exam.id)
                                        } else {
                                            service.deleteArchivedExam(exam.id)
                                        }
                                        if (!response.isSuccessful) {
                                            throw Exception(response.errorBody()?.string()
                                                ?.let { it1 ->
                                                    JSONObject(it1).getString("detail")
                                                        ?: response.errorBody()?.string()
                                                })
                                        }
                                        examDao.delete(exam)
                                        dataSet.removeAt(viewHolder.adapterPosition)
                                        EprobaApplication.instance.currentActivity?.runOnUiThread {
                                            notifyItemRemoved(viewHolder.adapterPosition)
                                            Snackbar.make(
                                                viewHolder.itemView.rootView,
                                                viewHolder.itemView.context.getString(
                                                    R.string.exam_deleted
                                                ),
                                                Snackbar.LENGTH_LONG
                                            ).show()
                                            dialog.dismiss()
                                        }
                                    } catch (e: Exception) {
                                        EprobaApplication.instance.currentActivity?.runOnUiThread {
                                            MaterialAlertDialogBuilder(
                                                viewHolder.itemView.context,
                                                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                                            )
                                                .setTitle(R.string.error_dialog_title)
                                                .setIcon(R.drawable.ic_error)
                                                .setMessage(
                                                    viewHolder.itemView.context.getString(
                                                        R.string.exam_deletion_error,
                                                        e.message
                                                    )
                                                )
                                                .setPositiveButton(android.R.string.ok, null)
                                                .show()
                                            dialog.dismiss()
                                        }
                                    }
                                }
                            }
                            .show()
                        true
                    }

                    R.id.archive_exam -> {
                        MaterialAlertDialogBuilder(
                            viewHolder.itemView.context,
                            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                        )
                            .setTitle(if (exam.isArchived) R.string.unarchive_exam_title else R.string.archive_exam_title)
                            .setMessage(
                                viewHolder.itemView.context.getString(
                                    if (exam.isArchived) R.string.unarchive_exam_confirmation else R.string.archive_exam_confirmation,
                                    exam.name + " - " + users.find { it.id == exam.userId }?.nicknameWithRank
                                )
                            )
                            .setIcon(if (exam.isArchived) R.drawable.unarchive_48px else R.drawable.archive_48px)
                            .setNeutralButton(R.string.cancel) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setPositiveButton(if (exam.isArchived) R.string.unarchive_exam else R.string.archive_exam) { dialog, _ ->
                                GlobalScope.launch {
                                    try {
                                        examDao.insert(
                                            if (!exam.isArchived) {
                                                service.updateExam(
                                                    exam.id,
                                                    "{\"is_archived\": true}".toRequestBody(
                                                        "application/json".toMediaTypeOrNull()
                                                    )
                                                )
                                            } else {
                                                service.updateExamInArchive(
                                                    exam.id,
                                                    "{\"is_archived\": false}".toRequestBody(
                                                        "application/json".toMediaTypeOrNull()
                                                    )
                                                )
                                            }
                                        )
                                        dataSet.removeAt(viewHolder.adapterPosition)
                                        EprobaApplication.instance.currentActivity?.runOnUiThread {
                                            notifyItemRemoved(viewHolder.adapterPosition)
                                            Snackbar.make(
                                                viewHolder.itemView.rootView,
                                                viewHolder.itemView.context.getString(
                                                    if (exam.isArchived) R.string.exam_unarchived else R.string.exam_archived
                                                ),
                                                Snackbar.LENGTH_LONG
                                            ).show()
                                            dialog.dismiss()
                                        }
                                    } catch (e: Exception) {
                                        EprobaApplication.instance.currentActivity?.runOnUiThread {
                                            MaterialAlertDialogBuilder(
                                                viewHolder.itemView.context,
                                                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                                            )
                                                .setTitle(R.string.error_dialog_title)
                                                .setIcon(R.drawable.ic_error)
                                                .setMessage(
                                                    viewHolder.itemView.context.getString(
                                                        if (exam.isArchived) R.string.exam_unarchiving_error else R.string.exam_archiving_error,
                                                        e.message
                                                    )
                                                )
                                                .setPositiveButton(android.R.string.ok) { errorDialog, _ ->
                                                    errorDialog.dismiss()
                                                }
                                                .show()
                                            dialog.dismiss()
                                        }
                                    }
                                }
                            }
                            .show()
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Set the media view.
        adView.mediaView = adView.findViewById(R.id.ad_media)
        // Set other ad assets.
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.storeView = adView.findViewById(R.id.ad_store)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
        // The headline is guaranteed to be in every NativeAd.
        (adView.headlineView as TextView).text = nativeAd.headline
        // These assets are not guaranteed to be in every NativeAd, so check before
        // showing them.
        if (nativeAd.body == null) {
            adView.bodyView!!.visibility = View.INVISIBLE
        } else {
            adView.bodyView!!.visibility = View.VISIBLE
            (adView.bodyView as TextView?)!!.text = nativeAd.body
        }

        if (nativeAd.callToAction == null) {
            adView.callToActionView!!.visibility = View.INVISIBLE
        } else {
            adView.callToActionView!!.visibility = View.VISIBLE
            (adView.callToActionView as Button?)?.text = nativeAd.callToAction
        }

        if (nativeAd.icon == null) {
            adView.iconView!!.visibility = View.GONE
        } else {
            (adView.iconView as ImageView?)?.setImageDrawable(
                nativeAd.icon!!.drawable
            )
            adView.iconView!!.visibility = View.VISIBLE
        }

        if (nativeAd.price == null) {
            adView.priceView!!.visibility = View.INVISIBLE
        } else {
            adView.priceView!!.visibility = View.VISIBLE
            (adView.priceView as TextView?)!!.text = nativeAd.price
        }

        if (nativeAd.store == null) {
            adView.storeView!!.visibility = View.INVISIBLE
        } else {
            adView.storeView!!.visibility = View.VISIBLE
            (adView.storeView as TextView?)!!.text = nativeAd.store
        }

        if (nativeAd.starRating == null) {
            adView.starRatingView!!.visibility = View.INVISIBLE
        } else {
            (adView.starRatingView as RatingBar?)?.rating = nativeAd.starRating!!.toFloat()
            adView.starRatingView!!.visibility = View.VISIBLE
        }

        if (nativeAd.advertiser == null) {
            adView.advertiserView!!.visibility = View.INVISIBLE
        } else {
            (adView.advertiserView as TextView?)!!.text = nativeAd.advertiser
            adView.advertiserView!!.visibility = View.VISIBLE
        }

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.

        // This method tells the Google Mobile Ads SDK that you have finished populating your
        // native ad view with this native ad.
        adView.setNativeAd(nativeAd)

    }


    fun filterList(filterList: List<Exam>) {
        dataSet.clear()
        dataSet.addAll(filterList)
        notifyDataSetChanged()
    }
}