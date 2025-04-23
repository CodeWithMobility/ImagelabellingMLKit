package com.android4you.imagelabellingmlkit

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

fun uriToBitmap(activity: Activity, uri: Uri): Bitmap {
    return if (Build.VERSION.SDK_INT < 28) {
        MediaStore.Images.Media.getBitmap(activity.contentResolver, uri)
    } else {
        val source = ImageDecoder.createSource(activity.contentResolver, uri)
        ImageDecoder.decodeBitmap(source)
    }
}