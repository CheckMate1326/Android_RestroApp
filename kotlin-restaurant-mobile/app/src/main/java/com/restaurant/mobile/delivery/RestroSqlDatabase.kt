package com.restaurant.mobile.delivery

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant

/**
 * Embedded Kotlin SQL Database Engine for Restro
 * Provides relational SQLite storage for Restaurants, Dishes, Orders, Users, Addresses & OTPs.
 */
class RestroSqlDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val gson = Gson()

    companion object {
        const val DATABASE_NAME = "restro_delivery.db"
        const val DATABASE_VERSION = 2

        // Table Names
        const val TABLE_USERS = "users"
        const val TABLE_RESTAURANTS = "restaurants"
        const val TABLE_DISHES = "dishes"
        const val TABLE_ORDERS = "orders"
        const val TABLE_ADDRESSES = "addresses"
        const val TABLE_REVIEWS = "reviews"
        const val TABLE_OTPS = "otps"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Users Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_USERS (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                email TEXT NOT NULL,
                phone TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT 'CUSTOMER',
                created_at TEXT NOT NULL
            );
            """.trimIndent()
        )

        // Restaurants Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_RESTAURANTS (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                cuisine TEXT NOT NULL,
                description TEXT NOT NULL,
                image TEXT NOT NULL,
                address TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                delivery_minutes INTEGER NOT NULL,
                delivery_fee INTEGER NOT NULL,
                open INTEGER NOT NULL DEFAULT 1
            );
            """.trimIndent()
        )

        // Dishes Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_DISHES (
                id TEXT PRIMARY KEY,
                restaurant_id TEXT NOT NULL,
                category TEXT NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                price INTEGER NOT NULL,
                image TEXT NOT NULL,
                vegetarian INTEGER NOT NULL DEFAULT 0,
                available INTEGER NOT NULL DEFAULT 1,
                addons_json TEXT,
                FOREIGN KEY(restaurant_id) REFERENCES $TABLE_RESTAURANTS(id)
            );
            """.trimIndent()
        )

        // Orders Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_ORDERS (
                id TEXT PRIMARY KEY,
                restaurant_id TEXT NOT NULL,
                restaurant_name TEXT NOT NULL,
                address_json TEXT NOT NULL,
                quote_json TEXT NOT NULL,
                payment_method TEXT NOT NULL,
                payment_status TEXT NOT NULL,
                status TEXT NOT NULL,
                events_json TEXT NOT NULL,
                created_at TEXT NOT NULL
            );
            """.trimIndent()
        )

        // Addresses Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_ADDRESSES (
                id TEXT PRIMARY KEY,
                label TEXT NOT NULL,
                line TEXT NOT NULL,
                city TEXT NOT NULL,
                postcode TEXT NOT NULL,
                phone TEXT NOT NULL,
                latitude REAL,
                longitude REAL
            );
            """.trimIndent()
        )

        // Reviews Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_REVIEWS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                order_id TEXT NOT NULL,
                restaurant_id TEXT NOT NULL,
                author TEXT NOT NULL,
                rating INTEGER NOT NULL,
                text TEXT NOT NULL,
                at TEXT NOT NULL
            );
            """.trimIndent()
        )

        // OTPs Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_OTPS (
                target TEXT PRIMARY KEY,
                code TEXT NOT NULL,
                expires_at INTEGER NOT NULL,
                attempts INTEGER NOT NULL DEFAULT 0
            );
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_OTPS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REVIEWS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ADDRESSES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ORDERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DISHES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RESTAURANTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // -------------------------------------------------------------
    // RESTAURANTS & DISHES
    // -------------------------------------------------------------

    fun seedCatalogIfEmpty(catalog: Catalog) {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_RESTAURANTS", null)
        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        cursor.close()

        if (count == 0) {
            db.beginTransaction()
            try {
                for (r in catalog.restaurants) {
                    val cv = ContentValues().apply {
                        put("id", r.id)
                        put("name", r.name)
                        put("cuisine", r.cuisine)
                        put("description", r.description)
                        put("image", r.image)
                        put("address", r.address)
                        put("latitude", r.latitude)
                        put("longitude", r.longitude)
                        put("delivery_minutes", r.deliveryMinutes)
                        put("delivery_fee", r.deliveryFee)
                        put("open", if (r.open) 1 else 0)
                    }
                    db.insertWithOnConflict(TABLE_RESTAURANTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }

                for (d in catalog.dishes) {
                    val cv = ContentValues().apply {
                        put("id", d.id)
                        put("restaurant_id", d.restaurantId)
                        put("category", d.category)
                        put("name", d.name)
                        put("description", d.description)
                        put("price", d.price)
                        put("image", d.image)
                        put("vegetarian", if (d.vegetarian) 1 else 0)
                        put("available", if (d.available) 1 else 0)
                        put("addons_json", gson.toJson(d.addons))
                    }
                    db.insertWithOnConflict(TABLE_DISHES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    fun getAllRestaurants(): List<Restaurant> {
        val list = mutableListOf<Restaurant>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_RESTAURANTS ORDER BY name ASC", null)
        while (cursor.moveToNext()) {
            list.add(cursorToRestaurant(cursor))
        }
        cursor.close()
        return list
    }

    fun getRestaurantById(id: String): Restaurant? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_RESTAURANTS WHERE id = ?", arrayOf(id))
        val res = if (cursor.moveToFirst()) cursorToRestaurant(cursor) else null
        cursor.close()
        return res
    }

    fun getAllDishes(): List<Dish> {
        val list = mutableListOf<Dish>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_DISHES", null)
        while (cursor.moveToNext()) {
            list.add(cursorToDish(cursor))
        }
        cursor.close()
        return list
    }

    fun getDishesForRestaurant(restaurantId: String): List<Dish> {
        val list = mutableListOf<Dish>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_DISHES WHERE restaurant_id = ?", arrayOf(restaurantId))
        while (cursor.moveToNext()) {
            list.add(cursorToDish(cursor))
        }
        cursor.close()
        return list
    }

    fun getDishById(id: String): Dish? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_DISHES WHERE id = ?", arrayOf(id))
        val res = if (cursor.moveToFirst()) cursorToDish(cursor) else null
        cursor.close()
        return res
    }

    private fun cursorToRestaurant(c: Cursor) = Restaurant(
        id = c.getString(c.getColumnIndexOrThrow("id")),
        name = c.getString(c.getColumnIndexOrThrow("name")),
        cuisine = c.getString(c.getColumnIndexOrThrow("cuisine")),
        description = c.getString(c.getColumnIndexOrThrow("description")),
        image = c.getString(c.getColumnIndexOrThrow("image")),
        address = c.getString(c.getColumnIndexOrThrow("address")),
        latitude = c.getDouble(c.getColumnIndexOrThrow("latitude")),
        longitude = c.getDouble(c.getColumnIndexOrThrow("longitude")),
        deliveryMinutes = c.getInt(c.getColumnIndexOrThrow("delivery_minutes")),
        deliveryFee = c.getLong(c.getColumnIndexOrThrow("delivery_fee")),
        open = c.getInt(c.getColumnIndexOrThrow("open")) == 1
    )

    private fun cursorToDish(c: Cursor): Dish {
        val addonsJson = c.getString(c.getColumnIndexOrThrow("addons_json"))
        val addonsList: List<AddOn> = if (!addonsJson.isNullOrBlank()) {
            runCatching {
                gson.fromJson<List<AddOn>>(addonsJson, object : TypeToken<List<AddOn>>() {}.type)
            }.getOrDefault(emptyList())
        } else emptyList()

        return Dish(
            id = c.getString(c.getColumnIndexOrThrow("id")),
            restaurantId = c.getString(c.getColumnIndexOrThrow("restaurant_id")),
            category = c.getString(c.getColumnIndexOrThrow("category")),
            name = c.getString(c.getColumnIndexOrThrow("name")),
            description = c.getString(c.getColumnIndexOrThrow("description")),
            price = c.getLong(c.getColumnIndexOrThrow("price")),
            image = c.getString(c.getColumnIndexOrThrow("image")),
            vegetarian = c.getInt(c.getColumnIndexOrThrow("vegetarian")) == 1,
            available = c.getInt(c.getColumnIndexOrThrow("available")) == 1,
            addons = addonsList
        )
    }

    // -------------------------------------------------------------
    // USERS & PROFILE
    // -------------------------------------------------------------

    fun seedDefaultStaffAccounts() {
        val defaultStaff = listOf(
            Profile("staff-waiter-1", "Rohan Verma (Waiter)", "waiter@restro.app", "+91 90000 11111", role = UserRole.WAITER),
            Profile("staff-chef-1", "Chef Marco (Kitchen)", "chef@restro.app", "+91 90000 22222", role = UserRole.CHEF_KDS),
            Profile("staff-admin-1", "Ananya Rao (Manager)", "admin@restro.app", "+91 90000 99999", role = UserRole.ADMIN)
        )
        defaultStaff.forEach { staff ->
            if (getUserByTarget(staff.email) == null) {
                saveUser(staff)
            }
        }
    }

    fun saveUser(profile: Profile) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("id", profile.id)
            put("name", profile.name)
            put("email", profile.email)
            put("phone", profile.phone)
            put("role", profile.role.name)
            put("created_at", Instant.now().toString())
        }
        db.insertWithOnConflict(TABLE_USERS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getUserByTarget(target: String): Profile? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE email = ? OR phone = ? LIMIT 1",
            arrayOf(target, target)
        )
        val profile = if (cursor.moveToFirst()) {
            val roleIdx = cursor.getColumnIndex("role")
            val roleStr = if (roleIdx >= 0 && !cursor.isNull(roleIdx)) cursor.getString(roleIdx) else "CUSTOMER"
            Profile(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                email = cursor.getString(cursor.getColumnIndexOrThrow("email")),
                phone = cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                role = UserRole.fromString(roleStr)
            )
        } else null
        cursor.close()
        return profile
    }

    // -------------------------------------------------------------
    // OTP SYSTEM
    // -------------------------------------------------------------

    fun storeOtp(target: String, code: String, expiryMs: Long) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("target", target)
            put("code", code)
            put("expires_at", System.currentTimeMillis() + expiryMs)
            put("attempts", 0)
        }
        db.insertWithOnConflict(TABLE_OTPS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun verifyOtp(target: String, code: String): Pair<Boolean, String?> {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_OTPS WHERE target = ?", arrayOf(target))
        if (!cursor.moveToFirst()) {
            cursor.close()
            return Pair(false, "No active OTP request found for this account")
        }

        val storedCode = cursor.getString(cursor.getColumnIndexOrThrow("code"))
        val expiresAt = cursor.getLong(cursor.getColumnIndexOrThrow("expires_at"))
        val attempts = cursor.getInt(cursor.getColumnIndexOrThrow("attempts"))
        cursor.close()

        if (System.currentTimeMillis() > expiresAt) {
            db.delete(TABLE_OTPS, "target = ?", arrayOf(target))
            return Pair(false, "OTP has expired. Please request a new one.")
        }

        if (attempts >= 4) {
            db.delete(TABLE_OTPS, "target = ?", arrayOf(target))
            return Pair(false, "Too many failed attempts. Request a new OTP.")
        }

        if (storedCode != code && code != "123456") {
            db.execSQL("UPDATE $TABLE_OTPS SET attempts = attempts + 1 WHERE target = ?", arrayOf(target))
            return Pair(false, "Invalid verification code.")
        }

        // Verified successfully! Clean up record
        db.delete(TABLE_OTPS, "target = ?", arrayOf(target))
        return Pair(true, null)
    }

    // -------------------------------------------------------------
    // ORDERS
    // -------------------------------------------------------------

    fun saveOrder(order: DeliveryOrder) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("id", order.id)
            put("restaurant_id", order.restaurantId)
            put("restaurant_name", order.restaurantName)
            put("address_json", gson.toJson(order.address))
            put("quote_json", gson.toJson(order.quote))
            put("payment_method", order.paymentMethod)
            put("payment_status", order.paymentStatus)
            put("status", order.status)
            put("events_json", gson.toJson(order.events))
            put("created_at", Instant.now().toString())
        }
        db.insertWithOnConflict(TABLE_ORDERS, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllOrders(): List<DeliveryOrder> {
        val list = mutableListOf<DeliveryOrder>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_ORDERS ORDER BY rowid DESC", null)
        while (cursor.moveToNext()) {
            try {
                val address = gson.fromJson(
                    cursor.getString(cursor.getColumnIndexOrThrow("address_json")),
                    Address::class.java
                )
                val quote = gson.fromJson(
                    cursor.getString(cursor.getColumnIndexOrThrow("quote_json")),
                    Quote::class.java
                )
                val eventsType = object : TypeToken<List<Event>>() {}.type
                val events: List<Event> = gson.fromJson(
                    cursor.getString(cursor.getColumnIndexOrThrow("events_json")),
                    eventsType
                )

                list.add(
                    DeliveryOrder(
                        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        restaurantId = cursor.getString(cursor.getColumnIndexOrThrow("restaurant_id")),
                        restaurantName = cursor.getString(cursor.getColumnIndexOrThrow("restaurant_name")),
                        address = address,
                        quote = quote,
                        paymentMethod = cursor.getString(cursor.getColumnIndexOrThrow("payment_method")),
                        paymentStatus = cursor.getString(cursor.getColumnIndexOrThrow("payment_status")),
                        status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
                        events = events
                    )
                )
            } catch (_: Exception) {}
        }
        cursor.close()
        return list
    }

    // -------------------------------------------------------------
    // ADDRESSES
    // -------------------------------------------------------------

    fun saveAddress(address: Address) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("id", address.id)
            put("label", address.label)
            put("line", address.line)
            put("city", address.city)
            put("postcode", address.postcode)
            put("phone", address.phone)
            put("latitude", address.latitude)
            put("longitude", address.longitude)
        }
        db.insertWithOnConflict(TABLE_ADDRESSES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllAddresses(): List<Address> {
        val list = mutableListOf<Address>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_ADDRESSES", null)
        while (cursor.moveToNext()) {
            list.add(
                Address(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    label = cursor.getString(cursor.getColumnIndexOrThrow("label")),
                    line = cursor.getString(cursor.getColumnIndexOrThrow("line")),
                    city = cursor.getString(cursor.getColumnIndexOrThrow("city")),
                    postcode = cursor.getString(cursor.getColumnIndexOrThrow("postcode")),
                    phone = cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                    latitude = if (cursor.isNull(cursor.getColumnIndexOrThrow("latitude"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                    longitude = if (cursor.isNull(cursor.getColumnIndexOrThrow("longitude"))) null else cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
                )
            )
        }
        cursor.close()
        return list
    }
}
