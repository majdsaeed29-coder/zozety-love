package com.zozety.love.utils

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object SecureStorage {
    
    private const val TAG = "SecureStorage"
    private const val PREFS_NAME = "zozety_ultra_secure_vault"
    
    fun savePassword(context: Context, password: String) {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            
            val sharedPrefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            // تخزين كلمة السر المشفرة بالتشفير المتقدم
            val encryptedPassword = CryptoUtils.encrypt(password)
            val superHash = CryptoUtils.superHash(password, 5000)
            val timestamp = System.currentTimeMillis()
            
            with(sharedPrefs.edit()) {
                putString("encrypted_password_v2", encryptedPassword)
                putString("password_hash_v2", superHash)
                putLong("creation_timestamp", timestamp)
                putBoolean("is_initialized_v2", true)
                putInt("hash_iterations", 5000)
                putString("encryption_type", "AES-GCM + PBKDF2")
                apply()
            }
            
            Log.d(TAG, "Password saved securely with advanced encryption")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving password: ${e.message}")
            
            // تخزين احتياطي في SharedPreferences عادية
            val hash = CryptoUtils.superHash(password, 100)
            context.getSharedPreferences("ZozetyBackupPrefs", Context.MODE_PRIVATE)
                .edit()
                .putString("backup_hash", hash)
                .putBoolean("backup_init", true)
                .apply()
        }
    }
    
    fun verifyPassword(context: Context, inputPassword: String): Boolean {
        return try {
            // المحاولة الأولى: التخزين المشفر المتقدم
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPrefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            // استخراج الهاش المخزن
            val storedHash = sharedPrefs.getString("password_hash_v2", null)
            
            if (storedHash != null) {
                // توليد هاش جديد من كلمة السر المدخلة
                val iterations = sharedPrefs.getInt("hash_iterations", 5000)
                val inputHash = CryptoUtils.superHash(inputPassword, iterations)
                
                val isValid = storedHash == inputHash
                Log.d(TAG, "Advanced hash verification: $isValid")
                
                return isValid
            }
            
            // المحاولة الثانية: النسخة القديمة من الهاش
            val oldStoredHash = sharedPrefs.getString("password_hash", null)
            if (oldStoredHash != null) {
                val inputHash = CryptoUtils.superHash(inputPassword, 1000)
                val isValid = oldStoredHash == inputHash
                
                if (isValid) {
                    // ترقية التخزين إلى النسخة الجديدة
                    savePassword(context, inputPassword)
                    Log.d(TAG, "Upgraded old hash to new encryption")
                }
                
                return isValid
            }
            
            // المحاولة الثالثة: التخزين الاحتياطي
            val backupHash = context.getSharedPreferences("ZozetyBackupPrefs", Context.MODE_PRIVATE)
                .getString("backup_hash", null)
            
            if (backupHash != null) {
                val inputHash = CryptoUtils.superHash(inputPassword, 100)
                return backupHash == inputHash
            }
            
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying password: ${e.message}")
            
            // المحاولة النهائية: التخزين العادي
            val normalHash = context.getSharedPreferences("ZozetyPrefs", Context.MODE_PRIVATE)
                .getString("pass_hash", null)
            
            normalHash?.let {
                val inputHash = CryptoUtils.superHash(inputPassword, 100)
                return it == inputHash
            }
            
            false
        }
    }
    
    fun isStorageInitialized(context: Context): Boolean {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPrefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            sharedPrefs.getBoolean("is_initialized_v2", false) ||
            sharedPrefs.getBoolean("is_initialized", false)
            
        } catch (e: Exception) {
            context.getSharedPreferences("ZozetyPrefs", Context.MODE_PRIVATE)
                .getBoolean("pass_init", false)
        }
    }
    
    fun clearAllData(context: Context) {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPrefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            sharedPrefs.edit().clear().apply()
            
            // مسح التخزين الاحتياطي أيضاً
            context.getSharedPreferences("ZozetyBackupPrefs", Context.MODE_PRIVATE)
                .edit().clear().apply()
            
            context.getSharedPreferences("ZozetyPrefs", Context.MODE_PRIVATE)
                .edit().clear().apply()
            
            Log.d(TAG, "All secure data cleared")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing data: ${e.message}")
        }
    }
    
    fun getStorageInfo(context: Context): Map<String, Any> {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPrefs = EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            mapOf(
                "initialized" to sharedPrefs.getBoolean("is_initialized_v2", false),
                "encryption_type" to sharedPrefs.getString("encryption_type", "Unknown"),
                "iterations" to sharedPrefs.getInt("hash_iterations", 0),
                "timestamp" to sharedPrefs.getLong("creation_timestamp", 0)
            )
            
        } catch (e: Exception) {
            mapOf("error" to e.message ?: "Unknown error")
        }
    }
}
