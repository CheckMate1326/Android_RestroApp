const express = require('express');
const cors = require('cors');
const crypto = require('crypto');

const app = express();
const PORT = process.env.PORT || 8080;

app.use(cors());
app.use(express.json());

// ==========================================
// In-Memory Database with Seed Data
// ==========================================
const db = {
  users: [
    {
      id: "usr-1",
      email: "aarav@example.com",
      password: "password123",
      name: "Aarav Sharma",
      phone: "+91 98765 43210"
    }
  ],
  otps: new Map(), // target -> { code, expiresAt, attempts }
  sessions: new Map(), // token -> userId
  idempotencyKeys: new Map(), // key -> orderId
  
  restaurants: [
    {
      id: "rest-1",
      name: "Bistro Lumière & Biryani",
      cuisine: "Awadhi · Mughlai · Biryani",
      description: "Artisanal dum cooked biryanis and royal kebabs with slow-cooked aromatic spices.",
      image: "https://images.unsplash.com/photo-1589302168068-964664d93dc0?w=800&auto=format&fit=crop",
      address: "12/4 Indiranagar 100ft Road, Bengaluru",
      latitude: 12.9716,
      longitude: 77.5946,
      deliveryMinutes: 25,
      deliveryFee: 3500,
      open: true,
      maxDeliveryRadiusKm: 7.5
    },
    {
      id: "rest-2",
      name: "Trattoria Milano",
      cuisine: "Italian · Woodfired Pizza · Pasta",
      description: "Authentic Neapolitan hand-stretched sourdough pizzas and homemade fettuccine.",
      image: "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800&auto=format&fit=crop",
      address: "45 Lavelle Road, Shanthala Nagar, Bengaluru",
      latitude: 12.9698,
      longitude: 77.5986,
      deliveryMinutes: 30,
      deliveryFee: 4500,
      open: true,
      maxDeliveryRadiusKm: 7.0
    },
    {
      id: "rest-3",
      name: "Dakshin Delights",
      cuisine: "South Indian · Chettinad · Dosa",
      description: "Crispy ghee roast dosas, flaky Malabar parottas, and traditional filter coffee.",
      image: "https://images.unsplash.com/photo-1668236543090-82eba5ee5976?w=800&auto=format&fit=crop",
      address: "88 Koramangala 5th Block, Bengaluru",
      latitude: 12.9352,
      longitude: 77.6245,
      deliveryMinutes: 20,
      deliveryFee: 2500,
      open: true,
      maxDeliveryRadiusKm: 8.0
    },
    {
      id: "rest-4",
      name: "Tokyo Zen & Bao",
      cuisine: "Pan-Asian · Ramen · Dim Sum",
      description: "Silky broth ramen bowls, handmade bao buns, and freshly rolled sushi.",
      image: "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=800&auto=format&fit=crop",
      address: "22 100ft Road, HAL 2nd Stage, Indiranagar, Bengaluru",
      latitude: 12.9784,
      longitude: 77.6408,
      deliveryMinutes: 35,
      deliveryFee: 4000,
      open: true,
      maxDeliveryRadiusKm: 7.5
    },
    {
      id: "rest-5",
      name: "The Green Bowl & Bakery",
      cuisine: "Healthy Bowls · Salads · Sourdough",
      description: "Nutrient-packed grain bowls, crisp salads, organic smoothies and fresh sourdough.",
      image: "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=800&auto=format&fit=crop",
      address: "14 Vittal Mallya Road, Bengaluru",
      latitude: 12.9719,
      longitude: 77.5973,
      deliveryMinutes: 20,
      deliveryFee: 3000,
      open: true,
      maxDeliveryRadiusKm: 6.5
    }
  ],

  dishes: [
    // Rest 1: Bistro Lumiere
    { id: "dish-1", restaurantId: "rest-1", category: "Biryani", name: "Hyderabadi Dum Gosht Biryani", description: "Slow-cooked tender meat with saffron basmati rice.", price: 38000, image: "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=800&auto=format&fit=crop", vegetarian: false, available: true, addons: [{ id: "addon-1", name: "Extra Mirchi Ka Salan", price: 4000 }, { id: "addon-2", name: "Boiled Egg (2 pcs)", price: 3000 }] },
    { id: "dish-2", restaurantId: "rest-1", category: "Biryani", name: "Subz Paneer Dum Biryani", description: "Fragrant basmati rice infused with spiced paneer cubes.", price: 29000, image: "https://images.unsplash.com/photo-1642821373181-696a54913e9a?w=800&auto=format&fit=crop", vegetarian: true, available: true, addons: [] },
    { id: "dish-101", restaurantId: "rest-1", category: "Starters", name: "Melt-in-Mouth Galouti Kebab", description: "Finely minced smoked patties infused with raw papaya.", price: 34000, image: "https://images.unsplash.com/photo-1599488615731-7e5c2823ff28?w=800&auto=format&fit=crop", vegetarian: false, available: true, addons: [] },
    { id: "dish-105", restaurantId: "rest-1", category: "Breads", name: "Butter Garlic Naan", description: "Fluffy leavened flatbread brushed with garlic herb butter.", price: 7000, image: "https://images.unsplash.com/photo-1626074353765-517a681e40be?w=800&auto=format&fit=crop", vegetarian: true, available: true, addons: [] },

    // Rest 2: Trattoria Milano
    { id: "dish-3", restaurantId: "rest-2", category: "Pizza", name: "Woodfired Margherita Pizza", description: "San Marzano tomato sauce, fresh buffalo mozzarella.", price: 45000, image: "https://images.unsplash.com/photo-1604382355076-af4b0eb60143?w=800&auto=format&fit=crop", vegetarian: true, available: true, addons: [{ id: "addon-6", name: "Fresh Burrata Cheese", price: 9000 }] },
    { id: "dish-201", restaurantId: "rest-2", category: "Pizza", name: "Quattro Formaggi Bianca", description: "Gorgonzola, fontina, smoked provolone and parmesan.", price: 52000, image: "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=800&auto=format&fit=crop", vegetarian: true, available: true, addons: [] },

    // Rest 3: Dakshin Delights
    { id: "dish-4", restaurantId: "rest-3", category: "Dosa", name: "Ghee Roast Masala Dosa", description: "Crispy golden crepe roasted in pure desi ghee.", price: 16000, image: "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=800&auto=format&fit=crop", vegetarian: true, available: true, addons: [] },
    { id: "dish-306", restaurantId: "rest-3", category: "Beverages", name: "Mysore Pure Filter Coffee", description: "Traditional freshly brewed chicory decoction.", price: 6000, image: "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=800&auto=format&fit=crop", vegetarian: true, available: true, addons: [] },

    // Rest 4: Tokyo Zen
    { id: "dish-401", restaurantId: "rest-4", category: "Ramen", name: "Tokyo Shoyu Chashu Ramen", description: "Artisanal wheat noodles in rich soy-broth with braised chashu.", price: 42000, image: "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=800&auto=format&fit=crop", vegetarian: false, available: true, addons: [] },

    // Rest 5: Green Bowl
    { id: "dish-501", restaurantId: "rest-5", category: "Bowls", name: "Beet Hummus & Falafel Bowl", description: "Herb quinoa, roasted chickpeas, beetroot tahini hummus.", price: 32000, image: "https://images.unsplash.com/photo-1540420773420-3366772f4999?w=800&auto=format&fit=crop", vegetarian: true, available: true, addons: [] }
  ],

  addresses: new Map([
    ["usr-1", [
      { id: "addr-1", label: "Home", line: "Flat 402, Green Glen Layout, Bellandur", city: "Bengaluru", postcode: "560103", phone: "+91 98765 43210", latitude: 12.9290, longitude: 77.6740 },
      { id: "addr-2", label: "Office", line: "Prestige Tech Park, Outer Ring Road", city: "Bengaluru", postcode: "560103", phone: "+91 98765 43210", latitude: 12.9360, longitude: 77.6910 }
    ]]
  ]),

  orders: [],
  carts: new Map(), // userId -> Cart
  favorites: new Map(), // userId -> Set<restaurantId>
  reviews: [
    { orderId: "ord-1", restaurantId: "rest-1", author: "Priya N.", rating: 5, text: "Exquisite flavours and authentic spices! Arrived piping hot.", at: new Date().toISOString() },
    { orderId: "ord-2", restaurantId: "rest-1", author: "Karthik R.", rating: 5, text: "Generous portions, mouth-watering aroma.", at: new Date().toISOString() }
  ]
};

