package com.android4you.imagelabellingmlkit

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ImageLabelingViewModel : ViewModel() {

    private val _labels = MutableStateFlow<List<ImageLabelResult>>(emptyList())
    val labels: StateFlow<List<ImageLabelResult>> = _labels

    private val _showBottomSheet = MutableStateFlow(false)
    val showBottomSheet: StateFlow<Boolean> = _showBottomSheet

    fun setShowBottomSheet(value: Boolean) {
        _showBottomSheet.value = value
    }

    fun detectLabels(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                _labels.value = labels.map {
                    ImageLabelResult(it.text, it.confidence)
                }
                _showBottomSheet.value = true // ✅ Trigger bottom sheet
            }
            .addOnFailureListener {
                _labels.value = emptyList()
                _showBottomSheet.value = false
            }
    }

    private val translator = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.HINDI)
            .build()
    )

    fun translateLabel(label: String, onResult: (String) -> Unit) {
        translator.downloadModelIfNeeded().addOnSuccessListener {
            translator.translate(label)
                .addOnSuccessListener { translated -> onResult(translated) }
                .addOnFailureListener { onResult("Translation failed") }
        }
    }
}
