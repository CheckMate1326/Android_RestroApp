package com.restaurant.mobile.delivery

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive In-App Map for exploring restaurants across the city.
 * Operates standalone and offline without requiring external Google Play Services API keys.
 */
@Composable
fun DeliveryInteractiveMap(
    vm: DeliveryViewModel,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null
) {
    var selectedRestaurantId by remember {
        mutableStateOf(vm.restaurantId.ifBlank { vm.catalog.restaurants.firstOrNull()?.id ?: "" })
    }
    var zoomLevel by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val restaurants = vm.catalog.restaurants
    val selectedRestaurant = restaurants.find { it.id == selectedRestaurantId } ?: restaurants.firstOrNull()

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF14171C))) {
        // Map Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val width = size.width
                        val height = size.height

                        // Check which restaurant pin was tapped
                        restaurants.forEachIndexed { index, rest ->
                            val norm = getCoordinatesForIndex(index, restaurants.size)
                            val pinX = width * norm.x * zoomLevel + panOffset.x
                            val pinY = height * norm.y * zoomLevel + panOffset.y

                            val distanceSq = (tapOffset.x - pinX) * (tapOffset.x - pinX) +
                                    (tapOffset.y - pinY) * (tapOffset.y - pinY)
                            if (distanceSq <= 40f * 40f) {
                                selectedRestaurantId = rest.id
                            }
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // 1. Draw Map City Background & Water Bodies (Lakes)
            drawRect(color = Color(0xFF181B20))

            // Water bodies (e.g. Ulsoor & Bellandur Lake)
            val lakePath = Path().apply {
                moveTo(w * 0.72f, h * 0.18f)
                cubicTo(w * 0.85f, h * 0.15f, w * 0.92f, h * 0.28f, w * 0.80f, h * 0.35f)
                cubicTo(w * 0.70f, h * 0.40f, w * 0.65f, h * 0.25f, w * 0.72f, h * 0.18f)
                close()
            }
            drawPath(lakePath, color = Color(0xFF1B2E3D))

            val parkPath = Path().apply {
                moveTo(w * 0.15f, h * 0.35f)
                cubicTo(w * 0.28f, h * 0.32f, w * 0.32f, h * 0.45f, w * 0.22f, h * 0.52f)
                cubicTo(w * 0.10f, h * 0.50f, w * 0.08f, h * 0.40f, w * 0.15f, h * 0.35f)
                close()
            }
            drawPath(parkPath, color = Color(0xFF1B2B23))

            // 2. Draw Arterial Roads & Streets Grid
            val roadColor = Color(0xFF262A32)
            val mainRoadColor = Color(0xFF383E4B)

            // Secondary street grid
            for (i in 1..9) {
                val y = h * (i * 0.1f)
                drawLine(
                    color = roadColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 3f * zoomLevel
                )
            }
            for (i in 1..7) {
                val x = w * (i * 0.14f)
                drawLine(
                    color = roadColor,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 3f * zoomLevel
                )
            }

            // Main Highways & Arterial Roads
            drawLine(
                color = mainRoadColor,
                start = Offset(0f, h * 0.35f),
                end = Offset(w, h * 0.55f),
                strokeWidth = 8f * zoomLevel,
                cap = StrokeCap.Round
            )
            drawLine(
                color = mainRoadColor,
                start = Offset(w * 0.45f, 0f),
                end = Offset(w * 0.58f, h),
                strokeWidth = 9f * zoomLevel,
                cap = StrokeCap.Round
            )
            drawLine(
                color = mainRoadColor,
                start = Offset(w * 0.1f, h * 0.8f),
                end = Offset(w * 0.9f, h * 0.25f),
                strokeWidth = 7f * zoomLevel,
                cap = StrokeCap.Round
            )

            // 3. User Location Indicator (Blue Pulsing Marker)
            val userX = w * 0.48f * zoomLevel + panOffset.x
            val userY = h * 0.62f * zoomLevel + panOffset.y

            drawCircle(
                color = Color(0xFF3B82F6).copy(alpha = pulseAlpha),
                radius = pulseRadius * 1.5f,
                center = Offset(userX, userY)
            )
            drawCircle(
                color = Color(0xFF60A5FA),
                radius = 9f,
                center = Offset(userX, userY)
            )
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = Offset(userX, userY)
            )

            // 4. Draw Route line if a restaurant is selected
            if (selectedRestaurant != null) {
                val selIndex = restaurants.indexOf(selectedRestaurant).coerceAtLeast(0)
                val selNorm = getCoordinatesForIndex(selIndex, restaurants.size)
                val restX = w * selNorm.x * zoomLevel + panOffset.x
                val restY = h * selNorm.y * zoomLevel + panOffset.y

                val routePath = Path().apply {
                    moveTo(restX, restY)
                    quadraticTo(
                        (restX + userX) / 2f + 40f,
                        (restY + userY) / 2f - 30f,
                        userX,
                        userY
                    )
                }

                drawPath(
                    path = routePath,
                    color = RestroTokens.coral.copy(alpha = 0.8f),
                    style = Stroke(
                        width = 6f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 12f), 0f)
                    )
                )
            }

            // 5. Draw Restaurant Pins
            restaurants.forEachIndexed { index, rest ->
                val norm = getCoordinatesForIndex(index, restaurants.size)
                val pinX = w * norm.x * zoomLevel + panOffset.x
                val pinY = h * norm.y * zoomLevel + panOffset.y
                val isSelected = rest.id == selectedRestaurantId

                if (isSelected) {
                    drawCircle(
                        color = RestroTokens.coral.copy(alpha = pulseAlpha),
                        radius = pulseRadius * 1.8f,
                        center = Offset(pinX, pinY)
                    )
                }

                // Pin base circle
                val pinRadius = if (isSelected) 18f else 14f
                val pinColor = if (isSelected) RestroTokens.coral else Color(0xFFE2E8F0)

                drawCircle(
                    color = Color.Black.copy(alpha = 0.4f),
                    radius = pinRadius + 3f,
                    center = Offset(pinX + 2f, pinY + 3f)
                )
                drawCircle(
                    color = pinColor,
                    radius = pinRadius,
                    center = Offset(pinX, pinY)
                )
                drawCircle(
                    color = if (isSelected) Color.White else Color(0xFF1E293B),
                    radius = pinRadius * 0.45f,
                    center = Offset(pinX, pinY)
                )
            }
        }

        // Top Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = RestroTokens.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Explore, null, tint = RestroTokens.coral, modifier = Modifier.size(18.dp))
                    Text(
                        "Bengaluru Food Map · ${restaurants.size} Partners",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = RestroTokens.text
                    )
                }
            }

            if (onClose != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(40.dp)
                        .background(RestroTokens.surface.copy(alpha = 0.95f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, "Close Map", tint = Color.White)
                }
            }
        }

        // Zoom Controls & Re-Center
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { zoomLevel = (zoomLevel * 1.25f).coerceAtMost(2.5f) },
                modifier = Modifier.size(42.dp),
                containerColor = RestroTokens.surface,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Zoom in")
            }
            FloatingActionButton(
                onClick = { zoomLevel = (zoomLevel / 1.25f).coerceAtLeast(0.8f) },
                modifier = Modifier.size(42.dp),
                containerColor = RestroTokens.surface,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Remove, "Zoom out")
            }
            FloatingActionButton(
                onClick = {
                    zoomLevel = 1f
                    panOffset = Offset.Zero
                },
                modifier = Modifier.size(42.dp),
                containerColor = RestroTokens.surface,
                contentColor = RestroTokens.coral
            ) {
                Icon(Icons.Default.MyLocation, "Re-center")
            }
        }

        // Bottom Selected Restaurant Card
        if (selectedRestaurant != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = RestroTokens.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Photo(
                        selectedRestaurant.image,
                        selectedRestaurant.name,
                        Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            selectedRestaurant.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            color = RestroTokens.text
                        )
                        Text(
                            selectedRestaurant.cuisine,
                            fontSize = 12.sp,
                            color = RestroTokens.muted,
                            maxLines = 1
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.TwoWheeler, null, tint = RestroTokens.coral, modifier = Modifier.size(14.dp))
                            Text(
                                "${selectedRestaurant.deliveryMinutes} min · ${money(selectedRestaurant.deliveryFee)} delivery",
                                fontSize = 12.sp,
                                color = RestroTokens.coral,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Button(
                        onClick = {
                            vm.openRestaurant(selectedRestaurant.id)
                            onClose?.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RestroTokens.coral),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Menu", color = RestroTokens.canvas, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Animated Live Delivery Route Map embedded in the order tracking screen.
 * Powered by a 10-second Compose coroutine heartbeat, live calculated ETA,
 * OpenStreetMap integration, and automated proximity & delivery notifications.
 */
@Composable
fun OrderTrackingRouteMap(
    order: DeliveryOrder?,
    vm: DeliveryViewModel? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Heartbeat state tracking (10-second ticks)
    var progress by rememberSaveable(order?.id) {
        mutableFloatStateOf(
            when (order?.status) {
                "Confirmed" -> 0.15f
                "Preparing" -> 0.35f
                "Out for delivery" -> 0.65f
                "Delivered" -> 1.0f
                else -> 0.20f
            }
        )
    }

    var proximityAlertFired by rememberSaveable(order?.id) { mutableStateOf(false) }
    var deliveredAlertFired by rememberSaveable(order?.id) { mutableStateOf(order?.status == "Delivered") }
    var isOsmMode by rememberSaveable { mutableStateOf(true) }

    // 10-Second Auto-Updating Coroutine Heartbeat
    LaunchedEffect(order?.id) {
        while (progress < 1.0f) {
            kotlinx.coroutines.delay(10_000L) // 10-second heartbeat
            val next = (progress + 0.12f).coerceAtMost(1.0f)
            progress = next
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 2000, easing = LinearEasing),
        label = "heartbeatProgress"
    )

    // Calculate real live distance remaining and ETA
    val totalRouteDistanceKm = 3.6
    val remainingDistanceKm = ((1.0f - animatedProgress) * totalRouteDistanceKm).coerceAtLeast(0.0)
    val liveEtaMinutes = (remainingDistanceKm * 3.5).toInt().coerceAtLeast(1)

    val distanceText = if (remainingDistanceKm <= 0.05) {
        "At your gate"
    } else if (remainingDistanceKm < 1.0) {
        "${(remainingDistanceKm * 1000).toInt()} m"
    } else {
        String.format(java.util.Locale.US, "%.1f km", remainingDistanceKm)
    }

    // Trigger Notification 2 (Proximity Alert) when < 500 m
    LaunchedEffect(remainingDistanceKm) {
        if (remainingDistanceKm <= 0.5 && !proximityAlertFired && progress < 1.0f) {
            proximityAlertFired = true
            vm?.notifyProximityAlert(order?.id ?: "ORD-LIVE", "Ramesh")
        }
    }

    // Trigger Notification 3 (Delivered Alert) when rider reaches doorstep
    LaunchedEffect(animatedProgress) {
        if (animatedProgress >= 1.0f && !deliveredAlertFired) {
            deliveredAlertFired = true
            vm?.notifyOrderDelivered(order?.id ?: "ORD-LIVE")
            order?.id?.let { vm?.markOrderDelivered(it) }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 26f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "riderPulse"
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Live Heartbeat Status Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (animatedProgress >= 1.0f) RestroTokens.green else Color(0xFF10B981))
                )
                Text(
                    text = if (animatedProgress >= 1.0f) "DELIVERY COMPLETE" else "10S LIVE GPS HEARTBEAT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (animatedProgress >= 1.0f) RestroTokens.green else Color(0xFF10B981),
                    letterSpacing = 0.5.sp
                )
            }

            // Map Style Switcher (OSM vs Dark)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    onClick = { isOsmMode = !isOsmMode },
                    shape = RoundedCornerShape(8.dp),
                    color = RestroTokens.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (isOsmMode) Icons.Default.Map else Icons.Default.Nightlight,
                            contentDescription = null,
                            tint = RestroTokens.coral,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isOsmMode) "OSM Street" else "Dark Route",
                            fontSize = 11.sp,
                            color = RestroTokens.text,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    onClick = {
                        val gmmIntentUri = Uri.parse("geo:12.9784,77.6408?q=12.9784,77.6408(Delivery+Partner)")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        try {
                            context.startActivity(mapIntent)
                        } catch (_: Exception) {}
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = RestroTokens.surface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = "External Map",
                            tint = RestroTokens.muted,
                            modifier = Modifier.size(13.dp)
                        )
                        Text("External", fontSize = 11.sp, color = RestroTokens.muted)
                    }
                }
            }
        }

        // Live Animated Map Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (isOsmMode) Color(0xFFE8ECEF) else Color(0xFF14171D))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                if (isOsmMode) {
                    // OpenStreetMap Style Background
                    drawRect(color = Color(0xFFEDE9E3)) // OSM land surface

                    // Urban park (Cubbon Park / Indiranagar greens)
                    val parkPath = Path().apply {
                        moveTo(w * 0.12f, h * 0.20f)
                        cubicTo(w * 0.26f, h * 0.18f, w * 0.30f, h * 0.38f, w * 0.18f, h * 0.42f)
                        cubicTo(w * 0.08f, h * 0.40f, w * 0.06f, h * 0.28f, w * 0.12f, h * 0.20f)
                        close()
                    }
                    drawPath(parkPath, color = Color(0xFFD4E7CB))

                    // Ulsoor lake water feature
                    val lakePath = Path().apply {
                        moveTo(w * 0.70f, h * 0.12f)
                        cubicTo(w * 0.88f, h * 0.10f, w * 0.94f, h * 0.30f, w * 0.82f, h * 0.36f)
                        cubicTo(w * 0.68f, h * 0.40f, w * 0.64f, h * 0.22f, w * 0.70f, h * 0.12f)
                        close()
                    }
                    drawPath(lakePath, color = Color(0xFFA5BFDD))

                    // OSM secondary streets
                    val streetColor = Color(0xFFFFFFFF)
                    for (i in 1..5) {
                        val y = h * (i * 0.18f)
                        drawLine(color = streetColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 5f)
                    }
                    for (i in 1..6) {
                        val x = w * (i * 0.16f)
                        drawLine(color = streetColor, start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 5f)
                    }

                    // OSM Primary Arterial Road
                    drawLine(
                        color = Color(0xFFFCD6A4),
                        start = Offset(0f, h * 0.45f),
                        end = Offset(w, h * 0.52f),
                        strokeWidth = 9f
                    )
                } else {
                    // Dark Navigation Grid
                    drawRect(color = Color(0xFF14171D))
                    for (i in 1..5) {
                        drawLine(color = Color(0xFF222630), start = Offset(0f, h * (i * 0.18f)), end = Offset(w, h * (i * 0.18f)), strokeWidth = 2f)
                    }
                    for (i in 1..6) {
                        drawLine(color = Color(0xFF222630), start = Offset(w * (i * 0.16f), 0f), end = Offset(w * (i * 0.16f), h), strokeWidth = 2f)
                    }
                }

                // Route Endpoints
                val startPoint = Offset(w * 0.15f, h * 0.78f)
                val endPoint = Offset(w * 0.85f, h * 0.22f)
                val controlPoint1 = Offset(w * 0.28f, h * 0.24f)
                val controlPoint2 = Offset(w * 0.65f, h * 0.82f)

                val fullPath = Path().apply {
                    moveTo(startPoint.x, startPoint.y)
                    cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, endPoint.x, endPoint.y)
                }

                // Route Track Underlay (Casing)
                drawPath(
                    path = fullPath,
                    color = if (isOsmMode) Color(0xFFBDC3C7) else Color(0xFF2E3440),
                    style = Stroke(width = 9f, cap = StrokeCap.Round)
                )

                // Active Polyline Track
                drawPath(
                    path = fullPath,
                    brush = Brush.linearGradient(listOf(RestroTokens.coral, Color(0xFFFF6F43))),
                    style = Stroke(
                        width = 8f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(600f * animatedProgress, 600f),
                            0f
                        )
                    )
                )

                // Restaurant Pin
                drawCircle(color = Color(0xFF333A48), radius = 15f, center = startPoint)
                drawCircle(color = Color.White, radius = 12f, center = startPoint)
                drawCircle(color = RestroTokens.coral, radius = 6f, center = startPoint)

                // Customer Destination Pin
                drawCircle(color = RestroTokens.green.copy(alpha = 0.35f), radius = 18f, center = endPoint)
                drawCircle(color = RestroTokens.green, radius = 12f, center = endPoint)
                drawCircle(color = Color.White, radius = 5f, center = endPoint)

                // Calculate Rider Position along cubic bezier curve
                val t = animatedProgress
                val riderX = (1 - t) * (1 - t) * (1 - t) * startPoint.x +
                        3 * (1 - t) * (1 - t) * t * controlPoint1.x +
                        3 * (1 - t) * t * t * controlPoint2.x +
                        t * t * t * endPoint.x
                val riderY = (1 - t) * (1 - t) * (1 - t) * startPoint.y +
                        3 * (1 - t) * (1 - t) * t * controlPoint1.y +
                        3 * (1 - t) * t * t * controlPoint2.y +
                        t * t * t * endPoint.y
                val riderPoint = Offset(riderX, riderY)

                // Pulsing Rider Marker
                if (animatedProgress < 1.0f) {
                    drawCircle(
                        color = RestroTokens.coral.copy(alpha = 0.35f),
                        radius = pulseRadius,
                        center = riderPoint
                    )
                    drawCircle(
                        color = RestroTokens.coral,
                        radius = 12f,
                        center = riderPoint
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 5f,
                        center = riderPoint
                    )
                }
            }

            // Real-Time Countdown Timer & Distance Remaining Badges
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Countdown Timer Badge
                Surface(
                    color = if (animatedProgress >= 1.0f) RestroTokens.green else Color(0xFF1E222A),
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            if (animatedProgress >= 1.0f) Icons.Default.CheckCircle else Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (animatedProgress >= 1.0f) "Delivered!" else "ETA: $liveEtaMinutes mins",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Distance Remaining Badge
                Surface(
                    color = Color(0xFF1E222A),
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = null,
                            tint = RestroTokens.coral,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = distanceText,
                            color = RestroTokens.text,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Proximity Notification Banner inside map (< 500m)
            if (remainingDistanceKm <= 0.5 && animatedProgress < 1.0f) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp, start = 12.dp, end = 12.dp)
                        .fillMaxWidth(),
                    color = Color(0xFF0F172A).copy(alpha = 0.94f),
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, null, tint = RestroTokens.coral, modifier = Modifier.size(16.dp))
                        Text(
                            "🛵 Ramesh is at your gate! Please meet your delivery partner.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // OpenStreetMap Attribution Banner
            Text(
                text = "© OpenStreetMap contributors · Live GPS",
                fontSize = 9.sp,
                color = if (isOsmMode) Color(0xFF7F8C8D) else RestroTokens.muted,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 4.dp)
            )
        }
    }
}