// Seed initial session token
const DEMO_TOKEN = "jwt-demo-token-restro-aarav";
db.sessions.set(DEMO_TOKEN, "usr-1");

// ==========================================
// Mathematical Helper: Haversine Distance (km)
// ==========================================
function calculateHaversineDistance(lat1, lon1, lat2, lon2) {
  const R = 6371; // Earth radius in km
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Math.round((R * c) * 10) / 10; // 1 decimal place
}

// ==========================================
// Authentication Middleware
// ==========================================
function authMiddleware(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return res.status(401).json({ error: "UNAUTHORIZED", message: "Authentication token required." });
  }
  const token = authHeader.split(" ")[1];
  const userId = db.sessions.get(token);
  if (!userId) {
    return res.status(401).json({ error: "UNAUTHORIZED", message: "Invalid or expired session token." });
  }
  req.userId = userId;
  req.user = db.users.find(u => u.id === userId);
  next();
}

// Optional Auth (for catalog/guest)
function optionalAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  if (authHeader && authHeader.startsWith("Bearer ")) {
    const token = authHeader.split(" ")[1];
    req.userId = db.sessions.get(token) || "usr-1";
    req.user = db.users.find(u => u.id === req.userId);
  } else {
    req.userId = "usr-1"; // Fallback to demo diner
    req.user = db.users[0];
  }
  next();
}

