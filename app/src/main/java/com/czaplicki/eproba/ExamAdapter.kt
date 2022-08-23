package com.czaplicki.eproba

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.material.divider.MaterialDividerItemDecoration


class ExamAdapter(context: Context, private val dataSet: List<Exam>) :
    RecyclerView.Adapter<ExamAdapter.ViewHolder>() {

    private var taskList: RecyclerView? = null
    var displayAds: Boolean =
        PreferenceManager.getDefaultSharedPreferences(context).getBoolean("ads", true)

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView
        val supervisor: TextView
        val progressPercentage: TextView
        val adFrame: FrameLayout

        init {
            // Define click listener for the ViewHolder's View.
            name = view.findViewById(R.id.name)
            supervisor = view.findViewById(R.id.supervisor)
            progressPercentage = view.findViewById(R.id.progressPercentage)
            adFrame = view.findViewById(R.id.ad_frame)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.exam_item, viewGroup, false)


        taskList = view.findViewById(R.id.task_list) as RecyclerView
        taskList!!.layoutManager = LinearLayoutManager(viewGroup.context)
        val mDividerItemDecoration = MaterialDividerItemDecoration(
            taskList!!.context,
            LinearLayoutManager(viewGroup.context).orientation
        )
        taskList!!.addItemDecoration(mDividerItemDecoration)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        if (displayAds && position == itemCount - 1) {
            viewHolder.name.text = viewHolder.itemView.context.getString(R.string.advertisement)
            viewHolder.supervisor.visibility = View.GONE
            viewHolder.progressPercentage.visibility = View.GONE
            taskList?.visibility = View.GONE
            viewHolder.adFrame.visibility = View.VISIBLE
            val builder = AdLoader.Builder(
                viewHolder.itemView.context,
                "ca-app-pub-3940256099942544/2247696110" // Test ad unit id
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
        viewHolder.name.text = dataSet[position].name
        if (dataSet[position].supervisor != null) {
            viewHolder.supervisor.visibility = View.VISIBLE
            viewHolder.supervisor.text = dataSet[position].supervisor.toString()
        }
        viewHolder.progressPercentage.text =
            if (dataSet[position].tasks.size == 0) "" else viewHolder.itemView.context.getString(
                R.string.progress_percentage,
                dataSet[position].tasks.filter { it.status == Task.Status.APPROVED }.size * 100 / dataSet[position].tasks.size
            )
        taskList?.adapter = TaskAdapter(dataSet[position].tasks)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = if (displayAds) dataSet.size + 1 else dataSet.size

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
}