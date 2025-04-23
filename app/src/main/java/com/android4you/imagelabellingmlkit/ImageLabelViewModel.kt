package com.android4you.imagelabellingmlkit

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ImageLabelingViewModel : ViewModel() {

    private val _labels = MutableStateFlow<List<ImageLabelResult>>(emptyList())
    val labels: StateFlow<List<ImageLabelResult>> = _labels

    fun detectLabels(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                _labels.value = labels.map {
                    ImageLabelResult(it.text, it.confidence)
                }
            }
            .addOnFailureListener {
                _labels.value = emptyList()
            }
    }
}
