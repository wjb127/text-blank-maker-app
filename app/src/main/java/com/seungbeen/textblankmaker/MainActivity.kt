package com.seungbeen.textblankmaker

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.seungbeen.textblankmaker.databinding.ActivityMainBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val client = OkHttpClient()
    private val API_URL = "https://api.openai.com/v1/chat/completions"
    private val API_KEY = BuildConfig.OPENAI_API_KEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.generateButton.setOnClickListener {
            val text = binding.inputText.text.toString()
            if (text.isNotEmpty()) {
                binding.progressBar.visibility = View.VISIBLE
                binding.generateButton.isEnabled = false
                makeApiCall(text) { result ->
                    runOnUiThread {
                        binding.outputText.setText(result)
                        binding.progressBar.visibility = View.GONE
                        binding.generateButton.isEnabled = true
                        binding.copyButton.visibility = View.VISIBLE
                    }
                }
            } else {
                Snackbar.make(binding.root, "텍스트를 입력해주세요", Snackbar.LENGTH_SHORT).show()
            }
        }

        binding.copyButton.setOnClickListener {
            val outputText = binding.outputText.text.toString()
            if (outputText.isNotEmpty()) {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("생성된 텍스트", outputText)
                clipboard.setPrimaryClip(clip)
                Snackbar.make(binding.root, "클립보드에 복사되었습니다", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun makeApiCall(text: String, callback: (String) -> Unit) {
        val prompt = """
            다음 텍스트는 학습 목적으로 사용될 것입니다. 
            텍스트의 단어들 중에서 약 10개 단어당 1개꼴로 랜덤하게 선택하여 빈칸(___)으로 만들어주세요.
            텍스트의 핵심 내용이 있는 경우 핵심 내용 위주로 빈칸을 만들어주세요.
            빈칸 문제 : $text
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("model", "gpt-3.5-turbo")
            put("messages", JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put("content", prompt)
            ))
            put("temperature", 0.7)
        }

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("API_ERROR", "API 호출 실패", e)
                callback("오류가 발생했습니다: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("API_ERROR", "API 응답 실패: ${response.code} - ${response.body?.string()}")
                        callback("API 호출 실패: ${response.code}")
                        return
                    }

                    val responseBody = response.body?.string()
                    val jsonResponse = JSONObject(responseBody)
                    val generatedText = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content") ?: ""
                    
                    callback(generatedText)
                }
            }
        })
    }
} 