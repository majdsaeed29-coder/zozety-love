package com.zozety.love

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.zozety.love.databinding.ActivityLoginBinding
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private val KEY_ALIAS = "ZozetyLove_Key"
    private val ANDROID_KEYSTORE = "AndroidKeyStore"
    
    companion object {
        private const val PREFS_NAME = "secure_prefs_zozety"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_IV = "password_iv"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // التحقق من تسجيل الدخول السابق
        if (isUserLoggedIn()) {
            startMainActivity()
            return
        }
        
        // تهيئة كلمة السر المشفرة لأول مرة
        initializeEncryptedPassword()
        
        setupLoginUI()
    }
    
    private fun setupLoginUI() {
        binding.btnLogin.setOnClickListener {
            val enteredPassword = binding.etPassword.text.toString().trim()
            
            if (enteredPassword.isEmpty()) {
                Toast.makeText(this, "الرجاء إدخال كلمة السر", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (validatePassword(enteredPassword)) {
                saveLoginState()
                startMainActivity()
            } else {
                Toast.makeText(this, "كلمة السر غير صحيحة", Toast.LENGTH_SHORT).show()
                binding.etPassword.text?.clear()
                binding.etPassword.requestFocus()
            }
        }
        
        binding.tvForgotPassword.setOnClickListener {
            showHintDialog()
        }
    }
    
    private fun validatePassword(password: String): Boolean {
        // كلمة السر الأصلية
        val originalPassword = "zezemajdlove"
        
        // مقارنة مباشرة (في بيئة آمنة، استخدم التشفير)
        return password == originalPassword || compareWithStoredHash(password)
    }
    
    private fun compareWithStoredHash(password: String): Boolean {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            
            val sharedPrefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            val storedHash = sharedPrefs.getString(KEY_PASSWORD_HASH, "")
            val currentHash = advancedHash(password)
            
            storedHash == currentHash
        } catch (e: Exception) {
            false
        }
    }
    
    private fun advancedHash(input: String): String {
        // خوارزمية تجزئة معقدة متعددة الطبقات
        val bytes = input.toByteArray(StandardCharsets.UTF_8)
        
        // الطبقة الأولى: SHA-256
        var hash = sha256(bytes)
        
        // الطبقة الثانية: إضافة الملح والتجزئة مرة أخرى
        val salt = "ZozetyLoveSalt2024!@#".toByteArray(StandardCharsets.UTF_8)
        hash = sha256(hash + salt)
        
        // الطبقة الثالثة: Base64 ثم تجزئة مرة أخرى
        val base64Hash = Base64.encodeToString(hash, Base64.NO_WRAP)
        hash = sha256(base64Hash.toByteArray(StandardCharsets.UTF_8))
        
        return bytesToHex(hash)
    }
    
    private fun sha256(input: ByteArray): ByteArray {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(input)
    }
    
    private fun sha256(input: String): ByteArray {
        return sha256(input.toByteArray(StandardCharsets.UTF_8))
    }
    
    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = "0123456789ABCDEF"[v ushr 4]
            hexChars[i * 2 + 1] = "0123456789ABCDEF"[v and 0x0F]
        }
        return String(hexChars)
    }
    
    private fun initializeEncryptedPassword() {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            
            val sharedPrefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            // تخزين هاش كلمة السر فقط إذا لم تكن موجودة
            if (!sharedPrefs.contains(KEY_PASSWORD_HASH)) {
                val passwordHash = advancedHash("zezemajdlove")
                with(sharedPrefs.edit()) {
                    putString(KEY_PASSWORD_HASH, passwordHash)
                    apply()
                }
            }
            
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error initializing encrypted password: ${e.message}")
        }
    }
    
    private fun saveLoginState() {
        try {
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
                apply()
            }
        } catch (e: Exception) {
            // Fallback to regular shared preferences
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
    }
    
    private fun showHintDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("تلميح")
            .setMessage("كلمة السر هي: zezemajdlove")
            .setPositiveButton("حسناً", null)
            .show()
    }
    
    override fun onBackPressed() {
        android.app.AlertDialog.Builder(this)
            .setTitle("تأكيد الخروج")
            .setMessage("هل تريد الخروج من التطبيق؟")
            .setPositiveButton("نعم") { _, _ -> finishAffinity() }
            .setNegativeButton("لا", null)
            .show()
    }
}
