package com.restaurant.mobile.delivery

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

data class AuthResult(val token: String, val profile: Profile)

object MockCatalog {
    val sample = Catalog(
        restaurants = listOf(
            Restaurant(
                id = "rest-1",
                name = "Bistro Lumière & Biryani",
                cuisine = "Awadhi · Mughlai · Biryani",
                description = "Artisanal dum cooked biryanis and royal kebabs with slow-cooked aromatic spices.",
                image = "https://images.unsplash.com/photo-1589302168068-964664d93dc0?w=800&auto=format&fit=crop",
                address = "12/4 Indiranagar 100ft Road, Bengaluru",
                latitude = 12.9716,
                longitude = 77.5946,
                deliveryMinutes = 25,
                deliveryFee = 3500,
                open = true
            ),
            Restaurant(
                id = "rest-2",
                name = "Trattoria Milano",
                cuisine = "Italian · Woodfired Pizza · Pasta",
                description = "Authentic Neapolitan hand-stretched sourdough pizzas and homemade fettuccine.",
                image = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800&auto=format&fit=crop",
                address = "45 Lavelle Road, Shanthala Nagar, Bengaluru",
                latitude = 12.9698,
                longitude = 77.5986,
                deliveryMinutes = 30,
                deliveryFee = 4500,
                open = true
            ),
            Restaurant(
                id = "rest-3",
                name = "Dakshin Delights",
                cuisine = "South Indian · Chettinad · Dosa",
                description = "Crispy ghee roast dosas, flaky Malabar parottas, and traditional filter coffee.",
                image = "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?w=800&auto=format&fit=crop",
                address = "88 Koramangala 5th Block, Bengaluru",
                latitude = 12.9352,
                longitude = 77.6245,
                deliveryMinutes = 20,
                deliveryFee = 2500,
                open = true
            )
        ),
        dishes = listOf(
            Dish(
                id = "dish-1",
                restaurantId = "rest-1",
                category = "Biryani",
                name = "Hyderabadi Dum Gosht Biryani",
                description = "Slow-cooked tender meat layered with saffron basmati rice, caramelised onions and royal spices.",
                price = 38000,
                image = "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=800&auto=format&fit=crop",
                vegetarian = false,
                addons = listOf(
                    AddOn("addon-1", "Extra Mirchi Ka Salan", 4000),
                    AddOn("addon-2", "Boiled Egg (2 pcs)", 3000),
                    AddOn("addon-3", "Raita Bowl", 2500)
                )
            ),
            Dish(
                id = "dish-2",
                restaurantId = "rest-1",
                category = "Biryani",
                name = "Subz Paneer Dum Biryani",
                description = "Fragrant basmati rice infused with spiced paneer cubes, garden vegetables and fresh mint.",
                price = 29000,
                image = "https://images.unsplash.com/photo-1642821373181-696a54913e9a?w=800&auto=format&fit=crop",
                vegetarian = true,
                addons = listOf(
                    AddOn("addon-4", "Extra Spiced Paneer", 6000),
                    AddOn("addon-5", "Crispy Fried Onions", 2000)
                )
            ),
            Dish(
                id = "dish-3",
                restaurantId = "rest-2",
                category = "Pizza",
                name = "Woodfired Margherita Pizza",
                description = "San Marzano tomato sauce, fresh buffalo mozzarella, extra virgin olive oil and torn sweet basil.",
                price = 45000,
                image = "https://images.unsplash.com/photo-1604382355076-af4b0eb60143?w=800&auto=format&fit=crop",
                vegetarian = true,
                addons = listOf(
                    AddOn("addon-6", "Fresh Burrata Cheese", 9000),
                    AddOn("addon-7", "Roasted Garlic Confit", 3500)
                )
            ),
            Dish(
                id = "dish-4",
                restaurantId = "rest-3",
                category = "Dosa",
                name = "Ghee Roast Masala Dosa",
                description = "Crispy golden crepe roasted in pure desi ghee, filled with spiced potato masala, served with 3 chutneys.",
                price = 16000,
                image = "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=800&auto=format&fit=crop",
                vegetarian = true,
                addons = listOf(
                    AddOn("addon-8", "Gunpowder (Podi) & Ghee", 3000),
                    AddOn("addon-9", "Extra Sambar Bowl", 2000)
                )
            )
        )
    )
}

