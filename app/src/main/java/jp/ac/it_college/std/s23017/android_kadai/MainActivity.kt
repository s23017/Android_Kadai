package jp.ac.it_college.std.s23017.android_kadai

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import jp.ac.it_college.std.s23017.android_kadai.databinding.ActivityMainBinding
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var correctAnswer: String? = null

    companion object {
        private const val DEBUG_TAG = "AsyncSample"
        private const val STAND_INFO_URL_TEMPLATE = "https://stand-by-me.herokuapp.com/api/v1/stands/"
        private const val CHARACTER_INFO_URL_TEMPLATE = "https://stand-by-me.herokuapp.com/api/v1/characters/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnReload.setOnClickListener {
            fetchStandData()
        }

        binding.btnChoice1.setOnClickListener { checkAnswer(binding.btnChoice1.text.toString()) }
        binding.btnChoice2.setOnClickListener { checkAnswer(binding.btnChoice2.text.toString()) }
        binding.btnChoice3.setOnClickListener { checkAnswer(binding.btnChoice3.text.toString()) }
        binding.btnChoice4.setOnClickListener { checkAnswer(binding.btnChoice4.text.toString()) }

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
        val randomNumber = (1..153).random()
        return "$STAND_INFO_URL_TEMPLATE$randomNumber"
    }

    private fun showStandData(result: String) {
        try {
            val root = JSONObject(result)
            val standName = root.getString("japaneseName")
            val standUserId = root.getString("standUser")

            binding.tvStandName.text = "スタンド名: $standName"

            // スタンド使いの情報を取得
            fetchStandUserData(standUserId)

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
            correctAnswer = userName

            // スタンド使いの情報を表示
            binding.tvStandUserName.text = "本体: $userName"

            // ランダムな選択肢を設定
            setRandomChoices(userName)

        } catch (e: Exception) {
            Log.e(DEBUG_TAG, "データの解析に失敗しました", e)
        }
    }

    private fun setRandomChoices(correctAnswer: String) {
        val fakeAnswers = getFakeAnswers(correctAnswer)
        val allChoices = (fakeAnswers + correctAnswer).shuffled()

        binding.btnChoice1.text = allChoices[0]
        binding.btnChoice2.text = allChoices[1]
        binding.btnChoice3.text = allChoices[2]
        binding.btnChoice4.text = allChoices[3]
    }

    private fun getFakeAnswers(correctAnswer: String): List<String> {
        // ダミーデータとして一旦固定の選択肢を設定（実際はAPIから取得したデータを使う）
        val fakeUsers = listOf("仗助", "承太郎", "ジョセフ", "花京院")
        return fakeUsers.filter { it != correctAnswer }.shuffled().take(3)
    }

    private fun checkAnswer(selectedAnswer: String) {
        if (selectedAnswer == correctAnswer) {
            Toast.makeText(this, "正解！", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "不正解！ 正しい答えは $correctAnswer です", Toast.LENGTH_LONG).show()
        }
        fetchStandData() // 次の問題へ進む
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
            } catch (ex: Exception) {
                Log.e(DEBUG_TAG, "データ取得エラー", ex)
                ""
            } finally {
                conn.disconnect()
            }
        }
    }

    private class StandUserDataBackgroundReceiver(private val urlString: String) : Callable<String> {
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
            } catch (ex: Exception) {
                Log.e(DEBUG_TAG, "データ取得エラー", ex)
                ""
            } finally {
                conn.disconnect()
            }
        }
    }
}
