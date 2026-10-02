package com.restaurant.restaurant_os.delivery

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.annotation.PostConstruct
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.server.ResponseStatusException

data class AddOn(val id: String, val name: String, val price: Long)

data class Dish(
    val id: String,
    val restaurantId: String,
    val category: String,
    val name: String,
    val description: String,
    val price: Long,
    val image: String,
    val vegetarian: Boolean,
    val available: Boolean = true,
    val addons: List<AddOn> = emptyList(),
)

data class Restaurant(
    val id: String,
    val name: String,
    val cuisine: String,
    val description: String,
    val image: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val deliveryMinutes: Int,
    val deliveryFee: Long,
    val open: Boolean = true,
)

data class Offer(
    val code: String,
    val description: String,
    val minimum: Long,
    val discount: Long,
    val expiresAt: String,
)

data class Catalog(
    val restaurants: List<Restaurant> = emptyList(),
    val dishes: List<Dish> = emptyList(),
    val offers: List<Offer> = emptyList(),
)

data class Profile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val reducedGlass: Boolean = false,
)

data class Address(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val line: String,
    val city: String,
    val postcode: String,
    val phone: String,
)

data class Line(val dishId: String, val quantity: Int, val addons: List<String> = emptyList())

data class Cart(val lines: List<Line> = emptyList(), val coupon: String = "")

data class QuoteLine(
    val dishId: String,
    val name: String,
    val quantity: Int,
    val addons: List<String>,
    val unitPrice: Long,
    val total: Long,
)

data class Quote(
    val restaurantId: String,
    val lines: List<QuoteLine>,
    val subtotal: Long,
    val tax: Long,
    val delivery: Long,
    val discount: Long,
    val total: Long,
)

data class Event(val status: String, val at: String = Instant.now().toString())

data class DeliveryOrder(
    val id: String,
    val restaurantId: String,
    val restaurantName: String,
    val address: Address,
    val quote: Quote,
    val paymentMethod: String,
    val paymentStatus: String = "Due on delivery",
    val status: String = "Confirmed",
    val events: List<Event> = listOf(Event("Confirmed")),
)

data class Review(
    val orderId: String,
    val restaurantId: String,
    val author: String,
    val rating: Int,
    val text: String,
    val at: String = Instant.now().toString(),
)

data class Credentials(val email: String, val password: String, val name: String = "")

data class Checkout(
    val addressId: String,
    val paymentMethod: String,
    val idempotencyKey: String,
    val expectedTotal: Long,
)

data class CheckoutReceipt(val request: Checkout, val order: DeliveryOrder)

