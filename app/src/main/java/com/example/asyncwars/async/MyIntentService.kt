package com.example.asyncwars.async

import android.app.IntentService
import android.content.Intent
import com.example.asyncwars.utils.BroadcasterUtil
import com.example.asyncwars.utils.DownloaderUtil


// Required constructor with a name for the service
class MyIntentService : IntentService("MyIntentService") {

    override fun onHandleIntent(intent: Intent?) {
        // Download Image
        val bmp = DownloaderUtil.downloadImage()

        // Send local broadcast with the bitmap as payload
        BroadcasterUtil.sendBitmap(applicationContext, bmp)
    }
}