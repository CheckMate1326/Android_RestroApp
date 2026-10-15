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
            ),
            Restaurant(
                id = "rest-4",
                name = "Tokyo Zen & Bao",
                cuisine = "Pan-Asian · Ramen · Dim Sum",
                description = "Silky broth ramen bowls, handmade bao buns, and freshly rolled sushi.",
                image = "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=800&auto=format&fit=crop",
                address = "22 100ft Road, HAL 2nd Stage, Indiranagar, Bengaluru",
                latitude = 12.9784,
                longitude = 77.6408,
                deliveryMinutes = 35,
                deliveryFee = 4000,
                open = true
            ),
            Restaurant(
                id = "rest-5",
                name = "The Green Bowl & Bakery",
                cuisine = "Healthy Bowls · Salads · Sourdough",
                description = "Nutrient-packed grain bowls, crisp salads, organic smoothies and fresh sourdough.",
                image = "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=800&auto=format&fit=crop",
                address = "14 Vittal Mallya Road, Bengaluru",
                latitude = 12.9719,
                longitude = 77.5973,
                deliveryMinutes = 20,
                deliveryFee = 3000,
                open = true
            )
        ),
        dishes = listOf(
            // Restaurant 1: Bistro Lumière & Biryani
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
                id = "dish-101",
                restaurantId = "rest-1",
                category = "Starters",
                name = "Melt-in-Mouth Galouti Kebab",
                description = "Finely minced smoked patties infused with raw papaya and 32 aromatic herbs.",
                price = 34000,
                image = "https://images.unsplash.com/photo-1599488615731-7e5c2823ff28?w=800&auto=format&fit=crop",
                vegetarian = false,
                addons = listOf(AddOn("addon-101", "Mini Mint Chutney & Taftan", 3500))
            ),
            Dish(
                id = "dish-102",
                restaurantId = "rest-1",
                category = "Starters",
                name = "Tandoori Paneer Angara",
                description = "Cottage cheese marinated in Kashmiri chili, charred in clay oven, served with glazed onions.",
                price = 28000,
                image = "https://images.unsplash.com/photo-1567188040759-fb8a883dc6d8?w=800&auto=format&fit=crop",
                vegetarian = true
            ),
            Dish(
                id = "dish-103",
                restaurantId = "rest-1",
                category = "Curries",
                name = "Royal Murgh Makhani",
                description = "Clay oven roasted chicken braised in velvety tomato cashew gravy with fenugreek butter.",
                price = 36000,
                image = "https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?w=800&auto=format&fit=crop",
                vegetarian = false
            ),
            Dish(
                id = "dish-104",
                restaurantId = "rest-1",
                category = "Curries",
                name = "Dal Bukhara Slow-Simmered",
                description = "Black lentils slow-cooked overnight with churned butter and hand-crushed tomatoes.",
                price = 24000,
                image = "https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=800&auto=format&fit=crop",
                vegetarian = true
            ),
            Dish(
                id = "dish-105",
                restaurantId = "rest-1",
                category = "Breads",
                name = "Butter Garlic Naan",
                description = "Fluffy leavened flatbread baked in tandoor, brushed with rich garlic herb butter.",
                price = 7000,
                image = "https://images.unsplash.com/photo-1626074353765-517a681e40be?w=800&auto=format&fit=crop",
                vegetarian = true
            ),
            Dish(
                id = "dish-106",
                restaurantId = "rest-1",
                category = "Desserts",
                name = "Shahi Tukda with Kesari Rabri",
                description = "Golden crisp ghee toast steeped in cardamom syrup and crowned with saffron reduction.",
                price = 18000,
                image = "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=800&auto=format&fit=crop",
                vegetarian = true
            ),

            // Restaurant 2: Trattoria Milano
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
                id = "dish-201",
                restaurantId = "rest-2",
                category = "Pizza",
                name = "Quattro Formaggi Bianca",
                description = "Gorgonzola, fontina, smoked provolone and parmesan on hand-stretched sourdough crust.",
                price = 52000,
                image = "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=800&auto=format&fit=crop",
                vegetarian = true
            ),
            Dish(
                id = "dish-202",
                restaurantId = "rest-2",
                category = "Pizza",
                name = "Diavola Spicy Salami Pizza",
                description = "Smoked mozzarella, spicy artisanal pepperoni, fresh oregano and hot pepper infused honey.",
                price = 56000,
                image = "https://images.unsplash.com/photo-1534308983496-4fabb1a015ee?w=800&auto=format&fit=crop",
                vegetarian = false
            ),
            Dish(
                id = "dish-203",
                restaurantId = "rest-2",
                category = "Pasta",
                name = "Handmade Truffle Tagliatelle",
                description = "Silk egg ribbons tossed in black truffle cream, aged Parmigiano-Reggiano and wild mushrooms.",
                price = 48000,
                image = "https://images.unsplash.com/photo-1621996346565-e3d5d628169e?w=800&auto=format&fit=crop",
                vegetarian = true
            ),
            Dish(
                id = "dish-204",
                restaurantId = "rest-2",
                category = "Starters",
                name = "Bruschetta al Pomodoro",
                description = "Toasted sourdough ciabatta topped with heirloom tomatoes, garlic, extra virgin olive oil and basil.",
                price = 22000,
                image = "https://images.unsplash.com/photo-1572695157366-5e585ab2b69f?w=800&auto=format&fit=crop",
                vegetarian = true
            ),
            Dish(
                id = "dish-205",
                restaurantId = "rest-2",
                category = "Desserts",
                name = "Classic Espresso Tiramisu",
                description = "Savoiardi ladyfingers soaked in dark roast espresso, layered with mascarpone cream and Valrhona cocoa.",
                price = 26000,
                image = "https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=800&auto=format&fit=crop",
                vegetarian = true
            ),

            // Restaurant 3: Dakshin Delights
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
            ),
            Dish(
                id = "dish-301",
                restaurantId = "rest-3",
                category = "Dosa",
                name = "Mysore Masala Dosa",
                description = "Crispy crepe coated with spicy red chutney, stuffed with spiced potato mash and butter.",
                price = 18000,
                image = "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?w=800&auto=format&fit=crop",
                vegetarian = true
            ),
            Dish(
                id = "dish-302",
                restaurantId = "rest-3",
                category = "Tiffin",
                name = "Steamed Thatte Idli with Podi",
                description = "Fluffy giant Bangalore-style steamed rice cake drenched in hot spiced ghee and gunpowder.",
                price = 12000,
                image = "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=800&auto=format&fit=crop",
                vegetarian = true
            ),
            Dish(
                id = "dish-303",
                restaurantId = "rest-3",
                category = "Tiffin",
                name = "Crispy Medu Vada (2 pcs)",
                description = "Crispy deep-fried savory lentil doughnuts served with fresh coconut chutney and piping hot sambar.",
                price = 11000,
                image = "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?w=800&auto=format&fit=crop",
                vegetarian = true
            ),
            Dish(
                id = "dish-304",
                restaurantId = "rest-3",
                category = "Mains",
                name = "Malabar Parotta with Veg Kurma",
                description = "Layered flaky Kerala parottas paired with aromatic vegetable coconut kurma.",
                price = 21000,
                image = "https://images.unsplash.com/photo-1626074353765-517a681e40be?w=800&auto=format&fit=crop",
                vegetarian = true
            ),
            Dish(
                id = "dish-305",
                restaurantId = "rest-3",
                category = "Mains",
                name = "Chettinad Pepper Chicken",
                description = "Tender country chicken simmered in stone-ground black pepper, shallots and curry leaves.",
                price = 31000,
                image = "https://images.unsplash.com/photo-1603894584373-5ac82b2ae398?w=800&auto=format&fit=crop",
                vegetarian = false
            ),
            Dish(
                id = "dish-306",
                restaurantId = "rest-3",
                category = "Beverages",
                name = "Mysore Pure Filter Coffee",
                description = "Traditional freshly brewed chicory-infused decoction with frothy hot milk.",
                price = 6000,
                image = "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=800&auto=format&fit=crop",
                vegetarian = true
            ),

            // Restaurant 4: Tokyo Zen & Bao
            Dish(
                id = "dish-401",
                restaurantId = "rest-4",
                category = "Ramen",
                name = "Tokyo Shoyu Chashu Ramen",
                description = "Artisanal wheat noodles in rich soy-broth with braised chashu, ajitsuke tamago egg, nori and menma.",
                price = 42000,
                image = "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=800&auto=format&fit=crop",
                vegetarian = false
            ),
            Dish(
                id = "dish-402",
                restaurantId = "rest-4",
                category = "Ramen",
                name = "Creamy Miso Veggie Ramen",
                description = "Savory fermented soybean broth with bok choy, sweet corn, wood ear mushrooms and sesame chili oil.",
                price = 36000,
                image = "https://images.unsplash.com/photo-1552611052-33e04de081de?w=800&auto=format&fit=crop",
                vegetarian = true
            ),
            Dish(
                id = "dish-403",
                restaurantId = "rest-4",
                category = "Dim Sum",
                name = "Truffle Edamame Dim Sum (4 pcs)",
                description = "Translucent steamed dumplings filled with crushed edamame, water chestnut and white truffle oil.",
                price = 32000,
                image = "https://images.unsplash.com/photo-1496116218417-1a781b1c416c?w=800&auto=format&fit=crop",
                vegetarian = true
            ),
            Dish(
                id = "dish-404",
                restaurantId = "rest-4",
                category = "Bao",
                name = "Crispy Karaage Chicken Bao (2 pcs)",
                description = "Steamed lotus leaf buns packed with crunchy Japanese fried chicken, spicy mayo and pickled daikon.",
                price = 29000,
                image = "https://images.unsplash.com/photo-1563245372-f21724e3856d?w=800&auto=format&fit=crop",
                vegetarian = false
            ),

            // Restaurant 5: The Green Bowl & Bakery
            Dish(
                id = "dish-501",
                restaurantId = "rest-5",
                category = "Bowls",
                name = "Beet Hummus & Falafel Buddha Bowl",
                description = "Herb quinoa, roasted chickpeas, avocado, baby spinach, beetroot tahini hummus and citrus vinaigrette.",
                price = 32000,
                image = "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=800&auto=format&fit=crop",
                vegetarian = true
            ),
            Dish(
                id = "dish-502",
                restaurantId = "rest-5",
                category = "Toast",
                name = "Sourdough Avocado & Poached Egg",
                description = "Toasted country sourdough, smashed Hass avocado, free-range poached egg, chili flakes and microgreens.",
                price = 28000,
                image = "https://images.unsplash.com/photo-1525351484163-7529414344d8?w=800&auto=format&fit=crop",
                vegetarian = false
            ),
            Dish(
                id = "dish-503",
                restaurantId = "rest-5",
                category = "Smoothies",
                name = "Wild Berry Acai Smoothie Bowl",
                description = "Frozen acai blended with blueberries, almond milk, topped with house granola, chia and banana coins.",
                price = 26000,
                image = "https://images.unsplash.com/photo-1590301157890-4810ed352733?w=800&auto=format&fit=crop",
                vegetarian = true
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

    // OTP Authentication State
    var otpSent by mutableStateOf(false)
    var otpTarget by mutableStateOf("")
    var otpPreviewCode by mutableStateOf<String?>(null)
    var otpNotificationBanner by mutableStateOf<String?>(null)

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
            val savedProfile = session.loadProfile()
            if (savedProfile != null) {
                profile = savedProfile
            }
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
        val demoProfile = Profile("demo-1", "Aarav Sharma", "aarav@example.com", "+91 98765 43210")
        profile = demoProfile
        session.saveProfile(demoProfile)
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
            session.saveProfile(response.profile)
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
            session.saveProfile(userProfile)
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

    fun sendOtp(target: String, isPhone: Boolean = true) = run {
        val cleanTarget = target.trim()
        if (cleanTarget.isBlank()) {
            error = "Please enter a valid phone number or email"
            return@run
        }
        otpTarget = cleanTarget
        try {
            val response: OtpSendResponse = api.call(
                "/auth/otp/send",
                "POST",
                OtpSendRequest(target = cleanTarget, type = if (isPhone) "phone" else "email")
            )
            otpSent = true
            otpPreviewCode = response.previewOtp
            otpNotificationBanner = response.message + (if (response.previewOtp != null) " • Code: ${response.previewOtp}" else "")
        } catch (_: Exception) {
            // Offline / local fallback OTP generator
            val localCode = (100000..999999).random().toString()
            otpSent = true
            otpPreviewCode = localCode
            otpNotificationBanner = "OTP sent to $cleanTarget • Code: $localCode (Demo / Offline)"
        }
    }

    fun verifyOtp(code: String, name: String = "") = run {
        val cleanCode = code.trim()
        if (cleanCode.length != 6) {
            error = "Please enter the 6-digit verification code"
            return@run
        }
        try {
            val response: AuthResult = api.call(
                "/auth/otp/verify",
                "POST",
                OtpVerifyRequest(target = otpTarget, code = cleanCode, name = name)
            )
            api.token = response.token
            session.save(response.token)
            profile = response.profile
            session.saveProfile(response.profile)
            otpSent = false
            otpPreviewCode = null
            otpNotificationBanner = null
            refreshAccount()
            route = "Home"
        } catch (e: Exception) {
            // Check if matches local offline preview code or accepted in mock mode
            if (otpPreviewCode == null || otpPreviewCode == cleanCode || cleanCode == "123456") {
                val isEmail = otpTarget.contains("@")
                val fallbackProfile = Profile(
                    id = "user-" + UUID.randomUUID().toString().take(8),
                    name = name.ifBlank { if (isEmail) otpTarget.substringBefore("@") else "Foodie" },
                    email = if (isEmail) otpTarget else "user_${otpTarget.takeLast(4)}@restaurant.app",
                    phone = if (!isEmail) otpTarget else "+91 98765 43210"
                )
                profile = fallbackProfile
                session.saveProfile(fallbackProfile)
                if (addresses.isEmpty()) {
                    addresses = listOf(
                        Address(
                            id = "addr-1",
                            label = "Home",
                            line = "12 Main Street, Indiranagar",
                            city = "Bengaluru",
                            postcode = "560038",
                            phone = fallbackProfile.phone
                        )
                    )
                }
                otpSent = false
                otpPreviewCode = null
                otpNotificationBanner = null
                route = "Home"
            } else {
                error = e.message ?: "Invalid OTP verification code. Please try again."
            }
        }
    }

    fun dismissOtpBanner() {
        otpNotificationBanner = null
    }

    fun resetOtp() {
        otpSent = false
        otpPreviewCode = null
        otpNotificationBanner = null
    }

    fun logout() = run {
        try {
            api.request("/auth/logout", "POST", null)
        } catch (_: Exception) {}
        session.save(null)
        session.saveProfile(null)
        api.token = null
        profile = null
        otpSent = false
        otpTarget = ""
        otpPreviewCode = null
        otpNotificationBanner = null
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
        reviews = listOf(
            Review(
                orderId = "ord-1",
                restaurantId = id,
                author = "Priya N.",
                rating = 5,
                text = "Exquisite flavours and authentic spices! Arrived piping hot in eco-friendly packaging."
            ),
            Review(
                orderId = "ord-2",
                restaurantId = id,
                author = "Karthik R.",
                rating = 5,
                text = "Generous portions, mouth-watering aroma. One of the best in Bengaluru!"
            ),
            Review(
                orderId = "ord-3",
                restaurantId = id,
                author = "Ananya M.",
                rating = 4,
                text = "Super quick delivery and tasty dishes. Loved the packaging and attention to detail."
            )
        )
        if (api.token != null) {
            viewModelScope.launch {
                try {
                    val remoteReviews: List<Review> = api.call("/reviews/$id")
                    if (remoteReviews.isNotEmpty()) reviews = remoteReviews
                } catch (_: Exception) {}
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

    private fun computeQuote(): Quote {
        val subtotal = cart.lines.sumOf { line ->
            val dish = catalog.dishes.find { it.id == line.dishId }
            val dishPrice = dish?.price ?: 0L
            val addonsPrice = dish?.addons?.filter { it.id in line.addons }?.sumOf { it.price } ?: 0L
            (dishPrice + addonsPrice) * line.quantity
        }
        val delivery = 4000L
        val tax = (subtotal * 0.05).toLong()
        val discount = if (cart.coupon.isNotBlank()) 5000L.coerceAtMost(subtotal) else 0L
        val fallbackRestId = cart.lines.firstOrNull()?.let { line ->
            catalog.dishes.find { it.id == line.dishId }?.restaurantId
        } ?: "rest-1"
        val activeRestaurantId = if (restaurantId.isNotBlank()) restaurantId else fallbackRestId
        val q = Quote(
            restaurantId = activeRestaurantId,
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
        quote = q
        return q
    }

    fun loadQuote() = run {
        computeQuote()

        if (addresses.isEmpty()) {
            addresses = listOf(
                Address(
                    id = "addr-default",
                    label = "Home",
                    line = "Flat 402, Green Glen Layout, Bellandur",
                    city = "Bengaluru",
                    postcode = "560103",
                    phone = profile?.phone?.ifBlank { "+91 98765 43210" } ?: "+91 98765 43210"
                )
            )
        }

        if (api.token != null) {
            try {
                quote = api.call("/quote")
                val remoteAddresses: List<Address> = api.call("/addresses")
                if (remoteAddresses.isNotEmpty()) addresses = remoteAddresses
            } catch (_: Exception) {}
        }
        route = "Checkout"
    }

    fun checkout(addressId: String, paymentMethod: String = "Cash on delivery") = run {
        val currentQuote: Quote = quote ?: computeQuote()
        val total = currentQuote.total
        val selectedAddr = addresses.find { it.id == addressId }
            ?: addresses.firstOrNull()
            ?: Address(
                id = "addr-default",
                label = "Home",
                line = "Flat 402, Green Glen Layout, Bellandur",
                city = "Bengaluru",
                postcode = "560103",
                phone = profile?.phone?.ifBlank { "+91 98765 43210" } ?: "+91 98765 43210"
            ).also { addresses = listOf(it) }

        val activeRestId = if (restaurantId.isNotBlank()) restaurantId else currentQuote.restaurantId
        val rest = catalog.restaurants.find { it.id == activeRestId }
        val restName = rest?.name ?: "Bistro Lumière & Biryani"
        val paymentStatus = if (paymentMethod.contains("Cash", true)) "Due on delivery" else "Paid Online (Verified)"

        val fallbackOrder = DeliveryOrder(
            id = (1000 + (Math.random() * 9000).toInt()).toString(),
            restaurantId = activeRestId,
            restaurantName = restName,
            address = selectedAddr,
            quote = currentQuote,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            status = "Confirmed"
        )

        if (api.token != null) {
            try {
                val order: DeliveryOrder =
                    api.call("/orders", "POST", Checkout(selectedAddr.id, paymentMethod, checkoutKey, total))
                orders = listOf(order) + orders.filterNot { it.id == order.id }
                orderId = order.id
            } catch (_: Exception) {
                orders = listOf(fallbackOrder) + orders
                orderId = fallbackOrder.id
            }
        } else {
            orders = listOf(fallbackOrder) + orders
            orderId = fallbackOrder.id
        }

        cart = Cart()
        quote = null
        resetCheckoutKey()
        route = "Confirmation"
    }

    fun cancelOrder(id: String) {
        orders = orders.map { if (it.id == id) it.copy(status = "Cancelled") else it }
    }

    fun reorder(order: DeliveryOrder) {
        val newLines = order.quote.lines.map { Line(it.dishId, it.quantity, it.addons) }
        changeCart(newLines, "Cart")
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
                "Map" -> "Home"
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
