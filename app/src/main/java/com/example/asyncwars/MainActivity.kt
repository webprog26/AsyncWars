package com.example.asyncwars

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.view.animation.AnimationUtils
import com.example.asyncwars.async.GetImageAsyncTask
import com.example.asyncwars.async.MyIntentService
import com.example.asyncwars.utils.*
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.Executors


/**
 * Main Screen
 */
class MainActivity : AppCompatActivity() {

    companion object {
        const val FILTER_ACTION_KEY = "MY_ACTION"
    }

    enum class MethodToDownloadImage {
        Thread, AsyncTask, IntentService, Handler, HandlerThread, Executor, RxJava, Coroutine
    }

    private val imageDownloadListener = object : ImageDownloadListener {
        override fun onSuccess(bitmap: Bitmap?) {
            // Update UI with downloaded bitmap
            imageView?.setImageBitmap(bitmap)
        }
    }

    // Create an instance of MyBroadcastReceiver
    private val myReceiver = MyBroadcastReceiver(imageDownloadListener)
    private val myRunnable = MyRunnable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup a rotating spinner in the UI
        val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_indefinitely)
        contentLoadingProgressBar.startAnimation(rotateAnimation)

        //region --------- Modify below to setup app to use a specific type of async method --------- //
        val doProcessingOnUiThread = true
        val methodToUse = MethodToDownloadImage.Thread
        //endregion

        // Setup the UI text
        if (doProcessingOnUiThread) {
            // Set the type of method used in UI
            textViewMethodUsed.text = "Calculating on UI thread: Fibonacci Number"
        } else {
            when (methodToUse) {
                MethodToDownloadImage.Thread -> setMethodBeingUsedInUi("Thread")
                MethodToDownloadImage.AsyncTask -> setMethodBeingUsedInUi("AsyncTask")
                MethodToDownloadImage.IntentService -> setMethodBeingUsedInUi("IntentService")
                MethodToDownloadImage.Handler -> setMethodBeingUsedInUi("Handler")
                MethodToDownloadImage.HandlerThread -> setMethodBeingUsedInUi("HandlerThread")
                MethodToDownloadImage.Executor -> setMethodBeingUsedInUi("Executor")
                MethodToDownloadImage.RxJava -> setMethodBeingUsedInUi("RxJava")
                MethodToDownloadImage.Coroutine -> setMethodBeingUsedInUi("Coroutine")
            }
        }

        buttonDownloadBitmap.setOnClickListener {
            // Reset the imageview
            imageView?.setImageBitmap(null)

            if (doProcessingOnUiThread) {
                // Processing on UI thread, blocking UI
                runUiBlockingProcessing()
            } else {
                when (methodToUse) {
                    MethodToDownloadImage.Thread -> getImageUsingThread()
                    MethodToDownloadImage.AsyncTask -> getImageUsingAsyncTask()
                    MethodToDownloadImage.IntentService -> getImageUsingIntentService()
                    MethodToDownloadImage.Handler -> getImageUsingHandler()
                    MethodToDownloadImage.HandlerThread -> getImageUsingHandlerThread()
                    MethodToDownloadImage.Executor -> getImageUsingExecutors()
                    MethodToDownloadImage.RxJava -> getImageUsingRx()
                    MethodToDownloadImage.Coroutine -> getImageUsingCoroutines()
                }
            }
        }
    }

    // ----------- Async Methods -----------//

    fun runUiBlockingProcessing() {
        // Processing
        showToast("Result: ${fibonacci(40)}")
    }

    fun getImageUsingThread() {
        // Download image
        val thread = Thread(myRunnable)
        thread.start()
    }

    fun getImageUsingIntentService() {
        // Download image
        val intent = Intent(this@MainActivity, MyIntentService::class.java)
        startService(intent)
    }

    fun getImageUsingExecutors() {
        // Download image
        val executor = Executors.newFixedThreadPool(4)
        executor.submit(myRunnable)
    }

    fun getImageUsingAsyncTask() {
        // Download image
        val myAsyncTask = GetImageAsyncTask(imageDownloadListener)
        myAsyncTask.execute()
    }

    fun getImageUsingHandler() {
        // Create a Handler using the main Looper
        val uiHandler = Handler(Looper.getMainLooper())

        // Create a new thread
        Thread {
            // Download image
            val bmp = DownloaderUtil.downloadImage()

            // Using the uiHandler update the UI
            uiHandler.post {
                imageView?.setImageBitmap(bmp)
            }
        }.start()
    }

    var handlerThread: HandlerThread? = null
    fun getImageUsingHandlerThread() {
        // Download image
        // Create a HandlerThread
        handlerThread = HandlerThread("MyHandlerThread")

        handlerThread?.let {
            // Start the HandlerThread
            it.start()
            // Get the Looper
            val looper = it.looper
            // Create a Handler using the obtained Looper
            val handler = Handler(looper)
            // Execute the Handler
            handler.post {
                // Download Image
                val bmp = DownloaderUtil.downloadImage()

                // Send local broadcast with the bitmap as payload
                BroadcasterUtil.sendBitmap(applicationContext, bmp)
            }
        }
    }

    var single: Disposable? = null
    fun getImageUsingRx() {
        // Download image
        single = Single.create<Bitmap> { emitter ->
            DownloaderUtil.downloadImage()?.let { bmp ->
                emitter.onSuccess(bmp)
            }
        }.observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe { bmp ->
                // Update UI with downloaded bitmap
                imageView?.setImageBitmap(bmp)
            }
    }

    fun getImageUsingCoroutines() {
        // TODO: add implementation here
    }

    // Implementing the Runnable interface to implement threads.
    inner class MyRunnable : Runnable {

        override fun run() {
            // Download Image
            val bmp = DownloaderUtil.downloadImage()

            // Update UI on the UI/Main Thread with downloaded bitmap
            runOnUiThread {
                imageView?.setImageBitmap(bmp)
            }
        }
    }

    // ----------- Helper Methods -----------//
    fun fibonacci(number: Int): Long {
        return if (number == 1 || number == 2) {
            1
        } else fibonacci(number - 1) + fibonacci(number - 2)
    }

    fun setMethodBeingUsedInUi(method: String) {
        // Set the type of method used in UI
        textViewMethodUsed.text = "Download image using:$method"
    }

    // ----------- Lifecycle Methods -----------//
    override fun onStart() {
        super.onStart()
        BroadcasterUtil.registerReceiver(this, myReceiver)
    }

    override fun onStop() {
        super.onStop()
        BroadcasterUtil.unregisterReceiver(this, myReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Quit and cleanup any instance of dangling HandlerThread
        handlerThread?.quit()

        // Cleanup disposable if it was created i.e. not null
        single?.dispose()
    }
}