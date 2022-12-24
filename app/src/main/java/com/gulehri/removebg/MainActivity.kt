package com.gulehri.removebg

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import com.gulehri.removebg.databinding.ActivityMainBinding
import com.slowmac.autobackgroundremover.BackgroundRemover
import com.slowmac.autobackgroundremover.OnBackgroundChangeListener
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var uri: Uri
    private lateinit var rBitmap: Bitmap
    private val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE

    private val imagePicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        it?.let {
            binding.bgSelect.setImageURI(it)
            uri = it
        } ?: "No Image Selected".snack()
    }

    private val storagePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                saveMediaToStorage(bitmap = rBitmap)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSelect.setOnClickListener {
            binding.bgRemove.setImageResource(0)
            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnRemove.setOnClickListener {
            removeBackground(uri)

        }

        binding.btnSave.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            )

                saveMediaToStorage(bitmap = rBitmap)
            else storagePermission.launch(permission)
        }
    }

    private fun saveMediaToStorage(bitmap: Bitmap?) {
        bitmap?.let {
            val filename = "${System.currentTimeMillis()}.png"
            var fos: OutputStream? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver?.also { resolver ->
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/*")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }
                    val imageUri: Uri? =
                        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    fos = imageUri?.let { resolver.openOutputStream(it) }
                }
            } else {
                val imagesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val image = File(imagesDir, filename)
                fos = FileOutputStream(image)
            }
            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                "Saved to Photos".snack()
            }
        } ?: "Remove Background First".snack()

    }


    private fun removeBackground(it: Uri) {

        val bm = contentResolver.openInputStream(it).use { data ->
            BitmapFactory.decodeStream(data)
        }

        BackgroundRemover.bitmapForProcessing(bm, false, object : OnBackgroundChangeListener {
            override fun onSuccess(bitmap: Bitmap) {
                binding.bgRemove.setImageBitmap(bitmap)
                rBitmap = bitmap
            }

            override fun onFailed(exception: Exception) {
                exception.message?.snack() ?: "Error".snack()
            }
        })
    }

    private fun String.snack() = Snackbar.make(binding.root, this, Snackbar.LENGTH_SHORT).show()


}