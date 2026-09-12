package com.yy21120.skycast.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.CircleOptions
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MarkerOptions
import com.yy21120.skycast.BuildConfig
import com.yy21120.skycast.data.ShootingSpot
import java.util.concurrent.atomic.AtomicBoolean

private const val AmapPackage = "com.autonavi.minimap"
private const val AmapPrivacyPreferences = "amap_privacy"
private const val AmapPrivacyAccepted = "accepted"
private val MapLand = Color(0xFFE8F0DF)
private val MapWater = Color(0xFF9CCBE5)
private val MapAccent = Color(0xFFE0613E)
private val MapGlassSurface = Color(0xFFF8EEF1)

@Composable
internal fun SunsetMapCard(
    spots: List<ShootingSpot>,
    selectedSpotId: String?,
    onSpotSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences(AmapPrivacyPreferences, Context.MODE_PRIVATE)
    }
    var privacyAccepted by remember { mutableStateOf(preferences.getBoolean(AmapPrivacyAccepted, false)) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    val selectedSpot = spots.firstOrNull { it.id == selectedSpotId } ?: spots.firstOrNull()
    val amapRuntimeSupported = remember {
        Build.SUPPORTED_ABIS.firstOrNull() in setOf("arm64-v8a", "armeabi-v7a")
    }

    Card(
        modifier = Modifier.testTag("wuhan-shooting-map-card"),
        colors = CardDefaults.cardColors(containerColor = MapGlassSurface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.68f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "武汉晚霞地图",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    when {
                        BuildConfig.AMAP_API_KEY.isBlank() -> "本地预览"
                        !amapRuntimeSupported -> "模拟器预览"
                        else -> "高德地图"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MapAccent,
                    fontWeight = FontWeight.Bold,
                )
            }

            when {
                BuildConfig.AMAP_API_KEY.isBlank() || !amapRuntimeSupported ->
                    SchematicWuhanMap(
                        spots = spots,
                        selectedSpotId = selectedSpotId,
                        onSpotSelected = onSpotSelected,
                    )
                !privacyAccepted -> MapPrivacyPlaceholder(onEnable = { showPrivacyDialog = true })
                else -> AmapMap(spots, onSpotSelected)
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                spots.forEach { spot ->
                    AssistChip(
                        onClick = { onSpotSelected(spot.id) },
                        label = { Text(if (spot.id == selectedSpotId) "✓ ${spot.name}" else spot.name) },
                    )
                }
            }

            selectedSpot?.let { spot ->
                Text(
                    "${spot.direction} · ${spot.description}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { openAmapRoute(context, spot, travelType = 0) }) {
                        Text("驾车导航")
                    }
                    OutlinedButton(onClick = { openAmapRoute(context, spot, travelType = 2) }) {
                        Text("步行导航")
                    }
                }
            }

        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("加载高德地图") },
            text = {
                Text(
                    "高德地图 SDK 将联网加载地图，并可能处理设备、网络和位置信息。" +
                        "SkyCast 当前不主动请求精确定位；导航会跳转到高德地图 App，由你决定是否授权定位。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        MapsInitializer.updatePrivacyShow(context.applicationContext, true, true)
                        MapsInitializer.updatePrivacyAgree(context.applicationContext, true)
                        preferences.edit().putBoolean(AmapPrivacyAccepted, true).apply()
                        privacyAccepted = true
                        showPrivacyDialog = false
                    },
                ) { Text("同意并加载") }
            },
            dismissButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("暂不加载") }
            },
        )
    }
}

@Composable
private fun MapPrivacyPlaceholder(onEnable: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFECE5EF)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("高德互动地图尚未加载", fontWeight = FontWeight.SemiBold)
            Button(onClick = onEnable) { Text("启用互动地图") }
        }
    }
}

@Composable
private fun AmapMap(
    spots: List<ShootingSpot>,
    onSpotSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val configured = remember { AtomicBoolean(false) }
    val mapView = remember {
        MapsInitializer.updatePrivacyShow(context.applicationContext, true, true)
        MapsInitializer.updatePrivacyAgree(context.applicationContext, true)
        MapView(context).apply { onCreate(Bundle()) }
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .clip(RoundedCornerShape(14.dp))
            .testTag("amap-shooting-map"),
        update = { view ->
            if (configured.compareAndSet(false, true)) {
                val map = view.map
                map.uiSettings.isZoomControlsEnabled = false
                map.uiSettings.isScaleControlsEnabled = true
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(30.57, 114.33), 11.5f))
                spots.forEach { spot ->
                    val point = LatLng(spot.latitude, spot.longitude)
                    map.addCircle(
                        CircleOptions()
                            .center(point)
                            .radius(650.0)
                            .fillColor(0x25FF7043)
                            .strokeColor(0x99E0613E.toInt())
                            .strokeWidth(2f),
                    )
                    map.addMarker(
                        MarkerOptions()
                            .position(point)
                            .title(spot.name)
                            .snippet(spot.description)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)),
                    )
                }
                map.setOnMarkerClickListener { marker ->
                    spots.firstOrNull { it.name == marker.title }?.let { onSpotSelected(it.id) }
                    marker.showInfoWindow()
                    true
                }
            }
        },
    )
}

