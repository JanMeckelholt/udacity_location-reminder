package com.udacity.project4.locationreminders.savereminder

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.location.LocationManagerCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.backgroundLocationApproved
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.postNotificationsApproved
import com.udacity.project4.requestBackgroundLocationPermissions
import com.udacity.project4.requestPostNotificationPermissions
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import timber.log.Timber


class SaveReminderFragment : BaseFragment() {

    // Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private var reminder: ReminderDataItem? = null
    private lateinit var pushNotificationPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var backgroundLocationPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_save_reminder
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        setDisplayHomeAsUpEnabled(true)
        binding.viewModel = _viewModel
        registerPostNotificationRequestPermissionLauncher()
        registerBackgroundLocationRequestPermissionLauncher()
        _viewModel.locationIsEnabled.observe(viewLifecycleOwner, Observer {
            if (it == true && _viewModel.backgroundLocationAccessGranted.value == true && reminder != null) {
                saveReminder()
            }
        })
        _viewModel.backgroundLocationAccessGranted.observe(viewLifecycleOwner, Observer {
            if (it == true && _viewModel.locationIsEnabled.value == true && reminder != null) {
                saveReminder()
            }
        })
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            val directions =
                SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment()
            _viewModel.navigationCommand.value = NavigationCommand.To(directions)
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            reminder = ReminderDataItem(
                title = title,
                description = description,
                location = location,
                latitude = latitude,
                longitude = longitude
            )
            saveReminder()
        }
    }

    private fun saveReminder() {
        if (_viewModel.locationIsEnabled.value != true) {
            checkDeviceLocationEnabled()
            return
        }
        if (_viewModel.backgroundLocationAccessGranted.value != true) {
            checkBackgroundLocationPermissions()
            return
        }
        if (reminder == null) {
            return
        }
        _viewModel.validateAndSaveReminder(reminder!!, true)
        checkPostNotificationPermissions()

    }


    override fun onDestroy() {
        super.onDestroy()
        _viewModel.onClear()
    }

    private fun registerBackgroundLocationRequestPermissionLauncher() {
        backgroundLocationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                Timber.i("Background Location Permission granted!")
                _viewModel.setBackgroundLocationAccessGranted(true)
                //saveReminder()
            } else {
                Timber.i("Background Location Permission not granted!")
                Snackbar.make(
                    binding.root,
                    R.string.background_location_permission_required,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
            }
        }
    }

    private fun registerPostNotificationRequestPermissionLauncher() {
        pushNotificationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                when (granted) {
                    true -> {
                        Timber.i("Post-Permission granted")
                    }

                    false -> {
                        Snackbar.make(
                            binding.root,
                            R.string.notification_required_error,
                            Snackbar.LENGTH_INDEFINITE
                        ).setAction(R.string.settings) {
                            startActivity(Intent().apply {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        }.show()
                    }
                }
            }
    }

    private fun checkPostNotificationPermissions() {
        if (!postNotificationsApproved(requireContext())) {
            requestPostNotificationPermissions(requireContext(), pushNotificationPermissionLauncher)
        }
    }

    private fun checkBackgroundLocationPermissions(resolve: Boolean = true) {
        if (backgroundLocationApproved(requireContext())) {
            _viewModel.setBackgroundLocationAccessGranted(true)
            return
        } else {
            _viewModel.setBackgroundLocationAccessGranted(false)
            if (resolve) {
                requestBackgroundLocationPermissions(
                    requireContext(),
                    backgroundLocationPermissionLauncher
                )
                checkBackgroundLocationPermissions(false)
            }
        }
        return
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun checkDeviceLocationEnabled(resolve: Boolean = true) {
        Timber.i("checkDeviceLocation")
        if (isLocationEnabled(requireContext())) {
            _viewModel.setLocationIsEnabled(true)
        }
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(500)
            .setMaxUpdateDelayMillis(1000)
            .build()
        val locationSettingsRequest =
            LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnFailureListener { e ->
                Timber.i("onFailure addLocationRequest ${e.message} - $e")
                when {
                    e is ResolvableApiException && resolve -> {
                        try {
                            e.startResolutionForResult(
                                requireActivity(), REQUEST_TURN_DEVICE_LOCATION_ON
                            )
                        } catch (sendEx: IntentSender.SendIntentException) {
                            Timber.e("Error getting location settings resolution: ${sendEx.message}")
                        }
                    }

                    e is ApiException && e.statusCode == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        AlertDialog.Builder(requireContext()).setTitle(R.string.settings_error)
                            .setMessage(R.string.location_and_internet_required_error)
                            .setPositiveButton(R.string.settings_location) { _, _ ->
                                startActivity(Intent().apply {
                                    action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            }.setNegativeButton(android.R.string.cancel) { dialog, _ ->
                                dialog.dismiss()
                            }.setIcon(android.R.drawable.ic_dialog_info).show()
                    }

                    else -> {
                        Timber.e("Location services have to be activated")
                        Snackbar.make(
                            binding.root,
                            R.string.location_required_error,
                            Snackbar.LENGTH_INDEFINITE
                        ).setAction(R.string.settings) {
                            startActivity(Intent().apply {
                                action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        }.show()
                    }

                }
            }
            .addOnSuccessListener {
                Timber.i("Successfully activated location")
                _viewModel.setLocationIsEnabled(true)
                //saveReminder()
            }
    }

}


private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29