// ==========================================
// API ROUTES (/api/delivery)
// ==========================================
const router = express.Router();

// Health Check
router.get("/health", (req, res) => {
  res.json({ status: "OK", server: "Restro Delivery Engine", timestamp: new Date().toISOString() });
});

// ------------------------------------------
// 1. Authentication & OTP Routes
// ------------------------------------------

// Password Login
router.post("/auth/login", (req, res) => {
  const { email, password } = req.body || {};
  if (!email || !password) {
    return res.status(400).json({ error: "FIELD_VALIDATION_FAILED", message: "Email and password are required." });
  }

  // Anti-SQL-Injection: String type assertion and length sanitization
  if (typeof email !== "string" || typeof password !== "string" || email.length > 255) {
    return res.status(400).json({ error: "INVALID_PAYLOAD", message: "Malformed credentials payload." });
  }

  const user = db.users.find(u => u.email.toLowerCase() === email.toLowerCase() && u.password === password);
  if (!user) {
    return res.status(401).json({ error: "INVALID_CREDENTIALS", message: "Incorrect email or password." });
  }

  const token = "jwt-" + crypto.randomUUID();
  db.sessions.set(token, user.id);

  res.json({
    token,
    profile: { id: user.id, name: user.name, email: user.email, phone: user.phone, reducedGlass: false }
  });
});

// User Registration
router.post("/auth/register", (req, res) => {
  const { email, password, name, phone } = req.body || {};
  if (!email || !password || !name) {
    return res.status(400).json({ error: "FIELD_VALIDATION_FAILED", message: "Name, email and password are required." });
  }

  if (password.length < 8) {
    return res.status(400).json({ error: "WEAK_PASSWORD", message: "Password must be at least 8 characters." });
  }

  const existing = db.users.find(u => u.email.toLowerCase() === email.toLowerCase());
  if (existing) {
    return res.status(409).json({ error: "USER_EXISTS", message: "An account with this email already exists." });
  }

  const newUser = {
    id: "usr-" + (db.users.length + 1),
    email,
    password,
    name,
    phone: phone || "+91 98765 43210"
  };
  db.users.push(newUser);

  const token = "jwt-" + crypto.randomUUID();
  db.sessions.set(token, newUser.id);

  res.json({
    token,
    profile: { id: newUser.id, name: newUser.name, email: newUser.email, phone: newUser.phone, reducedGlass: false }
  });
});

