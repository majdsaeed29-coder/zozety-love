package com.zozety.love.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    
    private const val TAG = "CryptoUtils"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "ZozetyLove_Key"
    private const val TRANSFORMATION_AES_GCM = "AES/GCM/NoPadding"
    private const val TRANSFORMATION_AES_CBC = "AES/CBC/PKCS5Padding"
    private const val IV_SIZE = 12 // 12 bytes for GCM
    private const val GCM_TAG_LENGTH = 128
    private const val ITERATION_COUNT = 10000
    private const val KEY_LENGTH = 256
    
    // 🔒 إنشاء مفتاح AES في Android KeyStore
    @SuppressLint("NewApi")
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
        
        return if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            
            val builder = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_LENGTH)
                .setRandomizedEncryptionRequired(true)
            
            // استخدام StrongBox إذا كان متاحاً (Android 9+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                builder.setIsStrongBoxBacked(true)
            }
            
            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
            
            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        } else {
            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }
    }
    
    // 🔐 تشفير النص باستخدام AES-GCM
    fun encrypt(text: String): String {
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION_AES_GCM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(text.toByteArray(StandardCharsets.UTF_8))
            
            // دمج IV والنص المشفر
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            
            Base64.encodeToString(combined, Base64.NO_WRAP)
            
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed: ${e.message}")
            
            // تشفير احتياطي باستخدام AES-CBC
            encryptFallback(text)
        }
    }
    
    // 🔓 فك تشفير النص
    fun decrypt(encryptedText: String): String {
        return try {
            val secretKey = getOrCreateSecretKey()
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            
            val iv = ByteArray(IV_SIZE)
            val encryptedBytes = ByteArray(combined.size - IV_SIZE)
            
            System.arraycopy(combined, 0, iv, 0, IV_SIZE)
            System.arraycopy(combined, IV_SIZE, encryptedBytes, 0, encryptedBytes.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION_AES_GCM)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8)
            
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed: ${e.message}")
            
            // محاولة فك التشفير بالطريقة الاحتياطية
            decryptFallback(encryptedText)
        }
    }
    
    // 🔄 تشفير احتياطي باستخدام AES-CBC
    private fun encryptFallback(text: String): String {
        return try {
            val password = "ZozetyLove2024SecretKey!@#".toCharArray()
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH)
            val key = factory.generateSecret(spec)
            val secretKey = SecretKeySpec(key.encoded, "AES")
            
            val cipher = Cipher.getInstance(TRANSFORMATION_AES_CBC)
            val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val ivSpec = IvParameterSpec(iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val encrypted = cipher.doFinal(text.toByteArray(StandardCharsets.UTF_8))
            
            // دمع IV و salt والنص المشفر
            val result = ByteArray(salt.size + iv.size + encrypted.size)
            System.arraycopy(salt, 0, result, 0, salt.size)
            System.arraycopy(iv, 0, result, salt.size, iv.size)
            System.arraycopy(encrypted, 0, result, salt.size + iv.size, encrypted.size)
            
            Base64.encodeToString(result, Base64.NO_WRAP)
            
        } catch (e: Exception) {
            Log.e(TAG, "Fallback encryption failed: ${e.message}")
            // إذا فشل كل شيء، إرجاع نص مشفر بسيط
            Base64.encodeToString(text.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        }
    }
    
    // 🔄 فك تشفير احتياطي
    private fun decryptFallback(encryptedText: String): String {
        return try {
            val decoded = Base64.decode(encryptedText, Base64.NO_WRAP)
            
            if (decoded.size < 32) { // إذا كان النص مشفراً بسيطاً
                return String(Base64.decode(encryptedText, Base64.NO_WRAP), StandardCharsets.UTF_8)
            }
            
            val password = "ZozetyLove2024SecretKey!@#".toCharArray()
            val salt = decoded.copyOfRange(0, 16)
            val iv = decoded.copyOfRange(16, 32)
            val encrypted = decoded.copyOfRange(32, decoded.size)
            
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val spec = PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH)
            val key = factory.generateSecret(spec)
            val secretKey = SecretKeySpec(key.encoded, "AES")
            
            val cipher = Cipher.getInstance(TRANSFORMATION_AES_CBC)
            val ivSpec = IvParameterSpec(iv)
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
            
        } catch (e: Exception) {
            Log.e(TAG, "Fallback decryption failed: ${e.message}")
            "DECRYPTION_ERROR"
        }
    }
    
    // 🔐 توليد تجزئة متقدمة متعددة الطبقات
    fun superHash(input: String, iterations: Int = 1000): String {
        var hash = input
        
        for (i in 1..iterations) {
            // طبقة 1: SHA-256
            hash = sha256(hash + "Salt${i}Zozety")
            
            // طبقة 2: إضافة بعض التحويلات
            if (i % 100 == 0) {
                hash = Base64.encodeToString(hash.toByteArray(), Base64.NO_WRAP)
            }
            
            // طبقة 3: قلب النص
            if (i % 200 == 0) {
                hash = hash.reversed()
            }
            
            // طبقة 4: إضافة بادئة ولاحقة
            if (i % 50 == 0) {
                hash = "ZT${hash}LY"
            }
        }
        
        return hash
    }
    
    // 🔐 تجزئة SHA-256
    private fun sha256(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
            bytesToHex(hashBytes)
        } catch (e: Exception) {
            Log.e(TAG, "SHA-256 failed: ${e.message}")
            input // إرجاع النص الأصلي في حالة الفشل
        }
    }
    
    // 🔐 تحويل بايتات إلى Hex
    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = "0123456789ABCDEF"[v ushr 4]
            hexChars[i * 2 + 1] = "0123456789ABCDEF"[v and 0x0F]
        }
        return String(hexChars)
    }
    
    // 🔐 إنشاء IV عشوائي
    fun generateRandomIV(): String {
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        return Base64.encodeToString(iv, Base64.NO_WRAP)
    }
    
    // 🔐 إنشاء مفتاح عشوائي
    fun generateRandomKey(): String {
        val key = ByteArray(32)
        SecureRandom().nextBytes(key)
        return Base64.encodeToString(key, Base64.NO_WRAP)
    }
}
