package com.emogoth.android.phone.mimi.util

import android.content.Context
import android.os.Handler
import android.util.Log
import com.emogoth.android.phone.mimi.BuildConfig
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableSubscriber
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.*
import okio.buffer
import okio.sink
import org.reactivestreams.Subscription
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap


/**
 * A queued download manager. Once started, it downloads each file sequentially unless a listener is attached.
 * Once a listener is attached, it is given priority and a download is started regardless the number of current downloads
 *
 *
 * @param concurrentDownloads the number of downloads to start
 * @property client the okhttp client to use for the downloads
 * @property downloadItems the list of items to download
 * @property app (optional) app context to determine if wifi is connected. If the context is non-null, wifi connectivity is used
 * @constructor Creates an empty group.
 */
class DownloadManager(private val client: OkHttpClient, private val downloadItems: List<DownloadItem>, concurrentDownloads: Int, private val app: Context? = null) {
    companion object {
        val TAG = DownloadManager::class.java.simpleName
        const val BUFFER_SIZE = 1024L
    }

    private val items: ArrayList<DownloadItem> = ArrayList(downloadItems)
    private val publisherMap = ConcurrentHashMap<Long, Flowable<Int>>()
    private val subscriberMap = ConcurrentHashMap<Long, Subscription>()
    private val callbackMap = ConcurrentHashMap<Long, DownloadListener>()

//    private val client: OkHttpClient by lazy {
//        val cookiePersistor = SharedPrefsCookiePersistor(MimiApplication.getInstance())
//        val jar = PersistentCookieJar(SetCookieCache(), cookiePersistor)
//
//        val builder = OkHttpClient.Builder()
////                .cache(cache)
//                .cookieJar(jar)
//                .followRedirects(true)
//                .followSslRedirects(true)
//                .connectTimeout(90, TimeUnit.SECONDS)
//                .readTimeout(90, TimeUnit.SECONDS)
//                .writeTimeout(90, TimeUnit.SECONDS)
//                .retryOnConnectionFailure(true)
//
//        builder.addNetworkInterceptor { chain: Interceptor.Chain ->
//            val originalRequest = chain.request()
//            val modifiedRequest = originalRequest.newBuilder()
//                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.117 Safari/537.36")
//                    .build()
//            chain.proceed(modifiedRequest)
//        }
//
//        if (BuildConfig.DEBUG) {
//            builder.addNetworkInterceptor(loggingInterceptor())
//            builder.addNetworkInterceptor(StethoInterceptor())
//        }
//
//        builder.build()
//    }

    private var maxConcurrent: Int = concurrentDownloads
        get() {
            if (app != null && !MimiPrefs.preloadEnabled(app)) {
                return 0
            }

            return field
        }
        set(value) {
            field = if (value > 0) {
                value
            } else {
                0
            }
        }

    init {

    }