@Composable
private fun SchematicWuhanMap(
    spots: List<ShootingSpot>,
    selectedSpotId: String?,
    onSpotSelected: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(116.dp)
            .clip(RoundedCornerShape(14.dp))
            .testTag("wuhan-shooting-map"),
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(spots) {
                    detectTapGestures { tap ->
                        val nearest = spots.minByOrNull { spot ->
                            val marker = mapOffset(
                                spot = spot,
                                width = size.width.toFloat(),
                                height = size.height.toFloat(),
                            )
                            (marker - tap).getDistance()
                        }
                        nearest?.let { spot ->
                            val marker = mapOffset(
                                spot = spot,
                                width = size.width.toFloat(),
                                height = size.height.toFloat(),
                            )
                            if ((marker - tap).getDistance() <= 32.dp.toPx()) {
                                onSpotSelected(spot.id)
                            }
                        }
                    }
                },
        ) {
            drawRect(MapLand)
            val yangtze = Path().apply {
                moveTo(-20f, size.height * 0.72f)
                cubicTo(
                    size.width * 0.24f,
                    size.height * 0.54f,
                    size.width * 0.52f,
                    size.height * 0.88f,
                    size.width + 20f,
                    size.height * 0.61f,
                )
            }
            drawPath(yangtze, MapWater, style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round))
            val hanRiver = Path().apply {
                moveTo(size.width * 0.18f, -10f)
                cubicTo(
                    size.width * 0.22f,
                    size.height * 0.35f,
                    size.width * 0.36f,
                    size.height * 0.47f,
                    size.width * 0.43f,
                    size.height * 0.69f,
                )
            }
            drawPath(hanRiver, MapWater, style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round))
            spots.forEach { spot ->
                val marker = mapOffset(spot, size.width, size.height)
                val selected = spot.id == selectedSpotId
                drawCircle(Color.White, if (selected) 12.dp.toPx() else 9.dp.toPx(), marker)
                drawCircle(if (selected) Color(0xFF55316F) else MapAccent, if (selected) 8.dp.toPx() else 6.dp.toPx(), marker)
            }
        }
        Text(
            "长江 · 汉江",
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF315C75),
        )
    }
}

private fun mapOffset(spot: ShootingSpot, width: Float, height: Float): Offset {
    val x = ((spot.longitude - 114.27) / (114.38 - 114.27)).toFloat().coerceIn(0.08f, 0.92f)
    val y = (1f - ((spot.latitude - 30.52) / (30.61 - 30.52)).toFloat()).coerceIn(0.08f, 0.92f)
    return Offset(width * x, height * y)
}

internal fun openAmapRoute(context: Context, spot: ShootingSpot, travelType: Int): Boolean {
    val routeUri = Uri.Builder()
        .scheme("amapuri")
        .authority("route")
        .appendPath("plan")
        .appendQueryParameter("sourceApplication", "SkyCast")
        .appendQueryParameter("dlat", spot.latitude.toString())
        .appendQueryParameter("dlon", spot.longitude.toString())
        .appendQueryParameter("dname", spot.name)
        .appendQueryParameter("dev", "1")
        .appendQueryParameter("t", travelType.toString())
        .build()
    val nativeIntent = Intent(Intent.ACTION_VIEW, routeUri).setPackage(AmapPackage)
    if (nativeIntent.resolveActivity(context.packageManager) != null) {
        return runCatching {
            context.startActivity(nativeIntent)
            true
        }.getOrDefault(false)
    }

    val webUri = Uri.Builder()
        .scheme("https")
        .authority("uri.amap.com")
        .appendPath("navigation")
        .appendQueryParameter("to", "${spot.longitude},${spot.latitude},${spot.name}")
        .appendQueryParameter("mode", if (travelType == 2) "walk" else "car")
        .appendQueryParameter("src", "SkyCast")
        .appendQueryParameter("coordinate", "wgs84")
        .appendQueryParameter("callnative", "1")
        .build()
    return runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        true
    }.getOrDefault(false)
}
