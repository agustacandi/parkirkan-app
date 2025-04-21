package dev.agustacandi.parkirkanapp.util.ext

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

fun Uri.compressAndCreateImageFile(context: Context): File? {
    try {
        // Baca gambar dari URI
        val inputStream = context.contentResolver.openInputStream(this) ?: return null
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        // Kompres gambar
        val compressedBitmap = if (bitmap.width > 1024 || bitmap.height > 1024) {
            // Hitung faktor untuk mengurangi ukuran gambar dengan mempertahankan aspek rasio
            val ratio = (1024.0 / bitmap.width).coerceAtMost(1024.0 / bitmap.height)
            val width = (bitmap.width * ratio).toInt()
            val height = (bitmap.height * ratio).toInt()
            Bitmap.createScaledBitmap(bitmap, width, height, true)
        } else {
            bitmap
        }

        // Konversi bitmap ke file
        val file = File(context.cacheDir, "vehicle_image_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        val byteArrayOutputStream = ByteArrayOutputStream()

        // Kompresi gambar ke JPEG dengan kualitas 80%
        compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
        outputStream.write(byteArrayOutputStream.toByteArray())
        outputStream.close()

        return file
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}