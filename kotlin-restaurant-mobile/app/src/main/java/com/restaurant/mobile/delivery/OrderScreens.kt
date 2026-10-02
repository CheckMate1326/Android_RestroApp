package com.restaurant.mobile.delivery

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

@Composable
fun OrdersScreen(vm: DeliveryViewModel) {
    val selected = vm.orders.find { it.id == vm.orderId }
    val lifecycle = LocalLifecycleOwner.current
    LaunchedEffect(vm.route, vm.orderId) {
        if (vm.route == "Tracking")
            lifecycle.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    vm.track()
                    delay(15000)
                }
            }
    }
    Page {
        Heading(
            when (vm.route) {
                "Confirmation" -> "Order confirmed."
                "Tracking" -> "From kitchen\nto your door."
                else -> "Your orders"
            }
        )
        if (vm.route == "Orders") {
            if (vm.orders.isEmpty()) {
                Text("Your first delicious delivery starts here.")
                Action("Find a restaurant") { vm.route = "Home" }
            }
            vm.orders.forEach { order ->
                Glass(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(order.restaurantName, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "${order.status} · ${money(order.quote.total)}",
                            color = RestroTokens.coral,
                        )
                        Text(order.quote.lines.joinToString { "${it.quantity} × ${it.name}" })
                        Action("View order") {
                            vm.orderId = order.id
                            vm.route = "Tracking"
                        }
                    }
                }
            }
        } else
            selected?.let { order ->
                Text(order.restaurantName, style = MaterialTheme.typography.headlineMedium)
                Text("Order ${order.id.take(8).uppercase()}", color = RestroTokens.muted)
                order.events.forEach { event ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = RestroTokens.green)
                        Column {
                            Text(event.status, fontWeight = FontWeight.Bold)
                            Text(
                                java.time.Instant.parse(event.at)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .format(
                                        java.time.format.DateTimeFormatter.ofPattern(
                                            "dd MMM, h:mm a"
                                        )
                                    ),
                                color = RestroTokens.muted,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
                Text(
                    "Status updates come directly from the restaurant.",
                    color = RestroTokens.muted,
                )
                Glass(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Delivery address", fontWeight = FontWeight.Bold)
                        Text(
                            "${order.address.line}, ${order.address.city} ${order.address.postcode}"
                        )
                        Text("${order.paymentMethod} · ${order.paymentStatus}")
                        BillRow("Total", order.quote.total)
                    }
                }
                if (vm.route == "Confirmation") Action("Track order") { vm.route = "Tracking" }
                else Action("Refresh status", !vm.busy, vm::track)
                if (order.status == "Delivered") Action("Rate your meal") { vm.route = "Review" }
            }
    }
}
