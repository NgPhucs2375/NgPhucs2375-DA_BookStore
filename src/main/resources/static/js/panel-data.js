(function () {
  var API_ROOT = "/api/panel";
  var charts = {};

  function esc(v) {
    return String(v || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/\"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function vnd(v) {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
      maximumFractionDigits: 0
    }).format(v || 0);
  }

  function authHeaders() {
    var token = localStorage.getItem('access_token') || sessionStorage.getItem('access_token');
    var headers = {};
    if (token) headers['Authorization'] = 'Bearer ' + token;
    if (!token && window.BookomDevUserId) headers['X-User-Id'] = String(window.BookomDevUserId);
    return headers;
  }

  function getJson(url) {
    return fetch(url, { headers: authHeaders() }).then(function (res) {
      if (res.status === 401 || res.status === 403) {
        // Redirect to a login page or show auth modal for admin
        try { localStorage.removeItem('access_token'); sessionStorage.removeItem('access_token'); } catch(e){}
        if (window.location.pathname !== '/login' && window.location.pathname.indexOf('/admin') !== -1) {
          window.location.href = '/login';
        }
        throw new Error("Unauthorized: " + res.status);
      }
      if (!res.ok) throw new Error("HTTP " + res.status);
      return res.json();
    });
  }

  function setText(id, val) {
    var el = document.getElementById(id);
    if (el) el.textContent = val;
  }

  function setHtml(id, html) {
    var el = document.getElementById(id);
    if (el) el.innerHTML = html;
  }

  function chart(id, type, data, options) {
    if (!window.Chart) return;
    var canvas = document.getElementById(id);
    if (!canvas) return;
    if (charts[id]) charts[id].destroy();
    charts[id] = new Chart(canvas, { type: type, data: data, options: options || {} });
  }

  function qs(params) {
    var usp = new URLSearchParams();
    Object.keys(params).forEach(function (k) {
      if (params[k] !== undefined && params[k] !== null) usp.set(k, params[k]);
    });
    return usp.toString();
  }

  function initAdminDashboard() {
    return getJson(API_ROOT + "/summary").then(function (data) {
      setText("metric-gmv", vnd(data.gmv));
      setText("metric-books", String(data.books || 0));
      setText("metric-categories", String(data.categories || 0));
      setText("metric-shops", String(data.shops || 0));

      var catStats = data.categoryStats || {};
      var rows = Object.keys(catStats)
        .slice(0, 8)
        .map(function (k) {
          return (
            "<tr>" +
            '<td class="px-4 py-3 font-bold">' + esc(k) + "</td>" +
            '<td class="px-4 py-3">Auto legal</td>' +
            '<td class="px-4 py-3">2026-03-25</td>' +
            '<td class="px-4 py-3 text-right"><span class="rounded bg-emerald-100 px-2 py-1 text-xs font-black text-emerald-700">On dinh</span></td>' +
            "</tr>"
          );
        })
        .join("");
      setHtml("admin-dashboard-shops", rows || '<tr><td class="px-4 py-3" colspan="4">Khong co du lieu</td></tr>');

      chart(
        "admin-category-chart",
        "bar",
        {
          labels: Object.keys(catStats),
          datasets: [{
            label: "So sach",
            data: Object.keys(catStats).map(function (k) { return catStats[k]; }),
            backgroundColor: "#D19C74"
          }]
        },
        { responsive: true, plugins: { legend: { display: false } } }
      );

      var stockBuckets = data.stockBuckets || {};
      chart(
        "admin-stock-chart",
        "doughnut",
        {
          labels: ["Low", "Normal", "High"],
          datasets: [{
            data: [stockBuckets.low || 0, stockBuckets.normal || 0, stockBuckets.high || 0],
            backgroundColor: ["#ea580c", "#D19C74", "#5D4037"]
          }]
        },
        { responsive: true }
      );
    });
  }

  function initAdminBooks() {
    var qEl = document.getElementById("books-q");
    var cEl = document.getElementById("books-category");
    var sEl = document.getElementById("books-stock");

    function loadCategories() {
      return getJson("/api/categories").then(function (cats) {
        if (!cEl) return;
        var opts = ['<option value="all">Tat ca danh muc</option>']
          .concat((cats || []).map(function (c) {
            return '<option value="' + esc(c.name) + '">' + esc(c.name) + '</option>';
          }))
          .join("");
        cEl.innerHTML = opts;
      }).catch(function () {
        if (cEl) cEl.innerHTML = '<option value="all">Tat ca danh muc</option>';
      });
    }

    function load() {
      var url = API_ROOT + "/books?" + qs({
        q: qEl ? qEl.value : "",
        category: cEl ? cEl.value : "all",
        stock: sEl ? sEl.value : "all"
      });

      return getJson(url).then(function (rows) {
        var html = (rows || []).map(function (r) {
          var badge = r.stockBucket === "low"
            ? '<span class="rounded bg-rose-100 px-2 py-1 text-xs font-black text-rose-700">Low</span>'
            : r.stockBucket === "normal"
              ? '<span class="rounded bg-amber-100 px-2 py-1 text-xs font-black text-amber-700">Normal</span>'
              : '<span class="rounded bg-emerald-100 px-2 py-1 text-xs font-black text-emerald-700">High</span>';

          return (
            "<tr>" +
            '<td class="px-4 py-3 font-bold">' + esc(r.title) + "</td>" +
            '<td class="px-4 py-3">' + esc(r.author) + "</td>" +
            '<td class="px-4 py-3 font-black text-brand-orange">' + vnd(r.price) + "</td>" +
            '<td class="px-4 py-3">' + esc(r.category) + "</td>" +
            '<td class="px-4 py-3">' + esc(r.stock) + "</td>" +
            '<td class="px-4 py-3 text-right">' + badge + "</td>" +
            "</tr>"
          );
        }).join("");

        setHtml("admin-books-body", html || '<tr><td class="px-4 py-3" colspan="6">Khong co du lieu</td></tr>');
      });
    }

    [qEl, cEl, sEl].forEach(function (el) {
      if (el) el.addEventListener("input", load);
      if (el) el.addEventListener("change", load);
    });

    return loadCategories().then(load);
  }

  function initAdminUsers() {
    var qEl = document.getElementById("users-q");
    var rEl = document.getElementById("users-role");
    var sEl = document.getElementById("users-status");

    function load() {
      var url = API_ROOT + "/users?" + qs({
        q: qEl ? qEl.value : "",
        role: rEl ? rEl.value : "all",
        status: sEl ? sEl.value : "all"
      });

      return getJson(url).then(function (rows) {
        var html = (rows || []).map(function (u) {
          var roleClass = u.role === "Seller" ? "bg-violet-100 text-violet-700" : "bg-slate-200 text-slate-700";
          var statusClass = u.status === "Active" ? "text-emerald-600" : "text-rose-600";
          return (
            "<tr>" +
            '<td class="px-4 py-3 font-bold">' + esc(u.name) + "</td>" +
            '<td class="px-4 py-3">' + esc(u.email) + "</td>" +
            '<td class="px-4 py-3"><span class="rounded px-2 py-1 text-xs font-black ' + roleClass + '">' + esc(u.role) + "</span></td>" +
            '<td class="px-4 py-3">' + esc(u.joined) + "</td>" +
            '<td class="px-4 py-3 font-black ' + statusClass + '">' + esc(u.status) + "</td>" +
            '<td class="px-4 py-3 text-right"><button class="rounded border border-brand-accent px-3 py-1 text-xs font-black">Chi tiet</button></td>' +
            "</tr>"
          );
        }).join("");

        setHtml("admin-users-body", html || '<tr><td class="px-4 py-3" colspan="6">Khong co du lieu</td></tr>');
      });
    }

    [qEl, rEl, sEl].forEach(function (el) {
      if (el) el.addEventListener("input", load);
      if (el) el.addEventListener("change", load);
    });

    return load();
  }

  function initAdminShops() {
    var qEl = document.getElementById("shops-q");

    function load() {
      var url = API_ROOT + "/shops?" + qs({ q: qEl ? qEl.value : "" });
      return getJson(url).then(function (rows) {
        var html = (rows || []).map(function (s) {
          return (
            "<tr>" +
            '<td class="px-4 py-3 font-bold">' + esc(s.shopName) + "</td>" +
            '<td class="px-4 py-3">' + esc(s.owner) + "</td>" +
            '<td class="px-4 py-3">' + esc(s.legal) + "</td>" +
            '<td class="px-4 py-3">' + esc(s.products) + "</td>" +
            '<td class="px-4 py-3">' + esc(s.joined) + "</td>" +
            '<td class="px-4 py-3 text-right"><button class="rounded-lg bg-emerald-500 px-3 py-1.5 text-white text-xs font-black">Phe duyet</button></td>' +
            "</tr>"
          );
        }).join("");

        setHtml("admin-shops-body", html || '<tr><td class="px-4 py-3" colspan="6">Khong co du lieu</td></tr>');
      });
    }

    if (qEl) qEl.addEventListener("input", load);
    return load();
  }

  function initSellerDashboard() {
    return Promise.all([
      getJson(API_ROOT + "/seller/analytics"),
      getJson(API_ROOT + "/seller/orders?status=all&q=")
    ]).then(function (res) {
      var ana = res[0] || {};
      var orders = res[1] || [];

      setText("seller-metric-revenue", vnd(ana.estimatedRevenue || 0));
      setText("seller-metric-pending", String((ana.orderStatusCounts && ana.orderStatusCounts["Cho xac nhan"]) || 0));
      setText("seller-metric-products", String(ana.bookCount || 0));
      setText("seller-metric-low", String(ana.lowStock || 0));

      var html = orders.slice(0, 8).map(function (o) {
        return (
          "<tr>" +
          '<td class="px-4 py-3 font-bold">#' + esc(o.id) + "</td>" +
          '<td class="px-4 py-3">' + esc(o.customer) + "</td>" +
          '<td class="px-4 py-3">' + esc(o.item) + "</td>" +
          '<td class="px-4 py-3 font-black text-brand-orange">' + vnd(o.value) + "</td>" +
          '<td class="px-4 py-3 text-right">' + esc(o.status) + "</td>" +
          "</tr>"
        );
      }).join("");

      setHtml("seller-dashboard-orders", html || '<tr><td class="px-4 py-3" colspan="5">Khong co du lieu</td></tr>');
    });
  }

  function initSellerOrders() {
    var qEl = document.getElementById("orders-q");
    var sEl = document.getElementById("orders-status");

    function load() {
      var url = API_ROOT + "/seller/orders?" + qs({
        q: qEl ? qEl.value : "",
        status: sEl ? sEl.value : "all"
      });

      return getJson(url).then(function (rows) {
        var html = (rows || []).map(function (o) {
          var tone = o.status === "Cho xac nhan" ? "text-amber-600" : o.status === "Dang giao" ? "text-indigo-600" : "text-emerald-600";
          return (
            "<tr>" +
            '<td class="px-4 py-3 font-bold">#' + esc(o.id) + "</td>" +
            '<td class="px-4 py-3">' + esc(o.customer) + "</td>" +
            '<td class="px-4 py-3">' + esc(o.item) + "</td>" +
            '<td class="px-4 py-3 font-black text-brand-orange">' + vnd(o.value) + "</td>" +
            '<td class="px-4 py-3 font-black ' + tone + '">' + esc(o.status) + "</td>" +
            '<td class="px-4 py-3 text-right"><button class="rounded border border-brand-accent px-3 py-1 text-xs font-black">Chi tiet</button></td>' +
            "</tr>"
          );
        }).join("");

        setHtml("seller-orders-body", html || '<tr><td class="px-4 py-3" colspan="6">Khong co du lieu</td></tr>');
      });
    }

    [qEl, sEl].forEach(function (el) {
      if (el) el.addEventListener("input", load);
      if (el) el.addEventListener("change", load);
    });

    return load();
  }

  function initSellerInventory() {
    var qEl = document.getElementById("inv-q");
    var cEl = document.getElementById("inv-category");
    var sEl = document.getElementById("inv-stock");

    function loadCategories() {
      return getJson("/api/categories").then(function (cats) {
        if (!cEl) return;
        var opts = ['<option value="all">Tat ca danh muc</option>']
          .concat((cats || []).map(function (c) { return '<option value="' + esc(c.name) + '">' + esc(c.name) + '</option>'; }))
          .join("");
        cEl.innerHTML = opts;
      });
    }

    function load() {
      var url = API_ROOT + "/books?" + qs({
        q: qEl ? qEl.value : "",
        category: cEl ? cEl.value : "all",
        stock: sEl ? sEl.value : "all"
      });

      return getJson(url).then(function (rows) {
        var html = (rows || []).map(function (r) {
          var badge = r.stockBucket === "low"
            ? '<span class="rounded bg-rose-100 px-2 py-1 text-xs font-black text-rose-700">Low</span>'
            : r.stockBucket === "normal"
              ? '<span class="rounded bg-amber-100 px-2 py-1 text-xs font-black text-amber-700">Normal</span>'
              : '<span class="rounded bg-emerald-100 px-2 py-1 text-xs font-black text-emerald-700">High</span>';

          return (
            "<tr>" +
            '<td class="px-4 py-3 font-bold">' + esc(r.title) + "</td>" +
            '<td class="px-4 py-3">' + esc(r.author) + "</td>" +
            '<td class="px-4 py-3 font-black text-brand-orange">' + vnd(r.price) + "</td>" +
            '<td class="px-4 py-3">' + esc(r.stock) + "</td>" +
            '<td class="px-4 py-3">' + badge + "</td>" +
            '<td class="px-4 py-3 text-right"><button class="rounded border border-brand-accent px-3 py-1 text-xs font-black">Cap nhat</button></td>' +
            "</tr>"
          );
        }).join("");

        setHtml("seller-inventory-body", html || '<tr><td class="px-4 py-3" colspan="6">Khong co du lieu</td></tr>');
      });
    }

    [qEl, cEl, sEl].forEach(function (el) {
      if (el) el.addEventListener("input", load);
      if (el) el.addEventListener("change", load);
    });

    return loadCategories().then(load);
  }

  function initSellerAnalytics() {
    return getJson(API_ROOT + "/seller/analytics").then(function (ana) {
      setText("seller-ana-revenue", vnd(ana.estimatedRevenue || 0));
      setText("seller-ana-avg", vnd(ana.averagePrice || 0));
      setText("seller-ana-count", String(ana.bookCount || 0));
      setText("seller-ana-low", String(ana.lowStock || 0));

      var categoryCounts = ana.categoryCounts || {};
      var categoryRevenue = ana.categoryRevenue || {};
      var rows = Object.keys(categoryCounts).map(function (k) {
        return (
          "<tr>" +
          '<td class="px-4 py-3 font-bold">' + esc(k) + "</td>" +
          '<td class="px-4 py-3">' + esc(categoryCounts[k]) + "</td>" +
          '<td class="px-4 py-3 font-black text-brand-orange">' + vnd(categoryRevenue[k]) + "</td>" +
          "</tr>"
        );
      }).join("");
      setHtml("seller-analytics-body", rows || '<tr><td class="px-4 py-3" colspan="3">Khong co du lieu</td></tr>');

      chart(
        "seller-category-chart",
        "bar",
        {
          labels: Object.keys(categoryRevenue),
          datasets: [{ label: "Doanh thu", data: Object.keys(categoryRevenue).map(function (k) { return categoryRevenue[k]; }), backgroundColor: "#D19C74" }]
        },
        { responsive: true, plugins: { legend: { display: false } } }
      );

      var orderStatus = ana.orderStatusCounts || {};
      chart(
        "seller-order-status-chart",
        "doughnut",
        {
          labels: Object.keys(orderStatus),
          datasets: [{ data: Object.keys(orderStatus).map(function (k) { return orderStatus[k]; }), backgroundColor: ["#ea580c", "#D19C74", "#5D4037"] }]
        },
        { responsive: true }
      );
    });
  }

  // Simple toast utility using Tailwind classes
  function ensureToastContainer(){
    var container = document.getElementById('bookom-toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'bookom-toast-container';
      container.className = 'fixed top-4 right-4 flex flex-col gap-2 z-50';
      document.body.appendChild(container);
    }
    return container;
  }

  function createToast(type, message, ttl){
    ttl = ttl || 4000;
    var container = ensureToastContainer();
    var bg = type === 'success' ? 'bg-emerald-500' : type === 'error' ? 'bg-rose-500' : 'bg-slate-700';
    var ico = type === 'success' ? '✓' : type === 'error' ? '!' : 'i';
    var el = document.createElement('div');
    el.className = 'max-w-sm w-auto text-white px-4 py-2 rounded shadow-lg flex items-center gap-3 ' + bg + ' opacity-0 translate-y-2 transition-all';
    el.innerHTML = '<span class="font-bold">' + ico + '</span><div class="flex-1 text-sm">' + (message || '') + '</div>';
    container.appendChild(el);
    // enter
    requestAnimationFrame(function(){ el.classList.remove('opacity-0'); el.classList.add('opacity-100'); el.style.transform = 'translateY(0)'; });
    setTimeout(function(){
      // leave
      el.classList.add('opacity-0');
      el.style.transform = 'translateY(-8px)';
      setTimeout(function(){ container.removeChild(el); if(container.children.length===0) container.remove(); }, 300);
    }, ttl);
  }

  window.BookomToast = {
    success: function(msg, ttl){ createToast('success', msg, ttl); },
    error: function(msg, ttl){ createToast('error', msg, ttl); },
    info: function(msg, ttl){ createToast('info', msg, ttl); }
  };

  window.BookomPanelData = {
    initAdminDashboard: initAdminDashboard,
    initAdminBooks: initAdminBooks,
    initAdminUsers: initAdminUsers,
    initAdminShops: initAdminShops,
    initSellerDashboard: initSellerDashboard,
    initSellerOrders: initSellerOrders,
    initSellerInventory: initSellerInventory,
    initSellerAnalytics: initSellerAnalytics
  };
})();
