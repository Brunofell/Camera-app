package com.example.camera

import android.content.ContentValues
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.camera.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private val mainBinding : ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val multiplePermissionId = 14
    private val multiplePermissionNameList = if (Build.VERSION.SDK_INT >= 33) {
        arrayListOf(
            android.Manifest.permission.CAMERA,
            // android.Manifest.permission.RECORD_AUDIO
        )
    } else {
        arrayListOf(
            android.Manifest.permission.CAMERA,
            // android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }


    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var cameraSelector: CameraSelector
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    // Declare a ImageView
    private lateinit var capturedImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(mainBinding.root)

        // Inicialize a ImageView
        capturedImageView = findViewById(R.id.capturedImageView)

        if (checkMultiplePermission()) {
            startCamera()
        }

        mainBinding.flipCameraIB.setOnClickListener{
            lensFacing = if(lensFacing == CameraSelector.LENS_FACING_FRONT){
                CameraSelector.LENS_FACING_BACK
            }else{
                CameraSelector.LENS_FACING_FRONT
            }
            bindCameraUserCase()
        }
        mainBinding.captureIB.setOnClickListener{
            takePhoto()
        }
        mainBinding.flashToggleIB.setOnClickListener{
            setFlashIcon(camera)
        }

        // filtros

        // Referência ao botão "Filtros"
        val filtersButton: Button = findViewById(R.id.filtersButton)
        // Referência ao Spinner de filtros
        val filtersSpinner: Spinner = findViewById(R.id.filtersSpinner)

        // Lista de opções de filtro
        val filterOptions = listOf("Sem filtro", "Tons de Cinza", "Negativo", "Sépia", "Ajustar Brilho e Contraste", "Detecção de Bordas (Sobel)")


        // Adaptador para o Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        filtersSpinner.adapter = adapter

        // Listener para o botão "Filtros"
        filtersButton.setOnClickListener {
            // Toggle a visibilidade do Spinner
            if (filtersSpinner.visibility == View.VISIBLE) {
                filtersSpinner.visibility = View.GONE
            } else {
                filtersSpinner.visibility = View.VISIBLE
            }
        }

        // Listener para o Spinner de filtros
        filtersSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Aqui você pode adicionar lógica para aplicar o filtro selecionado
                // Por exemplo, chame um método para aplicar o filtro com base na posição selecionada
                //applyFilter(position)
                // Oculte o Spinner após a seleção
                filtersSpinner.visibility = View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Nada a fazer aqui
            }
        }

        mainBinding.flipCameraIB.setOnClickListener {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            bindCameraUserCase()
        }
        mainBinding.captureIB.setOnClickListener {
            takePhoto()
        }
        mainBinding.flashToggleIB.setOnClickListener {
            setFlashIcon(camera)
        }
    }


    private fun checkMultiplePermission(): Boolean {
        val listPermissionNeeded = arrayListOf<String>()
        for (permission in multiplePermissionNameList) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                listPermissionNeeded.add(permission)
            }
        }
        if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionNeeded.toTypedArray(),
                multiplePermissionId
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == multiplePermissionId) {
            if (grantResults.isNotEmpty()) {
                var isGrant = true
                for (element in grantResults) {
                    if (element == PackageManager.PERMISSION_DENIED) {
                        isGrant = false
                    }
                }
                if (isGrant) {
                    // here all permission granted successfully
                    startCamera()
                } else {
                    var someDenied = false
                    for (permission in permissions) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                permission
                            )
                        ) {
                            if (ActivityCompat.checkSelfPermission(
                                    this,
                                    permission
                                ) == PackageManager.PERMISSION_DENIED
                            ) {
                                someDenied = true
                            }
                        }
                    }
                    if (someDenied) {
                        // here app Setting open because all permission is not granted
                        // and permanent denied
                        appSettingOpen(this)
                    } else {
                        // here warning permission show
                        warningPermissionDialog(this) { _: DialogInterface, which: Int ->
                            when (which) {
                                DialogInterface.BUTTON_POSITIVE ->
                                    checkMultiplePermission()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUserCase()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = maxOf(width, height).toDouble() / minOf(width, height)
        return if (abs(previewRatio - 4.0 / 3.0) <= abs (previewRatio - 16.0 / 9.0)) {
            AspectRatio.RATIO_4_3
        } else{
            AspectRatio.RATIO_16_9
        }
    }

    private fun bindCameraUserCase() {
        val screenAspectRatio = aspectRatio(
            mainBinding.previewView.width,
            mainBinding.previewView.height,
        )

        val rotation = mainBinding.previewView.display.rotation

        val resolutionSelector = ResolutionSelector.Builder().setAspectRatioStrategy(
            AspectRatioStrategy(
                screenAspectRatio,
                AspectRatioStrategy.FALLBACK_RULE_AUTO
            )
        )
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setSurfaceProvider(mainBinding.previewView.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
            .build()

        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector,preview, imageCapture
            )
        }catch (e:Exception){
            e.printStackTrace()
        }

    }

    private fun setFlashIcon(camera: Camera) {
        if(camera.cameraInfo.hasFlashUnit()){
            if(camera.cameraInfo.torchState.value == 0){
                camera.cameraControl.enableTorch(true)
                mainBinding.flashToggleIB.setImageResource(R.drawable.flash_off)
            }else{
                camera.cameraControl.enableTorch(false)
                mainBinding.flashToggleIB.setImageResource(R.drawable.flash_on)
            }
        }else{
            Toast.makeText(
                this,
                "Flash is Not Available",
                Toast.LENGTH_LONG
            ).show()
            mainBinding.flashToggleIB.isEnabled = false
        }
    }

    private fun takePhoto() {
        // Configurações para salvar a imagem
        val imageFolder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Images"
        )
        if (!imageFolder.exists()) {
            imageFolder.mkdir()
        }

        val fileName = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(System.currentTimeMillis()) + ".jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Images")
            }
        }

        // Opções de saída para a imagem capturada
        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = (lensFacing == CameraSelector.LENS_FACING_FRONT)
        }
        val outputOption =
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                OutputFileOptions.Builder(
                    contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                ).setMetadata(metadata).build()
            } else {
                val imageFile = File(imageFolder, fileName)
                OutputFileOptions.Builder(imageFile)
                    .setMetadata(metadata).build()
            }

        // Captura da imagem
        imageCapture.takePicture(
            outputOption,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    savedUri?.let {
                        // Inflar o layout da visualização em miniatura
                        val previewView = layoutInflater.inflate(R.layout.layout_preview_image, null)
                        val previewImageView = previewView.findViewById<ImageView>(R.id.previewImageView)
                        val closePreviewButton = previewView.findViewById<Button>(R.id.closePreviewButton)

                        // Exibir a imagem capturada na ImageView da visualização em miniatura
                        previewImageView.setImageURI(savedUri)

                        // Criar um AlertDialog para exibir a visualização em miniatura
                        val dialog = AlertDialog.Builder(this@MainActivity)
                            .setView(previewView)
                            .setCancelable(false)
                            .create()

                        // Definir o comportamento do botão de fechar
                        closePreviewButton.setOnClickListener {
                            dialog.dismiss()
                        }

                        // Exibir o AlertDialog com a visualização em miniatura
                        dialog.show()
                    }

                    // Exibir uma mensagem informando que a captura foi bem-sucedida
                    val message = "Photo Capture Succeeded: $savedUri"
                    Toast.makeText(
                        this@MainActivity,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    // Exibir uma mensagem em caso de erro na captura da imagem
                    Toast.makeText(
                        this@MainActivity,
                        exception.message.toString(),
                        Toast.LENGTH_LONG
                    ).show()
                }

            }
        )
    }



}

