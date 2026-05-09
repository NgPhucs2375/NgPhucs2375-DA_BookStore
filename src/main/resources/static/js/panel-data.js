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
    var periodEl = document.getElementById("seller-period-filter");
    var refreshEl = document.getElementById("seller-refresh-btn");

    function render(ana) {
      setText("seller-metric-revenue", vnd(ana.totalRevenue || 0));
      setText("seller-metric-completed", String(ana.completedOrders || 0));
      setText("seller-metric-aov", vnd(ana.averageOrderValue || 0));
      setText("seller-metric-rate", Math.round(ana.completionRate || 0) + "%");
      setText("seller-metric-units", String(ana.soldUnits || 0));
    }

    function load() {
      var days = periodEl ? Number(periodEl.value || 30) : 30;
      return getJson("/api/orders/seller/me/analytics?days=" + days).then(function (ana) {
        render(ana || {});
      }).catch(function (err) {
        console.error('Dashboard error (non-critical):', err);
      });
    }

    if (periodEl) {
      periodEl.addEventListener("change", load);
    }
    if (refreshEl) {
      refreshEl.addEventListener("click", load);
    }

    return load();
  }

  function initSellerOrders() {
    var qEl = document.getElementById("orders-q");
    var periodEl = document.getElementById("orders-period");
    var refreshEl = document.getElementById("orders-refresh");
    var latestAnalytics = null;

    function formatDate(value) {
      if (!value) return "-";
      var date = new Date(value);
      if (isNaN(date.getTime())) return "-";
      return date.toLocaleDateString("vi-VN", { year: "numeric", month: "2-digit", day: "2-digit" });
    }

    function load() {
      if (!window.ApiService || !ApiService.Order || !ApiService.Order.getSellerAnalytics) {
        setHtml("seller-orders-body", '<tr><td class="px-4 py-3" colspan="7">Thiếu ApiService</td></tr>');
        return Promise.resolve();
      }

      var query = (qEl ? qEl.value : "").toLowerCase();
      var days = periodEl ? Number(periodEl.value || 30) : 30;

      return ApiService.Order.getSellerAnalytics(days).then(function (analytics) {
        latestAnalytics = analytics || {};
        var rows = Array.isArray(latestAnalytics.recentTransactions) ? latestAnalytics.recentTransactions : [];

        var filtered = rows.filter(function (row) {
          var transactionId = String(row.transactionId || "").toLowerCase();
          var customerName = String(row.customerName || "").toLowerCase();
          var productName = String(row.productName || "").toLowerCase();

          var matchQuery = !query ||
            transactionId.includes(query) ||
            customerName.includes(query) ||
            productName.includes(query);

          return matchQuery;
        });

        var html = filtered.map(function (row) {
          return (
            '<tr>' +
            '<td class="px-4 py-3 font-black">' + esc(row.transactionId || "-") + '</td>' +
            '<td class="px-4 py-3">' + esc(formatDate(row.createdAt)) + '</td>' +
            '<td class="px-4 py-3 text-sm">' + esc(row.customerName || "--") + '</td>' +
            '<td class="px-4 py-3 text-sm">' + esc(row.productName || "--") + '</td>' +
            '<td class="px-4 py-3 font-black">' + esc(row.quantity || 0) + '</td>' +
            '<td class="px-4 py-3 font-black text-brand-orange">' + vnd(row.amount || 0) + '</td>' +
            '<td class="px-4 py-3">' + esc(row.paymentMethod || "COD") + '</td>' +
            '</tr>'
          );
        }).join("");

        setHtml("seller-orders-body", html || '<tr><td class="px-4 py-3" colspan="7">Không có dữ liệu</td></tr>');
      }).catch(function (err) {
        console.error('Seller orders load failed:', err);
        setHtml("seller-orders-body", '<tr><td class="px-4 py-3" colspan="7">Lỗi tải dữ liệu</td></tr>');
      });
    }

    [qEl, periodEl, refreshEl].forEach(function(el) {
      if (el) el.addEventListener("input", load);
      if (el) el.addEventListener("change", load);
    });

    window.viewOrderDetail = function(orderId) {
      alert('Trang seller orders hiện dùng cùng nguồn analytics mới nên không còn màn chi tiết riêng cho từng đơn.');
    };

    window.loadSellerOrders = load;

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
      // Use ApiService to load seller books from API
      if (typeof ApiService === 'undefined' || !ApiService.Book || !ApiService.Book.getSellerBooks) {
        console.error('ApiService.Book.getSellerBooks not available');
        setHtml("seller-inventory-body", '<tr><td colspan="6" class="px-4 py-3">Lỗi tải dữ liệu</td></tr>');
        return Promise.resolve();
      }

      var categoryId = (cEl && cEl.value !== "all") ? cEl.value : null;
      var query = qEl ? qEl.value : "";
      
      return ApiService.Book.getSellerBooks(query, categoryId, 0, 500).then(function(result) {
        // Handle both Page<Book> format and array format
        var books = result.content || result || [];
        
        if (!Array.isArray(books)) {
          books = [];
        }

        // Filter by stock if needed
        if (sEl && sEl.value !== "all") {
          var stockFilter = sEl.value;
          books = books.filter(function(b) {
            var stock = b.stockQuantity || 0;
            if (stockFilter === "low") return stock < 10;
            if (stockFilter === "normal") return stock >= 10 && stock < 50;
            if (stockFilter === "high") return stock >= 50;
            return true;
          });
        }

        var html = books.map(function(book) {
          var statusColor = book.approvalStatus === 'APPROVED' ? 'text-emerald-600' : 
                           book.approvalStatus === 'PENDING' ? 'text-amber-600' : 'text-red-600';
          var badge = book.stockQuantity < 10
            ? '<span class="rounded bg-rose-100 px-2 py-1 text-xs font-black text-rose-700">Low</span>'
            : book.stockQuantity < 50
              ? '<span class="rounded bg-amber-100 px-2 py-1 text-xs font-black text-amber-700">Normal</span>'
              : '<span class="rounded bg-emerald-100 px-2 py-1 text-xs font-black text-emerald-700">High</span>';
          
          return (
            "<tr>" +
            '<td class="px-4 py-3 font-bold">' + esc(book.title || '?') + "</td>" +
            '<td class="px-4 py-3">' + esc(book.author || '?') + "</td>" +
            '<td class="px-4 py-3 font-black text-brand-orange">' + vnd(book.price || 0) + "</td>" +
            '<td class="px-4 py-3">' + (book.stockQuantity || 0) + "</td>" +
            '<td class="px-4 py-3">' + badge + "</td>" +
            '<td class="px-4 py-3 text-right"><button class="rounded border border-brand-accent px-3 py-1 text-xs font-black" onclick="editBook(' + book.id + ')">Cập nhật</button></td>' +
            "</tr>"
          );
        }).join("");

        setHtml("seller-inventory-body", html || '<tr><td colspan="6" class="px-4 py-3">Không có sách</td></tr>');
      }).catch(function(err) {
        console.error('Failed to load books:', err);
        setHtml("seller-inventory-body", '<tr><td colspan="6" class="px-4 py-3">Lỗi tải dữ liệu từ server</td></tr>');
      });
    }

    [qEl, cEl, sEl].forEach(function (el) {
      if (el) el.addEventListener("input", load);
      if (el) el.addEventListener("change", load);
    });

    return loadCategories().then(load);
  }

  function initSellerAnalytics() {
    var periodEl = document.getElementById("seller-period-filter");
    var refreshEl = document.getElementById("seller-refresh-btn");
    var exportEl = document.getElementById("seller-export-btn");
    var revenueChart = null;
    var categoryChart = null;
    var sellerAnalyticsSnapshot = null;

    function formatDate(value) {
      if (!value) return "-";
      var date = new Date(value);
      if (isNaN(date.getTime())) return "-";
      return date.toLocaleDateString("vi-VN", { year: "numeric", month: "2-digit", day: "2-digit" });
    }

    function daysValue() {
      return periodEl ? Number(periodEl.value || 30) : 30;
    }

    function setCards(ana) {
      setText("seller-ana-revenue", vnd(ana.totalRevenue || 0));
      setText("seller-ana-completed", String(ana.completedOrders || 0));
      setText("seller-ana-aov", vnd(ana.averageOrderValue || 0));
      setText("seller-ana-rate", Math.round(ana.completionRate || 0) + "%");
      setText("seller-ana-units", String(ana.soldUnits || 0));
      setText("seller-period-label", ana.periodLabel || "30 ngày gần nhất");
    }

    function sanitizeExcelText(value) {
      return String(value == null ? "" : value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");
    }

    function toRows(items, columns) {
      return (items || []).map(function (item) {
        return "<tr>" + columns.map(function (column) {
          return "<td>" + sanitizeExcelText(typeof column.render === "function" ? column.render(item) : item[column.key]) + "</td>";
        }).join("") + "</tr>";
      }).join("");
    }

    function downloadExcelFile(ana) {
      if (!ana) {
        alert("Chưa có dữ liệu để export");
        return;
      }

      var summaryTable = [
        ["Mục", "Giá trị"],
        ["Kỳ", ana.periodLabel || "30 ngày gần nhất"],
        ["Tổng doanh thu", vnd(ana.totalRevenue || 0)],
        ["Đơn hoàn thành", String(ana.completedOrders || 0)],
        ["AOV", vnd(ana.averageOrderValue || 0)],
        ["Tỷ lệ hoàn thành", Math.round(ana.completionRate || 0) + "%"],
        ["Sản phẩm đã bán", String(ana.soldUnits || 0)]
      ].map(function (row) {
        return "<tr><td>" + sanitizeExcelText(row[0]) + "</td><td>" + sanitizeExcelText(row[1]) + "</td></tr>";
      }).join("");

      var revenueRows = toRows(ana.revenueTimeline || [], [
        { key: "label", render: function (item) { return item.label; } },
        { key: "revenue", render: function (item) { return vnd(item.revenue || 0); } },
        { key: "orderCount", render: function (item) { return item.orderCount || 0; } },
        { key: "soldUnits", render: function (item) { return item.soldUnits || 0; } }
      ]);

      var categoryRows = toRows(ana.categoryRevenue || [], [
        { key: "categoryName", render: function (item) { return item.categoryName; } },
        { key: "revenue", render: function (item) { return vnd(item.revenue || 0); } },
        { key: "soldUnits", render: function (item) { return item.soldUnits || 0; } },
        { key: "sharePercent", render: function (item) { return Math.round(item.sharePercent || 0) + "%"; } }
      ]);

      var topProductRows = toRows(ana.topSellingProducts || [], [
        { key: "title", render: function (item) { return item.title; } },
        { key: "stockQuantity", render: function (item) { return item.stockQuantity || 0; } },
        { key: "soldUnits", render: function (item) { return item.soldUnits || 0; } },
        { key: "revenue", render: function (item) { return vnd(item.revenue || 0); } },
        { key: "progressPercent", render: function (item) { return Math.round(item.progressPercent || 0) + "%"; } }
      ]);

      var lowStockRows = toRows(ana.lowStockProducts || [], [
        { key: "title", render: function (item) { return item.title; } },
        { key: "stockQuantity", render: function (item) { return item.stockQuantity || 0; } },
        { key: "soldUnits", render: function (item) { return item.soldUnits || 0; } },
        { key: "note", render: function (item) { return item.note || "Theo dõi"; } }
      ]);

      var transactionRows = toRows(ana.recentTransactions || [], [
        { key: "transactionId", render: function (item) { return item.transactionId; } },
        { key: "createdAt", render: function (item) { return formatDate(item.createdAt); } },
        { key: "customerName", render: function (item) { return item.customerName; } },
        { key: "productName", render: function (item) { return item.productName; } },
        { key: "quantity", render: function (item) { return item.quantity || 0; } },
        { key: "amount", render: function (item) { return vnd(item.amount || 0); } },
        { key: "paymentMethod", render: function (item) { return item.paymentMethod || "COD"; } }
      ]);

      var html = '<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40">' +
        '<head><meta charset="UTF-8"><meta http-equiv="Content-Type" content="text/html; charset=UTF-8"></head><body>' +
        '<h2>Seller Analytics Export</h2>' +
        '<table border="1"><thead><tr><th>Mục</th><th>Giá trị</th></tr></thead><tbody>' + summaryTable + '</tbody></table><br />' +
        '<h3>Doanh thu theo thời gian</h3><table border="1"><thead><tr><th>Ngày</th><th>Doanh thu</th><th>Số đơn</th><th>Số SP</th></tr></thead><tbody>' + revenueRows + '</tbody></table><br />' +
        '<h3>Doanh thu theo danh mục</h3><table border="1"><thead><tr><th>Danh mục</th><th>Doanh thu</th><th>Số SP</th><th>Tỷ lệ</th></tr></thead><tbody>' + categoryRows + '</tbody></table><br />' +
        '<h3>Top sản phẩm bán chạy</h3><table border="1"><thead><tr><th>Sản phẩm</th><th>Tồn kho</th><th>Đã bán</th><th>Doanh thu</th><th>So sánh</th></tr></thead><tbody>' + topProductRows + '</tbody></table><br />' +
        '<h3>Top sản phẩm tồn kho ít nhất</h3><table border="1"><thead><tr><th>Sản phẩm</th><th>Tồn kho</th><th>Đã bán</th><th>Ghi chú</th></tr></thead><tbody>' + lowStockRows + '</tbody></table><br />' +
        '<h3>Giao dịch gần đây</h3><table border="1"><thead><tr><th>Mã GD</th><th>Ngày</th><th>Khách hàng</th><th>Sản phẩm</th><th>SL</th><th>Thành tiền</th><th>Thanh toán</th></tr></thead><tbody>' + transactionRows + '</tbody></table>' +
        '</body></html>';

      var blob = new Blob([html], { type: 'application/vnd.ms-excel;charset=utf-8;' });
      var url = URL.createObjectURL(blob);
      var link = document.createElement('a');
      link.href = url;
      link.download = 'seller-analytics-' + new Date().toISOString().slice(0, 10) + '.xls';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    }

    function renderRevenueChart(ana) {
      if (!window.Chart) return;
      var canvas = document.getElementById("seller-revenue-chart");
      if (!canvas) return;
      if (revenueChart) revenueChart.destroy();
      var points = ana.revenueTimeline || [];
      revenueChart = new Chart(canvas.getContext("2d"), {
        type: "line",
        data: {
          labels: points.map(function (item) { return item.label; }),
          datasets: [{
            label: "Doanh thu",
            data: points.map(function (item) { return item.revenue || 0; }),
            borderColor: "#D19C74",
            backgroundColor: "rgba(209, 156, 116, 0.18)",
            fill: true,
            tension: 0.35
          }]
        },
        options: {
          responsive: true,
          plugins: {
            legend: { display: false },
            tooltip: {
              callbacks: {
                label: function (ctx) { return vnd(ctx.raw || 0); }
              }
            }
          },
          scales: {
            y: { beginAtZero: true }
          }
        }
      });
    }

    function renderCategoryChart(ana) {
      if (!window.Chart) return;
      var canvas = document.getElementById("seller-category-chart");
      if (!canvas) return;
      if (categoryChart) categoryChart.destroy();
      var rows = ana.categoryRevenue || [];
      categoryChart = new Chart(canvas.getContext("2d"), {
        type: "doughnut",
        data: {
          labels: rows.map(function (item) { return item.categoryName; }),
          datasets: [{
            data: rows.map(function (item) { return item.revenue || 0; }),
            backgroundColor: ["#D19C74", "#ea580c", "#5D4037", "#fbbf24", "#0ea5e9", "#10b981"]
          }]
        },
        options: {
          responsive: true,
          plugins: {
            legend: { position: "bottom" },
            tooltip: {
              callbacks: {
                label: function (ctx) { return ctx.label + ": " + vnd(ctx.raw || 0); }
              }
            }
          }
        }
      });
    }

    function renderTopProducts(ana) {
      var rows = (ana.topSellingProducts || []).map(function (item, index) {
        var progress = Math.max(0, Math.min(100, item.progressPercent || 0));
        return (
          "<tr>" +
          '<td class="px-4 py-4 align-top font-black text-[#5D4037]">' + (index + 1) + ".</td>" +
          '<td class="px-4 py-4 align-top">' +
            '<div class="font-black">' + esc(item.title) + "</div>" +
            '<div class="text-xs text-[#5D4037]/60">Tồn kho: ' + esc(item.stockQuantity) + "</div>" +
          '</td>' +
          '<td class="px-4 py-4 align-top">' + esc(item.soldUnits) + "</td>" +
          '<td class="px-4 py-4 align-top font-black text-[#ea580c]">' + vnd(item.revenue || 0) + "</td>" +
          '<td class="px-4 py-4 align-top w-48">' +
            '<div class="h-2 rounded-full bg-[#FAF5E8] overflow-hidden"><div class="h-2 rounded-full bg-[#D19C74]" style="width:' + progress + '%"></div></div>' +
          '</td>' +
          '</tr>'
        );
      }).join("");

      setHtml("seller-top-products-body", rows || '<tr><td class="px-4 py-4" colspan="5">Khong co du lieu</td></tr>');
    }

    function renderLowStock(ana) {
      var rows = (ana.lowStockProducts || []).map(function (item) {
        var danger = (item.stockQuantity || 0) <= 5 ? 'text-rose-600' : 'text-amber-600';
        return (
          '<tr>' +
          '<td class="px-4 py-4 font-black">' + esc(item.title) + '</td>' +
          '<td class="px-4 py-4 ' + danger + ' font-black">' + esc(item.stockQuantity) + '</td>' +
          '<td class="px-4 py-4">' + esc(item.soldUnits) + '</td>' +
          '<td class="px-4 py-4">' + esc(item.note || 'Theo dõi') + '</td>' +
          '</tr>'
        );
      }).join("");

      setHtml("seller-low-stock-body", rows || '<tr><td class="px-4 py-4" colspan="4">Khong co du lieu</td></tr>');
    }

    function renderTransactions(ana) {
      var rows = (ana.recentTransactions || []).map(function (item) {
        return (
          '<tr>' +
          '<td class="px-4 py-4 font-black">' + esc(item.transactionId) + '</td>' +
          '<td class="px-4 py-4">' + esc(item.createdAt ? formatDate(item.createdAt) : '-') + '</td>' +
          '<td class="px-4 py-4">' + esc(item.customerName) + '</td>' +
          '<td class="px-4 py-4">' + esc(item.productName) + '</td>' +
          '<td class="px-4 py-4">' + esc(item.quantity) + '</td>' +
          '<td class="px-4 py-4 font-black text-[#ea580c]">' + vnd(item.amount || 0) + '</td>' +
          '<td class="px-4 py-4">' + esc(item.paymentMethod || 'COD') + '</td>' +
          '</tr>'
        );
      }).join("");

      setHtml("seller-transactions-body", rows || '<tr><td class="px-4 py-4" colspan="7">Khong co giao dich</td></tr>');
    }

    function renderAlerts(ana) {
      var alerts = (ana.lowStockProducts || []).filter(function (item) { return (item.stockQuantity || 0) <= 5; });
      var html = alerts.map(function (item) {
        return '<div class="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm font-bold text-rose-700">' +
          esc(item.title) + ' - chỉ còn ' + esc(item.stockQuantity) + ' sản phẩm' +
          '</div>';
      }).join('');
      setHtml('seller-stock-alerts', html || '<div class="rounded-xl border border-[#E5CBB5] bg-white px-4 py-3 text-sm font-bold text-[#5D4037]/70">Không có cảnh báo tồn kho</div>');
    }

    function load() {
      return getJson('/api/orders/seller/me/analytics?days=' + daysValue()).then(function (ana) {
        sellerAnalyticsSnapshot = ana || {};
        setCards(ana || {});
        renderRevenueChart(ana || {});
        renderCategoryChart(ana || {});
        renderTopProducts(ana || {});
        renderLowStock(ana || {});
        renderTransactions(ana || {});
        renderAlerts(ana || {});
      }).catch(function (err) {
        console.error('Seller analytics load failed:', err);
        setHtml('seller-transactions-body', '<tr><td class="px-4 py-4" colspan="7">Lỗi tải dữ liệu</td></tr>');
      });
    }

    if (periodEl) {
      periodEl.addEventListener('change', load);
    }
    if (refreshEl) {
      refreshEl.addEventListener('click', load);
    }

    if (exportEl) {
      exportEl.addEventListener('click', function () {
        downloadExcelFile(sellerAnalyticsSnapshot);
      });
    }

    window.loadSellerAnalytics = load;
    window.exportSellerAnalyticsExcel = function () {
      downloadExcelFile(sellerAnalyticsSnapshot);
    };
    return load();
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
