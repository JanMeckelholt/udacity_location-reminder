package com.udacity.project4.locationreminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.Constants
import com.udacity.project4.errorMessage
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.asDomainModell
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber

/**
 * Triggered by the Geofence.  Since we can have many Geofences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB
 *
 * Or users can add the reminders and then close the app, So our app has to run in the background
 * and handle the geofencing in the background.
 * To do that you can use https://developer.android.com/reference/android/support/v4/app/JobIntentService to do that.
 *
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    val remindersLocalRepository: RemindersLocalRepository by inject(RemindersLocalRepository::class.java)
    val viewModel: RemindersListViewModel by inject(RemindersListViewModel::class.java)
    override fun onReceive(context: Context, intent: Intent) {
        Timber.i("receiving... $intent")
        if (intent.action == Constants.ACTION_GEOFENCE_EVENT) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            if (geofencingEvent != null) {
                if (geofencingEvent.hasError()) {
                    Timber.e(errorMessage(context, geofencingEvent.errorCode))
                    return
                }
                if (geofencingEvent.triggeringGeofences == null) {
                    Timber.e(errorMessage(context, -1))
                    return
                }
                if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                    Timber.i("geofence entered")
                    val reminderId = when {
                        geofencingEvent.triggeringGeofences!!.isNotEmpty() -> geofencingEvent.triggeringGeofences!![0].requestId
                        else -> {
                            Timber.e("No Geofence Trigger Found!")
                            return
                        }
                    }
                    viewModel.viewModelScope.launch {
                        val reminder = remindersLocalRepository.getReminder(reminderId) as Result.Success
                        sendNotification(context, reminder.data.asDomainModell())
                    }
                }
            }
        }
    }
}