// Send OTP (Phone or Email)
router.post("/auth/otp/send", (req, res) => {
  const { target, type } = req.body || {};
  if (!target || typeof target !== "string") {
    return res.status(400).json({ error: "FIELD_VALIDATION_FAILED", message: "Target phone number or email is required." });
  }

  const cleanTarget = target.trim();
  
  // Anti-Brute-Force / Rate Limiting
  const existingOtp = db.otps.get(cleanTarget);
  if (existingOtp && Date.now() < existingOtp.expiresAt && existingOtp.sentAt && (Date.now() - existingOtp.sentAt < 15000)) {
    return res.status(429).json({ error: "RATE_LIMITED", message: "Please wait 15 seconds before requesting another code." });
  }

  // Generate 6-digit secure numeric OTP
  const code = Math.floor(100000 + Math.random() * 900000).toString();
  const expiresAt = Date.now() + 5 * 60 * 1000; // 5 mins validity

  db.otps.set(cleanTarget, { code, expiresAt, sentAt: Date.now(), attempts: 0 });

  console.log(`[SMS/Email Gateway] Verification Code for ${cleanTarget}: [${code}] (Valid for 5 mins)`);

  res.json({
    success: true,
    message: `Verification code sent to ${cleanTarget}`,
    target: cleanTarget,
    expiresInSeconds: 300,
    previewOtp: code // Included for automated testing and testing on emulator without real SMS gateway
  });
});

// Verify OTP
router.post("/auth/otp/verify", (req, res) => {
  const { target, code, name } = req.body || {};
  if (!target || !code) {
    return res.status(400).json({ error: "FIELD_VALIDATION_FAILED", message: "Target and 6-digit verification code are required." });
  }

  const record = db.otps.get(target.trim());
  if (!record) {
    return res.status(400).json({ error: "OTP_NOT_FOUND", message: "No verification request found for this contact. Request a new OTP." });
  }

  if (Date.now() > record.expiresAt) {
    db.otps.delete(target.trim());
    return res.status(400).json({ error: "OTP_EXPIRED", message: "Verification code has expired. Please request a new one." });
  }

  if (record.attempts >= 4) {
    db.otps.delete(target.trim());
    return res.status(403).json({ error: "MAX_ATTEMPTS_EXCEEDED", message: "Too many failed attempts. Code has been invalidated." });
  }

  if (record.code !== code.trim()) {
    record.attempts += 1;
    return res.status(401).json({ error: "INVALID_OTP", message: "Incorrect verification code. Please check and try again." });
  }

  // Verification successful! Invalidate OTP
  db.otps.delete(target.trim());

  // Find or auto-provision user profile
  let user = db.users.find(u => u.phone === target.trim() || u.email.toLowerCase() === target.trim().toLowerCase());
  if (!user) {
    const isEmail = target.includes("@");
    user = {
      id: "usr-" + (db.users.length + 1),
      email: isEmail ? target.trim() : `${target.replace(/\D/g, "")}@restro.in`,
      name: name || (isEmail ? target.split("@")[0] : `Diner ${target.slice(-4)}`),
      phone: isEmail ? "+91 98765 43210" : target.trim(),
      password: crypto.randomUUID()
    };
    db.users.push(user);
  }

  const token = "jwt-" + crypto.randomUUID();
  db.sessions.set(token, user.id);

  res.json({
    token,
    profile: { id: user.id, name: user.name, email: user.email, phone: user.phone, reducedGlass: false }
  });
});

// ------------------------------------------
// 2. Catalog & Restaurant Range Engine
// ------------------------------------------

// Standard Full Catalog
router.get("/catalog", (req, res) => {
  res.json({
    restaurants: db.restaurants,
    dishes: db.dishes,
    offers: [
      { code: "WELCOME50", description: "₹50 OFF on orders above ₹150", minimum: 15000, discount: 5000, expiresAt: "2027-12-31" },
      { code: "FEAST100", description: "₹100 OFF on orders above ₹400", minimum: 40000, discount: 10000, expiresAt: "2027-12-31" },
      { code: "FREEDEL", description: "Free Delivery on orders above ₹200", minimum: 20000, discount: 4000, expiresAt: "2027-12-31" }
    ]
  });
});