@Service
class DeliveryService(
    private val db: JdbcTemplate,
    private val json: ObjectMapper,
    manager: PlatformTransactionManager,
) {
    private val tx = TransactionTemplate(manager)
    private val random = SecureRandom()
    @Volatile
    final var catalog = Catalog()
        private set

    @PostConstruct
    fun init() {
        db.execute(
            "CREATE TABLE IF NOT EXISTS delivery_users (id VARCHAR(36) PRIMARY KEY, email VARCHAR(254) UNIQUE NOT NULL, password VARCHAR(512) NOT NULL, profile CLOB NOT NULL)"
        )
        db.execute(
            "CREATE TABLE IF NOT EXISTS delivery_sessions (token VARCHAR(64) PRIMARY KEY, owner VARCHAR(36) NOT NULL, expires BIGINT NOT NULL)"
        )
        db.execute(
            "CREATE TABLE IF NOT EXISTS delivery_docs (kind VARCHAR(32), id VARCHAR(100), owner VARCHAR(36), body CLOB NOT NULL, PRIMARY KEY(kind,id,owner))"
        )
        db.execute("CREATE TABLE IF NOT EXISTS delivery_lock (id INT PRIMARY KEY)")
        db.update("MERGE INTO delivery_lock KEY(id) VALUES (1)")
        val candidates = listOfNotNull(
            System.getenv("RESTRO_CATALOG_FILE"),
            System.getProperty("restro.catalog.file"),
            "config/catalog.json",
            "../config/catalog.json",
            "config/catalog.example.json",
            "../config/catalog.example.json",
        )
        val catalogFile = candidates.map { java.io.File(it) }.firstOrNull { it.isFile }
        if (catalogFile != null) {
            catalog = json.readValue(catalogFile)
            check(
                catalog.restaurants.all {
                    it.id.matches(Regex("[a-zA-Z0-9_-]{1,80}")) &&
                        it.deliveryFee in 0..100000 &&
                        it.latitude in -90.0..90.0 &&
                        it.longitude in -180.0..180.0 &&
                        it.deliveryMinutes in 1..240 &&
                        it.image.startsWith("https://")
                }
            )
            check(
                catalog.offers.all {
                    it.minimum in 0..10000000 &&
                        it.discount in 0..10000000 &&
                        it.code.length in 1..30 &&
                        runCatching { Instant.parse(it.expiresAt) }.isSuccess
                }
            )
            check(catalog.restaurants.map { it.id }.distinct().size == catalog.restaurants.size)
            check(catalog.dishes.map { it.id }.distinct().size == catalog.dishes.size)
            check(
                catalog.dishes.all { d ->
                    d.id.matches(Regex("[a-zA-Z0-9_-]{1,80}")) &&
                        d.addons.map { it.id }.distinct().size == d.addons.size &&
                        d.price in 1..10000000 &&
                        catalog.restaurants.any { it.id == d.restaurantId } &&
                        d.image.startsWith("https://") &&
                        d.addons.all { it.price in 0..1000000 }
                }
            )
        }
    }

    fun bad(message: String): Nothing =
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, message)

    fun missing(): Nothing =
        throw ResponseStatusException(HttpStatus.NOT_FOUND, "This item is no longer available.")

    fun unauthorized(): Nothing =
        throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in again.")

    private fun digest(s: String) =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") {
            "%02x".format(it)
        }

    private fun password(value: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(value.toCharArray(), salt, 600000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encodePassword(value: String): String {
        val salt = ByteArray(16).also(random::nextBytes)
        return Base64.getEncoder().encodeToString(salt) +
            ":" +
            Base64.getEncoder().encodeToString(password(value, salt))
    }

    private fun matches(value: String, encoded: String): Boolean {
        val parts = encoded.split(":")
        return MessageDigest.isEqual(
            password(value, Base64.getDecoder().decode(parts[0])),
            Base64.getDecoder().decode(parts[1]),
        )
    }

    fun auth(c: Credentials, register: Boolean): Map<String, Any> {
        val email = c.email.trim().lowercase()
        if (
            !email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) ||
                email.length > 254 ||
                c.password.length !in 10..128
        )
            bad("Enter a valid email and a password of 10–128 characters.")
        if (register) {
            if (c.name.trim().length !in 2..80) bad("Enter your name (2–80 characters).")
            val id = UUID.randomUUID().toString()
            try {
                db.update(
                    "INSERT INTO delivery_users VALUES (?,?,?,?)",
                    id,
                    email,
                    encodePassword(c.password),
                    json.writeValueAsString(Profile(id, c.name.trim(), email)),
                )
            } catch (e: org.springframework.dao.DuplicateKeyException) {
                bad("Unable to create this account. Try signing in.")
            }
        }
        val rows =
            db.queryForList("SELECT id,password,profile FROM delivery_users WHERE email=?", email)
        if (rows.isEmpty()) {
            password(c.password, ByteArray(16))
            unauthorized()
        }
        val row = rows.first()
        if (!matches(c.password, row["PASSWORD"].toString())) unauthorized()
        val id = row["ID"].toString()
        val token =
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(ByteArray(32).also(random::nextBytes))
        db.update("DELETE FROM delivery_sessions WHERE expires<?", Instant.now().epochSecond)
        db.update(
            "INSERT INTO delivery_sessions VALUES (?,?,?)",
            digest(token),
            id,
            Instant.now().plusSeconds(604800).epochSecond,
        )
        return mapOf("token" to token, "profile" to profile(id))
    }

    fun owner(header: String?): String {
        val token =
            header?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ") ?: unauthorized()
        return db.queryForList(
                "SELECT owner FROM delivery_sessions WHERE token=? AND expires>?",
                digest(token),
                Instant.now().epochSecond,
            )
            .firstOrNull()
            ?.get("OWNER")
            ?.toString() ?: unauthorized()
    }

    fun logout(header: String?) {
        owner(header)
        db.update(
            "DELETE FROM delivery_sessions WHERE token=?",
            digest(header!!.removePrefix("Bearer ")),
        )
    }

    fun profile(owner: String): Profile =
        json.readValue(
            db.queryForObject(
                "SELECT profile FROM delivery_users WHERE id=?",
                String::class.java,
                owner,
            )!!
        )

    fun updateProfile(owner: String, p: Profile): Profile {
        if (
            p.name.trim().length !in 2..80 ||
                (p.phone.isNotEmpty() && !p.phone.matches(Regex("[+0-9 ()-]{7,20}")))
        )
            bad("Check your name and phone number.")
        val saved =
            profile(owner)
                .copy(name = p.name.trim(), phone = p.phone, reducedGlass = p.reducedGlass)
        db.update(
            "UPDATE delivery_users SET profile=? WHERE id=?",
            json.writeValueAsString(saved),
            owner,
        )
        return saved
    }

    private inline fun <reified T> get(kind: String, id: String, owner: String): T? =
        db.queryForList(
                "SELECT body FROM delivery_docs WHERE kind=? AND id=? AND owner=?",
                String::class.java,
                kind,
                id,
                owner,
            )
            .firstOrNull()
            ?.let { json.readValue<T>(it) }

    private inline fun <reified T> list(kind: String, owner: String): List<T> =
        db.queryForList(
                "SELECT body FROM delivery_docs WHERE kind=? AND owner=?",
                String::class.java,
                kind,
                owner,
            )
            .map { json.readValue<T>(it) }

    private fun save(kind: String, id: String, owner: String, body: Any) {
        db.update(
            "MERGE INTO delivery_docs (kind,id,owner,body) KEY(kind,id,owner) VALUES (?,?,?,?)",
            kind,
            id,
            owner,
            json.writeValueAsString(body),
        )
    }

    fun addresses(owner: String) = list<Address>("address", owner)

    fun saveAddress(owner: String, a: Address): Address {
        val trimmedPostcode = a.postcode.trim().replace(" ", "")
        val trimmedPhone = a.phone.trim()
        val trimmedLine = a.line.trim()
        val trimmedCity = a.city.trim()
        val trimmedLabel = a.label.trim().ifEmpty { "Home" }
        val id = if (a.id.isBlank()) UUID.randomUUID().toString() else a.id.trim()
        val clean = a.copy(
            id = id,
            label = trimmedLabel,
            line = trimmedLine,
            city = trimmedCity,
            postcode = trimmedPostcode,
            phone = trimmedPhone
        )
        if (
            clean.id.length > 36 ||
                clean.label.length !in 1..30 ||
                clean.line.length !in 3..200 ||
                clean.city.length !in 2..80 ||
                !clean.postcode.matches(Regex("[1-9][0-9]{5}")) ||
                !clean.phone.matches(Regex("[+0-9 ()-]{7,20}"))
        )
            bad("Enter a complete address, six-digit PIN code and phone number.")
        if (addresses(owner).size >= 20 && addresses(owner).none { it.id == clean.id })
            bad("You can save up to 20 addresses.")
        save("address", clean.id, owner, clean)
        return clean
    }

    fun deleteAddress(owner: String, id: String) {
        db.update("DELETE FROM delivery_docs WHERE kind='address' AND id=? AND owner=?", id, owner)
    }

    fun cart(owner: String) = get<Cart>("cart", "current", owner) ?: Cart()

    fun saveCart(owner: String, cart: Cart): Cart = locked {
        if (cart.lines.isNotEmpty()) quote(cart)
        if (cart.coupon.length > 30) bad("Invalid offer code.")
        save("cart", "current", owner, cart)
        cart
    }

    fun quote(cart: Cart): Quote {
        if (cart.lines.isEmpty() || cart.lines.size > 50)
            bad("Add between 1 and 50 items to your cart.")
        val dishes =
            cart.lines.map { line ->
                catalog.dishes.find { it.id == line.dishId && it.available }
                    ?: bad("An item is no longer available. Update your cart.")
            }
        val restaurant =
            catalog.restaurants.find { it.id == dishes.first().restaurantId && it.open }
                ?: bad("This restaurant is currently closed.")
        if (dishes.any { it.restaurantId != restaurant.id })
            bad("Order from one restaurant at a time.")
        val lines =
            cart.lines.zip(dishes).map { (line, dish) ->
                if (
                    line.quantity !in 1..20 ||
                        line.addons.size != line.addons.distinct().size ||
                        line.addons.any { id -> dish.addons.none { it.id == id } }
                )
                    bad("Check item quantities and add-ons.")
                val unit =
                    dish.price + dish.addons.filter { it.id in line.addons }.sumOf { it.price }
                QuoteLine(
                    dish.id,
                    dish.name,
                    line.quantity,
                    dish.addons.filter { it.id in line.addons }.map { it.name },
                    unit,
                    unit * line.quantity,
                )
            }

        val subtotal = lines.sumOf { it.total }
        // Catalog price is tax inclusive. Merchant tax policy must be configured before tax is
        // added.
        val discount =
            if (cart.coupon.isBlank()) 0L
            else {
                val offer =
                    catalog.offers.find {
                        it.code.equals(cart.coupon, true) &&
                            Instant.parse(it.expiresAt).isAfter(Instant.now())
                    } ?: bad("This offer is unavailable. Remove it to continue.")
                if (subtotal < offer.minimum) bad("The cart does not meet this offer's minimum.")
                offer.discount.coerceIn(0, subtotal)
            }
        return Quote(
            restaurant.id,
            lines,
            subtotal,
            0,
            restaurant.deliveryFee,
            discount,
            subtotal + restaurant.deliveryFee - discount,
        )
    }

    private fun <T : Any> locked(block: () -> T): T =
        tx.execute {
            db.queryForObject("SELECT id FROM delivery_lock WHERE id=1 FOR UPDATE", Int::class.java)
            block()
        }!!

    fun checkout(owner: String, c: Checkout): DeliveryOrder = locked {
        if (!c.idempotencyKey.matches(Regex("[a-zA-Z0-9-]{16,80}")))
            bad("Invalid checkout reference.")
        get<CheckoutReceipt>("checkout", c.idempotencyKey, owner)?.let {
            if (it.request != c)
                throw ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This checkout was already completed. Check your orders before placing another.",
                )
            return@locked it.order
        }
        if (c.paymentMethod != "Cash on delivery") bad("This payment method is not available.")
        val address =
            get<Address>("address", c.addressId, owner) ?: bad("Choose a saved delivery address.")
        val quote = quote(cart(owner))
        if (quote.total != c.expectedTotal)
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Prices changed. Review the updated total.",
            )
        val order =
            DeliveryOrder(
                UUID.randomUUID().toString(),
                quote.restaurantId,
                catalog.restaurants.first { it.id == quote.restaurantId }.name,
                address,
                quote,
                c.paymentMethod,
            )
        save("order", order.id, owner, order)
        save("checkout", c.idempotencyKey, owner, CheckoutReceipt(c, order))
        save("cart", "current", owner, Cart())
        order
    }

    fun orders(owner: String) =
        list<DeliveryOrder>("order", owner).sortedByDescending { it.events.first().at }

    fun order(owner: String, id: String) = get<DeliveryOrder>("order", id, owner) ?: missing()

    fun favorites(owner: String) = get<List<String>>("favorites", "current", owner) ?: emptyList()

    fun favorite(owner: String, id: String, selected: Boolean): List<String> = locked {
        if (catalog.restaurants.none { it.id == id }) missing()
        val values = favorites(owner).toMutableSet()
        if (selected) values.add(id) else values.remove(id)
        save("favorites", "current", owner, values)
        values.toList()
    }

    fun reviews(id: String): List<Review> =
        db.query(
                "SELECT body FROM delivery_docs WHERE kind='review'",
                { rs, _ -> json.readValue<Review>(rs.getString(1)) },
            )
            .filter { it.restaurantId == id }
            .sortedByDescending { it.at }
            .take(100)

    fun review(owner: String, id: String, rating: Int, text: String): Review {
        val order = order(owner, id)
        if (order.status != "Delivered") bad("You can review an order after delivery.")
        if (rating !in 1..5 || text.trim().length !in 3..1000)
            bad("Choose 1–5 stars and write 3–1000 characters.")
        val review = Review(id, order.restaurantId, profile(owner).name, rating, text.trim())
        save("review", id, owner, review)
        return review
    }

    fun staffOrders(): List<Map<String, Any>> =
        db.query(
            "SELECT owner,body FROM delivery_docs WHERE kind='order'",
            { rs, _ ->
                mapOf(
                    "owner" to rs.getString(1),
                    "order" to json.readValue<DeliveryOrder>(rs.getString(2)),
                )
            },
        )

    fun advance(owner: String, id: String, status: String): DeliveryOrder = locked {
        val old = order(owner, id)
        val sequence = listOf("Confirmed", "Preparing", "Ready", "Out for delivery", "Delivered")
        if (
            sequence.indexOf(status) != sequence.indexOf(old.status) + 1 ||
                old.status == "Delivered"
        )
            bad("Invalid order status transition.")
        val updated =
            old.copy(
                status = status,
                paymentStatus = if (status == "Delivered") "Collected" else old.paymentStatus,
                events = old.events + Event(status),
            )
        save("order", id, owner, updated)
        updated
    }
}
