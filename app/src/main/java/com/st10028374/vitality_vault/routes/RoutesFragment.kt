package com.st10028374.vitality_vault.routes

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import com.google.android.gms.location.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location
import com.st10028374.vitality_vault.R
import com.st10028374.vitality_vault.routes.adapter.RoutesAdapter
import com.st10028374.vitality_vault.routes.models.LocationPoint
import com.st10028374.vitality_vault.routes.models.RouteModel
import com.st10028374.vitality_vault.routes.utils.TrackingUtils
import com.st10028374.vitality_vault.routes.viewmodel.RoutesViewModel

class RoutesFragment : Fragment() {

    // Mapbox
    private var mapView: MapView? = null
    private var polylineAnnotationManager: com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager? = null

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: Location? = null

    // ViewModel
    private lateinit var viewModel: RoutesViewModel

    // UI Components
    private lateinit var btnPlay: FloatingActionButton
    private lateinit var btnPause: MaterialButton
    private lateinit var btnStop: MaterialButton
    private lateinit var chronometer: Chronometer
    private lateinit var tvDistance: TextView
    private lateinit var tvPace: TextView
    private lateinit var tvHeartRate: TextView
    private lateinit var tvCalories: TextView
    private lateinit var trackingStatsCard: CardView
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomSection: LinearLayout
    private lateinit var routesAdapter: RoutesAdapter

    // Tracking variables
    private var chronoBase = 0L
    private var pauseOffset = 0L
    private val pathPoints = mutableListOf<Point>()

