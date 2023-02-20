package com.senshuu.setu

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.senshuu.setu.databinding.ActivityMainBinding
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val gson = Gson()
    private val okHttpClient = OkHttpClient()

    private val executorService: ExecutorService = Executors.newFixedThreadPool(5)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.sendRequestBtn.setOnClickListener {
            sendRequestWithOkHttp()
        }
    }

    private fun sendRequestWithOkHttp() {
        val request = Request.Builder()
            .url("https://api.lolicon.app/setu/v2?num=10&r18=0")
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (responseData != null) {
                    val picList = parseJSONWithGson(responseData)
                    showResponse(picList)
                }
            }
        })
    }

    private fun parseJSONWithGson(jsonData: String): List<String> {
        val picList = mutableListOf<String>()
        try {
            val response = gson.fromJson(jsonData, LoliconResponse::class.java)
            for (pic in response.data) {
                picList.add(pic.urls.original)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return picList
    }

    private fun showResponse(picList: List<String>) {
        runOnUiThread {
            binding.imageContainer.removeAllViews()

            for (picUrl in picList) {
                executorService.execute {
                    try {
                        val url = URL(picUrl)
                        val connection = url.openConnection()
                        connection.doInput = true
                        connection.connect()
                        val input = connection.getInputStream()
                        val bitmap = BitmapFactory.decodeStream(input)

                        runOnUiThread {
                            val imageView = CustomImageView(this@MainActivity)
                            imageView.setImageBitmap(bitmap)
                            binding.imageContainer.addView(imageView)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    data class LoliconResponse(
        val error: String,
        val data: List<LoliconPic>
    )

    data class LoliconPic(
        val pid: Long,
        val p: Int,
        val uid: Long,
        val title: String,
        val author: String,
        val r18: Boolean,
        val width: Int,
        val height: Int,
        val tags: List<String>,
        val ext: String,
        val aiType: Int,
        val uploadDate: Long,
        val urls: Urls
    )

    data class Urls(
        val original: String
    )

    override fun onDestroy() {
        super.onDestroy()
        executorService.shutdownNow()
    }
}
