package com.zozety.love

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

object TextToSpeechHelper {
    
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private const val TAG = "TextToSpeechHelper"
    
    fun initialize(context: Context, onInitListener: (Boolean) -> Unit = {}) {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale("ar", "SA"))
                
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Arabic language is not supported")
                    isInitialized = false
                    onInitListener(false)
                } else {
                    isInitialized = true
                    Log.d(TAG, "TextToSpeech initialized successfully")
                    onInitListener(true)
                }
            } else {
                Log.e(TAG, "TextToSpeech initialization failed")
                isInitialized = false
                onInitListener(false)
            }
        }
        
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "Speech started: $utteranceId")
            }
            
            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "Speech completed: $utteranceId")
            }
            
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "Speech error: $utteranceId")
            }
        })
    }
    
    fun speak(context: Context, text: String) {
        if (!isInitialized) {
            initialize(context) { success ->
                if (success) {
                    speakText(text)
                }
            }
        } else {
            speakText(text)
        }
    }
    
    private fun speakText(text: String) {
        if (textToSpeech == null) return
        
        val cleanText = text.replace(Regex("[^\\p{L}\\p{N}\\p{P}\\p{Z}]"), "")
        
        textToSpeech?.speak(
            cleanText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "love_message_${System.currentTimeMillis()}"
        )
    }
    
    fun stop() {
        textToSpeech?.stop()
    }
    
    fun shutdown() {
        textToSpeech?.shutdown()
        textToSpeech = null
        isInitialized = false
    }
    
    fun isSpeaking(): Boolean {
        return textToSpeech?.isSpeaking ?: false
    }
}