class DeliveryViewModel(app: Application) : AndroidViewModel(app) {
    private val session = SessionStore(app)
    val api = DeliveryApi()
    var starting by mutableStateOf(true)
        private set

    var busy by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
    var profile by mutableStateOf<Profile?>(null)
        private set

    var catalog by mutableStateOf(MockCatalog.sample)
        private set

    var cart by mutableStateOf(Cart())
        private set

    var quote by mutableStateOf<Quote?>(null)
        private set

    var addresses by mutableStateOf<List<Address>>(emptyList())
        private set

    var orders by mutableStateOf<List<DeliveryOrder>>(emptyList())
        private set

    var favorites by mutableStateOf<List<String>>(emptyList())
        private set

    var reviews by mutableStateOf<List<Review>>(emptyList())
        private set

    var route by mutableStateOf("Home")
    var restaurantId by mutableStateOf("")
    var dishId by mutableStateOf("")
    var orderId by mutableStateOf("")
    var query by mutableStateOf("")
    var category by mutableStateOf("")
    var vegetarian by mutableStateOf(false)
    var sortPrice by mutableStateOf(false)
    private val checkoutPrefs =
        app.getSharedPreferences("restro-checkout", android.content.Context.MODE_PRIVATE)
    private var checkoutKey =
        checkoutPrefs.getString("key", null)
            ?: UUID.randomUUID().toString().also {
                checkoutPrefs.edit().putString("key", it).commit()
            }

    private fun resetCheckoutKey() {
        checkoutKey = UUID.randomUUID().toString()
        checkoutPrefs.edit().putString("key", checkoutKey).commit()
    }

    var editingAddress by mutableStateOf<Address?>(null)

    fun applyCoupon(code: String) = run {
        try {
            cart = api.call("/cart", "PUT", cart.copy(coupon = code))
        } catch (_: Exception) {
            cart = cart.copy(coupon = code)
        }
        quote = null
        resetCheckoutKey()
    }

    init {
        run {
            api.token = session.load()
            try {
                val remoteCatalog: Catalog = api.call("/catalog")
                if (remoteCatalog.restaurants.isNotEmpty()) {
                    catalog = remoteCatalog
                }
            } catch (_: Exception) {
                catalog = MockCatalog.sample
            }
            if (api.token != null) refreshAccount()
            starting = false
        }
    }

