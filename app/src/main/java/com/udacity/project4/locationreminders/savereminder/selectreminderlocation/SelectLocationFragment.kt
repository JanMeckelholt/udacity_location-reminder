package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.requestFineLocationPermissions
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.Locale


class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var permissionLauncher : ActivityResultLauncher<String>
    private var marker: Marker? = null


    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_select_location
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        registerFineLocationRequestPermissionLauncher()

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnSave.setOnClickListener {
            if (marker == null)  {
                _viewModel.showToast.value = getString(R.string.err_select_location)
            } else {
                AlertDialog.Builder(requireContext()).setTitle(R.string.select_location_alert)
                    .setMessage(R.string.location_select_question)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        _viewModel.showToast.value = getString(
                            R.string.location_selected
                        )
                        findNavController().navigateUp()
                    }.setNegativeButton(android.R.string.cancel) { dialog, _ ->
                        _viewModel.selectedPOI.value = null
                        marker?.remove()
                        marker = null
                        dialog.dismiss()
                    }.setIcon(android.R.drawable.ic_dialog_info).show()
            }
        }
        return binding.root
    }

    private fun onLocationSelected(poi: PointOfInterest) {
        _viewModel.selectedPOI.value = poi
        _viewModel.latitude.value = poi.latLng.latitude
        _viewModel.longitude.value = poi.latLng.longitude
        _viewModel.reminderSelectedLocationStr.value = poi.name
    }

    private fun onLocationSelected(latLng: LatLng, title: String) {
        _viewModel.selectedPOI.value = null
        _viewModel.latitude.value = latLng.latitude
        _viewModel.longitude.value = latLng.longitude
        _viewModel.reminderSelectedLocationStr.value = title
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }

        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }

        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }

        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        var location = FALLBACK_LOCATION
        map = googleMap
        setMapStyle(map)
        val zoomLevel = 15f
        enableMyLocation()
        fusedLocationClient.lastLocation.addOnSuccessListener(
            requireActivity(),
            OnSuccessListener<Location?> { lastLocation ->
                if (lastLocation != null) {
                    location = LatLng(lastLocation.latitude, lastLocation.longitude)
                    map.addMarker(MarkerOptions().position(location))
                }
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel))

            })
        setPoiClick(map)
        _viewModel.showSnackBar.value = getString(R.string.select_poi)
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style)
            )
            if (!success) {
                Timber.e("Style parsing failed.")
            } else Timber.i("successfully applied custom map style")
        } catch (e: Resources.NotFoundException) {
            Timber.e("Cannot find style: $e")
        }
    }

    private fun registerFineLocationRequestPermissionLauncher(){
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                when (granted) {
                    true -> {
                        enableMyLocation()
                    }
                    false -> {
                        Snackbar.make(binding.root, R.string.foreground_location_permission_required, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.settings) {
                                startActivity(Intent().apply {
                                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                })
                            }
                            .show()
                    }
                }
            }
    }


    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestFineLocationPermissions(requireContext(), permissionLauncher)
        } else {
            Timber.i("mylocation is enabled")
            map.isMyLocationEnabled = true
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            marker?.remove()
            marker = null
            marker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
            marker?.showInfoWindow()
            onLocationSelected(poi)
        }
        map.setOnMapClickListener { latLng ->
            marker?.remove()
            marker = null
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2.5f",
                latLng.latitude,
                latLng.longitude
            )
            marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
            marker?.showInfoWindow()
            onLocationSelected(latLng, snippet)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Companion.REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            }
        }
    }



    companion object {

        private const val LAT_MERCEDES_SINDELFINGEN = 48.703760052656605
        private const val LNG_MERCEDES_SINDELFINGEN = 8.988854911984626
        private val FALLBACK_LOCATION = LatLng(LAT_MERCEDES_SINDELFINGEN, LNG_MERCEDES_SINDELFINGEN)
        private const val REQUEST_LOCATION_PERMISSION = 1
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
    }
}