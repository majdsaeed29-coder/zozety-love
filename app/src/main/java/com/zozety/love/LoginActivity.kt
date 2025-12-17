package com.zozety.love

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.zozety.love.databinding.ActivityLoginBinding
import com.zozety.love.utils.CryptoUtils
import com.zozety.love.utils.SecureStorage
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

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
        
        // تهيئة التشفير والتخزين الآمن
        initializeSecurity()
        
        // التحقق من تسجيل الدخول السابق
        if (isUserLoggedIn()) {
            startMainActivity()
            return
        }
        
        setupLoginUI()
    }
    
    private fun setupLoginUI() {
        binding.btnLogin.setOnClickListener {
            val enteredPassword = binding.etPassword.text.toString().trim()
            
            if (enteredPassword.isEmpty()) {
                showToast("الرجاء إدخال كلمة السر")
                return@setOnClickListener
            }
            
            if (validatePassword(enteredPassword)) {
                saveLoginState()
                showToast("تم الدخول بنجاح! 💖")
                startMainActivity()
            } else {
                showToast("كلمة السر غير صحيحة")
                binding.etPassword.text?.clear()
                binding.etPassword.requestFocus()
            }
        }
        
        binding.tvForgotPassword.setOnClickListener {
            showHintDialog()
        }
        
        binding.etPassword.setOnEditorActionListener { _, _, _ ->
            binding.btnLogin.performClick()
            true
        }
    }
    
    private fun validatePassword(password: String): Boolean {
        // الطريقة 1: التشفير المتقدم والتجزئة
        val isSecureValid = SecureStorage.verifyPassword(this, password)
        
        // الطريقة 2: التحقق المباشر (للتطوير)
        val isDirectValid = password == DEFAULT_PASSWORD
        
        // الطريقة 3: التحقق من التجزئة المحلية
        val storedHash = getSharedPreferences("ZozetyPrefs", Context.MODE_PRIVATE)
            .getString("pass_hash", null)
        val isHashValid = storedHash == generateSuperHash(password)
        
        Log.d(TAG, "Password validation: Secure=$isSecureValid, Direct=$isDirectValid, Hash=$isHashValid")
        
        return isSecureValid || isDirectValid || isHashValid
    }
    
    private fun initializeSecurity() {
        // تهيئة التخزين الآمن لأول مرة
        if (!isPasswordInitialized()) {
            Log.d(TAG, "Initializing secure storage...")
            SecureStorage.savePassword(this, DEFAULT_PASSWORD)
            
            // تخزين تجزئة احتياطية
            val hash = generateSuperHash(DEFAULT_PASSWORD)
            getSharedPreferences("ZozetyPrefs", Context.MODE_PRIVATE)
                .edit()
                .putString("pass_hash", hash)
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
            Base64.encodeToString(hashBytes, Base64.NO_WRAP)
        }
    }
    
    private fun saveLoginState() {
        try {
            // التخزين المشفر
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
                apply()
            }
            
            // تخزين احتياطي
            getSharedPreferences("ZozetyPrefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", true)
                .putLong("lastLogin", System.currentTimeMillis())
                .apply()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving login state: ${e.message}")
            // تخزين عادي كبديل
            getSharedPreferences("ZozetyPrefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", true)
                .apply()
        }
    }
    
    private fun isUserLoggedIn(): Boolean {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPrefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPrefs.getBoolean("is_logged_in", false)
        } catch (e: Exception) {
            getSharedPreferences("ZozetyPrefs", Context.MODE_PRIVATE)
                .getBoolean("isLoggedIn", false)
        }
    }
    
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
        
        // تأثير انتقال سلس
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
    
    private fun showHintDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("💖 تلميح خاص")
            .setMessage("كلمة السر هي: zezemajdlove\n\nهذا التطبيق خاص بنا فقط، لا تشارك كلمة السر مع أحد.")
            .setPositiveButton("فهمت") { _, _ ->
                binding.etPassword.setText(DEFAULT_PASSWORD)
                binding.btnLogin.performClick()
            }
            .setNegativeButton("رجوع", null)
            .setIcon(R.drawable.ic_heart)
            .show()
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun onBackPressed() {
        android.app.AlertDialog.Builder(this)
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
