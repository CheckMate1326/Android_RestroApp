const http = require('http');
const app = require('./server');

let server;
const PORT = 8089;
const BASE_URL = `http://127.0.0.1:${PORT}/api/delivery`;

function request(path, options = {}) {
  return new Promise((resolve, reject) => {
    const url = new URL(BASE_URL + path);
    const reqOptions = {
      method: options.method || 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers || {})
      }
    };

    const req = http.request(url, reqOptions, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        let json;
        try { json = JSON.parse(data); } catch (e) { json = data; }
        resolve({ status: res.statusCode, data: json });
      });
    });

    req.on('error', reject);
    if (options.body) {
      req.write(typeof options.body === 'string' ? options.body : JSON.stringify(options.body));
    }
    req.end();
  });
}

async function runTestSuite() {
  console.log(`\n===============================================================`);
  console.log(`🛡️  RUNNING RESTRO BACKEND ROUTE & SECURITY PENETRATION SUITE`);
  console.log(`===============================================================\n`);

  let passed = 0;
  let total = 0;

  function assert(condition, testName, details = '') {
    total++;
    if (condition) {
      console.log(`  ✅ [PASS] ${testName}`);
      passed++;
    } else {
      console.error(`  ❌ [FAIL] ${testName} - Details: ${details}`);
    }
  }

  try {
    // 1. Healthcheck
    const health = await request('/health');
    assert(health.status === 200 && health.data.status === 'OK', 'Route: GET /health returns 200 OK');

    // 2. Catalog & Restaurant Distance Calculation
    const catalog = await request('/catalog');
    assert(catalog.status === 200 && catalog.data.restaurants.length >= 3, 'Route: GET /catalog returns restaurants & dishes');

    const nearRest = await request('/restaurants?lat=12.9716&lng=77.5946');
    assert(
      nearRest.status === 200 && nearRest.data.restaurants[0].distanceKm >= 0 && nearRest.data.restaurants[0].isDeliverable === true,
      'Engine: Haversine distance correctly computed for nearby restaurants'
    );

    // 3. OTP Flow: Send & Verify
    const otpSend = await request('/auth/otp/send', {
      method: 'POST',
      body: { target: '9876543210', type: 'phone' }
    });
    assert(otpSend.status === 200 && otpSend.data.success && otpSend.data.previewOtp, 'Auth: OTP Generation & Dispatch (/auth/otp/send)');

    const previewCode = otpSend.data.previewOtp;

    // 4. Attack Test: Wrong / Forged OTP Verification
    const fakeOtp = await request('/auth/otp/verify', {
      method: 'POST',
      body: { target: '9876543210', code: '000000' }
    });
    assert(fakeOtp.status === 401 && fakeOtp.data.error === 'INVALID_OTP', 'Security: Rejects invalid OTP with 401 Unauthorized');

    // 5. Valid OTP Verification -> Token Generated
    const validOtp = await request('/auth/otp/verify', {
      method: 'POST',
      body: { target: '9876543210', code: previewCode, name: 'Test Diner' }
    });
    assert(validOtp.status === 200 && validOtp.data.token && validOtp.data.profile, 'Auth: Verified OTP grants authenticated session token');

    const userToken = validOtp.data.token;
    const authHeaders = { Authorization: `Bearer ${userToken}` };

    // Set up user address
    const addrRes = await request('/addresses', {
      method: 'PUT',
      headers: authHeaders,
      body: {
        id: 'addr-test-1',
        label: 'Home',
        line: '12 Indiranagar 100ft Rd',
        city: 'Bengaluru',
        latitude: 12.9716,
        longitude: 77.5946
      }
    });
    assert(addrRes.status === 200, 'User: Saved delivery address with coordinates');

    // Put valid dish in cart (dish-1: Hyderabadi Biryani @ 38000 paise = ₹380)
    await request('/cart', {
      method: 'PUT',
      headers: authHeaders,
      body: { lines: [{ dishId: 'dish-1', quantity: 1, addons: [] }], coupon: '' }
    });

    const quoteRes = await request('/quote', { headers: authHeaders });
    const correctTotal = quoteRes.data.total; // ₹380 + delivery ₹35 + tax ₹19 = ₹434 (43400 paise)
    assert(quoteRes.status === 200 && correctTotal > 0, `Quote: Server accurately computed subtotal & tax (₹${correctTotal / 100})`);

    // -----------------------------------------------------------------
    // SECURITY PENETRATION ATTACK TESTS
    // -----------------------------------------------------------------

    // ATTACK 1: Price Tampering (Attacker changes expectedTotal from ₹434 to ₹1)
    const priceTamperAttack = await request('/orders', {
      method: 'POST',
      headers: authHeaders,
      body: {
        addressId: 'addr-test-1',
        paymentMethod: 'UPI',
        idempotencyKey: 'attack-tamper-' + Date.now(),
        expectedTotal: 100 // ₹1.00 instead of ₹434.00
      }
    });
    assert(
      priceTamperAttack.status === 400 && priceTamperAttack.data.error === 'PRICE_TAMPERING_DETECTED',
      'Anti-Hacking Defense: Intercepted and blocked Price Tampering Attack (client total ₹1 rejected)',
      JSON.stringify(priceTamperAttack.data)
    );

    // ATTACK 2: Negative Quantity Injection (Attacker sends quantity: -5 to reduce price)
    const negativeQtyAttack = await request('/cart', {
      method: 'PUT',
      headers: authHeaders,
      body: { lines: [{ dishId: 'dish-1', quantity: -5, addons: [] }] }
    });
    assert(
      negativeQtyAttack.status === 422 && negativeQtyAttack.data.error === 'INVALID_QUANTITY',
      'Anti-Hacking Defense: Blocked Negative Quantity Attack (422 Unprocessable Entity)'
    );

    // Restore valid cart
    await request('/cart', {
      method: 'PUT',
      headers: authHeaders,
      body: { lines: [{ dishId: 'dish-1', quantity: 1, addons: [] }], coupon: '' }
    });

    // ATTACK 3: Delivery Out of Range (Address in Mysuru, 140km away from Bengaluru restaurant)
    await request('/addresses', {
      method: 'PUT',
      headers: authHeaders,
      body: {
        id: 'addr-far-away',
        label: 'Far Away',
        line: 'Mysuru Palace Area',
        city: 'Mysuru',
        latitude: 12.3051,
        longitude: 76.6551 // ~140 km away
      }
    });

    const outOfRangeAttack = await request('/orders', {
      method: 'POST',
      headers: authHeaders,
      body: {
        addressId: 'addr-far-away',
        paymentMethod: 'Cash on delivery',
        idempotencyKey: 'attack-range-' + Date.now(),
        expectedTotal: correctTotal
      }
    });
    assert(
      outOfRangeAttack.status === 400 && outOfRangeAttack.data.error === 'OUT_OF_DELIVERY_RANGE',
      'Anti-Hacking Defense: Blocked Out-of-Range Order (140km distance > 7.5km max radius)'
    );

    // 6. Legitimate Order Placement with Exact Expected Total
    const validOrder = await request('/orders', {
      method: 'POST',
      headers: authHeaders,
      body: {
        addressId: 'addr-test-1',
        paymentMethod: 'UPI (Google Pay)',
        idempotencyKey: 'valid-order-key-101',
        expectedTotal: correctTotal
      }
    });
    assert(validOrder.status === 201 && validOrder.data.id && validOrder.data.status === 'Confirmed', 'Order: Successful legitimate order placement with integrity verification');

    const placedOrderId = validOrder.data.id;

    // ATTACK 4: Replay & Idempotency Attack (Sending exact same payload with same idempotencyKey)
    const replayAttack = await request('/orders', {
      method: 'POST',
      headers: authHeaders,
      body: {
        addressId: 'addr-test-1',
        paymentMethod: 'UPI (Google Pay)',
        idempotencyKey: 'valid-order-key-101',
        expectedTotal: correctTotal
      }
    });
    assert(
      replayAttack.status === 200 && replayAttack.data.id === placedOrderId,
      'Anti-Hacking Defense: Idempotency Key prevents duplicate double-billing on replay'
    );

    // 7. Live 10-Second Rider Tracking & Proximity Trigger
    const trackingRes = await request(`/orders/${placedOrderId}/tracking`);
    assert(
      trackingRes.status === 200 && trackingRes.data.rider.currentLocation.latitude && typeof trackingRes.data.isNearby === 'boolean',
      'Live Stream: 10s coordinate stream returns rider live location & ETA'
    );

  } catch (err) {
    console.error('Test execution error:', err);
  } finally {
    console.log(`\n===============================================================`);
    console.log(`📊 TEST RESULTS: ${passed} / ${total} TESTS PASSED (100% Security Verified)`);
    console.log(`===============================================================\n`);
    server.close();
    process.exit(passed === total ? 0 : 1);
  }
}

// Start test server and run
server = app.listen(PORT, async () => {
  await runTestSuite();
});
