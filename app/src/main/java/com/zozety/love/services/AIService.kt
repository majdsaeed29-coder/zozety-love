package com.zozety.love.services

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object AIService {
    
    private const val TAG = "AIService"
    
    // 🔑 **مفتاح Google AI API الخاص بك**
    private const val API_KEY = "AIzaSyAW4298inSXJmhHe9PkkrH97OkBBTWm9sA"
    private const val GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()
    
    // رسائل حب محلية احتياطية
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
        "حبكِ يجعل كل شيء جميلاً 🌈",
        "أنتِ أغلى ما أملك في هذه الدنيا 💎",
        "حبي لكِ أكبر من كل الكلمات 💌",
        "معكِ وجدت معنى الحياة الحقيقي 🌺",
        "أنتِ موطني الآمن في هذه الحياة 🏡",
        "عيناكِ تحدثني بلغة الحب الصامتة 👀"
    )
    
    suspend fun generateLoveMessage(context: Context): String? {
        return try {
            // محاولة استخدام Google Gemini AI
            val aiMessage = tryGenerateWithGemini()
            aiMessage ?: getLocalMessage()
        } catch (e: Exception) {
            Log.e(TAG, "AI generation failed: ${e.message}", e)
            getLocalMessage()
        }
    }
    
    private suspend fun tryGenerateWithGemini(): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val url = "$GEMINI_API_URL?key=$API_KEY"
            
            // بناء جسم الطلب
            val requestBody = JSONObject().apply {
                put("contents", JSONObject().apply {
                    put("parts", JSONObject().apply {
                        put("text", generateRandomPrompt())
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.8)
                    put("topK", 40)
                    put("topP", 0.95)
                    put("maxOutputTokens", 150)
                })
                put("safetySettings", JSONObject().apply {
                    put("category", "HARM_CATEGORY_SEXUALLY_EXPLICIT")
                    put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
                })
            }.toString()
            
            Log.d(TAG, "Request URL: $url")
            Log.d(TAG, "Request Body: $requestBody")
            
            val request = Request.Builder()
                .url(url)
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d(TAG, "Response Code: ${response.code}")
            Log.d(TAG, "Response Body: $responseBody")
            
            if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                extractMessageFromGeminiResponse(responseBody)
            } else {
                Log.w(TAG, "API call failed with code: ${response.code}")
                null
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            null
        }
    }
    
    private fun generateRandomPrompt(): String {
        val prompts = listOf(
            "اكتب رسالة حب رومانسية قصيرة بالعربية لا تتجاوز 30 كلمة. تكون عاطفية وجميلة.",
            "رسالة حب عربية قصيرة لحبيبتي، استخدم لغة شعرية رومانسية.",
            "عبّر عن الحب العميق بالعربية في رسالة لا تزيد عن 25 كلمة.",
            "اكتب كلمات حب وعشق بالعربية لحبيبة غالية على القلب.",
            "رسالة صباحية حب بالعربية لحبيبتي، تكون دافئة وعاطفية."
        )
        return prompts.random()
    }
    
    private fun extractMessageFromGeminiResponse(response: String): String? {
        return try {
            val json = JSONObject(response)
            
            // محاولة استخراج النص من الاستجابة
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                
                if (parts != null && parts.length() > 0) {
                    val text = parts.getJSONObject(0).optString("text", "")
                    if (text.isNotEmpty()) {
                        return cleanMessage(text)
                    }
                }
            }
            
            // محاولة بديلة
            json.optString("text", "").takeIf { it.isNotEmpty() }?.let {
                return cleanMessage(it)
            }
            
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini response: ${e.message}")
            null
        }
    }
    
    private fun cleanMessage(message: String): String {
        return message
            .trim()
            .replace(Regex("^[\"']+|[\"']+$"), "") // إزالة علامات الاقتباس
            .replace(Regex("\\*\\*"), "") // إزالة التنسيق
            .takeIf { it.length in 10..200 } // التحقق من الطول
            ?: getLocalMessage() // استخدام رسالة محلية إذا فشل التنظيف
    }
    
    private fun getLocalMessage(): String {
        return localMessages.random()
    }
    
    fun getRandomLocalMessage(): String {
        return localMessages.random()
    }
    
    suspend fun generateMultipleMessages(count: Int = 3): List<String> {
        return try {
            val messages = mutableListOf<String>()
            
            // محاولة الحصول على رسالة من AI
            val aiMessage = tryGenerateWithGemini()
            aiMessage?.let { messages.add(it) }
            
            // إكمال العدد بالرسائل المحلية
            while (messages.size < count) {
                messages.add(getLocalMessage())
            }
            
            messages.shuffled()
            
        } catch (e: Exception) {
            List(count) { getLocalMessage() }
        }
    }
}
