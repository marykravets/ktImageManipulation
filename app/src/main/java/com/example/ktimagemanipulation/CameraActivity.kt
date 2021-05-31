package com.example.ktimagemanipulation

import android.annotation.TargetApi
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import org.opencv.android.CameraBridgeViewBase
import java.util.*

open class CameraActivity : Activity() {
    protected open val cameraViewList: List<CameraBridgeViewBase?>
        get() {
            return ArrayList<CameraBridgeViewBase?>()
        }

    protected fun onCameraPermissionGranted() {
        val cameraViews = cameraViewList
        if (cameraViews != null) {
            for (cameraBridgeViewBase in cameraViews) {
                cameraBridgeViewBase?.setCameraPermissionGranted()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        var havePermission = true
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission("android.permission.CAMERA") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf("android.permission.CAMERA"), 200)
            havePermission = false
        }
        if (havePermission) {
            onCameraPermissionGranted()
        }
    }

    @TargetApi(23)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == 200 && grantResults.size > 0 && grantResults[0] == 0) {
            onCameraPermissionGranted()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
