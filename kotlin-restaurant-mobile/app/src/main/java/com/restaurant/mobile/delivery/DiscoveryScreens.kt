package com.restaurant.mobile.delivery

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun AuthScreen(vm: DeliveryViewModel) {
    var authMode by rememberSaveable { mutableStateOf(0) } // 0: Fast OTP, 1: Password
    var otpType by rememberSaveable { mutableStateOf(0) } // 0: Phone, 1: Email
    var phoneInput by rememberSaveable { mutableStateOf("") }
    var emailOtpInput by rememberSaveable { mutableStateOf("") }
    var otpCode by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }

    var register by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Page {
        vm.catalog.dishes.firstOrNull()?.let {
            Photo(
                it.image,
                it.name,
                Modifier.fillMaxWidth().height(180.dp).clip(RestroTokens.shape),
            )
        }

        Heading(
            if (authMode == 0) "Fast & Secure\nSign In."
            else if (register) "Your next favourite\nstarts here."
            else "Something good\nis on its way.",
            if (authMode == 0) "Verify via instant 6-digit OTP to mobile or email."
            else if (register) "Create your Restro account."
            else "Sign in to discover, save and order.",
        )

        // Auth Mode Switcher Tab
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RestroTokens.surface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = {
                    authMode = 0
                    vm.resetOtp()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (authMode == 0) RestroTokens.coral else Color.Transparent,
                    contentColor = if (authMode == 0) Color.White else RestroTokens.muted
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Phone / OTP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Button(
                onClick = { authMode = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (authMode == 1) RestroTokens.coral else Color.Transparent,
                    contentColor = if (authMode == 1) Color.White else RestroTokens.muted
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Password", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        if (authMode == 0) {
            // OTP Authentication Flow
            if (!vm.otpSent) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = otpType == 0,
                        onClick = { otpType = 0 },
                        label = { Text("Mobile Phone") },
                        leadingIcon = { Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = otpType == 1,
                        onClick = { otpType = 1 },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, null, modifier = Modifier.size(16.dp)) }
                    )
                }

                if (otpType == 0) {
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Phone Number") },
                        placeholder = { Text("+91 98765 43210") },
                        leadingIcon = { Icon(Icons.Default.Call, null, tint = RestroTokens.coral) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = emailOtpInput,
                        onValueChange = { emailOtpInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email Address") },
                        placeholder = { Text("you@example.com") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = RestroTokens.coral) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Your Name (Optional)") },
                    placeholder = { Text("Aarav Sharma") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Action(
                    label = "Send Verification OTP",
                    enabled = !vm.busy && if (otpType == 0) phoneInput.trim().length >= 8 else emailOtpInput.contains("@")
                ) {
                    val target = if (otpType == 0) phoneInput else emailOtpInput
                    vm.sendOtp(target, isPhone = (otpType == 0))
                }
            } else {
                // OTP Sent View
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = RestroTokens.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = RestroTokens.green)
                            Text(
                                "OTP sent to ${vm.otpTarget}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        vm.otpNotificationBanner?.let { banner ->
                            Surface(
                                color = RestroTokens.coral.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Notifications, null, tint = RestroTokens.coral, modifier = Modifier.size(18.dp))
                                    Text(
                                        banner,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (vm.otpPreviewCode != null) {
                                        TextButton(
                                            onClick = { otpCode = vm.otpPreviewCode ?: "" },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Text("Autofill", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RestroTokens.coral)
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { if (it.length <= 6) otpCode = it.filter { ch -> ch.isDigit() } },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Enter 6-Digit Code") },
                            placeholder = { Text("• • • • • •") },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = RestroTokens.coral) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )

                        Action(
                            label = "Verify & Continue",
                            enabled = !vm.busy && otpCode.length == 6
                        ) {
                            vm.verifyOtp(otpCode, name)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                vm.resetOtp()
                                otpCode = ""
                            }) {
                                Text("Change Target", fontSize = 12.sp)
                            }
                            TextButton(onClick = {
                                otpCode = ""
                                vm.sendOtp(vm.otpTarget, isPhone = (otpType == 0))
                            }) {
                                Text("Resend OTP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // Password Authentication Flow
            if (register) Field("Your name", name, { name = it })
            Field("Email", email, { email = it })
            Field("Password · at least 10 characters", password, { password = it }, true)
            Action(
                if (register) "Create account" else "Sign in",
                !vm.busy &&
                    email.isNotBlank() &&
                    password.length >= 10 &&
                    (!register || name.trim().length >= 2),
            ) {
                vm.auth(email, password, name, register)
            }
            TextButton(onClick = { register = !register }) {
                Text(
                    if (register) "Already have an account? Sign in"
                    else "New to Restro? Create an account"
                )
            }
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.1f),
            modifier = Modifier.padding(vertical = 4.dp)
        )

        OutlinedButton(
            onClick = { vm.demoLogin() },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Fastfood, null)
            Spacer(Modifier.width(8.dp))
            Text("Explore as Demo Diner", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun Discovery(vm: DeliveryViewModel) {
    val restaurant = vm.catalog.restaurants.find { it.id == vm.restaurantId }
    val detail = vm.route == "Restaurant"
    val dishes =
        vm.catalog.dishes
            .filter {
                (!detail || it.restaurantId == vm.restaurantId) &&
                    (!vm.vegetarian || it.vegetarian) &&
                    (vm.category.isBlank() || it.category == vm.category) &&
                    (vm.query.isBlank() || it.name.contains(vm.query, true))
            }
            .let { if (vm.sortPrice) it.sortedBy { d -> d.price } else it }
    val restaurants =
        vm.catalog.restaurants.filter {
            (vm.route != "Favorites" || it.id in vm.favorites) &&
                (vm.query.isBlank() ||
                    it.name.contains(vm.query, true) ||
                    it.cuisine.contains(vm.query, true) ||
                    vm.catalog.dishes.any { d ->
                        d.restaurantId == it.id && d.name.contains(vm.query, true)
                    })
        }
    LazyColumn(
        Modifier.fillMaxSize().testTag("discovery-list"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Heading(
                if (detail) restaurant?.name ?: "Restaurant"
                else if (vm.route == "Favorites") "Your favourites" else "What sounds\ngood today?",
                if (detail) restaurant?.description else "Discover a delicious reason to stay in.",
            )
        }
        if (detail && restaurant != null)
            item {
                RestaurantCard(restaurant, vm, false)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { vm.route = "Reviews" },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFB800), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reviews (${vm.reviews.size})")
                    }
                    Button(
                        onClick = { vm.route = "Map" },
                        colors = ButtonDefaults.buttonColors(containerColor = RestroTokens.surface),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Map, null, tint = RestroTokens.coral, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("View on Map", color = RestroTokens.text)
                    }
                }
            }

        if (!detail && vm.route != "Favorites") {
            item {
                Surface(
                    onClick = { vm.route = "Map" },
                    color = RestroTokens.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(RestroTokens.coral.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Explore, null, tint = RestroTokens.coral, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Interactive City Food Map", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Explore restaurants & delivery radius live on map", fontSize = 12.sp, color = RestroTokens.muted)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = RestroTokens.muted)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = vm.query,
                onValueChange = { vm.query = it },
                placeholder = {
                    Text(if (detail) "Search this menu" else "Restaurants, dishes, cravings")
                },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (vm.query.isNotEmpty())
                        IconButton(onClick = { vm.query = "" }) {
                            Icon(Icons.Default.Close, "Clear search")
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
            )
        }
        item {
            val availableCategories = if (detail) {
                vm.catalog.dishes.filter { it.restaurantId == vm.restaurantId }.map { it.category }.distinct()
            } else {
                vm.catalog.dishes.map { it.category }.distinct()
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = vm.vegetarian,
                        onClick = { vm.vegetarian = !vm.vegetarian },
                        label = { Text("Vegetarian") },
                    )
                }
                item {
                    FilterChip(
                        selected = vm.sortPrice,
                        onClick = { vm.sortPrice = !vm.sortPrice },
                        label = { Text("Price: low to high") },
                    )
                }
                item {
                    FilterChip(
                        selected = vm.category.isEmpty(),
                        onClick = { vm.category = "" },
                        label = { Text("All") },
                    )
                }
                items(availableCategories) { category ->
                    FilterChip(
                        selected = vm.category == category,
                        onClick = { vm.category = category },
                        label = { Text(category) },
                    )
                }
            }
        }
        if (!detail) {
            items(restaurants, key = { "restaurant-${it.id}" }) { RestaurantCard(it, vm, true) }
            if (restaurants.isEmpty())
                item {
                    Text(
                        if (vm.route == "Favorites")
                            "Save a restaurant using its heart button to find it here."
                        else "No restaurants found. Try another search.",
                        color = RestroTokens.muted,
                    )
                }
        }
        if (vm.route != "Favorites") {
            item {
                Text(
                    if (detail) "On the menu" else "Find your next favourite",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            items(dishes, key = { "dish-${it.id}" }) { dish ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable {
                            vm.dishId = dish.id
                            vm.restaurantId = dish.restaurantId
                            vm.route = "Food"
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (dish.vegetarian) "● VEGETARIAN" else "NON-VEGETARIAN",
                            fontSize = 10.sp,
                            color = if (dish.vegetarian) RestroTokens.green else RestroTokens.muted,
                        )
                        Text(dish.name, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        Text(
                            dish.description,
                            color = RestroTokens.muted,
                            maxLines = 2,
                            fontSize = 13.sp,
                        )
                        Text(money(dish.price), fontWeight = FontWeight.Bold)
                        Text(
                            if (dish.available) "Customize +" else "Unavailable",
                            color = RestroTokens.coral,
                        )
                    }
                    Photo(dish.image, dish.name, Modifier.size(116.dp).clip(RestroTokens.shape))
                }
                HorizontalDivider(color = Color.White.copy(alpha = .08f))
            }
            if (dishes.isEmpty())
                item {
                    Text("No dishes match these filters.")
                    TextButton(
                        onClick = {
                            vm.query = ""
                            vm.category = ""
                            vm.vegetarian = false
                        }
                    ) {
                        Text("Clear filters")
                    }
                }
        }
    }
}

@Composable
fun RestaurantCard(r: Restaurant, vm: DeliveryViewModel, clickable: Boolean) {
    val layer = rememberGraphicsLayer()
    val backdrop = remember(layer) { Backdrop(layer) }
    Box(
        Modifier.fillMaxWidth()
            .height(268.dp)
            .clip(RestroTokens.shape)
            .then(if (clickable) Modifier.clickable { vm.openRestaurant(r.id) } else Modifier)
    ) {
        Photo(r.image, r.name, Modifier.fillMaxSize().capture(backdrop))
        FilledIconButton(
            onClick = { vm.favorite(r.id) },
            enabled = !vm.busy,
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
            colors =
                IconButtonDefaults.filledIconButtonColors(
                    containerColor = RestroTokens.canvas.copy(alpha = .7f)
                ),
        ) {
            Icon(
                if (r.id in vm.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                if (r.id in vm.favorites) "Remove favourite" else "Save restaurant",
                tint = RestroTokens.coral,
            )
        }
        Glass(
            Modifier.align(Alignment.BottomCenter).padding(10.dp).fillMaxWidth(),
            backdrop,
            vm.profile?.reducedGlass == true,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(r.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(r.cuisine, fontSize = 13.sp)
                Text(
                    if (r.open)
                        "${r.deliveryMinutes}–${r.deliveryMinutes+10} min · ${money(r.deliveryFee)} delivery"
                    else "Currently closed",
                    color = RestroTokens.muted,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

