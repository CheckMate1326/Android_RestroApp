package com.restaurant.mobile.delivery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DeliveryLandmarkPreset(
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val isDemoOutOfRange: Boolean = false
)

/**
 * Bottom Sheet for selecting the active delivery landmark / location.
 * Drives real-time proximity sorting, distance calculations, and out-of-range indicators.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandmarkPickerSheet(
    vm: DeliveryViewModel,
    onDismiss: () -> Unit
) {
    val presets = listOf(
        DeliveryLandmarkPreset(
            name = "Indiranagar, Bengaluru",
            description = "100ft Road · Central Hub",
            latitude = 12.9784,
            longitude = 77.6408
        ),
        DeliveryLandmarkPreset(
            name = "Koramangala 5th Block",
            description = "Jyoti Nivas College Road",
            latitude = 12.9352,
            longitude = 77.6245
        ),
        DeliveryLandmarkPreset(
            name = "Lavelle Road, Shanthala Nagar",
            description = "Near UB City & Vittal Mallya",
            latitude = 12.9698,
            longitude = 77.5986
        ),
        DeliveryLandmarkPreset(
            name = "Bellandur, Bengaluru",
            description = "Green Glen Layout · Outer Ring Rd",
            latitude = 12.9304,
            longitude = 77.6784
        ),
        DeliveryLandmarkPreset(
            name = "Whitefield, EPIP Zone",
            description = "12 km away · Demonstrates Out-of-Range Guard",
            latitude = 12.9863,
            longitude = 77.7338,
            isDemoOutOfRange = true
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = RestroTokens.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = RestroTokens.muted) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, null, tint = RestroTokens.coral, modifier = Modifier.size(18.dp))
                        Text("Select Delivery Landmark", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                    Text("Nearest restaurants recalculate live by distance", fontSize = 12.sp, color = RestroTokens.muted)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Dismiss")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                    val isSelected = vm.currentLandmark.contains(preset.name.substringBefore(","))
                    Surface(
                        onClick = {
                            vm.setDeliveryLandmark(preset.name, preset.latitude, preset.longitude)
                            onDismiss()
                        },
                        color = if (isSelected) RestroTokens.coral.copy(alpha = 0.15f) else Color(0xFF161920),
                        shape = RoundedCornerShape(14.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, RestroTokens.coral) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) RestroTokens.coral else Color(0xFF232832)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Place,
                                    null,
                                    tint = if (isSelected) Color.White else RestroTokens.muted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(preset.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (preset.isDemoOutOfRange) {
                                        Surface(
                                            color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                "OUT OF RANGE DEMO",
                                                color = Color(0xFFEF4444),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(preset.description, fontSize = 12.sp, color = RestroTokens.muted)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.CheckCircle, null, tint = RestroTokens.coral)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