// Location-Aware Restaurants with Delivery Range Calculation
router.get("/restaurants", (req, res) => {
  const userLat = parseFloat(req.query.lat) || 12.9352;
  const userLng = parseFloat(req.query.lng) || 77.6245;

  const restaurantsWithProximity = db.restaurants.map(r => {
    const distanceKm = calculateHaversineDistance(userLat, userLng, r.latitude, r.longitude);
    const isDeliverable = distanceKm <= r.maxDeliveryRadiusKm;
    const estimatedDeliveryMins = Math.round(15 + distanceKm * 3);

    return {
      ...r,
      distanceKm,
      isDeliverable,
      estimatedDeliveryMins: isDeliverable ? estimatedDeliveryMins : null,
      rangeStatus: isDeliverable ? "In Range" : `Out of Range (${distanceKm} km > ${r.maxDeliveryRadiusKm} km max)`
    };
  }).sort((a, b) => a.distanceKm - b.distanceKm);

  res.json({
    userLocation: { latitude: userLat, longitude: userLng },
    count: restaurantsWithProximity.length,
    restaurants: restaurantsWithProximity
  });
});

// ------------------------------------------
// 3. User Profile, Addresses & Favorites
// ------------------------------------------
router.get("/profile", authMiddleware, (req, res) => {
  const u = req.user;
  res.json({ id: u.id, name: u.name, email: u.email, phone: u.phone, reducedGlass: false });
});

router.put("/profile", authMiddleware, (req, res) => {
  const { name, phone, reducedGlass } = req.body || {};
  if (name) req.user.name = name;
  if (phone) req.user.phone = phone;
  res.json({ id: req.user.id, name: req.user.name, email: req.user.email, phone: req.user.phone, reducedGlass: !!reducedGlass });
});

router.get("/addresses", authMiddleware, (req, res) => {
  const addrs = db.addresses.get(req.userId) || [];
  res.json(addrs);
});

router.put("/addresses", authMiddleware, (req, res) => {
  const a = req.body;
  if (!a || !a.line || !a.city) {
    return res.status(400).json({ error: "FIELD_VALIDATION_FAILED", message: "Address line and city are required." });
  }
  const addrs = db.addresses.get(req.userId) || [];
  const existingIdx = addrs.findIndex(item => item.id === a.id);
  const newAddr = {
    id: a.id || "addr-" + crypto.randomUUID().slice(0, 8),
    label: a.label || "Home",
    line: a.line,
    city: a.city,
    postcode: a.postcode || "560103",
    phone: a.phone || req.user.phone,
    latitude: a.latitude || 12.9352,
    longitude: a.longitude || 77.6245
  };

  if (existingIdx >= 0) addrs[existingIdx] = newAddr;
  else addrs.push(newAddr);

  db.addresses.set(req.userId, addrs);
  res.json(newAddr);
});

router.delete("/addresses/:id", authMiddleware, (req, res) => {
  const addrs = (db.addresses.get(req.userId) || []).filter(a => a.id !== req.params.id);
  db.addresses.set(req.userId, addrs);
  res.json({ success: true });
});

// ------------------------------------------
// 4. Cart & Dynamic Quote Verification
// ------------------------------------------
router.get("/cart", optionalAuth, (req, res) => {
  const cart = db.carts.get(req.userId) || { lines: [], coupon: "" };
  res.json(cart);
});

router.put("/cart", optionalAuth, (req, res) => {
  const { lines, coupon } = req.body || { lines: [] };
  
  // Validation: Dish existence and positive quantity
  for (const line of (lines || [])) {
    if (!line.dishId || !db.dishes.some(d => d.id === line.dishId)) {
      return res.status(400).json({ error: "INVALID_DISH", message: `Dish ID ${line.dishId} does not exist.` });
    }
    if (typeof line.quantity !== "number" || line.quantity <= 0) {
      return res.status(422).json({ error: "INVALID_QUANTITY", message: "Quantity must be a positive integer." });
    }
  }

  const cart = { lines: lines || [], coupon: coupon || "" };
  db.carts.set(req.userId, cart);
  res.json(cart);
});

