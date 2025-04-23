package com.android4you.imagelabellingmlkit

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.android4you.imagelabellingmlkit.ui.theme.ImageLabellingMLkitTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

class MainActivity : ComponentActivity() {
    private val viewModel by lazy { ImageLabelingViewModel() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImageLabellingMLkitTheme {
                val navController = rememberNavController()
                NavGraph(navController, viewModel)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(navController: NavHostController, viewModel: ImageLabelingViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val executor = ContextCompat.getMainExecutor(context)

    // Permissions
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    if (cameraPermissionState.status.isGranted) {
        Box(Modifier.fillMaxSize()) {
            AndroidView(factory = { context ->
                val previewView = PreviewView(context)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    imageCapture = ImageCapture.Builder().build()

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                }, executor)

                previewView
            }, modifier = Modifier.fillMaxSize())

            Button(
                onClick = {
                    val imageCaptureInstance = imageCapture
                    if (imageCaptureInstance != null) {
                        val photoFile = File(
                            context.cacheDir,
                            "captured_${System.currentTimeMillis()}.jpg"
                        )

                        val outputOptions =
                            ImageCapture.OutputFileOptions.Builder(photoFile).build()

                        imageCaptureInstance.takePicture(
                            outputOptions,
                            executor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    // Small delay to ensure file write completes
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        val bitmap =
                                            BitmapFactory.decodeFile(photoFile.absolutePath)
                                        if (bitmap != null) {
                                            // Pass the bitmap to the ViewModel for further processing
                                            viewModel.detectLabels(bitmap)
                                            navController.popBackStack() // Navigate back to HomeScreen
                                            Log.e("CameraScreen", "Image capture success")
                                        } else {
                                            Log.e("CameraScreen", "Bitmap decoding failed.")
                                        }
                                    }, 100)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e(
                                        "CameraScreen",
                                        "Image capture failed: ${exception.message}",
                                        exception
                                    )
                                }
                            }
                        )
                    } else {
                        Log.e("CameraScreen", "ImageCapture use case is not ready.")
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text("Capture")
            }

        }
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Camera permission is required to use this feature.")
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Grant Permission")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: ImageLabelingViewModel) {
    val context = LocalContext.current
    val labels by viewModel.labels.collectAsState()
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
    val showBottomSheet by viewModel.showBottomSheet.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = uriToBitmap(context as Activity, uri)
            selectedImage = bitmap
            viewModel.detectLabels(bitmap)
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Image Labelling") })
    }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { launcher.launch("image/*") }) { Text("Gallery") }
                Button(onClick = { navController.navigate(Screen.Camera.route) }) { Text("Camera") }
            }

            selectedImage?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        if (showBottomSheet) {
            LabelBottomSheet(labels = labels, viewModel = viewModel) {
                viewModel.setShowBottomSheet(false) // hide on dismiss
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelBottomSheet(
    labels: List<ImageLabelResult>,
    viewModel: ImageLabelingViewModel,
    onDismiss: () -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Detected Labels", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))

            labels.forEach { label ->
                var translatedText by remember { mutableStateOf("...") }

                LaunchedEffect(label.text) {
                    viewModel.translateLabel(label.text) {
                        translatedText = it
                    }
                }

                Text("${label.text} → $translatedText", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
