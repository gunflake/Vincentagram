package com.example.vincentagram.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.vincentagram.MainActivity
import com.example.vincentagram.R
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {
    var PICK_IMAGE_FROM_ALBUM = 0
    var storage : FirebaseStorage? = null
    var photoUri : Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        // Initial Storage
        storage = FirebaseStorage.getInstance()

        // Open the album
        var photoPickerIndent = Intent(Intent.ACTION_PICK)
        photoPickerIndent.type = "image/*"
        startActivityForResult(photoPickerIndent, PICK_IMAGE_FROM_ALBUM)

        // Upload Image
        addPhoto_btn_upload.setOnClickListener {
            contentUpload()
        }
    }

    private fun contentUpload() {
        // Make FileName

        var timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "IMAGE_" + timeStamp + "_.png"
        var storageRef = storage?.reference?.child("images")?.child(imageFileName)
        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            Toast.makeText(this, getString(R.string.upload_success), Toast.LENGTH_LONG).show()
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_FROM_ALBUM){
            if(resultCode == Activity.RESULT_OK){
                // 사진을 선택했을 떄
                photoUri = data?.data
                addPhoto_image.setImageURI(photoUri)
            }else{
                // 사진 선택을 취소했을 때
                finish()
            }
        }
    }
}
