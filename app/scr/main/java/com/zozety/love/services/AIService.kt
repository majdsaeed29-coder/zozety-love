package com.zozety.love.services

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object AIService {
    
    private const val TAG = "AIService"
    
    // ✅ **مفتاح Google AI الخاص بك**
    private const val API_KEY = "AIzaSyAW4298inSXJmhHe9PkkrH97OkBBTWm9sA"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val localMessages = listOf(
        "حبيبتي، أنتِ النور الذي يضيء حياتي 🌟",
        "كل يوم معكِ هو عيد لحبي ❤️",
        "أنتِ أجمل قصة حب في حياتي 📖",
        "قلبي يهتف باسمكِ في كل نبضة 💓",
        "حضنكِ هو ملاذي الآمن 🤗",
        "عيناكِ بحر من الحب والعطاء 🌊",
        "أنتِ الحلم الذي أصبح حقيقة 💫",
        "سأظل أحبكِ إلى الأبد ♾️",
        "وجودكِ في حياتي هو أعظم هدية 🎁",
        "حبكِ يجعل كل شيء جميلاً 🌈"
    )
    
    suspend fun generateLoveMessage(context: Context): String? {
        return try {
            // محاولة استخدام Google Generative AI API
            val aiMessage = tryGenerateWithGoogleAI()
            aiMessage ?: getLocalMessage()
        } catch (e: Exception) {
            Log.e(TAG, "AI generation failed: ${e.message}")
            getLocalMessage()
        }
    }
    
    private suspend fun tryGenerateWithGoogleAI(): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            // استخدام Gemini API من Google
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$API_KEY"
            
            val requestBody = """
            {
                "contents": [{
                    "parts": [{
                        "text": "اكتب رسالة حب رومانسية قصيرة باللغة العربية لا تتجاوز 30 كلمة. تكون عاطفية وجميلة."
                    }]
                }],
                "generationConfig": {
                    "temperature": 0.9,
                    "topK": 1,
                    "topP": 1,
                    "maxOutputTokens": 100,
                    "stopSequences": []
                },
                "safetySettings": [{
                    "category": "HARM_CATEGORY_HARASSMENT",
                    "threshold": "BLOCK_MEDIUM_AND_ABOVE"
                }]
            }
            """.trimIndent()
            
            val request = Request.Builder()
                .url(url)
                .post(RequestBody.create("application/json".toMediaType(), requestBody))
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d(TAG, "Google AI Response: $responseBody")
                
                // استخراج النص من الرد
                extractMessageFromResponse(responseBody)
            } else {
                Log.w(TAG, "API call failed: ${response.code}")
                null
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}")
            null
        }
    }
    
    private fun extractMessageFromResponse(response: String?): String? {
        if (response.isNullOrEmpty()) return null
        
        return try {
            val json = JSONObject(response)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    val firstPart = parts.getJSONObject(0)
                    firstPart.optString("text", "").trim()
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response: ${e.message}")
            null
        }
    }
    
    private fun getLocalMessage(): String {
        return localMessages.random()
    }
}