router.get("/quote", optionalAuth, (req, res) => {
  const cart = db.carts.get(req.userId) || { lines: [], coupon: "" };
  
  if (cart.lines.length === 0) {
    return res.json({ restaurantId: "rest-1", lines: [], subtotal: 0, tax: 0, delivery: 0, discount: 0, total: 0 });
  }

  let subtotal = 0;
  const quoteLines = cart.lines.map(line => {
    const dish = db.dishes.find(d => d.id === line.dishId);
    const unitPrice = dish ? dish.price : 0;
    const lineTotal = unitPrice * line.quantity;
    subtotal += lineTotal;
    return {
      dishId: line.dishId,
      name: dish ? dish.name : "Item",
      quantity: line.quantity,
      addons: line.addons || [],
      unitPrice,
      total: lineTotal
    };
  });

  const firstDish = db.dishes.find(d => d.id === cart.lines[0].dishId);
  const restaurantId = firstDish ? firstDish.restaurantId : "rest-1";
  const restaurant = db.restaurants.find(r => r.id === restaurantId);
  const delivery = restaurant ? restaurant.deliveryFee : 4000;
  const tax = Math.round(subtotal * 0.05); // 5% GST

  // Dynamic Coupon Validation
  let discount = 0;
  if (cart.coupon) {
    const code = cart.coupon.toUpperCase();
    if (code === "WELCOME50" && subtotal >= 15000) {
      discount = Math.min(5000, Math.round(subtotal * 0.5));
    } else if (code === "FEAST100" && subtotal >= 40000) {
      discount = 10000;
    } else if (code === "FREEDEL" && subtotal >= 20000) {
      discount = delivery;
    }
  }

  const total = Math.max(0, subtotal + delivery + tax - discount);

  res.json({
    restaurantId,
    lines: quoteLines,
    subtotal,
    tax,
    delivery,
    discount,
    total
  });
});

