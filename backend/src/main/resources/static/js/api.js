/**
 * ShopEase – Shared API Service
 * Centralised Fetch wrapper for all backend REST calls.
 * Base URL: http://localhost:8080
 */

const API_BASE = '';

// ─── Auth helpers ─────────────────────────────────────────────────────────────
const Auth = {
  TOKEN_KEY: 'shopease_token',
  USER_KEY:  'shopease_user',

  getToken()  { return localStorage.getItem(this.TOKEN_KEY); },
  getUser()   { const u = localStorage.getItem(this.USER_KEY); try { return u ? JSON.parse(u) : null; } catch { return null; } },
  isLoggedIn(){ return !!this.getToken(); },
  isCustomer(){ const u = this.getUser(); return u && u.role === 'ROLE_CUSTOMER'; },

  save(token, user) {
    localStorage.setItem(this.TOKEN_KEY, token);
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  },
  clear() {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    localStorage.removeItem('shopease_cart_count');
  },
  authHeader() {
    const t = this.getToken();
    return t ? { 'Authorization': `Bearer ${t}` } : {};
  }
};

// ─── Core Fetch ──────────────────────────────────────────────────────────────
async function apiFetch(path, options = {}) {
  const url = API_BASE + path;
  const headers = {
    'Content-Type': 'application/json',
    ...Auth.authHeader(),
    ...(options.headers || {})
  };

  try {
    const res = await fetch(url, { ...options, headers });
    const json = await res.json().catch(() => null);

    if (res.status === 401) {
      Auth.clear();
      if (!window.location.pathname.includes('login')) {
        window.location.href = 'login.html?expired=1';
      }
      throw new ApiError('Session expired. Please log in again.', 401, json);
    }

    if (!res.ok) {
      const msg = json?.message || json?.error || `HTTP ${res.status}`;
      throw new ApiError(msg, res.status, json);
    }

    return json;
  } catch (err) {
    if (err instanceof ApiError) throw err;
    throw new ApiError('Network error. Please check your connection.', 0, null);
  }
}

class ApiError extends Error {
  constructor(message, status, raw) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.raw = raw;
  }
}

// ─── Auth API ─────────────────────────────────────────────────────────────────
const AuthAPI = {
  async register(payload) {
    // POST /api/auth/register
    // Body: { email, password, fullName, phoneNumber }
    return apiFetch('/api/auth/register', { method: 'POST', body: JSON.stringify(payload) });
  },
  async login(payload) {
    // POST /api/auth/login
    // Body: { email, password }
    return apiFetch('/api/auth/login', { method: 'POST', body: JSON.stringify(payload) });
  }
};

// ─── Products API ─────────────────────────────────────────────────────────────
const ProductAPI = {
  async list({ query, category, brand, page, size, sortBy, sortDirection } = {}) {
    // GET /api/products?query=&category=&brand=&page=&size=&sortBy=&sortDirection=
    const params = new URLSearchParams();
    if (query)         params.set('query', query);
    if (category)      params.set('category', category);
    if (brand)         params.set('brand', brand);
    if (page != null)  params.set('page', page);
    if (size != null)  params.set('size', size);
    if (sortBy)        params.set('sortBy', sortBy);
    if (sortDirection) params.set('sortDirection', sortDirection);
    const qs = params.toString();
    return apiFetch(`/api/products${qs ? '?' + qs : ''}`);
  },
  async get(id) {
    // GET /api/products/{id}
    return apiFetch(`/api/products/${id}`);
  },
  async getReviews(id) {
    // GET /api/products/{id}/reviews
    return apiFetch(`/api/products/${id}/reviews`);
  },
  async addReview(id, payload) {
    // POST /api/products/{id}/reviews  (CUSTOMER required)
    // Body: { rating, comment, orderItemId? }
    return apiFetch(`/api/products/${id}/reviews`, { method: 'POST', body: JSON.stringify(payload) });
  }
};

// ─── Cart API ─────────────────────────────────────────────────────────────────
const CartAPI = {
  async get() {
    // GET /api/cart  (CUSTOMER)
    return apiFetch('/api/cart');
  },
  async addItem(productId, quantity) {
    // POST /api/cart/items  Body: { productId, quantity }
    return apiFetch('/api/cart/items', { method: 'POST', body: JSON.stringify({ productId, quantity }) });
  },
  async updateItem(itemId, quantity) {
    // PUT /api/cart/items/{itemId}  Body: { quantity }
    return apiFetch(`/api/cart/items/${itemId}`, { method: 'PUT', body: JSON.stringify({ quantity }) });
  },
  async removeItem(itemId) {
    // DELETE /api/cart/items/{itemId}
    return apiFetch(`/api/cart/items/${itemId}`, { method: 'DELETE' });
  },
  async clear() {
    // DELETE /api/cart
    return apiFetch('/api/cart', { method: 'DELETE' });
  }
};

