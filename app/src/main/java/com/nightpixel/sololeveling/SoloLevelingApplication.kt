package com.nightpixel.sololeveling

import android.app.Application
import com.nightpixel.sololeveling.data.AppDatabase

class SoloLevelingApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
