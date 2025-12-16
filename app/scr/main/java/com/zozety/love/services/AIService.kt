package com.zozety.love.services

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.java.GenerativeModelFutures
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import kotlinx.coroutines.*
import java.util.concurrent.Executors

object AIService {
    
    private const val TAG = "AIService"
    private const val API_KEY = "AIzaSyAW4298inSXJmhHe9PkkrH97OkBBTWm9sA" // استخدم مفتاحك
    
    // رسائل محلية احتياطية
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
            // محاولة استخدام Google Gemini AI
            val aiMessage = tryGenerateWithGemini()
            aiMessage ?: getLocalMessage()
        } catch (e: Exception) {
            Log.e(TAG, "AI generation failed: ${e.message}")
            getLocalMessage()
        }
    }
    
    private suspend fun tryGenerateWithGemini(): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            // تهيئة نموذج Gemini
            val generativeModel = GenerativeModel(
                modelName = "gemini-pro",
                apiKey = API_KEY
            )
            
            val generativeModelFutures = GenerativeModelFutures(generativeModel)
            
            // كتابة سؤال للذكاء الاصطناعي
            val prompt = "اكتب رسالة حب رومانسية قصيرة باللغة العربية، لا تتجاوز 50 كلمة، تكون عاطفية وجميلة"
            
            val response: GenerateContentResponse = generativeModelFutures.generateContent(prompt)
            
            val text = response.text
            Log.d(TAG, "Gemini Response: $text")
            
            // تنظيف النص
            text?.trim()?.takeIf { it.isNotEmpty() }
            
        } catch (e: Exception) {
            Log.e(TAG, "Gemini error: ${e.message}")
            null
        }
    }
    
    private fun getLocalMessage(): String {
        return localMessages.random()
    }
}
