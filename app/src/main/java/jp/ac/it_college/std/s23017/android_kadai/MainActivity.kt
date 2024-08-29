package jp.ac.it_college.std.s23017.android_kadai

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
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
    private var correctStandName: String? = null

    companion object {
        private const val DEBUG_TAG = "AsyncSample"
        private const val STAND_INFO_URL_TEMPLATE = "https://stand-by-me.herokuapp.com/api/v1/stands/"
        private const val CHARACTER_INFO_URL_TEMPLATE = "https://stand-by-me.herokuapp.com/api/v1/characters/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ボタンのクリックリスナー
        binding.btnChoice1.setOnClickListener { checkAnswer(binding.btnChoice1.text.toString()) }
        binding.btnChoice2.setOnClickListener { checkAnswer(binding.btnChoice2.text.toString()) }
        binding.btnChoice3.setOnClickListener { checkAnswer(binding.btnChoice3.text.toString()) }
        binding.btnChoice4.setOnClickListener { checkAnswer(binding.btnChoice4.text.toString()) }

        binding.btnReload.setOnClickListener {
            fetchStandData()
            binding.tvResult.text = ""
        }

        // 初回データ取得
        fetchStandData()
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
            val standUserId = root.getString("standUser")
            correctStandName = standName

            // 本体名を取得
            fetchStandUserData(standUserId)

            // ランダムな選択肢を生成
            generateRandomChoices(standName)

        } catch (e: Exception) {
            Log.e(DEBUG_TAG, "データの解析に失敗しました", e)
        }
    }

    private fun fetchStandUserData(userId: String) {
        val url = "$CHARACTER_INFO_URL_TEMPLATE$userId"
        val backgroundReceiver = StandUserDataBackgroundReceiver(url)
        val executeService = Executors.newSingleThreadExecutor()
        val future = executeService.submit(backgroundReceiver)
        val result = future.get()
        showStandUserData(result)
    }

    private fun showStandUserData(result: String) {
        try {
            val root = JSONObject(result)
            val userName = root.getString("japaneseName")

            // スタンド使いの情報を表示
            binding.tvStandUserName.text = "「 $userName 」"

        } catch (e: Exception) {
            Log.e(DEBUG_TAG, "データの解析に失敗しました", e)
        }
    }

    private fun generateRandomChoices(correctAnswer: String) {
        val options = mutableListOf(correctAnswer)

        while (options.size < 4) {
            val randomStand = fetchRandomStandName()
            if (!options.contains(randomStand)) {
                options.add(randomStand)
            }
        }
        options.shuffle()

        // ボタンに選択肢を表示
        binding.btnChoice1.text = options[0]
        binding.btnChoice2.text = options[1]
        binding.btnChoice3.text = options[2]
        binding.btnChoice4.text = options[3]
    }

    private fun fetchRandomStandName(): String {
        val url = getStandInfoUrl()
        val backgroundReceiver = StandDataBackgroundReceiver(url)
        val executeService = Executors.newSingleThreadExecutor()
        val future = executeService.submit(backgroundReceiver)
        val result = future.get()
        return JSONObject(result).getString("japaneseName")
    }

    private fun checkAnswer(selectedAnswer: String) {
        if (selectedAnswer == correctStandName) {
            binding.tvResult.text = "グッド！"
        } else {
            binding.tvResult.text = "不正解！ 答えは: $correctStandName"
        }
    }

    private inner class StandDataBackgroundReceiver(private val url: String) : Callable<String> {
        override fun call(): String {
            var result = ""
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.requestMethod = "GET"
                connection.connect()

                val stream = connection.inputStream
                result = stream.bufferedReader().use { it.readText() }

            } catch (e: SocketTimeoutException) {
                Log.w(DEBUG_TAG, "通信タイムアウト", e)
            } catch (e: Exception) {
                Log.e(DEBUG_TAG, "通信失敗", e)
            } finally {
                connection.disconnect()
            }
            return result
        }
    }

    private inner class StandUserDataBackgroundReceiver(private val url: String) : Callable<String> {
        override fun call(): String {
            var result = ""
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.requestMethod = "GET"
                connection.connect()

                val stream = connection.inputStream
                result = stream.bufferedReader().use { it.readText() }

            } catch (e: SocketTimeoutException) {
                Log.w(DEBUG_TAG, "通信タイムアウト", e)
            } catch (e: Exception) {
                Log.e(DEBUG_TAG, "通信失敗", e)
            } finally {
                connection.disconnect()
            }
            return result
        }
    }
}
