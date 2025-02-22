package com.example.textblankmaker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import android.util.Log
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.snackbar.Snackbar
import android.view.View
import com.google.android.material.progressindicator.CircularProgressIndicator

class MainActivity : AppCompatActivity() {
    private val client = OkHttpClient()
    private val API_KEY = BuildConfig.OPENAI_API_KEY
    private val API_URL = "https://api.openai.com/v1/chat/completions"
    private lateinit var progressIndicator: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val inputText = findViewById<TextInputEditText>(R.id.inputText)
        val generateButton = findViewById<MaterialButton>(R.id.generateButton)
        val resultText = findViewById<TextInputEditText>(R.id.resultText)
        val copyButton = findViewById<MaterialButton>(R.id.copyButton)
        progressIndicator = findViewById(R.id.progressIndicator)

        generateButton.setOnClickListener {
            val text = inputText.text.toString()
            if (text.isEmpty()) {
                showSnackbar("텍스트를 입력해주세요")
                return@setOnClickListener
            }
            
            if (text.length > 1000) {
                showSnackbar("텍스트가 너무 깁니다. 1000자 이내로 입력해주세요.")
                return@setOnClickListener
            }

            progressIndicator.visibility = View.VISIBLE
            makeApiCall(text) { response ->
                runOnUiThread {
                    progressIndicator.visibility = View.GONE
                    resultText.setText(response)
                }
            }
        }

        copyButton.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("빈칸 문제", resultText.text.toString())
            clipboard.setPrimaryClip(clip)
            showSnackbar("클립보드에 복사되었습니다")
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
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
                        .getString("content")
                    
                    callback(generatedText)
                }
            }
        })
    }
} 