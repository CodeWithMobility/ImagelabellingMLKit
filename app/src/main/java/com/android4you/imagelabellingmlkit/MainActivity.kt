package com.android4you.imagelabellingmlkit

import android.app.Activity
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android4you.imagelabellingmlkit.ui.theme.ImageLabellingMLkitTheme

class MainActivity : ComponentActivity() {
    private val viewModel = ImageLabelingViewModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImageLabellingMLkitTheme {
                ImageLabelingScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageLabelingScreen(viewModel: ImageLabelingViewModel) {
    val context = LocalContext.current
    val labels by viewModel.labels.collectAsState()
    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = uriToBitmap(context as Activity, uri)
            selectedImage = bitmap
            viewModel.detectLabels(bitmap)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Image Labeling") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Button(onClick = { launcher.launch("image/*") }) {
                Text("Pick Image")
            }

            Spacer(modifier = Modifier.height(16.dp))

            selectedImage?.let { image ->
                Image(bitmap = image.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth().height(200.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Detected Labels:")
                labels.forEach {
                    Text("- ${it.text} (Conf: ${(it.confidence * 100).toInt()}%)")
                }
            }
        }
    }
}
