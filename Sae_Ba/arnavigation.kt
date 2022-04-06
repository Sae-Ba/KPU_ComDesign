package com.tuk.tukar

import android.content.Intent
import android.location.Location
import android.widget.Toast
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.mapboxsdk.location.LocationComponent
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener
import com.mapbox.services.android.navigation.v5.route.RouteFetcher
import com.mapbox.services.android.navigation.v5.route.RouteListener
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.vision.VisionManager
import com.mapbox.vision.ar.VisionArManager
import com.mapbox.vision.ar.core.models.ManeuverType
import com.mapbox.vision.ar.core.models.Route
import com.mapbox.vision.ar.core.models.RoutePoint
import com.mapbox.vision.ar.view.gl.VisionArView
import com.mapbox.vision.mobile.core.interfaces.VisionEventsListener
import com.mapbox.vision.mobile.core.models.position.GeoCoordinate
import com.mapbox.vision.performance.ModelPerformance
import com.mapbox.vision.performance.ModelPerformanceMode
import com.mapbox.vision.performance.ModelPerformanceRate
import com.mapbox.vision.utils.VisionLogger
import kotlinx.android.synthetic.main.activity_arnavigation.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

open class arnavigation : baseActivity(), RouteListener, ProgressChangeListener, OffRouteListener {
    companion object {
        var TAG = arnavigation::class.java.simpleName
    }

    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var routeFetcher: RouteFetcher
    private lateinit var lastRouteProgress: RouteProgress
    private lateinit var directionsRoute: DirectionsRoute
    private var visionManagerWasInit = false
    private var navigationWasStarted = false

    private val arLocationEngine by lazy {
        LocationEngineProvider.getBestLocationEngine(this)
    }

    private val arLocationEngineRequest by lazy {
        LocationEngineRequest.Builder(0)
            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
            .setFastestInterval(1000)
            .build()
    }

