package com.restaurant.mobile.delivery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TableBookingTest {

    @Test
    fun testTableBookingCreationAndDefaults() {
        val booking = TableBooking(
            userId = "usr-1",
            userName = "Aarav Sharma",
            userPhone = "+91 98765 43210",
            tableId = "tbl-1",
            tableNumber = "T1",
            bookingDate = "2026-10-20",
            timeSlot = "19:00 - 20:30",
            partySize = 4
        )

        assertNotNull(booking.id)
        assertTrue(booking.id.isNotBlank())
        assertEquals("usr-1", booking.userId)
        assertEquals("Aarav Sharma", booking.userName)
        assertEquals("+91 98765 43210", booking.userPhone)
        assertEquals("tbl-1", booking.tableId)
        assertEquals("T1", booking.tableNumber)
        assertEquals("2026-10-20", booking.bookingDate)
        assertEquals("19:00 - 20:30", booking.timeSlot)
        assertEquals(4, booking.partySize)
        assertEquals("Main Dining Room", booking.seatingArea)
        assertEquals("", booking.specialRequests)
        assertEquals("CONFIRMED", booking.status)
        assertNotNull(booking.createdAt)
        assertTrue(booking.createdAt.isNotBlank())
    }

    @Test
    fun testTableBookingStatusConstants() {
        assertEquals("CONFIRMED", TableBookingStatus.CONFIRMED)
        assertEquals("SEATED", TableBookingStatus.SEATED)
        assertEquals("COMPLETED", TableBookingStatus.COMPLETED)
        assertEquals("CANCELLED", TableBookingStatus.CANCELLED)
        assertEquals(4, TableBookingStatus.ALL.size)
        assertTrue(TableBookingStatus.ALL.contains("CONFIRMED"))
    }

    @Test
    fun testGenerateId() {
        val id = TableBooking.generateId()
        assertTrue(id.startsWith("TB-"))
        assertEquals(11, id.length) // "TB-" (3) + 8 chars = 11
    }
}
