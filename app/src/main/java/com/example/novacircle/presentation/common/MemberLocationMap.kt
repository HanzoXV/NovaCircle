package com.example.novacircle.presentation.common

import android.graphics.Color as AndroidColor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap

private const val OPENFREEMAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun MemberLocationMap(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
    zoom: Double = 15.0
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        MapLibre.getInstance(context)
    }

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    val mapView = remember {
        MapView(context).apply { onCreate(null) }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> mapView.onStart()
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> mapView.onStop()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.getMapAsync { map ->
                mapLibreMap = map
                map.setStyle(OPENFREEMAP_STYLE_URL) {
                    val position = LatLng(latitude, longitude)
                    map.cameraPosition = CameraPosition.Builder().target(position).zoom(zoom).build()
                    map.addMarker(MarkerOptions().position(position).icon(IconFactory.getInstance(mapView.context).defaultMarker()))
                }
            }
            mapView
        },
        update = { view ->
            mapLibreMap?.let { map ->
                val position = LatLng(latitude, longitude)
                map.markers.forEach { map.removeMarker(it) }
                map.addMarker(MarkerOptions().position(position).icon(IconFactory.getInstance(view.context).defaultMarker()))
                map.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLng(position))
            }
        }
    )
}