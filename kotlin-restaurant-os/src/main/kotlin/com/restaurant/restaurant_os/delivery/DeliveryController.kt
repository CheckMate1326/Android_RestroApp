package com.restaurant.restaurant_os.delivery

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.*
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.server.ResponseStatusException

@Component
class AccessFilter : OncePerRequestFilter() {
    private val attempts = ConcurrentHashMap<String, Pair<Long, Int>>()

    override fun doFilterInternal(
        req: HttpServletRequest,
        res: HttpServletResponse,
        chain: FilterChain,
    ) {
        res.setHeader("X-Content-Type-Options", "nosniff")
        res.setHeader("Cache-Control", "no-store")
        if (req.contentLengthLong > 65536) {
            res.sendError(413)
            return
        }
        val path = req.requestURI
        if (path.startsWith("/api/delivery/auth/")) {
            val now = System.currentTimeMillis() / 60000
            if (attempts.size > 10000) attempts.entries.removeIf { it.value.first < now }
            val count =
                attempts
                    .compute(req.remoteAddr) { _, old ->
                        if (old?.first == now) now to (old.second + 1) else now to 1
                    }!!
                    .second
            if (count > 12) {
                res.status = 429
                res.setHeader("Retry-After", "60")
                res.writer.write("{\"message\":\"Too many attempts. Try again in a minute.\"}")
                return
            }
        }
        val customerApi =
            path.startsWith("/api/delivery/") && !path.startsWith("/api/delivery/staff/")
        val publicMenu = req.method == "GET" && path in listOf("/api/categories", "/api/menu-items")
        if (!customerApi && !publicMenu) {
            val user = System.getenv("RESTRO_STAFF_USER")
            val pass = System.getenv("RESTRO_STAFF_PASSWORD")
            val actual = req.getHeader("Authorization") ?: ""
            val expected =
                if (!user.isNullOrBlank() && !pass.isNullOrBlank())
                    "Basic " + Base64.getEncoder().encodeToString("$user:$pass".toByteArray())
                else ""
            if (
                expected.isEmpty() ||
                    !MessageDigest.isEqual(actual.toByteArray(), expected.toByteArray())
            ) {
                res.status = 401
                res.setHeader("WWW-Authenticate", "Basic realm=\"Restro staff\"")
                return
            }
        }
        chain.doFilter(req, res)
    }
}

@RestController
@RequestMapping("/api/delivery")
class DeliveryController(private val service: DeliveryService) {
    @GetMapping("/catalog") fun catalog() = service.catalog

    @PostMapping("/auth/register") fun register(@RequestBody c: Credentials) = service.auth(c, true)

    @PostMapping("/auth/login") fun login(@RequestBody c: Credentials) = service.auth(c, false)

    @PostMapping("/auth/logout")
    fun logout(@RequestHeader("Authorization", required = false) h: String?) = service.logout(h)

    @GetMapping("/profile")
    fun profile(@RequestHeader("Authorization", required = false) h: String?) =
        service.profile(service.owner(h))

    @PutMapping("/profile")
    fun profile(
        @RequestHeader("Authorization", required = false) h: String?,
        @RequestBody p: Profile,
    ) = service.updateProfile(service.owner(h), p)

    @GetMapping("/addresses")
    fun addresses(@RequestHeader("Authorization", required = false) h: String?) =
        service.addresses(service.owner(h))

    @PutMapping("/addresses")
    fun address(
        @RequestHeader("Authorization", required = false) h: String?,
        @RequestBody a: Address,
    ) = service.saveAddress(service.owner(h), a)

    @PostMapping("/addresses")
    fun createAddress(
        @RequestHeader("Authorization", required = false) h: String?,
        @RequestBody a: Address,
    ) = service.saveAddress(service.owner(h), a)

    @DeleteMapping("/addresses/{id}")
    fun delete(
        @RequestHeader("Authorization", required = false) h: String?,
        @PathVariable id: String,
    ) = service.deleteAddress(service.owner(h), id)

    @GetMapping("/cart")
    fun cart(@RequestHeader("Authorization", required = false) h: String?) =
        service.cart(service.owner(h))

    @PutMapping("/cart")
    fun cart(@RequestHeader("Authorization", required = false) h: String?, @RequestBody c: Cart) =
        service.saveCart(service.owner(h), c)

    @GetMapping("/quote")
    fun quote(@RequestHeader("Authorization", required = false) h: String?) =
        service.quote(service.cart(service.owner(h)))

    @PostMapping("/orders")
    fun checkout(
        @RequestHeader("Authorization", required = false) h: String?,
        @RequestBody c: Checkout,
    ) = service.checkout(service.owner(h), c)

    @GetMapping("/orders")
    fun orders(@RequestHeader("Authorization", required = false) h: String?) =
        service.orders(service.owner(h))

    @GetMapping("/orders/{id}")
    fun order(
        @RequestHeader("Authorization", required = false) h: String?,
        @PathVariable id: String,
    ) = service.order(service.owner(h), id)

    @GetMapping("/favorites")
    fun favorites(@RequestHeader("Authorization", required = false) h: String?) =
        service.favorites(service.owner(h))

    @PutMapping("/favorites/{id}")
    fun favorite(
        @RequestHeader("Authorization", required = false) h: String?,
        @PathVariable id: String,
        @RequestBody body: Map<String, Boolean>,
    ) = service.favorite(service.owner(h), id, body["selected"] == true)

    @GetMapping("/reviews/{id}") fun reviews(@PathVariable id: String) = service.reviews(id)

    @PostMapping("/reviews/{id}")
    fun review(
        @RequestHeader("Authorization", required = false) h: String?,
        @PathVariable id: String,
        @RequestBody body: Map<String, String>,
    ) = service.review(service.owner(h), id, body["rating"]?.toIntOrNull() ?: 0, body["text"] ?: "")

    @GetMapping("/staff/orders") fun staffOrders() = service.staffOrders()

    @PutMapping("/staff/orders/{owner}/{id}")
    fun advance(
        @PathVariable owner: String,
        @PathVariable id: String,
        @RequestBody body: Map<String, String>,
    ) = service.advance(owner, id, body["status"] ?: "")
}

@RestControllerAdvice
class DeliveryErrors {
    @ExceptionHandler(ResponseStatusException::class)
    fun known(e: ResponseStatusException): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(e.statusCode)
            .body(mapOf("message" to (e.reason ?: "Unable to complete this request.")))

    @ExceptionHandler(
        org.springframework.http.converter.HttpMessageNotReadableException::class,
        IllegalArgumentException::class,
    )
    fun input(e: Exception) =
        ResponseEntity.badRequest().body(mapOf("message" to "Check the information and try again."))

    @ExceptionHandler(Exception::class)
    fun unexpected(e: Exception) =
        ResponseEntity.internalServerError()
            .body(mapOf("message" to "Unable to complete this request. Please try again."))
}
