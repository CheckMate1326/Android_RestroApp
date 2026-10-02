package com.restaurant.mobile.delivery

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
fun CartScreen(vm: DeliveryViewModel) {
    Page {
        Heading("Your bag", "A few good choices.")
        if (vm.cart.lines.isEmpty()) {
            Text("Your bag is empty. Find something you'll love.")
            Action("Explore restaurants") { vm.route = "Home" }
        }
        vm.cart.lines.forEachIndexed { index, line ->
            val dish = vm.catalog.dishes.find { it.id == line.dishId }
            Glass(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        dish?.name ?: "Unavailable item",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        dish
                            ?.addons
                            ?.filter { it.id in line.addons }
                            ?.joinToString { it.name }
                            .orEmpty(),
                        color = RestroTokens.muted,
                    )
                    Quantity(
                        line.quantity,
                        {
                            if (!vm.busy)
                                vm.changeCart(
                                    vm.cart.lines.mapIndexedNotNull { i, l ->
                                        if (i != index) l
                                        else if (l.quantity > 1) l.copy(quantity = l.quantity - 1)
                                        else null
                                    }
                                )
                        },
                        {
                            if (!vm.busy)
                                vm.changeCart(
                                    vm.cart.lines.mapIndexed { i, l ->
                                        if (i == index) l.copy(quantity = l.quantity + 1) else l
                                    }
                                )
                        },
                    )
                }
            }
        }
        if (vm.cart.lines.isNotEmpty()) {
            vm.catalog.offers.forEach { offer ->
                OutlinedButton(onClick = { vm.applyCoupon(offer.code) }, enabled = !vm.busy) {
                    Text("${offer.code} · ${offer.description}")
                }
            }
            if (vm.cart.coupon.isNotBlank())
                TextButton(onClick = { vm.applyCoupon("") }) { Text("Remove ${vm.cart.coupon}") }
            Text(
                "Delivery charges and the final total are calculated at checkout.",
                color = RestroTokens.muted,
            )
            Action("Review checkout", !vm.busy, vm::loadQuote)
        }
    }
}

@Composable
fun CheckoutScreen(vm: DeliveryViewModel) {
    var addressId by rememberSaveable { mutableStateOf("") }
    val quote = vm.quote
    Page {
        Heading(if (vm.route == "Payment") "Payment" else "The last little details")
        Text("Deliver to", style = MaterialTheme.typography.headlineMedium)
        vm.addresses.forEach { a ->
            Row(
                Modifier.fillMaxWidth().clickable { addressId = a.id },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = addressId == a.id, onClick = { addressId = a.id })
                Column {
                    Text(a.label, fontWeight = FontWeight.Bold)
                    Text("${a.line}, ${a.city} ${a.postcode}", color = RestroTokens.muted)
                }
            }
        }
        TextButton(onClick = { vm.route = "Addresses" }) { Text("Manage addresses") }
        quote?.let {
            Glass(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BillRow("Items (tax included)", it.subtotal)
                    BillRow("Delivery", it.delivery)
                    if (it.discount > 0) BillRow("Offer savings", -it.discount)
                    HorizontalDivider()
                    BillRow("Total", it.total)
                }
            }
        }
        Text("Cash on delivery", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            "Pay when your order arrives. Online payments are currently unavailable.",
            color = RestroTokens.muted,
        )
        Action(
            if (vm.route == "Payment") "Place order · ${money(quote?.total ?: 0)}"
            else "Continue to payment",
            !vm.busy && quote != null && vm.addresses.any { it.id == addressId },
        ) {
            if (vm.route == "Payment") vm.checkout(addressId) else vm.route = "Payment"
        }
    }
}