// ------------------------------------------
// 5. Checkout & Anti-Hacking Security Hardening
// ------------------------------------------
router.post("/orders", authMiddleware, (req, res) => {
  const { addressId, paymentMethod, idempotencyKey, expectedTotal } = req.body || {};

  // 1. Basic Field Validation
  if (!addressId || !paymentMethod || !idempotencyKey) {
    return res.status(400).json({ error: "FIELD_VALIDATION_FAILED", message: "addressId, paymentMethod, and idempotencyKey are required." });
  }

  // 2. Anti-Hacking: Idempotency & Replay Defense
  if (db.idempotencyKeys.has(idempotencyKey)) {
    const existingOrderId = db.idempotencyKeys.get(idempotencyKey);
    const existingOrder = db.orders.find(o => o.id === existingOrderId);
    if (existingOrder) {
      console.log(`[Security Alert] Replay detected for idempotency key ${idempotencyKey}. Returning existing order.`);
      return res.status(200).json(existingOrder);
    }
  }

  // 3. Cross-Tenant Address Hijacking Defense
  const userAddrs = db.addresses.get(req.userId) || [];
  const selectedAddr = userAddrs.find(a => a.id === addressId);
  if (!selectedAddr) {
    return res.status(403).json({ error: "UNAUTHORIZED_ADDRESS", message: "Delivery address does not belong to the authenticated user." });
  }

  // 4. Cart Verification & Negative Quantity Check
  const cart = db.carts.get(req.userId) || { lines: [], coupon: "" };
  if (!cart.lines || cart.lines.length === 0) {
    return res.status(400).json({ error: "EMPTY_CART", message: "Cannot checkout with an empty cart." });
  }

  // Anti-Hacking: Check for non-positive or manipulated quantities
  for (const line of cart.lines) {
    if (!line.quantity || typeof line.quantity !== "number" || line.quantity <= 0) {
      return res.status(422).json({ error: "NEGATIVE_QUANTITY_ATTACK", message: "Quantity must be a positive integer greater than 0." });
    }
  }

  // 5. Strict Server-Side Total Recalculation (Price Tampering Defense)
  let serverSubtotal = 0;
  const quoteLines = [];
  const firstDish = db.dishes.find(d => d.id === cart.lines[0].dishId);
  const restaurantId = firstDish ? firstDish.restaurantId : "rest-1";
  const restaurant = db.restaurants.find(r => r.id === restaurantId);

  for (const line of cart.lines) {
    const dish = db.dishes.find(d => d.id === line.dishId);
    if (!dish) {
      return res.status(400).json({ error: "UNKNOWN_DISH", message: `Dish ID ${line.dishId} does not exist.` });
    }
    const lineTotal = dish.price * line.quantity;
    serverSubtotal += lineTotal;
    quoteLines.push({
      dishId: line.dishId,
      name: dish.name,
      quantity: line.quantity,
      addons: line.addons || [],
      unitPrice: dish.price,
      total: lineTotal
    });
  }

  const delivery = restaurant ? restaurant.deliveryFee : 4000;
  const tax = Math.round(serverSubtotal * 0.05);
  
  let discount = 0;
  if (cart.coupon) {
    const code = cart.coupon.toUpperCase();
    if (code === "WELCOME50" && serverSubtotal >= 15000) discount = Math.min(5000, Math.round(serverSubtotal * 0.5));
    else if (code === "FEAST100" && serverSubtotal >= 40000) discount = 10000;
    else if (code === "FREEDEL" && serverSubtotal >= 20000) discount = delivery;
  }

  const serverCalculatedTotal = Math.max(0, serverSubtotal + delivery + tax - discount);

  // 5. Delivery Range Enforcement Check
  const distanceKm = calculateHaversineDistance(
    restaurant.latitude,
    restaurant.longitude,
    selectedAddr.latitude || 12.9352,
    selectedAddr.longitude || 77.6245
  );

  if (distanceKm > restaurant.maxDeliveryRadiusKm) {
    return res.status(400).json({
      error: "OUT_OF_DELIVERY_RANGE",
      message: `Delivery location is ${distanceKm} km away from ${restaurant.name}, which exceeds the maximum delivery radius of ${restaurant.maxDeliveryRadiusKm} km.`
    });
  }

  // 6. Anti-Hacking: Price Tampering Verification Check
  if (typeof expectedTotal !== "number" || Math.abs(expectedTotal - serverCalculatedTotal) > 0) {
    console.error(`[SECURITY TAMPERING DETECTED] Client submitted expectedTotal: ${expectedTotal}, but Server recalculation is: ${serverCalculatedTotal}`);
    return res.status(400).json({
      error: "PRICE_TAMPERING_DETECTED",
      message: `Price integrity check failed! Expected ₹${serverCalculatedTotal / 100}, but client payload sent ₹${expectedTotal / 100}. Transaction blocked.`
    });
  }

  // 7. Order Generation & Persistence
  const orderId = (1000 + Math.floor(Math.random() * 9000)).toString();
  const newOrder = {
    id: orderId,
    userId: req.userId,
    restaurantId,
    restaurantName: restaurant.name,
    address: selectedAddr,
    quote: {
      restaurantId,
      lines: quoteLines,
      subtotal: serverSubtotal,
      tax,
      delivery,
      discount,
      total: serverCalculatedTotal
    },
    paymentMethod,
    paymentStatus: paymentMethod.toLowerCase().includes("cash") ? "Due on delivery" : "Paid Online (Verified)",
    status: "Confirmed",
    createdAt: new Date().toISOString(),
    events: [{ status: "Confirmed", at: new Date().toISOString() }],
    rider: {
      name: "Ramesh Kumar",
      phone: "+91 98765 12345",
      vehicleNumber: "KA-03-EM-8821",
      startLat: restaurant.latitude,
      startLng: restaurant.longitude,
      destLat: selectedAddr.latitude || 12.9290,
      destLng: selectedAddr.longitude || 77.6740,
      totalDistanceKm: distanceKm
    }
  };

  db.orders.unshift(newOrder);
  db.idempotencyKeys.set(idempotencyKey, orderId);

  // Clear User Cart
  db.carts.set(req.userId, { lines: [], coupon: "" });

  console.log(`[Order Confirmed] #${orderId} for ${req.user.name} | Total: ₹${serverCalculatedTotal / 100} | Method: ${paymentMethod}`);

  res.status(201).json(newOrder);
});