    // Permission launcher
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                initializeLocationTracking()
            }
            else -> {
                Toast.makeText(
                    requireContext(),
                    "Location permission is required for tracking",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_routes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[RoutesViewModel::class.java]

        // Initialize offline repository - CRITICAL for offline support
        viewModel.initOfflineRepository(requireContext())

        // Initialize UI
        initializeViews(view)
        setupMapbox()
        setupRecyclerView()
        setupObservers()

        // Check permissions
        checkLocationPermissions()

        // Load saved routes from offline storage
        loadRoutes()
    }

    private fun initializeViews(view: View) {
        mapView = view.findViewById(R.id.mapView)
        btnPlay = view.findViewById(R.id.btnPlay)
        btnPause = view.findViewById(R.id.btnPause)
        btnStop = view.findViewById(R.id.btnStop)
        chronometer = view.findViewById(R.id.chronometer)
        tvDistance = view.findViewById(R.id.tvDistance)
        tvPace = view.findViewById(R.id.tvPace)
        tvHeartRate = view.findViewById(R.id.tvHeartRate)
        tvCalories = view.findViewById(R.id.tvCalories)
        trackingStatsCard = view.findViewById(R.id.trackingStatsCard)
        recyclerView = view.findViewById(R.id.recyclerViewRoutes)
        progressBar = view.findViewById(R.id.progressBar)
        bottomSection = view.findViewById(R.id.bottomSection)

        btnPlay.setOnClickListener { startTracking() }
        btnPause.setOnClickListener { togglePause() }
        btnStop.setOnClickListener { showStopConfirmationDialog() }
    }

    private fun setupMapbox() {
        mapView?.getMapboxMap()?.loadStyleUri(Style.DARK) {
            mapView?.location?.updateSettings {
                enabled = true
                pulsingEnabled = true
            }

            mapView?.annotations?.let { annotationApi ->
                polylineAnnotationManager = annotationApi.createPolylineAnnotationManager()
            }
        }
    }

    private fun setupRecyclerView() {
        routesAdapter = RoutesAdapter(
            onRouteClick = { route -> showRouteDetailsDialog(route) },
            onPlayClick = { route -> startSavedRoute(route) }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = routesAdapter
    }

    private fun setupObservers() {
        viewModel.isTracking.observe(viewLifecycleOwner) { isTracking ->
            if (isTracking) {
                btnPlay.visibility = View.GONE
                bottomSection.visibility = View.GONE
                trackingStatsCard.visibility = View.VISIBLE
            } else {
                btnPlay.visibility = View.VISIBLE
                bottomSection.visibility = View.VISIBLE
                trackingStatsCard.visibility = View.GONE
            }
        }

        viewModel.isPaused.observe(viewLifecycleOwner) { isPaused ->
            if (isPaused) {
                btnPause.text = "Resume"
                btnPause.icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_media_play)
                chronometer.stop()
                pauseOffset = SystemClock.elapsedRealtime() - chronometer.base
            } else {
                btnPause.text = "Pause"
                btnPause.icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_media_pause)
                if (viewModel.isTracking.value == true) {
                    chronometer.base = SystemClock.elapsedRealtime() - pauseOffset
                    chronometer.start()
                }
            }
        }

        viewModel.trackingData.observe(viewLifecycleOwner) { data ->
            updateTrackingUI(data)
        }

        viewModel.savedRoutes.observe(viewLifecycleOwner) { routes ->
            routesAdapter.submitList(routes)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.saveComplete.observe(viewLifecycleOwner) { complete ->
            if (complete) {
                showRouteSavedDialog()
                viewModel.resetSaveComplete()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        // NEW: Observe success messages (for sync notifications)
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearSuccess()
            }
        }
    }

    private fun checkLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                initializeLocationTracking()
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initializeLocationTracking() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    handleLocationUpdate(location)
                }
            }
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                currentLocation = it
                centerMapOnLocation(it)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        viewModel.startTracking()
        pathPoints.clear()
        polylineAnnotationManager?.deleteAll()

        chronoBase = SystemClock.elapsedRealtime()
        chronometer.base = chronoBase
        chronometer.start()
        pauseOffset = 0L

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 3000
        ).apply {
            setMinUpdateIntervalMillis(1000)
            setMaxUpdateDelayMillis(5000)
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            requireActivity().mainLooper
        )
    }

    private fun togglePause() {
        if (viewModel.isPaused.value == true) {
            viewModel.resumeTracking()
        } else {
            viewModel.pauseTracking()
        }
    }

    private fun showStopConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Stop Tracking")
            .setMessage("Are you sure you want to stop tracking?")
            .setPositiveButton("Yes") { _, _ -> stopTracking() }
            .setNegativeButton("No", null)
            .show()
    }

    private fun stopTracking() {
        viewModel.stopTracking()
        chronometer.stop()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        showSaveRouteDialog()
    }

    private fun handleLocationUpdate(location: Location) {
        if (viewModel.isTracking.value == true && viewModel.isPaused.value == false) {
            currentLocation = location

            val locationPoint = LocationPoint(
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = System.currentTimeMillis()
            )
            viewModel.addLocationPoint(locationPoint)

            val point = Point.fromLngLat(location.longitude, location.latitude)
            pathPoints.add(point)
            drawPath()
            centerMapOnLocation(location)

            val elapsed = SystemClock.elapsedRealtime() - chronometer.base
            viewModel.updateTrackingData(elapsed)
        }
    }

    private fun updateTrackingUI(data: com.st10028374.vitality_vault.routes.models.TrackingData) {
        tvDistance.text = "${TrackingUtils.formatDistance(data.distance)} km"
        tvPace.text = "${TrackingUtils.formatPace(data.currentPace)} /km"
        tvHeartRate.text = "${data.estimatedHeartRate} bpm"
        tvCalories.text = "${String.format("%.0f", data.caloriesBurnt)} kcal"
    }

    private fun drawPath() {
        if (pathPoints.size < 2) return

        polylineAnnotationManager?.deleteAll()

        val lineString = LineString.fromLngLats(pathPoints)
        val polylineAnnotationOptions = PolylineAnnotationOptions()
            .withGeometry(lineString)
            .withLineColor("#7B68EE")
            .withLineWidth(5.0)

        polylineAnnotationManager?.create(polylineAnnotationOptions)
    }

    private fun centerMapOnLocation(location: Location) {
        val cameraOptions = CameraOptions.Builder()
            .center(Point.fromLngLat(location.longitude, location.latitude))
            .zoom(16.0)
            .build()

        mapView?.getMapboxMap()?.setCamera(cameraOptions)
    }

    private fun showSaveRouteDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_save_route, null)
        val etRouteName = dialogView.findViewById<EditText>(R.id.etRouteName)

        AlertDialog.Builder(requireContext())
            .setTitle("Save Route")
            .setMessage("Enter a name for your route:")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val routeName = etRouteName.text.toString().trim()
                showLoadingDialog()
                saveRoute(routeName)
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    private var loadingDialog: AlertDialog? = null

    private fun showLoadingDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_loading, null)
        loadingDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
        loadingDialog?.show()
    }

    /**
     * Save route to offline storage (Room DB)
     * Automatically syncs to Firebase when online
     */
    private fun saveRoute(routeName: String) {
        viewModel.saveRouteOffline(routeName)

        viewModel.isSaving.observe(viewLifecycleOwner) { saving ->
            if (!saving && loadingDialog?.isShowing == true) {
                loadingDialog?.dismiss()
            }
        }
    }

    private fun showRouteSavedDialog() {
        val data = viewModel.trackingData.value ?: return

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_route_summary, null)

        dialogView.findViewById<TextView>(R.id.tvSummaryDistance).text =
            "${TrackingUtils.formatDistance(data.distance)} km"
        dialogView.findViewById<TextView>(R.id.tvSummaryDuration).text =
            TrackingUtils.formatDuration(data.duration)
        dialogView.findViewById<TextView>(R.id.tvSummaryPace).text =
            "${TrackingUtils.formatPace(data.averagePace)} /km"
        dialogView.findViewById<TextView>(R.id.tvSummaryHeartRate).text =
            "${data.estimatedHeartRate} bpm"
        dialogView.findViewById<TextView>(R.id.tvSummaryCalories).text =
            "${String.format("%.0f", data.caloriesBurnt)} kcal"

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                resetTracking()
            }
            .setCancelable(false)
            .show()
    }

    private fun showRouteDetailsDialog(route: RouteModel) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_route_summary, null)

        dialogView.findViewById<TextView>(R.id.tvSummaryDistance).text =
            "${TrackingUtils.formatDistance(route.distance)} km"
        dialogView.findViewById<TextView>(R.id.tvSummaryDuration).text =
            TrackingUtils.formatDuration(route.duration)
        dialogView.findViewById<TextView>(R.id.tvSummaryPace).text =
            "${TrackingUtils.formatPace(route.averagePace)} /km"
        dialogView.findViewById<TextView>(R.id.tvSummaryHeartRate).text =
            "${route.estimatedHeartRate} bpm"
        dialogView.findViewById<TextView>(R.id.tvSummaryCalories).text =
            "${String.format("%.0f", route.caloriesBurnt)} kcal"

        AlertDialog.Builder(requireContext())
            .setTitle(route.routeName)
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .setNeutralButton("Delete") { _, _ ->
                deleteRoute(route.id)
            }
            .show()
    }

    private fun startSavedRoute(route: RouteModel) {
        Toast.makeText(requireContext(), "Starting route: ${route.routeName}", Toast.LENGTH_SHORT).show()
        startTracking()
    }

    /**
     * Delete route from offline storage
     * Also deletes from Firebase if online
     */
    private fun deleteRoute(routeId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Route")
            .setMessage("Are you sure you want to delete this route?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteRouteOffline(routeId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Load routes from offline storage (Room DB)
     * Attempts to sync first if online, then loads from local database
     */
    private fun loadRoutes() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Try to sync immediately if online
                viewModel.syncData()

                // Small delay to let sync complete
                kotlinx.coroutines.delay(500)
            } catch (e: Exception) {
                // Sync failed or offline, continue to load local data
            }

            // Load routes from local database (works offline)
            viewModel.loadRoutesOffline()
        }
    }

    /**
     * Manually trigger sync of all unsynced data
     * Optional: Can be called from a sync button
     */
    private fun manualSync() {
        Toast.makeText(requireContext(), "Syncing data...", Toast.LENGTH_SHORT).show()
        viewModel.syncData()
    }

    /**
     * Check and display unsynced count
     * Optional: Can be called to show pending items
     */
    private fun checkUnsyncedCount() {
        viewModel.getUnsyncedCount { workouts, routes ->
            if (workouts > 0 || routes > 0) {
                Toast.makeText(
                    requireContext(),
                    "Pending sync: $workouts workouts, $routes routes",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun resetTracking() {
        pathPoints.clear()
        polylineAnnotationManager?.deleteAll()
        chronometer.base = SystemClock.elapsedRealtime()
        pauseOffset = 0L
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDestroy()
        if (::fusedLocationClient.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        mapView = null
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }
}