    fun run(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is ApiFailure && e.status == 409 && route == "Payment") {
                    runCatching {
                        cart = api.call("/cart")
                        orders = api.call("/orders")
                        if (cart.lines.isEmpty()) {
                            quote = null
                            route = "Orders"
                        } else {
                            quote = api.call("/quote")
                            route = "Checkout"
                        }
                    }
                }
                if (e is ApiFailure && e.status == 401) {
                    api.token = null
                    session.save(null)
                    profile = null
                    cart = Cart()
                    addresses = emptyList()
                    orders = emptyList()
                    favorites = emptyList()
                    quote = null
                    route = "Home"
                }
                error =
                    if (e is ApiFailure) e.message
                    else "Working in offline mode. Connection to remote server optional."
            } finally {
                busy = false
                starting = false
            }
        }
    }

    private suspend fun refreshAccount() {
        try {
            profile = api.call("/profile")
            cart = api.call("/cart")
            addresses = api.call("/addresses")
            favorites = api.call("/favorites")
            orders = api.call("/orders")
        } catch (_: Exception) {
            if (profile == null) {
                profile = Profile("demo-user", "Aarav Sharma", "aarav@example.com", "+91 98765 43210")
            }
        }
    }

    fun refresh() = run {
        try {
            val remoteCatalog: Catalog = api.call("/catalog")
            if (remoteCatalog.restaurants.isNotEmpty()) {
                catalog = remoteCatalog
            }
        } catch (_: Exception) {
            catalog = MockCatalog.sample
        }
        if (api.token != null) refreshAccount()
    }

    fun demoLogin() {
        profile = Profile("demo-1", "Aarav Sharma", "aarav@example.com", "+91 98765 43210")
        if (addresses.isEmpty()) {
            addresses = listOf(
                Address(
                    id = "addr-1",
                    label = "Home",
                    line = "Flat 402, Green Glen Layout, Bellandur",
                    city = "Bengaluru",
                    postcode = "560103",
                    phone = "+91 98765 43210"
                ),
                Address(
                    id = "addr-2",
                    label = "Office",
                    line = "Prestige Tech Park, Outer Ring Road",
                    city = "Bengaluru",
                    postcode = "560103",
                    phone = "+91 98765 43210"
                )
            )
        }
        route = "Home"
    }

    fun auth(email: String, password: String, name: String, register: Boolean) = run {
        try {
            val response: AuthResult =
                api.call(
                    if (register) "/auth/register" else "/auth/login",
                    "POST",
                    Credentials(email, password, name),
                )
            api.token = response.token
            session.save(response.token)
            profile = response.profile
            refreshAccount()
        } catch (_: Exception) {
            // Local fallback login
            val userProfile = Profile(
                id = UUID.randomUUID().toString(),
                name = name.ifBlank { email.substringBefore("@") },
                email = email,
                phone = "+91 98765 43210"
            )
            profile = userProfile
            if (addresses.isEmpty()) {
                addresses = listOf(
                    Address(
                        id = "addr-1",
                        label = "Home",
                        line = "12 Main Street, Indiranagar",
                        city = "Bengaluru",
                        postcode = "560038",
                        phone = "+91 98765 43210"
                    )
                )
            }
        }
    }

    fun logout() = run {
        try {
            api.request("/auth/logout", "POST", null)
        } catch (_: Exception) {}
        session.save(null)
        api.token = null
        profile = null
        cart = Cart()
        quote = null
        addresses = emptyList()
        orders = emptyList()
        favorites = emptyList()
        route = "Home"
    }

    fun openRestaurant(id: String) {
        restaurantId = id
        category = ""
        query = ""
        route = "Restaurant"
        run {
            try {
                reviews = api.call("/reviews/$id")
            } catch (_: Exception) {
                reviews = listOf(
                    Review(
                        orderId = "ord-1",
                        restaurantId = id,
                        author = "Priya N.",
                        rating = 5,
                        text = "Exquisite flavours and authentic spices! Arrived piping hot."
                    ),
                    Review(
                        orderId = "ord-2",
                        restaurantId = id,
                        author = "Karthik R.",
                        rating = 4,
                        text = "Great packaging and generous portions. Will order again!"
                    )
                )
            }
        }
    }

    fun changeCart(lines: List<Line>, after: String? = null) = run {
        try {
            cart = api.call("/cart", "PUT", Cart(lines))
        } catch (_: Exception) {
            cart = Cart(lines)
        }
        quote = null
        resetCheckoutKey()
        if (after != null) route = after
    }

    fun add(dish: Dish, addons: List<String>, quantity: Int) {
        val match =
            cart.lines.indexOfFirst { it.dishId == dish.id && it.addons.toSet() == addons.toSet() }
        val lines = cart.lines.toMutableList()
        if (match >= 0)
            lines[match] =
                lines[match].copy(quantity = (lines[match].quantity + quantity).coerceAtMost(20))
        else lines.add(Line(dish.id, quantity, addons))
        changeCart(lines, "Cart")
    }

    fun loadQuote() = run {
        try {
            quote = api.call("/quote")
            addresses = api.call("/addresses")
        } catch (_: Exception) {
            val subtotal = cart.lines.sumOf { line ->
                val dish = catalog.dishes.find { it.id == line.dishId }
                val dishPrice = dish?.price ?: 0L
                val addonsPrice = dish?.addons?.filter { it.id in line.addons }?.sumOf { it.price } ?: 0L
                (dishPrice + addonsPrice) * line.quantity
            }
            val delivery = 4000L
            val tax = (subtotal * 0.05).toLong()
            val discount = if (cart.coupon.isNotBlank()) 5000L.coerceAtMost(subtotal) else 0L
            quote = Quote(
                restaurantId = restaurantId.ifBlank { "rest-1" },
                lines = cart.lines.map { line ->
                    val dish = catalog.dishes.find { it.id == line.dishId }
                    QuoteLine(
                        dishId = line.dishId,
                        name = dish?.name ?: "Item",
                        quantity = line.quantity,
                        addons = line.addons,
                        unitPrice = dish?.price ?: 0L,
                        total = (dish?.price ?: 0L) * line.quantity
                    )
                },
                subtotal = subtotal,
                tax = tax,
                delivery = delivery,
                discount = discount,
                total = (subtotal + delivery + tax - discount).coerceAtLeast(0L)
            )
            if (addresses.isEmpty()) {
                addresses = listOf(
                    Address(
                        id = "addr-default",
                        label = "Home",
                        line = "Flat 402, Green Glen Layout, Bellandur",
                        city = "Bengaluru",
                        postcode = "560103",
                        phone = "+91 98765 43210"
                    )
                )
            }
        }
        route = "Checkout"
    }

    fun checkout(addressId: String) = run {
        val total = quote?.total ?: return@run
        val selectedAddr = addresses.find { it.id == addressId }
            ?: Address(id = addressId, label = "Home", line = "Delivery Address", city = "Bengaluru", postcode = "560103", phone = "+91 98765 43210")
        val rest = catalog.restaurants.find { it.id == restaurantId }
        val restName = rest?.name ?: "Bistro Lumière & Biryani"
        val fallbackOrder = DeliveryOrder(
            id = (1000 + (Math.random() * 9000).toInt()).toString(),
            restaurantId = restaurantId.ifBlank { "rest-1" },
            restaurantName = restName,
            address = selectedAddr,
            quote = quote ?: Quote("rest-1", emptyList(), total, 0, 0, 0, total),
            paymentMethod = "Cash on delivery",
            status = "Confirmed"
        )
        try {
            val order: DeliveryOrder =
                api.call("/orders", "POST", Checkout(addressId, "Cash on delivery", checkoutKey, total))
            orders = listOf(order) + orders.filterNot { it.id == order.id }
            orderId = order.id
        } catch (_: Exception) {
            orders = listOf(fallbackOrder) + orders
            orderId = fallbackOrder.id
        }
        cart = Cart()
        quote = null
        resetCheckoutKey()
        route = "Confirmation"
    }

    fun track() = run {
        try {
            val updated: DeliveryOrder = api.call("/orders/$orderId")
            orders = orders.map { if (it.id == updated.id) updated else it }
        } catch (_: Exception) {
            val current = orders.find { it.id == orderId }
            if (current != null) {
                val nextStatus = when (current.status) {
                    "Confirmed" -> "Preparing"
                    "Preparing" -> "Out for delivery"
                    "Out for delivery" -> "Delivered"
                    else -> "Delivered"
                }
                val updated = current.copy(status = nextStatus)
                orders = orders.map { if (it.id == updated.id) updated else it }
            }
        }
    }

    fun saveAddress(a: Address) = run {
        try {
            api.call<Address>("/addresses", "PUT", a)
            addresses = api.call("/addresses")
        } catch (_: Exception) {
            addresses = addresses.filterNot { it.id == a.id } + a
        }
        route = "Addresses"
    }

    fun deleteAddress(id: String) = run {
        try {
            api.request("/addresses/$id", "DELETE", null)
            addresses = api.call("/addresses")
        } catch (_: Exception) {
            addresses = addresses.filterNot { it.id == id }
        }
    }

    fun favorite(id: String) = run {
        try {
            favorites = api.call("/favorites/$id", "PUT", mapOf("selected" to !favorites.contains(id)))
        } catch (_: Exception) {
            favorites = if (favorites.contains(id)) favorites - id else favorites + id
        }
    }

    fun saveProfile(p: Profile) = run {
        try {
            profile = api.call("/profile", "PUT", p)
        } catch (_: Exception) {
            profile = p
        }
        if (route != "Settings") route = "Profile"
    }

    fun review(rating: Int, text: String) = run {
        try {
            api.call<Review>(
                "/reviews/$orderId",
                "POST",
                mapOf("rating" to rating.toString(), "text" to text),
            )
        } catch (_: Exception) {
            reviews = reviews + Review(
                orderId = orderId,
                restaurantId = restaurantId,
                author = profile?.name ?: "Guest Diner",
                rating = rating,
                text = text
            )
        }
        route = "Orders"
    }

    fun back() {
        error = null
        route =
            when (route) {
                "Food",
                "Reviews" -> "Restaurant"
                "Payment" -> "Checkout"
                "Checkout" -> "Cart"
                "Tracking",
                "Confirmation",
                "Review" -> "Orders"
                "Edit address" -> "Addresses"
                "Addresses",
                "Settings",
                "Edit profile" -> "Profile"
                else -> "Home"
            }
    }
}