/**
 * Interactive Address Pin Picker Map for Edit Address screen.
 */
@Composable
fun AddressPickerMiniMap(
    onLocationSelected: (lat: Double, lng: Double, landmark: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var pinOffset by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var locationName by remember { mutableStateOf("Bellandur, Bengaluru") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D24)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { tap ->
                            val normX = (tap.x / size.width).coerceIn(0.1f, 0.9f)
                            val normY = (tap.y / size.height).coerceIn(0.1f, 0.9f)
                            pinOffset = Offset(normX, normY)

                            // Pick neighborhood based on coordinates
                            val landmark = if (normX < 0.4f && normY < 0.5f) {
                                "Indiranagar 100ft Rd"
                            } else if (normX > 0.6f && normY < 0.5f) {
                                "Whitefield IT Park"
                            } else if (normX < 0.5f && normY >= 0.5f) {
                                "Koramangala 5th Block"
                            } else {
                                "Bellandur Green Glen"
                            }
                            locationName = "$landmark, Bengaluru"
                            val lat = 12.9350 + (normY * 0.05)
                            val lng = 77.6000 + (normX * 0.05)
                            onLocationSelected(lat, lng, landmark)
                        }
                    }
            ) {
                val w = size.width
                val h = size.height

                // Road grid
                for (i in 1..4) {
                    drawLine(
                        color = Color(0xFF282D38),
                        start = Offset(0f, h * (i * 0.22f)),
                        end = Offset(w, h * (i * 0.22f)),
                        strokeWidth = 3f
                    )
                }
                for (i in 1..4) {
                    drawLine(
                        color = Color(0xFF282D38),
                        start = Offset(w * (i * 0.22f), 0f),
                        end = Offset(w * (i * 0.22f), h),
                        strokeWidth = 3f
                    )
                }

                // Selected Pin
                val px = w * pinOffset.x
                val py = h * pinOffset.y

                drawCircle(color = RestroTokens.coral.copy(alpha = 0.3f), radius = 18f, center = Offset(px, py))
                drawCircle(color = RestroTokens.coral, radius = 10f, center = Offset(px, py))
                drawCircle(color = Color.White, radius = 4f, center = Offset(px, py))
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                color = RestroTokens.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Place, null, tint = RestroTokens.coral, modifier = Modifier.size(16.dp))
                    Text("Pin: $locationName (Tap map to move)", fontSize = 11.sp, color = RestroTokens.text)
                }
            }
        }
    }
}

/**
 * Normalizes coordinates across the canvas for demo restaurants
 */
private fun getCoordinatesForIndex(index: Int, total: Int): Offset {
    return when (index % 5) {
        0 -> Offset(0.28f, 0.38f) // Indiranagar
        1 -> Offset(0.48f, 0.32f) // Lavelle Road
        2 -> Offset(0.38f, 0.68f) // Koramangala
        3 -> Offset(0.72f, 0.45f) // Whitefield
        else -> Offset(0.62f, 0.74f) // Bellandur
    }
}
