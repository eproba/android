package com.czaplicki.eproba

import android.app.Application
import com.google.android.material.color.DynamicColors

class EprobaApplication: Application() {
    override fun onCreate() {
        // Apply dynamic color
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}