// ─── Checkout API ─────────────────────────────────────────────────────────────
const CheckoutAPI = {
  async applyCoupon(couponCode) {
    // POST /api/coupons/apply  Body: { couponCode }
    return apiFetch('/api/coupons/apply', { method: 'POST', body: JSON.stringify({ couponCode }) });
  },
  async checkout(payload) {
    // POST /api/checkout
    // Body: { deliveryAddress: { recipientName, phoneNumber, streetLine1, streetLine2, city, state, postalCode, country },
    //         paymentMethod, couponCode }
    return apiFetch('/api/checkout', { method: 'POST', body: JSON.stringify(payload) });
  }
};

// ─── Payment API ──────────────────────────────────────────────────────────────
const PaymentAPI = {
  async initiate(orderId, payload = {}) {
    // POST /api/payments/{orderId}/initiate
    // Body: { paymentMethod?, idempotencyKey?, simulatedOutcome? }
    return apiFetch(`/api/payments/${orderId}/initiate`, { method: 'POST', body: JSON.stringify(payload) });
  },
  async getStatus(orderId) {
    // GET /api/payments/{orderId}/status
    return apiFetch(`/api/payments/${orderId}/status`);
  },
  async sendWebhook(payload) {
    // POST /api/payments/webhook  (public)
    // Body: { eventId, paymentReference, gatewayReference, paymentStatus, failureReason, signature }
    return apiFetch('/api/payments/webhook', { method: 'POST', body: JSON.stringify(payload) });
  }
};

// ─── Orders API ───────────────────────────────────────────────────────────────
const OrderAPI = {
  async list() {
    // GET /api/orders (CUSTOMER)
    return apiFetch('/api/orders');
  },
  async get(orderId) {
    // GET /api/orders/{id} (CUSTOMER)
    return apiFetch(`/api/orders/${orderId}`);
  }
};

// ─── Toast Notifications ──────────────────────────────────────────────────────
const Toast = {
  _container: null,
  _init() {
    if (this._container) return;
    this._container = document.createElement('div');
    this._container.id = 'toast-container';
    document.body.appendChild(this._container);
  },
  show(title, message, type = 'info', duration = 4000) {
    this._init();
    const icons = { success: '✅', danger: '❌', warning: '⚠️', info: 'ℹ️' };
    const t = document.createElement('div');
    t.className = `toast toast-${type}`;
    t.innerHTML = `
      <span class="toast-icon">${icons[type] || 'ℹ️'}</span>
      <div class="toast-body">
        ${title ? `<div class="toast-title">${title}</div>` : ''}
        <div class="toast-msg">${message}</div>
      </div>
      <button class="toast-close" aria-label="Dismiss">✕</button>`;
    t.querySelector('.toast-close').onclick = () => t.remove();
    this._container.appendChild(t);
    if (duration > 0) setTimeout(() => { t.style.animation = 'fadeOut 0.3s ease forwards'; setTimeout(() => t.remove(), 300); }, duration);
    return t;
  },
  success(msg, title = 'Success')  { return this.show(title, msg, 'success'); },
  error(msg, title = 'Error')      { return this.show(title, msg, 'danger'); },
  warning(msg, title = 'Warning')  { return this.show(title, msg, 'warning'); },
  info(msg, title = 'Info')        { return this.show(title, msg, 'info'); }
};

// ─── UI Helpers ───────────────────────────────────────────────────────────────
function formatCurrency(amount) {
  if (amount == null) return '—';
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 2 }).format(amount);
}