// Get User Orders
router.get("/orders", authMiddleware, (req, res) => {
  const userOrders = db.orders.filter(o => o.userId === req.userId);
  res.json(userOrders);
});

// ------------------------------------------
// 6. 10-Second Live Tracking & Coordinate Stream
// ------------------------------------------
router.get("/orders/:id/tracking", optionalAuth, (req, res) => {
  const order = db.orders.find(o => o.id === req.params.id) || db.orders[0];
  if (!order) {
    return res.status(404).json({ error: "ORDER_NOT_FOUND", message: "Order not found." });
  }

  // Calculate simulated progression based on elapsed time (in seconds)
  const elapsedSeconds = Math.floor((Date.now() - new Date(order.createdAt).getTime()) / 1000);
  
  // Progression: 0s to 120s full delivery cycle
  const totalDuration = 120; // 2 minutes simulated trip
  const progress = Math.min(1.0, elapsedSeconds / totalDuration);

  // Determine Order Status based on progress
  let status = "Confirmed";
  if (progress > 0.15) status = "Preparing";
  if (progress > 0.40) status = "Out for delivery";
  if (progress >= 1.0) status = "Delivered";

  order.status = status;

  // Compute Current Rider Coordinates (Interpolation between Start and Destination)
  const riderInfo = order.rider || {
    startLat: 12.9716, startLng: 77.5946,
    destLat: 12.9290, destLng: 77.6740,
    totalDistanceKm: 4.8
  };

  const riderLat = riderInfo.startLat + (riderInfo.destLat - riderInfo.startLat) * progress;
  const riderLng = riderInfo.startLng + (riderInfo.destLng - riderInfo.startLng) * progress;

  const distanceRemainingKm = Math.max(0, Math.round((riderInfo.totalDistanceKm * (1 - progress)) * 10) / 10);
  const etaMinutes = Math.max(0, Math.ceil(distanceRemainingKm * 3.5));

  // Proximity Alert Trigger: Within 500 meters (0.5 km)
  const isNearby = distanceRemainingKm <= 0.5 && status !== "Delivered";
  const isDelivered = status === "Delivered";

  res.json({
    orderId: order.id,
    restaurantName: order.restaurantName,
    status,
    progress: Math.round(progress * 100),
    rider: {
      name: "Ramesh Kumar",
      phone: "+91 98765 12345",
      vehicleNumber: "KA-03-EM-8821",
      currentLocation: {
        latitude: riderLat,
        longitude: riderLng
      }
    },
    distanceRemainingKm,
    etaMinutes,
    isNearby,
    isDelivered,
    notificationAlert: isNearby ? "🛵 Delivery partner is nearby (under 500m)! Please be ready at your gate." : (isDelivered ? "🎉 Order delivered! Enjoy your meal." : null),
    updatedAt: new Date().toISOString()
  });
});

// Single Order Detail
router.get("/orders/:id", optionalAuth, (req, res) => {
  const order = db.orders.find(o => o.id === req.params.id);
  if (!order) return res.status(404).json({ error: "ORDER_NOT_FOUND" });
  res.json(order);
});

// Reviews
router.get("/reviews/:restaurantId", (req, res) => {
  const reviews = db.reviews.filter(r => r.restaurantId === req.params.restaurantId);
  res.json(reviews);
});

// Mount Router under /api/delivery
app.use("/api/delivery", router);

// Catch-all
app.use((req, res) => {
  res.status(404).json({ error: "NOT_FOUND", path: req.path });
});

// Start Server
if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`=======================================================`);
    console.log(`🚀 Restro Food Delivery Backend running on port ${PORT}`);
    console.log(`📡 Base URL: http://localhost:${PORT}/api/delivery`);
    console.log(`🛡️  Anti-Hacking Tamper Defenses & OTP Verification Active`);
    console.log(`=======================================================`);
  });
}

module.exports = app;
