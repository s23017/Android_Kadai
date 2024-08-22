package jp.ac.it_college.std.s23017.android_kadai

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import jp.ac.it_college.std.s23017.android_kadai.databinding.ActivityMainBinding
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private val reloadInterval: Long = 5000 // 5秒

    companion object {
        private const val DEBUG_TAG = "AsyncSample"
        private const val STAND_INFO_URL_TEMPLATE = "https://stand-by-me.herokuapp.com/api/v1/stands/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnReload.setOnClickListener {
            fetchStandData()
        }

        // 初回データ取得
        fetchStandData()

        // 5秒ごとにリロード
        handler.postDelayed(object : Runnable {
            override fun run() {
                fetchStandData()
                handler.postDelayed(this, reloadInterval)
            }
        }, reloadInterval)
    }

    private fun fetchStandData() {
        val url = getStandInfoUrl()
        val backgroundReceiver = StandDataBackgroundReceiver(url)
        val executeService = Executors.newSingleThreadExecutor()
        val future = executeService.submit(backgroundReceiver)
        val result = future.get()
        showStandData(result)
    }

    private fun getStandInfoUrl(): String {
        val randomNumber = (1..100).random() // 1から100のランダムな整数を生成
        return "$STAND_INFO_URL_TEMPLATE$randomNumber"
    }

    private fun showStandData(result: String) {
        try {
            val root = JSONObject(result)
            val standName = root.getString("japaneseName")

            binding.tvStandName.text = "スタンド名: $standName"

        } catch (e: Exception) {
            Log.e(DEBUG_TAG, "データの解析に失敗しました", e)
        }
    }

    private class StandDataBackgroundReceiver(private val urlString: String) : Callable<String> {
        override fun call(): String {
            val url = URL(urlString)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                requestMethod = "GET"
            }
            return try {
                conn.connect()
                val result = conn.inputStream.reader().readText()
                result
            } catch (ex: SocketTimeoutException) {
                Log.w(DEBUG_TAG, "通信タイムアウト", ex)
                ""
            } catch (ex: Exception) {
                Log.e(DEBUG_TAG, "データ取得エラー", ex)
                ""
            } finally {
                conn.disconnect()
            }
        }
    }
}
