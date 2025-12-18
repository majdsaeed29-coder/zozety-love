package com.zozety.love

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.zozety.love.databinding.ActivityLoginBinding
import com.zozety.love.utils.CryptoUtils
import com.zozety.love.utils.SecureStorage
import java.nio.charset.StandardCharsets
import java.util.*

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    
    companion object {
        private const val TAG = "LoginActivity"
        private const val PREFS_NAME = "zozety_secure_vault"
        private const val DEFAULT_PASSWORD = "zezemajdlove"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 🔍 تشخيص حالة الدخول
        Log.d(TAG, "=== LoginActivity Started ===")
        Log.d(TAG, "DEFAULT_PASSWORD: $DEFAULT_PASSWORD")
        
        val testPrefs = getSharedPreferences("ZozetyPrefs", Context.MODE_PRIVATE)
        val isLoggedIn = testPrefs.getBoolean("isLoggedIn", false)
        Log.d(TAG, "Current login state: $isLoggedIn")
        
        // التحقق من تسجيل الدخول السابق
        if (isUserLoggedIn()) {
            Log.d(TAG, "User is already logged in, redirecting...")
            startMainActivity()
            return
        }
        
        // تهيئة التشفير والتخزين الآمن
        initializeSecurity()
        setupLoginUI()
    }
    
    private fun setupLoginUI() {
        // ⚠️ تم إزالة "نسيت كلمة السر" نهائياً
        // binding.tvForgotPassword.setOnClickListener { showHintDialog() }
        
        binding.btnLogin.setOnClickListener {
            val enteredPassword = binding.etPassword.text.toString().trim()
            
            if (enteredPassword.isEmpty()) {
                showToast("الرجاء إدخال كلمة السر")
                return@setOnClickListener
            }
            
            // إظهار تقدم
            binding.btnLogin.text = "جارٍ التحقق..."
            binding.btnLogin.isEnabled = false
            
            // التحقق في خلفية لمنع تجميد الواجهة
            Thread {
                val isValid = validatePassword(enteredPassword)
                
                runOnUiThread {
                    binding.btnLogin.text = "دخول إلى عالم الحب"
                    binding.btnLogin.isEnabled = true
                    
                    if (isValid) {
                        saveLoginState()
                        showToast("تم الدخول بنجاح! 💖")
                        startMainActivity()
                    } else {
                        showToast("كلمة السر غير صحيحة")
                        binding.etPassword.text?.clear()
                        binding.etPassword.requestFocus()
                    }
                }
            }.start()
        }
        
        binding.etPassword.setOnEditorActionListener { _, _, _ ->
            binding.btnLogin.performClick()
            true
        }
    }
    
    private fun validatePassword(password: String): Boolean {
        // ✅ الإصدار المصحح - ترتيب أولويات التحقق
        
        // 1. التحقق المباشر (الأسرع والأضمن)
        if (password == DEFAULT_PASSWORD) {
            Log.d(TAG, "✅ Password valid: Direct match")
            return true
        }
        
        // 2. التحقق من التخزين الآمن
        return try {
            val isValid = SecureStorage.verifyPassword(this, password)
            Log.d(TAG, "🔒 Secure storage validation: $isValid")
            isValid
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Secure validation failed: ${e.message}")
            
            // 3. التحقق الاحتياطي (التجزئة المحلية)
            val storedHash = getSharedPreferences("ZozetyPrefs", Context.MODE_PRIVATE)
                .getString("pass_hash", null)
            
            if (storedHash != null) {
                val inputHash = generateSuperHash(password)
                val isHashValid = storedHash == inputHash
                Log.d(TAG, "📊 Hash validation: $isHashValid")
                return isHashValid
            }
            
            false
        }
    }
    
    private fun initializeSecurity() {
        // تهيئة التخزين الآمن لأول مرة
        if (!isPasswordInitialized()) {
            Log.d(TAG, "🔐 Initializing secure storage...")
            SecureStorage.savePassword(this, DEFAULT_PASSWORD)
            
            // تخزين تجزئة احتياطية
            val hash = generateSuperHash(DEFAULT_PASSWORD)
            getSharedPreferences("ZozetyPrefs", Context.MODE_PRIVATE)
                .edit()
                .putString("pass_hash", hash)
                .putBoolean("pass_init", true)
                .apply()
        }
    }
    
    private fun isPasswordInitialized(): Boolean {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPrefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPrefs.getBoolean("is_initialized", false)
        } catch (e: Exception) {
            getSharedPreferences("ZozetyPrefs", Context.MODE_PRIVATE)
                .getBoolean("pass_init", false)
        }
    }
    
    private fun generateSuperHash(input: String): String {
        return try {
            CryptoUtils.superHash(input, 1000)
        } catch (e: Exception) {
            // تجزئة احتياطية بسيطة
            val bytes = input.toByteArray(StandardCharsets.UTF_8)
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(bytes)
            Base64.getEncoder().encodeToString(hashBytes)
        }
    }
    
    private fun saveLoginState() {
        Log.d(TAG, "💾 Saving login state...")
        
        try {
            // 1. التخزين المشفر (EncryptedSharedPreferences)
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPrefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            with(sharedPrefs.edit()) {
                putBoolean("is_logged_in", true)
                putLong("login_time", System.currentTimeMillis())
                putString("device_id", Build.DEVICE)
                apply()  // ✅ استخدم apply() بدل commit()
            }
            
            // 2. التخزين العادي (احتياطي)
            getSharedPreferences("ZozetyPrefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", true)
                .putLong("lastLogin", System.currentTimeMillis())
                .apply()  // ✅ استخدم apply()
            
            Log.d(TAG, "✅ Login state saved successfully in both storages")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving login state: ${e.message}")
            
            // 3. تخزين عادي كبديل أخير
            getSharedPreferences("ZozetyPrefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", true)
                .apply()
            
            Log.d(TAG, "⚠️ Used fallback storage")
        }
    }
    
    private fun isUserLoggedIn(): Boolean {
        // التحقق من كلا نظامي التخزين
        return try {
            // 1. التخزين المشفر
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPrefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val encryptedLoggedIn = sharedPrefs.getBoolean("is_logged_in", false)
            
            // 2. التخزين العادي
            val normalLoggedIn = getSharedPreferences("ZozetyPrefs", Context.MODE_PRIVATE)
                .getBoolean("isLoggedIn", false)
            
            val result = encryptedLoggedIn || normalLoggedIn
            Log.d(TAG, "🔍 Login check: Encrypted=$encryptedLoggedIn, Normal=$normalLoggedIn, Result=$result")
            
            result
        } catch (e: Exception) {
            // 3. التحقق الاحتياطي
            getSharedPreferences("ZozetyPrefs", Context.MODE_PRIVATE)
                .getBoolean("isLoggedIn", false)
        }
    }
    
    private fun startMainActivity() {
        Log.d(TAG, "🚀 Starting MainActivity...")
        
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        
        // تأكد من إنهاء النشاط بعد الانتقال
        finish()
        
        // تأثير انتقال سلس
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onBackPressed() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ تأكيد الخروج")
            .setMessage("هل تريد حقاً الخروج من عالم حبنا؟")
            .setPositiveButton("نعم، أريد الخروج") { _, _ -> 
                finishAffinity()
            }
            .setNegativeButton("لا، سأبقى معكِ 💖", null)
            .setCancelable(false)
            .show()
    }
}
