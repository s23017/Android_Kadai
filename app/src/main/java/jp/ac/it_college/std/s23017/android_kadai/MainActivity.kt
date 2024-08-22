package jp.ac.it_college.std.s23017.android_kadai

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import jp.ac.it_college.std.s23017.android_kadai.databinding.ActivityMainBinding
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val DEBUG_TAG = "AsyncSample"
        private const val STAND_INFO_URL = "https://stand-by-me.herokuapp.com/api/v1/stands/1"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fetchStandData()
    }

    private fun fetchStandData() {
        val backgroundReceiver = StandDataBackgroundReceiver(STAND_INFO_URL)
        val executeService = Executors.newSingleThreadExecutor()
        val future = executeService.submit(backgroundReceiver)
        val result = future.get()
        showStandData(result)
    }

    private fun showStandData(result: String) {
        try {
            // ルートのオブジェクトを生成
            val root = JSONObject(result)
            // スタンド名を取得
            val standName = root.getString("name")

            // 画面にデータを表示
            binding.tvStandName.text = "スタンド名: $standName"
        } catch (e: Exception) {
            Log.e(DEBUG_TAG, "データの解析に失敗しました", e)
        }
    }

    private class StandDataBackgroundReceiver(private val urlString: String) : Callable<String> {
        override fun call(): String {
            // URL オブジェクトの生成
            val url = URL(urlString)
            // HttpURLConnection オブジェクトを取得
            val conn = (url.openConnection() as HttpURLConnection).apply {
                // 接続の設定
                connectTimeout = 10000
                readTimeout = 10000
                requestMethod = "GET"
            }
            return try {
                // 接続
                conn.connect()
                // レスポンスデータを取得
                val result = conn.inputStream.reader().readText()
                // try ブロックの結果
                result
            } catch (ex: SocketTimeoutException) {
                Log.w(DEBUG_TAG, "通信タイムアウト", ex)
                // catch ブロックの結果 (空文字列)
                ""
            } finally {
                conn.disconnect()
            }
        }
    }
}