    fun start() {
        val size = if (maxConcurrent > downloadItems.size) {
            downloadItems.size
        } else {
            maxConcurrent
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Running start() with a size of $size")
        }

        for (i in 0 until size) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Starting download for ${downloadItems[i].url}")
            }
            createSubscriber(downloadItems[i])
        }
    }

    fun cancel(id: Long) {
        subscriberMap[id]?.cancel()
        subscriberMap.remove(id)
    }

    private fun runNext() {
        if (maxConcurrent == 0) {
            return
        }

        val handler = Handler()
        handler.postDelayed({
            synchronized(items) {
                if (items.size > 0) {
                    createSubscriber(items[0])
                }
            }
        }, 2000)

    }

    fun addListener(id: Long, listener: DownloadListener): DownloadItem {
        synchronized(callbackMap) {
            var item: DownloadItem? = null
            for (downloadItem in downloadItems) {
                if (downloadItem.id == id) {
                    item = downloadItem
                    break
                }
            }

            if (item == null) {
                return DownloadItem.empty()
            }

            callbackMap[item.id] = listener

            if (publisherMap[item.id] == null) {
                createSubscriber(item)
            }

            return item
        }
    }

    fun removeListener(id: Long) {
        subscriberMap[id]?.cancel()
        callbackMap.remove(id)
    }

    fun reset(item: DownloadItem) {
        publisherMap.remove(item.id)
    }

    fun start(item: DownloadItem) {
        if (callbackMap[item.id] != null && subscriberMap[item.id] != null) {
            subscriberMap[item.id]?.cancel()
            subscriberMap.remove(item.id)

            createSubscriber(item)
        }
    }

    private fun createSubscriber(item: DownloadItem) {
        synchronized(publisherMap) {

            for (i in 0 until items.size) {
                if (items[i].id == item.id) {
                    items.removeAt(i)
                    break
                }
            }

            if (publisherMap[item.id] != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Download already started; skipping")
                }
                return
            }
            publisherMap[item.id] = downloadToFile(client, item.url, item.file)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())

            publisherMap[item.id]?.subscribe(object : FlowableSubscriber<Int> {
                override fun onComplete() {
                    callbackMap[item.id]?.onComplete()

                    publisherMap.remove(item.id)
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Finished download for ${item.url}")
                    }
                    runNext()
                }

                override fun onSubscribe(s: Subscription) {
                    subscriberMap[item.id] = s
                    s.request(Long.MAX_VALUE)
                }

                override fun onNext(t: Int?) {
                    callbackMap[item.id]?.onBytesReceived(t ?: 0)
                }

                override fun onError(t: Throwable?) {
                    if (item.file.exists()) {
                        item.file.delete()
                    }
                    publisherMap.remove(item.id)
                    callbackMap[item.id]?.onError(t
                            ?: Exception("Unknown (exception object is null)"))
                }

            })
        }
    }

    fun clear() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Removing listeners from subscriber map")
        }
        for (entry in subscriberMap) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Cancelling ${entry.key}")
            }
            entry.value.cancel()
        }
    }

    fun destroy() {
        clear()
        maxConcurrent = 0
        items.clear()
    }
}

fun downloadToFile(client: OkHttpClient, url: String, file: File?): Flowable<Int> {
    if (BuildConfig.DEBUG) {
        Log.d(DownloadManager.TAG, "Downloading file: ${file?.absolutePath}")
    }
    return Flowable.create({ emitter ->
        if (file != null && file.exists()) {
            if (BuildConfig.DEBUG) {
                Log.d(DownloadManager.TAG, "File exists: ${file.absolutePath}; manually calling onComplete()")
            }
            emitter.onComplete()
            return@create
        }

        if (BuildConfig.DEBUG) {
            Log.d(DownloadManager.TAG, "Starting file download: ${file?.absolutePath}")
        }

        val req = Request.Builder().url(url).get().build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                emitter.tryOnError(e)
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body
                    if (body == null) {
                        emitter.tryOnError(IllegalStateException("Response from $url returned an empty body"))
                        return
                    }

                    val contentLength = body.contentLength()
                    val source = body.source()
                    val sink = if (file != null) {
                        file.createNewFile()
                        file.sink().buffer()
                    } else null
                    val sinkBuffer = sink?.buffer

                    if (sinkBuffer == null) {
                        emitter.tryOnError(IllegalStateException("Could not write to file; File object is null"))
                        return
                    }

                    var totalBytes = 0L
                    while (!emitter.isCancelled) {
                        val count = source.read(sinkBuffer, DownloadManager.BUFFER_SIZE)
                        if (count == -1L) break
                        sink.emit()

                        totalBytes += count
                        val progress = ((totalBytes.toFloat() / contentLength) * 100).toInt()
                        emitter.onNext(progress)
                    }

                    sink.flush()
                    sink.close()

                    source.close()

                    if (emitter.isCancelled) {
                        file?.delete()
                    }

                    if (totalBytes <= 0L) {
                        val fileException = if (file == null) Exception("Could not write to null file") else NoSuchFileException(file, null, "Wrote a 0-byte file")
                        Log.e(DownloadManager.TAG, "Error writing file", fileException)

                        emitter.tryOnError(fileException)
                    } else {
                        emitter.onComplete()
                    }
                } catch (e: Exception) {
                    emitter.tryOnError(e)
                } finally {
                    response.close()
                }
            }
        })
    }, BackpressureStrategy.BUFFER)
}

data class DownloadItem(val id: Long, val url: String, val thumbUrl: String, val width: Int, val height: Int, val file: File, val saveFileName: String) {
    companion object {
        fun empty(): DownloadItem {
            return DownloadItem(-1, "", "", 0, 0, File(""), "")
        }
    }
}

interface DownloadListener {
    fun onBytesReceived(progress: Int)
    fun onComplete()
    fun onError(t: Throwable)
}