function formatDate(isoString) {
  if (!isoString) return '—';
  return new Date(isoString).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

function formatDateShort(isoString) {
  if (!isoString) return '—';
  return new Date(isoString).toLocaleDateString('en-IN', { year: 'numeric', month: 'short', day: 'numeric' });
}

function renderStars(rating, max = 5) {
  const r = parseFloat(rating) || 0;
  let html = '<span class="stars">';
  for (let i = 1; i <= max; i++) {
    html += `<span class="star ${i <= Math.floor(r) ? 'filled' : (i - 0.5 <= r ? 'half' : '')}">★</span>`;
  }
  html += '</span>';
  return html;
}

function orderStatusBadge(status) {
  return `<span class="badge status-${status}">${status?.replace(/_/g, ' ') || '—'}</span>`;
}

function paymentStatusBadge(status) {
  return `<span class="badge payment-${status}">${status || '—'}</span>`;
}

function setLoadingBtn(btn, loading, loadingText = 'Please wait…') {
  if (!btn) return;
  if (loading) {
    btn.dataset.origText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = `<span class="spinner spinner-sm spinner-white"></span> ${loadingText}`;
  } else {
    btn.disabled = false;
    btn.innerHTML = btn.dataset.origText || btn.innerHTML;
  }
}

function requireAuth(redirectTo = '') {
  if (!Auth.isLoggedIn()) {
    const back = redirectTo || window.location.href;
    window.location.href = `login.html?redirect=${encodeURIComponent(back)}`;
    return false;
  }
  return true;
}

// ─── Navbar Renderer ──────────────────────────────────────────────────────────
function renderNavbar(activePage = '') {
  const user = Auth.getUser();
  const isLoggedIn = Auth.isLoggedIn();

  const userSection = isLoggedIn
    ? `<li>
         <a href="orders.html" class="${activePage === 'orders' ? 'active' : ''}">
           <span>📦</span> Orders
         </a>
       </li>
       <li>
         <span class="nav-user-name">👤 ${user?.fullName?.split(' ')[0] || 'Account'}</span>
       </li>
       <li>
         <a href="#" id="nav-logout" class="btn-ghost btn-sm btn">Logout</a>
       </li>`
    : `<li>
         <a href="login.html" class="${activePage === 'login' ? 'active' : ''} btn-nav-login">Login</a>
       </li>
       <li>
         <a href="register.html" class="${activePage === 'register' ? 'active' : ''}">Register</a>
       </li>`;

  document.getElementById('navbar-mount')?.remove();

  const nav = document.createElement('nav');
  nav.className = 'navbar';
  nav.id = 'navbar-mount';
  nav.innerHTML = `
    <div class="container">
      <div class="nav-inner">
        <a class="nav-brand" href="index.html">
          <span class="brand-icon">🛍️</span>
          <span>ShopEase</span>
        </a>
        <div class="nav-search" id="nav-search-box">
          <input type="text" id="nav-search-input" placeholder="Search products…" autocomplete="off" />
          <button class="nav-search-btn" id="nav-search-btn" aria-label="Search">🔍</button>
        </div>
        <button class="nav-hamburger" id="nav-hamburger" aria-label="Toggle menu">☰</button>
        <ul class="nav-links" id="nav-links">
          <li><a href="products.html" class="${activePage === 'products' ? 'active' : ''}"><span>🏪</span> Products</a></li>
          <li>
            <a href="cart.html" class="nav-cart-link ${activePage === 'cart' ? 'active' : ''}">
              🛒 Cart
              <span class="cart-badge hidden" id="nav-cart-badge">0</span>
            </a>
          </li>
          ${userSection}
        </ul>
      </div>
    </div>`;

  document.body.insertBefore(nav, document.body.firstChild);

  // Hamburger
  document.getElementById('nav-hamburger')?.addEventListener('click', () => {
    document.getElementById('nav-links')?.classList.toggle('open');
  });

  // Logout
  document.getElementById('nav-logout')?.addEventListener('click', e => {
    e.preventDefault();
    Auth.clear();
    Toast.info('You have been logged out.');
    setTimeout(() => window.location.href = 'index.html', 500);
  });

  // Search
  const searchInput = document.getElementById('nav-search-input');
  const doSearch = () => {
    const q = searchInput?.value.trim();
    if (q) window.location.href = `products.html?query=${encodeURIComponent(q)}`;
  };
  document.getElementById('nav-search-btn')?.addEventListener('click', doSearch);
  searchInput?.addEventListener('keydown', e => { if (e.key === 'Enter') doSearch(); });

  // Cart badge
  updateCartBadge();
}

function renderFooter() {
  const footer = document.createElement('footer');
  footer.innerHTML = `
    <div class="container">
      <div class="footer-inner">
        <span class="footer-brand">🛍️ ShopEase</span>
        <div class="footer-links">
          <a href="index.html">Home</a>
          <a href="products.html">Products</a>
          <a href="cart.html">Cart</a>
          <a href="orders.html">My Orders</a>
        </div>
        <span class="footer-copy">© 2026 ShopEase · Team 5 Project</span>
      </div>
    </div>`;
  document.body.appendChild(footer);
}

async function updateCartBadge() {
  if (!Auth.isLoggedIn() || !Auth.isCustomer()) return;
  try {
    const res = await CartAPI.get();
    const count = res?.data?.totalItems || 0;
    const badge = document.getElementById('nav-cart-badge');
    if (badge) {
      badge.textContent = count;
      count > 0 ? badge.classList.remove('hidden') : badge.classList.add('hidden');
    }
    localStorage.setItem('shopease_cart_count', count);
  } catch { /* silent */ }
}
