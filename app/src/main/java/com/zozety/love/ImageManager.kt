package com.zozety.love

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object ImageManager {
    
    private const val TAG = "ImageManager"
    private const val IMAGE_DIR = "love_images"
    private const val MAX_IMAGES = 20
    
    suspend fun downloadAndSaveImage(context: Context, imageUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpsURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.doInput = true
                connection.connect()
                
                if (connection.responseCode == 200) {
                    val inputStream = connection.inputStream
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    
                    if (bitmap != null) {
                        saveBitmapToStorage(context, bitmap)
                    } else {
                        null
                    }
                } else {
                    Log.w(TAG, "Failed to download image: ${connection.responseCode}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading image: ${e.message}")
                null
            }
        }
    }
    
    private fun saveBitmapToStorage(context: Context, bitmap: Bitmap): String {
        val imagesDir = File(context.filesDir, IMAGE_DIR)
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        
        cleanupOldImages(imagesDir)
        
        val fileName = "love_${System.currentTimeMillis()}.jpg"
        val imageFile = File(imagesDir, fileName)
        
        try {
            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.flush()
            outputStream.close()
            
            Log.d(TAG, "Image saved: ${imageFile.absolutePath}")
            return imageFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image: ${e.message}")
            throw e
        }
    }
    
    private fun cleanupOldImages(imagesDir: File) {
        val files = imagesDir.listFiles()
        if (files != null && files.size > MAX_IMAGES) {
            files.sortedBy { it.lastModified() }
                .take(files.size - MAX_IMAGES)
                .forEach { it.delete() }
        }
    }
    
    fun getLocalImages(context: Context): List<String> {
        val imagesDir = File(context.filesDir, IMAGE_DIR)
        if (!imagesDir.exists()) return emptyList()
        
        return imagesDir.listFiles()
            ?.filter { it.isFile && (it.name.endsWith(".jpg") || it.name.endsWith(".png")) }
            ?.map { it.absolutePath }
            ?: emptyList()
    }
}
