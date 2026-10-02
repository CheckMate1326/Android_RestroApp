package com.restaurant.mobile.delivery

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AddressesScreen(vm: DeliveryViewModel) {
    var label by
        rememberSaveable(vm.editingAddress?.id) {
            mutableStateOf(vm.editingAddress?.label ?: "Home")
        }
    var line by
        rememberSaveable(vm.editingAddress?.id) { mutableStateOf(vm.editingAddress?.line ?: "") }
    var city by
        rememberSaveable(vm.editingAddress?.id) { mutableStateOf(vm.editingAddress?.city ?: "") }
    var pin by
        rememberSaveable(vm.editingAddress?.id) {
            mutableStateOf(vm.editingAddress?.postcode ?: "")
        }
    var phone by
        rememberSaveable(vm.editingAddress?.id) { mutableStateOf(vm.editingAddress?.phone ?: "") }
    var delete by remember { mutableStateOf<Address?>(null) }
    Page {
        Heading(if (vm.route == "Edit address") "A place for good food." else "Your addresses")
        if (vm.route == "Edit address") {
            Field("Label", label, { label = it })
            Field("Flat, building and street", line, { line = it })
            Field("City", city, { city = it })
            Field("Six-digit PIN code", pin, { pin = it })
            Field("Contact phone", phone, { phone = it })
            Action("Save address", !vm.busy) {
                vm.saveAddress(
                    Address(
                        id = vm.editingAddress?.id ?: java.util.UUID.randomUUID().toString(),
                        label = label,
                        line = line,
                        city = city,
                        postcode = pin,
                        phone = phone,
                    )
                )
            }
        } else {
            vm.addresses.forEach { a ->
                Glass(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text(a.label, fontWeight = FontWeight.Bold)
                        Text("${a.line}, ${a.city} ${a.postcode}")
                        TextButton(
                            onClick = {
                                vm.editingAddress = a
                                vm.route = "Edit address"
                            }
                        ) {
                            Text("Edit address")
                        }
                        TextButton(onClick = { delete = a }) { Text("Delete address") }
                    }
                }
            }
            if (vm.addresses.isEmpty()) Text("Save an address to make checkout easier.")
            Action("Add address") {
                vm.editingAddress = null
                vm.route = "Edit address"
            }
            if (vm.cart.lines.isNotEmpty()) Action("Return to checkout", !vm.busy, vm::loadQuote)
        }
    }
    delete?.let { a ->
        AlertDialog(
            onDismissRequest = { delete = null },
            title = { Text("Delete ${a.label}?") },
            text = {
                Text(
                    "This removes the address from your saved places. Existing orders keep their delivery address."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteAddress(a.id)
                        delete = null
                    }
                ) {
                    Text("Delete address")
                }
            },
            dismissButton = { TextButton(onClick = { delete = null }) { Text("Keep address") } },
        )
    }
}

@Composable
fun ReviewsScreen(vm: DeliveryViewModel) {
    var rating by rememberSaveable { mutableIntStateOf(5) }
    var text by rememberSaveable { mutableStateOf("") }
    Page {
        Heading(if (vm.route == "Review") "How was your meal?" else "From the table")
        if (vm.route == "Review") {
            Row {
                (1..5).forEach { n ->
                    IconButton(onClick = { rating = n }) {
                        Icon(
                            if (n <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                            "$n stars",
                            tint = RestroTokens.coral,
                        )
                    }
                }
            }
            Field("Your review", text, { text = it })
            Action("Publish review", !vm.busy && text.trim().length >= 3) {
                vm.review(rating, text)
            }
        } else {
            if (vm.reviews.isEmpty())
                Text("No reviews yet. Reviews appear after a completed delivery.")
            vm.reviews.forEach { r ->
                Text("${r.rating} / 5 · ${r.author}", fontWeight = FontWeight.Bold)
                Text(r.text)
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun ProfileScreen(vm: DeliveryViewModel) {
    val p = vm.profile ?: return
    var name by rememberSaveable(p.id) { mutableStateOf(p.name) }
    var phone by rememberSaveable(p.id) { mutableStateOf(p.phone) }
    var logout by remember { mutableStateOf(false) }
    Page {
        Heading(
            if (vm.route == "Settings") "Make yourself at home."
            else if (vm.route == "Edit profile") "Your details"
            else "Hello, ${p.name.substringBefore(' ')}."
        )
        when (vm.route) {
            "Edit profile" -> {
                Field("Name", name, { name = it })
                Field("Phone", phone, { phone = it })
                Text(p.email, color = RestroTokens.muted)
                Action("Save profile", !vm.busy) {
                    vm.saveProfile(p.copy(name = name, phone = phone))
                }
            }
            "Settings" -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Reduce transparency")
                        Text(
                            "Stronger contrast, less blur",
                            color = RestroTokens.muted,
                            fontSize = 13.sp,
                        )
                    }
                    Switch(
                        checked = p.reducedGlass,
                        onCheckedChange = { vm.saveProfile(p.copy(reducedGlass = it)) },
                        enabled = !vm.busy,
                    )
                }
                Text(
                    "Your session is encrypted on this device. Sign out to revoke access.",
                    color = RestroTokens.muted,
                )
            }
            else -> {
                Text(p.email, color = RestroTokens.muted)
                listOf("Edit profile", "Addresses", "Favorites", "Orders", "Settings").forEach {
                    route ->
                    OutlinedButton(
                        onClick = { vm.route = route },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) {
                        Text(route, Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
                TextButton(onClick = { logout = true }) { Text("Sign out") }
            }
        }
    }
    if (logout)
        AlertDialog(
            onDismissRequest = { logout = false },
            title = { Text("Sign out of Restro?") },
            text = { Text("Your saved cart and orders will be here when you sign in again.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        logout = false
                        vm.logout()
                    }
                ) {
                    Text("Sign out")
                }
            },
            dismissButton = { TextButton(onClick = { logout = false }) { Text("Stay signed in") } },
        )
}
