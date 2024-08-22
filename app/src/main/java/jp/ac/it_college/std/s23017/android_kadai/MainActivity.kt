package jp.ac.it_college.std.s23017.android_kadai

import android.os.Bundle
import android.util.Log

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import jp.ac.it_college.std.s23017.android_kadai.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val DEBUG_TAG = "AsyncSample"
        private const val STAND_INFO_URL_TEMPLATE = "https://stand-by-me.herokuapp.com/api/v1/stands/"
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

        // 初期データの取得
        fetchStandData()

        // リロードボタンのクリックリスナーを設定
        binding.btnReload.setOnClickListener {
            fetchStandData()
        }
    }

    private fun fetchStandData() {
        CoroutineScope(Dispatchers.IO).launch {
            val url = getStandInfoUrl()
            val result = fetchStandDataFromUrl(url)
            withContext(Dispatchers.Main) {
                showStandData(result)
            }
        }
    }

    private fun getStandInfoUrl(): String {
        val randomNumber = (1..33).random()
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

    private fun fetchStandDataFromUrl(urlString: String): String {
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 10000
            requestMethod = "GET"
        }
        return try {
            conn.connect()
            conn.inputStream.reader().readText()
        } catch (ex: Exception) {
            Log.e(DEBUG_TAG, "データ取得エラー", ex)
            ""
        } finally {
            conn.disconnect()
        }
    }
}
