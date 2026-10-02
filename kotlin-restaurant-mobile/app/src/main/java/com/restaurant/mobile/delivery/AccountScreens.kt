package com.restaurant.mobile.delivery

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
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

