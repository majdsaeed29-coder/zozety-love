package com.zozety.love

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.zozety.love.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    
    companion object {
        private const val PREF_NAME = "zozety_secure_prefs"
        private const val KEY_PASSWORD = "user_password"
        private const val KEY_LOGGED_IN = "is_logged_in"
        private const val DEFAULT_PASSWORD = "ZozetyLove2024"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // التحقق إذا كان المستخدم مسجل دخول بالفعل
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
                Toast.makeText(this, "الرجاء إدخال كلمة السر", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (validatePassword(enteredPassword)) {
                saveLoginState(enteredPassword)
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
        val savedPassword = getSavedPassword()
        return password == savedPassword || password == DEFAULT_PASSWORD
    }
    
    private fun getSavedPassword(): String {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPrefs = EncryptedSharedPreferences.create(
                PREF_NAME,
                masterKeyAlias,
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPrefs.getString(KEY_PASSWORD, DEFAULT_PASSWORD) ?: DEFAULT_PASSWORD
        } catch (e: Exception) {
            // Fallback to regular shared preferences
            getSharedPreferences("ZozetyPrefs", Context.MODE_PRIVATE)
                .getString("password", DEFAULT_PASSWORD) ?: DEFAULT_PASSWORD
        }
    }
    
    private fun saveLoginState(password: String) {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPrefs = EncryptedSharedPreferences.create(
                PREF_NAME,
                masterKeyAlias,
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            with(sharedPrefs.edit()) {
                putBoolean(KEY_LOGGED_IN, true)
                putString(KEY_PASSWORD, password)
                apply()
            }
        } catch (e: Exception) {
            // Fallback
            getSharedPreferences("ZozetyPrefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", true)
                .putString("password", password)
                .apply()
        }
    }
    
    private fun isUserLoggedIn(): Boolean {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPrefs = EncryptedSharedPreferences.create(
                PREF_NAME,
                masterKeyAlias,
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPrefs.getBoolean(KEY_LOGGED_IN, false)
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
            .setMessage("كلمة السر الافتراضية هي: $DEFAULT_PASSWORD")
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