    private val locationCallback by lazy {
        object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {}

            override fun onFailure(exception: Exception) {}
        }
    }

    // This dummy points will be used to build route. For real world test this needs to be changed to real values for
    // source and target locations.

    protected open fun setArRenderOptions(visionArView: VisionArView) {
        visionArView.setFenceVisible(true)
    }

    override fun onPermissionsGranted() {
        startVisionManager()
        startNavigation()
    }

    override fun initViews() {
        setContentView(R.layout.activity_arnavigation)
    }

    override fun onStart() {
        super.onStart()
        startVisionManager()
        startNavigation()
    }

    override fun onStop() {
        super.onStop()
        stopVisionManager()
        stopNavigation()
    }

    private fun startVisionManager() {
        if (allPermissionsGranted() && !visionManagerWasInit) {
            VisionManager.create()
            VisionManager.setModelPerformance(
                ModelPerformance.On(
                    ModelPerformanceMode.DYNAMIC,
                    ModelPerformanceRate.LOW
                )
            )
            VisionManager.start()
            VisionManager.visionEventsListener = object : VisionEventsListener {}
            VisionArManager.create(VisionManager)
            mapbox_ar_view.setArManager(VisionArManager)
            setArRenderOptions(mapbox_ar_view)

            visionManagerWasInit = true
        }
    }

    private fun stopVisionManager() {
        if (visionManagerWasInit) {
            VisionArManager.destroy()
            VisionManager.stop()
            VisionManager.destroy()

            visionManagerWasInit = false
        }
    }

    private fun startNavigation() {
        if (allPermissionsGranted() && !navigationWasStarted) {
            mapboxNavigation = MapboxNavigation(
                this,
                getString(R.string.mapbox_access_token),
                MapboxNavigationOptions.builder().build()
            )

            routeFetcher = RouteFetcher(this, getString(R.string.mapbox_access_token))
            routeFetcher.addRouteListener(this)

            try {
                arLocationEngine.requestLocationUpdates(
                    arLocationEngineRequest,
                    locationCallback,
                    mainLooper
                )
            } catch (se: SecurityException) {
                VisionLogger.e(TAG, se.toString())
            }

            initDirectionsRoute()

            mapboxNavigation.addOffRouteListener(this)
            mapboxNavigation.addProgressChangeListener(this)

            navigationWasStarted = true
        }
    }

    private fun stopNavigation() {
        if (navigationWasStarted) {
            arLocationEngine.removeLocationUpdates(locationCallback)

            mapboxNavigation.removeProgressChangeListener(this)
            mapboxNavigation.removeOffRouteListener(this)
            mapboxNavigation.stopNavigation()

            navigationWasStarted = false
        }
    }

    private fun initDirectionsRoute() {
        val originlng = intent.getDoubleExtra("originlong",123.5)
        val originlat = intent.getDoubleExtra("originlate",123.5)
        val destilng = intent.getDoubleExtra("destilong",123.5)
        val destilat = intent.getDoubleExtra("destilate",123.5)

        val originpoint = Point.fromLngLat(originlng, originlat)
        val destinationpoint = Point.fromLngLat(destilng, destilat)

        NavigationRoute.builder(this)
            .accessToken(getString(R.string.mapbox_access_token))
            .origin(originpoint)
            .destination(destinationpoint)
            .build()
            .getRoute(object : Callback<DirectionsResponse> {
                override fun onResponse(
                    call: Call<DirectionsResponse>,
                    response: Response<DirectionsResponse>
                ) {
                    if (response.body() == null || response.body()!!.routes().isEmpty()) {
                        return
                    }

                    directionsRoute = response.body()!!.routes()[0]
                    mapboxNavigation.startNavigation(directionsRoute)

                    VisionArManager.setRoute(
                        Route(
                            directionsRoute.getRoutePoints(),
                            directionsRoute.duration()?.toFloat() ?: 0f,
                            "",
                            ""
                        )
                    )
                }

                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

    override fun onErrorReceived(throwable: Throwable?) {
        throwable?.printStackTrace()

        mapboxNavigation.stopNavigation()
        Toast.makeText(this, "Can not calculate the route requested", Toast.LENGTH_SHORT).show()
    }

    override fun onResponseReceived(response: DirectionsResponse, routeProgress: RouteProgress?) {
        mapboxNavigation.stopNavigation()
        if (response.routes().isEmpty()) {
            Toast.makeText(this, "Can not calculate the route requested", Toast.LENGTH_SHORT).show()
        } else {
            mapboxNavigation.startNavigation(response.routes()[0])

            val route = response.routes()[0]

            VisionArManager.setRoute(
                Route(
                    route.getRoutePoints(),
                    route.duration()?.toFloat() ?: 0f,
                    "",
                    ""
                )
            )
        }
    }

    override fun onProgressChange(location: Location, routeProgress: RouteProgress) {
        lastRouteProgress = routeProgress
    }

    override fun userOffRoute(location: Location) {
        routeFetcher.findRouteFromRouteProgress(location, lastRouteProgress)
    }

    private fun DirectionsRoute.getRoutePoints(): Array<RoutePoint> {
        val routePoints = arrayListOf<RoutePoint>()
        legs()?.forEach { leg ->
            leg.steps()?.forEach { step ->
                val maneuverPoint = RoutePoint(
                    GeoCoordinate(
                        latitude = step.maneuver().location().latitude(),
                        longitude = step.maneuver().location().longitude()
                    ),
                    step.maneuver().type().mapToManeuverType()
                )
                routePoints.add(maneuverPoint)

                step.geometry()
                    ?.buildStepPointsFromGeometry()
                    ?.map { geometryStep ->
                        RoutePoint(
                            GeoCoordinate(
                                latitude = geometryStep.latitude(),
                                longitude = geometryStep.longitude()
                            )
                        )
                    }
                    ?.let { stepPoints ->
                        routePoints.addAll(stepPoints)
                    }
            }
        }

        return routePoints.toTypedArray()
    }

    private fun String.buildStepPointsFromGeometry(): List<Point> {
        return PolylineUtils.decode(this, Constants.PRECISION_6)
    }

    private fun String?.mapToManeuverType(): ManeuverType = when (this) {
        "turn" -> ManeuverType.Turn
        "depart" -> ManeuverType.Depart
        "arrive" -> ManeuverType.Arrive
        "merge" -> ManeuverType.Merge
        "on ramp" -> ManeuverType.OnRamp
        "off ramp" -> ManeuverType.OffRamp
        "fork" -> ManeuverType.Fork
        "roundabout" -> ManeuverType.Roundabout
        "exit roundabout" -> ManeuverType.RoundaboutExit
        "end of road" -> ManeuverType.EndOfRoad
        "new name" -> ManeuverType.NewName
        "continue" -> ManeuverType.Continue
        "rotary" -> ManeuverType.Rotary
        "roundabout turn" -> ManeuverType.RoundaboutTurn
        "notification" -> ManeuverType.Notification
        "exit rotary" -> ManeuverType.RoundaboutExit
        else -> ManeuverType.None
    }
}