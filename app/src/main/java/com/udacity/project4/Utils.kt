package com.udacity.project4

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.GeofenceStatusCodes
import timber.log.Timber

fun errorMessage(context: Context, errorCode: Int): String {
    val resources = context.resources
    return when (errorCode) {
        GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> resources.getString(
            R.string.geofence_not_available
        )

        GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> resources.getString(
            R.string.geofence_too_many_geofences
        )

        GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> resources.getString(
            R.string.geofence_too_many_pending_intents
        )

        else -> resources.getString(R.string.geofence_unknown_error)
    }
}

fun postNotificationsApproved(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ))
}


fun requestPostNotificationPermissions(
    context: Context,
    requestPermissionLauncher: ActivityResultLauncher<String>
) {
    Timber.i("requesting Post Notification Permission (Build Version: ${Build.VERSION.SDK_INT})")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
        ) {
            Timber.i("launching Permission request for Post Notification")
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

fun requestFineLocationPermissions(
    context: Context,
    requestPermissionLauncher: ActivityResultLauncher<String>
) {
    if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    ) {
        Timber.i("launching Permission request for Post Notification")
        requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

fun requestBackgroundLocationPermissions(
    context: Context,
    requestPermissionLauncher: ActivityResultLauncher<String>
) {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) return
    if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    ) {
        Timber.i("launching Permission request for Background Location")
        requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }
}

fun backgroundLocationApproved(context: Context) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ))
    } else {
        true
    }

private fun foregroundLocationApproved(context: Context) =
    (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ))

@TargetApi(29)
fun foregroundAndBackgroundLocationPermissionApproved(context: Context): Boolean {
    Timber.i("checkForegroundAndBackground")
    return foregroundLocationApproved(context) && backgroundLocationApproved(context)
}