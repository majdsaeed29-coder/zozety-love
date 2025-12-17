package com.zozety.love

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.zozety.love.databinding.ActivityMainBinding
import com.zozety.love.receivers.NotificationReceiver
import com.zozety.love.services.AIService
import com.zozety.love.services.MessageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var textToSpeech: TextToSpeech
    private var mediaPlayer: MediaPlayer? = null
    private var isSpeaking = false
    private val PERMISSION_REQUEST_CODE = 100
    
    // رسائل الحب المحلية
    private val loveMessages = listOf(
        "حبيبتي، أنتِ أجمل ما في حياتي ❤️",
        "كلما ابتسمتِ، يشرق الكون كله 🌟",
        "أنتِ الحب الذي طالما حلمت به 🌹",
        "معكِ أشعر أنني أغنى إنسان في العالم 💖",
        "عيناكِ تسبحان في بحر من الجمال 🌊",
        "قلبي ينبض باسمكِ في كل لحظة 💓",
        "أنتِ القصيدة التي لم تكتمل بعد 📖",
        "حبكِ هو أجمل شعور عرفته 🥰",
        "أنتِ نعمة من الله في حياتي 🙏",
        "سأظل أحبكِ حتى بعد نهاية الزمن ⏳"
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // تهيئة TextToSpeech
        textToSpeech = TextToSpeech(this, this)
        
        // طلب الأذونات
        checkAndRequestPermissions()
        
        // إعداد واجهة المستخدم
        setupUI()
        
        // بدء خدمة الرسائل
        startMessageService()
        
        // عرض رسالة حب عشوائية
        showRandomLoveMessage()
    }
    
    private fun setupUI() {
        // أزرار التحكم
        binding.btnNewMessage.setOnClickListener {
            showRandomLoveMessage()
        }
        
        binding.btnSpeak.setOnClickListener {
            speakCurrentMessage()
        }
        
        binding.btnPlayMusic.setOnClickListener {
            playLoveMusic()
        }
        
        binding.btnStopMusic.setOnClickListener {
            stopMusic()
        }
        
        binding.btnAddImage.setOnClickListener {
            selectImageFromGallery()
        }
        
        binding.btnSendLove.setOnClickListener {
            sendLoveNotification()
        }
        
        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }
    
    private fun showRandomLoveMessage() {
        val randomMessage = loveMessages.random()
        binding.tvLoveMessage.text = randomMessage
    }
    
    private fun speakCurrentMessage() {
        val message = binding.tvLoveMessage.text.toString()
        if (message.isNotEmpty()) {
            if (isSpeaking) {
                textToSpeech.stop()
                binding.btnSpeak.text = "🔊 تكلم"
                isSpeaking = false
            } else {
                textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
                binding.btnSpeak.text = "⏸️ إيقاف"
                isSpeaking = true
            }
        }
    }
    
    private fun playLoveMusic() {
        try {
            if (mediaPlayer == null) {
                // حاول تحميل ملف موسيقى من raw
                mediaPlayer = MediaPlayer.create(this, R.raw.love_song)
                mediaPlayer?.isLooping = true
            }
            
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                binding.btnPlayMusic.text = "🎵 تشغيل الموسيقى"
            } else {
                mediaPlayer?.start()
                binding.btnPlayMusic.text = "⏸️ إيقاف الموسيقى"
            }
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في تشغيل الموسيقى", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        binding.btnPlayMusic.text = "🎵 تشغيل الموسيقى"
    }
    
    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, 101)
    }
    
    private fun sendLoveNotification() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val aiMessage = AIService.generateLoveMessage(this@MainActivity)
                val message = aiMessage ?: loveMessages.random()
                
                // إرسال الإشعار
                NotificationReceiver.sendLoveNotification(
                    this@MainActivity,
                    message
                )
                
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "تم إرسال رسالة حب ❤️",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "خطأ في إرسال الرسالة",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun startMessageService() {
        val intent = Intent(this, MessageService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    
    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("إعدادات Zozety Love")
            .setMessage("كلمة السر الافتراضية: ZozetyLove2024\n\nيمكنك تغييرها من LoginActivity.kt")
            .setPositiveButton("حسناً", null)
            .setNegativeButton("خروج") { _, _ -> finish() }
            .show()
    }
    
    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale("ar", "SA"))
            
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Toast.makeText(this, "اللغة العربية غير مدعومة", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "فشل تهيئة TextToSpeech", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "تم منح جميع الأذونات", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            imageUri?.let {
                Glide.with(this)
                    .load(it)
                    .into(binding.ivLoveImage)
                
                Toast.makeText(this, "تم إضافة الصورة بنجاح", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
        stopMusic()
    }
}
