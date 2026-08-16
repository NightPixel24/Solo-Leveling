package com.nightpixel.sololeveling

import android.app.Application
import com.nightpixel.sololeveling.data.AppDatabase
import com.nightpixel.sololeveling.data.backup.BackupManager

class SoloLevelingApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val backupManager: BackupManager by lazy { BackupManager(database) }
}
