package com.restaurant.restaurant_os

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.restaurant.restaurant_os.delivery.*
import java.util.UUID
import java.util.concurrent.Executors
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.web.server.ResponseStatusException

class DeliveryServiceTests {
    private lateinit var service: DeliveryService
    private lateinit var owner: String
    private lateinit var stranger: String

    @BeforeEach
    fun setup() {
        val ds =
            DriverManagerDataSource("jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1", "sa", "")
        service =
            DeliveryService(
                JdbcTemplate(ds),
                jacksonObjectMapper(),
                DataSourceTransactionManager(ds),
            )
        service.init()
        val field =
            DeliveryService::class.java.getDeclaredField("catalog").apply { isAccessible = true }
        field.set(
            service,
            Catalog(
                listOf(
                    Restaurant(
                        "r",
                        "Restaurant",
                        "Indian",
                        "Description",
                        "https://example.com/r.jpg",
                        "Bengaluru",
                        12.9,
                        77.6,
                        30,
                        3500,
                    )
                ),
                listOf(
                    Dish(
                        "d",
                        "r",
                        "Mains",
                        "Meal",
                        "Description",
                        25000,
                        "https://example.com/d.jpg",
                        true,
                        addons = listOf(AddOn("extra", "Extra", 4000)),
                    )
                ),
            ),
        )
        owner = account("owner@example.com")
        stranger = account("other@example.com")
    }

    private fun account(email: String): String {
        val response = service.auth(Credentials(email, "a-long-test-password", "Test user"), true)
        return service.owner("Bearer ${response["token"]}")
    }

    private fun address() =
        service.saveAddress(
            owner,
            Address(
                label = "Home",
                line = "12 Test Street",
                city = "Bengaluru",
                postcode = "560001",
                phone = "9876543210",
            ),
        )

    @Test
    fun `cart enforces IDs quantities addons and server prices`() {
        for (line in
            listOf(
                Line("missing", 1),
                Line("d", -1),
                Line("d", 21),
                Line("d", 1, listOf("bad")),
                Line("d", 1, listOf("extra", "extra")),
            )) {
            assertThrows(ResponseStatusException::class.java) {
                service.saveCart(owner, Cart(listOf(line)))
            }
        }
        val quote = service.quote(Cart(listOf(Line("d", 2, listOf("extra")))))
        assertEquals(61500L, quote.total)
        assertEquals(0, service.cart(owner).lines.size)
    }

    @Test
    fun `checkout is atomic owned and idempotent under concurrency`() {
        val a = address()
        service.saveCart(owner, Cart(listOf(Line("d", 2))))
        val request = Checkout(a.id, "Cash on delivery", UUID.randomUUID().toString(), 53500)
        val pool = Executors.newFixedThreadPool(4)
        try {
            val results =
                pool
                    .invokeAll(
                        (1..4).map {
                            java.util.concurrent.Callable { service.checkout(owner, request) }
                        }
                    )
                    .map { it.get() }
            assertEquals(1, results.map { it.id }.distinct().size)
            assertEquals(1, service.orders(owner).size)
            assertTrue(service.cart(owner).lines.isEmpty())
            assertThrows(ResponseStatusException::class.java) {
                service.order(stranger, results.first().id)
            }
            assertEquals("Due on delivery", results.first().paymentStatus)
        } finally {
            pool.shutdownNow()
        }
    }

    @Test
    fun `foreign address wrong total and online payment cannot place an order`() {
        val a = address()
        service.saveCart(stranger, Cart(listOf(Line("d", 1))))
        assertThrows(ResponseStatusException::class.java) {
            service.checkout(
                stranger,
                Checkout(a.id, "Cash on delivery", UUID.randomUUID().toString(), 28500),
            )
        }
        service.saveCart(owner, Cart(listOf(Line("d", 1))))
        assertThrows(ResponseStatusException::class.java) {
            service.checkout(
                owner,
                Checkout(a.id, "Cash on delivery", UUID.randomUUID().toString(), 1),
            )
        }
        assertThrows(ResponseStatusException::class.java) {
            service.checkout(owner, Checkout(a.id, "Card", UUID.randomUUID().toString(), 28500))
        }
        assertEquals(1, service.cart(owner).lines.size)
        assertTrue(service.orders(owner).isEmpty())
    }

    @Test
    fun `reviews require delivered owned orders and transitions are sequential`() {
        val a = address()
        service.saveCart(owner, Cart(listOf(Line("d", 1))))
        val order =
            service.checkout(
                owner,
                Checkout(a.id, "Cash on delivery", UUID.randomUUID().toString(), 28500),
            )
        assertThrows(ResponseStatusException::class.java) {
            service.review(owner, order.id, 5, "Great meal")
        }
        assertThrows(ResponseStatusException::class.java) {
            service.advance(owner, order.id, "Delivered")
        }
        listOf("Preparing", "Ready", "Out for delivery", "Delivered").forEach {
            service.advance(owner, order.id, it)
        }
        assertEquals(5, service.review(owner, order.id, 5, "Great meal").rating)
        assertThrows(ResponseStatusException::class.java) {
            service.review(stranger, order.id, 5, "Great meal")
        }
        assertEquals(5, service.order(owner, order.id).events.size)
    }

    @Test
    fun `session revocation and profile ownership`() {
        val auth = service.auth(Credentials("owner@example.com", "a-long-test-password"), false)
        val header = "Bearer ${auth["token"]}"
        assertEquals(owner, service.owner(header))
        service.logout(header)
        assertThrows(ResponseStatusException::class.java) { service.owner(header) }
        val p = service.updateProfile(owner, Profile(stranger, "Updated", "attacker@example.com"))
        assertEquals(owner, p.id)
        assertEquals("owner@example.com", p.email)
        assertTrue(service.addresses(stranger).isEmpty())
    }
}
