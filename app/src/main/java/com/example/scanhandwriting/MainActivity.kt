package com.example.scanhandwriting

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.renderscript.ScriptGroup.Input
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.scanhandwriting.databinding.ActivityMainBinding
import com.example.scanhandwriting.databinding.BottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var bsbinding: BottomSheetBinding

    private companion object{
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101
    }

    private var imgUri : Uri? = null
    private lateinit var cameraPermission : Array<String>
    private lateinit var storagePermission : Array<String>

    private lateinit var progressDialog : ProgressDialog

    private lateinit var textRecognizer : TextRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cameraPermission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        if(SpinnerData.startRang == 0){
            textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        } else if(SpinnerData.startRang == 1){
            textRecognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        }

        val clickListener = View.OnClickListener { view ->
            when(view.id){
                R.id.cameraBtn -> {showInputImageDialog(0)}
                R.id.galleryBtn -> {showInputImageDialog(1)}
                R.id.scanBtn -> {
                    if (imgUri == null){
                        showToast("이미지 넣어주세오")
                    } else{
                        recognizeTextFromImage()
                    }
                }
            }
        }

        binding.cameraBtn.setOnClickListener(clickListener)
        binding.galleryBtn.setOnClickListener(clickListener)
        binding.scanBtn.setOnClickListener(clickListener)
    }


    // 이미지 인식, 내용 복사 ---------------------------------------------------------------------------
    private fun recognizeTextFromImage() {
        progressDialog.setMessage("이미지 준비중...")
        progressDialog.show()

        try {
            val inputImage = InputImage.fromFilePath(this, imgUri!!)

            progressDialog.setMessage("이미지 번역중...")

            val textTaskResult = textRecognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    progressDialog.dismiss()
                    val recognizedText = text.text
                    showBottomSheet(recognizedText)
                }
                .addOnFailureListener { e->
                    progressDialog.dismiss()
                    showToast("${e.message}")
                }
        } catch (e : Exception){
            progressDialog.dismiss()
            showToast("${e.message}")
        }
    }

    // 갤러리, 카메라 요청 -----------------------------------------------------------------------------
    private fun showInputImageDialog(id : Int){
        if (id == 0){
            if(checkCameraPremission()){
                pickImageCamera()
            } else{
                requestCameraPermission()
            }
        } else if (id == 1){
            if(checkStoragePremission()){
                pickImageGallery()
            } else{
                requestStoragePermission()
            }
        }
    }

    private fun pickImageGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"

        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == Activity.RESULT_OK){
                val data = result.data
                imgUri = data!!.data

                binding.imageView.setImageURI(imgUri)
            } else{
                showToast("취소 되었습니다...!")
            }
        }

    private fun pickImageCamera(){
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Sample Title")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description")

        imgUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == Activity.RESULT_OK){

                binding.imageView.setImageURI(imgUri)
            } else{
                showToast("취소 되었습니다...!")
            }
        }

    // 갤러리, 카메라 권한 확인 -------------------------------------------------------------------------
    private fun checkStoragePremission() : Boolean{
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkCameraPremission() : Boolean{

        val cameraRequest = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val storageRequest = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        return cameraRequest && storageRequest
    }

    private fun requestStoragePermission(){
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE)
    }

    private fun requestCameraPermission(){
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            CAMERA_REQUEST_CODE->{
                if(grantResults.isNotEmpty()){
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED

                    if(cameraAccepted && storageAccepted){
                        pickImageCamera()
                    }
                    else{
                        showToast("권한이 부족합니다")
                    }
                }
            }

            STORAGE_REQUEST_CODE->{
                if(grantResults.isNotEmpty()){
                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED

                    if(storageAccepted){
                        pickImageGallery()
                    }
                    else{
                        showToast("권한이 부족합니다")
                    }
                }
            }
        }
    }

    // BottomSheet 함수 -----------------------------------------------------------------------------

    var translatorKorean : Translator? = null
    var translatorEnglish : Translator? = null
    var booleanKorean = false
    var booleanEnglish = false

    private fun showBottomSheet(recognizerText : String){
        val bottomSheetDialog = BottomSheetDialog(this)

        bsbinding = BottomSheetBinding.inflate(layoutInflater)

        bottomSheetDialog.setContentView(bsbinding.root)
        bsbinding.copyContent.text = recognizerText

        bsbinding.copyBtn.setOnClickListener {
            val contentToCopy = bsbinding.copyContent.text.toString()
            copyClipBoard(contentToCopy)
        }

        when(SpinnerData.targetRang){ // 번역할 거
            0 -> {

                var englishOption : TranslatorOptions

                if (SpinnerData.startRang == 0){
                    englishOption = TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.ENGLISH)
                        .setTargetLanguage(TranslateLanguage.ENGLISH)
                        .build()
                }else{
                    englishOption = TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.KOREAN)
                        .setTargetLanguage(TranslateLanguage.ENGLISH)
                        .build()
                }

                translatorEnglish = Translation.getClient(englishOption)
                downloadModel(0)
            }
            1 -> {

                var koreanOption : TranslatorOptions

                if (SpinnerData.startRang == 1){
                    koreanOption = TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.KOREAN)
                        .setTargetLanguage(TranslateLanguage.KOREAN)
                        .build()
                }else{
                    koreanOption = TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.ENGLISH)
                        .setTargetLanguage(TranslateLanguage.KOREAN)
                        .build()
                }

                translatorKorean = Translation.getClient(koreanOption)
                downloadModel(1)
            }
        }

        bsbinding.transBtn.setOnClickListener {
            translation(SpinnerData.targetRang)
        }

        bottomSheetDialog.show()
    }

    private fun copyClipBoard(content : String){
        val clipBoard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", content)
        clipBoard.setPrimaryClip(clip)
        Toast.makeText(this, "클립보드에 복사됨: $content", Toast.LENGTH_SHORT).show()
    }

    private fun translation(target: Int){
        when(target){
            0 -> {
                if (booleanEnglish){
                    showToast("영어로 번역됨")
                    translatorEnglish!!.translate(bsbinding.copyContent.text.toString())
                        .addOnSuccessListener { string ->
                            bsbinding.copyContent.text = string
                        }
                        .addOnFailureListener { e ->
                            showToast("${e.message}")
                        }
                }
            }
            1 -> {
                if (booleanKorean){
                    showToast("한국어로 번역됨")
                    translatorKorean!!.translate(bsbinding.copyContent.text.toString())
                        .addOnSuccessListener { string ->
                            bsbinding.copyContent.text = string
                        }
                        .addOnFailureListener { e ->
                            showToast("${e.message}")
                        }
                }
            }
        }
    }

    private fun downloadModel(target : Int){
        var conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        when(target){
            0 -> {
                translatorEnglish!!.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener { conditions ->
                        booleanEnglish = true
                    }
                    .addOnFailureListener { e ->
                        booleanEnglish = false
                    }
            }
            1 -> {
                translatorKorean!!.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener { conditions ->
                        booleanKorean = true
                    }
                    .addOnFailureListener { e ->
                        booleanKorean = false
                    }
            }
        }
    }

    private fun showToast(msg : String){
        Toast.makeText(this, "${msg}", Toast.LENGTH_SHORT).show()
    }
}