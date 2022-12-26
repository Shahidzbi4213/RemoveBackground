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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var uri: Uri?= null
    private  var rBitmap: Bitmap? = null
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
            uri?.let {
                removeBackground(it)
            } ?: "Select Image First".snack()


        }

        binding.btnSave.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            )

                rBitmap?.let {
                    saveMediaToStorage(bitmap = rBitmap)
                } ?: "Remove Background First".snack()

            else storagePermission.launch(permission)
        }
    }

    private fun saveMediaToStorage(bitmap: Bitmap?) {
            CoroutineScope(Dispatchers.IO).launch {
                val filename = "${System.currentTimeMillis()}.png"
                var fos: OutputStream? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentResolver?.also { resolver ->
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/*")
                            put(
                                MediaStore.MediaColumns.RELATIVE_PATH,
                                Environment.DIRECTORY_PICTURES
                            )
                        }
                        val imageUri: Uri? =
                            resolver.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                            )
                        fos = imageUri?.let { resolver.openOutputStream(it) }
                    }
                } else {
                    val imagesDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val image = File(imagesDir, filename)
                    fos = FileOutputStream(image)
                }
                fos?.use {
                    bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, it)
                    "Saved to Photos".snack()
                }

            }
        }

    private fun removeBackground(uri: Uri) {

        CoroutineScope(Dispatchers.IO).launch {
            val bm = contentResolver.openInputStream(uri).use { data ->
                BitmapFactory.decodeStream(data)
            }

            BackgroundRemover.bitmapForProcessing(bm, false, object : OnBackgroundChangeListener {
                override fun onSuccess(bitmap: Bitmap) {
                    rBitmap = bitmap
                    CoroutineScope(Dispatchers.Main).launch {
                        binding.bgRemove.setImageBitmap(bitmap)
                    }

                }

                override fun onFailed(exception: Exception) {
                    CoroutineScope(Dispatchers.Main).launch {
                    exception.message?.snack() ?: "Error".snack()
                    }

                }
            })
        }

    }

    private fun String.snack() = Snackbar.make(binding.root, this, Snackbar.LENGTH_SHORT).show()


}