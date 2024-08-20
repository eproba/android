package com.czaplicki.eproba.ui.manage_worksheets

import android.os.Bundle
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
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.czaplicki.eproba.EprobaApplication
import com.czaplicki.eproba.R
import com.czaplicki.eproba.api.EprobaService
import com.czaplicki.eproba.db.Worksheet
import com.czaplicki.eproba.db.WorksheetDao
import com.czaplicki.eproba.db.Task
import com.czaplicki.eproba.db.User
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Response
import java.time.format.DateTimeFormatter
import java.util.UUID


class ManagedWorksheetAdapter(
    private val dataSet: MutableList<Worksheet>,
    private val users: List<User>,
    private val service: EprobaService,
    val worksheetDao: WorksheetDao
) :
    RecyclerView.Adapter<ManagedWorksheetAdapter.ViewHolder>() {


    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView
        val supervisor: TextView
        val lastUpdate: TextView
        val lastUpdateString: TextView
        val progressPercentage: TextView
        val adFrame: FrameLayout
        val taskList: RecyclerView
        val menuButton: ImageButton

        init {
            // Define click listener for the ViewHolder's View.
            name = view.findViewById(R.id.name)
            supervisor = view.findViewById(R.id.supervisor)
            lastUpdate = view.findViewById(R.id.last_update)
            lastUpdateString = view.findViewById(R.id.last_update_string)
            progressPercentage = view.findViewById(R.id.progressPercentage)
            adFrame = view.findViewById(R.id.ad_frame)
            taskList = view.findViewById<RecyclerView>(R.id.task_list)!!
            menuButton = view.findViewById(R.id.menu_button)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.worksheet_item, viewGroup, false)
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
        val worksheet = dataSet[position]

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        if (worksheet.id == UUID.fromString("00000000-0000-0000-0000-000000000000") && worksheet.name == "no_worksheets") {
            viewHolder.name.text = viewHolder.itemView.context.getString(R.string.no_worksheets)
            viewHolder.supervisor.visibility = View.GONE
            viewHolder.lastUpdate.visibility = View.GONE
            viewHolder.lastUpdateString.visibility = View.GONE
            viewHolder.progressPercentage.visibility = View.GONE
            viewHolder.taskList.visibility = View.GONE
            viewHolder.menuButton.visibility = View.GONE
            viewHolder.adFrame.visibility = View.GONE
            return
        } else if (worksheet.id == UUID.fromString("00000000-0000-0000-0000-000000000000") && worksheet.name == "ad") {
            viewHolder.name.text = viewHolder.itemView.context.getString(R.string.advertisement)
            viewHolder.supervisor.visibility = View.GONE
            viewHolder.lastUpdate.visibility = View.GONE
            viewHolder.lastUpdateString.visibility = View.GONE
            viewHolder.progressPercentage.visibility = View.GONE
            viewHolder.taskList.visibility = View.GONE
            viewHolder.menuButton.visibility = View.GONE
            viewHolder.adFrame.visibility = View.VISIBLE
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
            worksheet.name + " - " + users.find { it.id == worksheet.userId }?.nicknameWithRank
        if (worksheet.isArchived) {
            viewHolder.name.text = viewHolder.name.text.toString() + " (archiwum)"
        }
        if (worksheet.supervisor != null) {
            viewHolder.supervisor.visibility = View.VISIBLE
            viewHolder.supervisor.text =
                users.find { it.id == worksheet.supervisor }?.fullNameWithNickname
        } else {
            viewHolder.supervisor.visibility = View.GONE
        }
        viewHolder.lastUpdate.visibility = View.VISIBLE
        viewHolder.lastUpdateString.visibility = View.VISIBLE
        viewHolder.lastUpdate.text =
            DateTimeFormatter.ofPattern("dd.MM.yyyy").format(worksheet.lastUpdate)
        viewHolder.progressPercentage.visibility = View.VISIBLE
        viewHolder.progressPercentage.text =
            if (worksheet.tasks.size == 0) "" else viewHolder.itemView.context.getString(
                R.string.progress_percentage,
                worksheet.tasks.filter { it.status == Task.Status.APPROVED }.size * 100 / worksheet.tasks.size
            )
        viewHolder.taskList.visibility = View.VISIBLE
        viewHolder.taskList.adapter =
            ManagedTaskAdapter(worksheet, users, viewHolder.progressPercentage, service)
        viewHolder.adFrame.visibility = View.GONE
        viewHolder.menuButton.visibility = View.VISIBLE
        viewHolder.menuButton.setOnClickListener {
            val popup = PopupMenu(viewHolder.itemView.context, viewHolder.menuButton)
            popup.menuInflater.inflate(R.menu.worksheet_menu, popup.menu)
            popup.menu.findItem(R.id.archive_worksheet).title =
                if (worksheet.isArchived) viewHolder.itemView.context.getString(
                    R.string.unarchive_worksheet
                ) else viewHolder.itemView.context.getString(R.string.archive_worksheet)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.edit_worksheet -> {
                        val bundle = Bundle()
                        bundle.putString("initialData", Gson().toJson(dataSet[position]))
                        bundle.putString(
                            "initialDataNickname",
                            users.find { it.id == worksheet.userId }?.nickname
                        )
                        Navigation.findNavController(viewHolder.itemView)
                            .navigate(R.id.navigation_edit_worksheet, bundle)
                        true
                    }

                    R.id.delete_worksheet -> {
                        MaterialAlertDialogBuilder(
                            viewHolder.itemView.context,
                            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                        )
                            .setTitle(R.string.delete_worksheet_title)
                            .setMessage(
                                viewHolder.itemView.context.getString(
                                    R.string.delete_worksheet_confirmation,
                                    worksheet.name + " - " + users.find { it.id == worksheet.userId }?.nicknameWithRank
                                )
                            )
                            .setIcon(R.drawable.delete_forever_48px)
                            .setNeutralButton(R.string.cancel) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setPositiveButton(R.string.delete_worksheet) { dialog, _ ->
                                GlobalScope.launch {
                                    try {
                                        val response: Response<Void> = if (!worksheet.isArchived) {
                                            service.deleteWorksheet(worksheet.id)
                                        } else {
                                            service.deleteArchivedWorksheet(worksheet.id)
                                        }
                                        if (!response.isSuccessful) {
                                            throw Exception(response.errorBody()?.string()
                                                ?.let { it1 ->
                                                    JSONObject(it1).getString("detail")
                                                        ?: response.errorBody()?.string()
                                                })
                                        }
                                        worksheetDao.delete(worksheet)
                                        dataSet.removeAt(viewHolder.adapterPosition)
                                        EprobaApplication.instance.currentActivity?.runOnUiThread {
                                            notifyItemRemoved(viewHolder.adapterPosition)
                                            Snackbar.make(
                                                viewHolder.itemView.rootView,
                                                viewHolder.itemView.context.getString(
                                                    R.string.worksheet_deleted
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
                                                        R.string.worksheet_deletion_error,
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

                    R.id.archive_worksheet -> {
                        MaterialAlertDialogBuilder(
                            viewHolder.itemView.context,
                            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
                        )
                            .setTitle(if (worksheet.isArchived) R.string.unarchive_worksheet_title else R.string.archive_worksheet_title)
                            .setMessage(
                                viewHolder.itemView.context.getString(
                                    if (worksheet.isArchived) R.string.unarchive_worksheet_confirmation else R.string.archive_worksheet_confirmation,
                                    worksheet.name + " - " + users.find { it.id == worksheet.userId }?.nicknameWithRank
                                )
                            )
                            .setIcon(if (worksheet.isArchived) R.drawable.unarchive_48px else R.drawable.archive_48px)
                            .setNeutralButton(R.string.cancel) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setPositiveButton(if (worksheet.isArchived) R.string.unarchive_worksheet else R.string.archive_worksheet) { dialog, _ ->
                                GlobalScope.launch {
                                    try {
                                        worksheetDao.insert(
                                            if (!worksheet.isArchived) {
                                                service.updateWorksheet(
                                                    worksheet.id,
                                                    "{\"is_archived\": true}".toRequestBody(
                                                        "application/json".toMediaTypeOrNull()
                                                    )
                                                )
                                            } else {
                                                service.updateWorksheetInArchive(
                                                    worksheet.id,
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
                                                    if (worksheet.isArchived) R.string.worksheet_unarchived else R.string.worksheet_archived
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
                                                        if (worksheet.isArchived) R.string.worksheet_unarchiving_error else R.string.worksheet_archiving_error,
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


    fun filterList(filterList: List<Worksheet>) {
        dataSet.clear()
        dataSet.addAll(filterList)
        notifyDataSetChanged()
    }
}