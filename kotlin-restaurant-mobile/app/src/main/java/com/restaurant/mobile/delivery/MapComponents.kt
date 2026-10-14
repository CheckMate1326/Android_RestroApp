package com.restaurant.mobile.delivery

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
 */
@Composable
fun OrderTrackingRouteMap(
    order: DeliveryOrder?,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "riderPulse"
    )

    // Calculate rider progress (0.0 to 1.0) along route based on status
    val targetProgress = when (order?.status) {
        "Confirmed" -> 0.12f
        "Preparing" -> 0.35f
        "Out for delivery" -> 0.72f
        "Delivered" -> 1.0f
        else -> 0.5f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
        label = "routeProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF16191F))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Background City Grid
            for (i in 1..4) {
                drawLine(
                    color = Color(0xFF222630),
                    start = Offset(0f, h * (i * 0.22f)),
                    end = Offset(w, h * (i * 0.22f)),
                    strokeWidth = 2f
                )
            }
            for (i in 1..4) {
                drawLine(
                    color = Color(0xFF222630),
                    start = Offset(w * (i * 0.22f), 0f),
                    end = Offset(w * (i * 0.22f), h),
                    strokeWidth = 2f
                )
            }

            // Route Endpoints: Restaurant (Start) & Customer Address (End)
            val startPoint = Offset(w * 0.18f, h * 0.75f)
            val endPoint = Offset(w * 0.82f, h * 0.25f)
            val controlPoint1 = Offset(w * 0.32f, h * 0.20f)
            val controlPoint2 = Offset(w * 0.68f, h * 0.80f)

            val fullPath = Path().apply {
                moveTo(startPoint.x, startPoint.y)
                cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    endPoint.x, endPoint.y
                )
            }

            // Draw Full Route Track (Subtle Gray)
            drawPath(
                path = fullPath,
                color = Color(0xFF333A48),
                style = Stroke(width = 8f, cap = StrokeCap.Round)
            )

            // Draw Completed Route Track (Coral Gradient)
            drawPath(
                path = fullPath,
                brush = Brush.linearGradient(listOf(RestroTokens.coral, Color(0xFFFFB088))),
                style = Stroke(
                    width = 8f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(500f * animatedProgress, 500f),
                        0f
                    )
                )
            )

            // Draw Restaurant Icon Pin
            drawCircle(color = Color(0xFF333A48), radius = 16f, center = startPoint)
            drawCircle(color = Color(0xFF101113), radius = 13f, center = startPoint)
            drawCircle(color = RestroTokens.coral, radius = 6f, center = startPoint)

            // Draw Customer Destination Pin
            drawCircle(color = RestroTokens.green.copy(alpha = 0.3f), radius = 18f, center = endPoint)
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

            // Pulsing live rider circle
            drawCircle(
                color = RestroTokens.coral.copy(alpha = 0.4f),
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

        // Overlay status pill
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            color = RestroTokens.surface.copy(alpha = 0.9f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.TwoWheeler, null, tint = RestroTokens.coral, modifier = Modifier.size(16.dp))
                Text(
                    text = when (order?.status) {
                        "Confirmed" -> "Partner assigned to order"
                        "Preparing" -> "Food being cooked at restaurant"
                        "Out for delivery" -> "Rider en route · 12 mins"
                        "Delivered" -> "Delivered to your door"
                        else -> "Live delivery tracking"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RestroTokens.text
                )
            }
        }

        // Restaurant & Destination Labels
        Text(
            text = "Restaurant",
            fontSize = 11.sp,
            color = RestroTokens.muted,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 10.dp)
        )
        Text(
            text = "Your Doorstep",
            fontSize = 11.sp,
            color = RestroTokens.green,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 16.dp, top = 10.dp)
        )
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
