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
    var dateFromEl = document.getElementById("date-from");
    var dateToEl = document.getElementById("date-to");
    var refreshEl = document.getElementById("orders-refresh");
    var currentStatus = 'ALL';
    var latestAnalytics = null;

    function formatDate(value) {
      if (!value) return "-";
      var date = new Date(value);
      if (isNaN(date.getTime())) return "-";
      return date.toLocaleDateString("en-GB", { year: "numeric", month: "short", day: "2-digit" });
    }

    function getStatusInfo(status) {
      switch (status) {
        case 'COMPLETED': return { label: 'Đã hoàn thành', color: '#10b981' };
        case 'SHIPPING': return { label: 'Đang giao', color: '#3b82f6' };
        case 'PENDING_PAYMENT': return { label: 'Chờ xử lý', color: '#f59e0b' };
        case 'CANCELLED': return { label: 'Đã hủy', color: '#ef4444' };
        case 'PROCESSING': return { label: 'Đang xử lý', color: '#8b5cf6' };
        default: return { label: status || 'Chờ xử lý', color: '#f59e0b' };
      }
    }

    function load() {
      if (!window.ApiService || !ApiService.Order || !ApiService.Order.getSellerAnalytics) {
        setHtml("seller-orders-body", '<tr><td class="px-6 py-5 text-center" colspan="7">Thiếu ApiService</td></tr>');
        return Promise.resolve();
      }

      var days = 90; // Fetch more for filtering
      return ApiService.Order.getSellerAnalytics(days).then(function (analytics) {
        latestAnalytics = analytics || {};
        var rows = Array.isArray(latestAnalytics.recentTransactions) ? latestAnalytics.recentTransactions : [];

        // Apply filters
        var filtered = rows.filter(function (row) {
          // Status filter
          if (currentStatus !== 'ALL' && (row.status || 'PENDING_PAYMENT') !== currentStatus) {
            return false;
          }

          // Date filter
          if (dateFromEl && dateFromEl.value) {
            if (new Date(row.createdAt) < new Date(dateFromEl.value)) return false;
          }
          if (dateToEl && dateToEl.value) {
            if (new Date(row.createdAt) > new Date(dateToEl.value)) return false;
          }

          return true;
        });

        // Update count
        if (document.getElementById("order-count")) setText("order-count", filtered.length);
        if (document.getElementById("pagination-range")) setText("pagination-range", "Đang hiển thị 1-" + filtered.length + " trong số " + filtered.length);

        var html = filtered.map(function (row) {
          var statusInfo = getStatusInfo(row.status);
          var initials = (row.customerName || "U").substring(0, 1).toUpperCase();
          
          return (
            '<tr class="hover:bg-[#FAF5E8]/30 transition-colors">' +
            '<td class="px-6 py-5 text-xs font-black text-[#5D4037]/60">#' + esc(row.subOrderId || row.transactionId || "0000") + '</td>' +
            '<td class="px-6 py-5">' +
              '<div class="flex items-center gap-3">' +
                '<div class="h-8 w-8 rounded-full bg-[#E5CBB5] flex items-center justify-center text-[10px] font-black text-[#5D4037]">' + initials + '</div>' +
                '<span class="text-xs font-black text-[#5D4037]">' + esc(row.customerName || "Unknown") + '</span>' +
              '</div>' +
            '</td>' +
            '<td class="px-6 py-5 text-[11px] font-bold text-[#5D4037]/50 max-w-[200px] truncate">' + esc(row.address || "No address") + '</td>' +
            '<td class="px-6 py-5 text-xs font-bold text-[#5D4037]">' + esc(formatDate(row.createdAt)) + '</td>' +
            '<td class="px-6 py-5 text-xs font-black text-[#5D4037] text-right">' + vnd(row.amount || 0) + '</td>' +
            '<td class="px-6 py-5">' +
              '<div class="flex items-center gap-2">' +
                '<span class="status-dot" style="background-color: ' + statusInfo.color + '"></span>' +
                '<span class="text-xs font-bold" style="color: ' + statusInfo.color + '">' + statusInfo.label + '</span>' +
              '</div>' +
            '</td>' +
            '<td class="px-6 py-5 text-center">' +
              '<div class="flex items-center justify-center gap-3">' +
                '<button class="text-[#5D4037]/30 hover:text-[#5D4037]"><svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path></svg></button>' +
                '<button class="text-[#5D4037]/30 hover:text-[#5D4037]"><svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path></svg></button>' +
              '</div>' +
            '</td>' +
            '</tr>'
          );
        }).join("");

        setHtml("seller-orders-body", html || '<tr><td class="px-6 py-10 text-center font-bold text-[#5D4037]/40" colspan="7">Không có đơn hàng nào khớp với bộ lọc</td></tr>');
      }).catch(function (err) {
        console.error('Seller orders load failed:', err);
        setHtml("seller-orders-body", '<tr><td class="px-6 py-5 text-center" colspan="7">Lỗi tải dữ liệu</td></tr>');
      });
    }

    window.filterByStatus = function(status) {
      currentStatus = status;
      // Update tab UI
      document.querySelectorAll('.order-tab').forEach(function(tab) {
        tab.classList.remove('active', 'text-[#5D4037]', 'border-[#5D4037]');
        tab.classList.add('text-[#5D4037]/50', 'border-transparent');
        
        var isMatch = false;
        if (status === 'ALL' && tab.textContent.includes('Tất cả')) isMatch = true;
        if (status === 'SHIPPING' && tab.textContent.includes('Đang giao')) isMatch = true;
        if (status === 'PENDING_PAYMENT' && tab.textContent.includes('Chờ xử lý')) isMatch = true;
        if (status === 'COMPLETED' && tab.textContent.includes('Đã hoàn thành')) isMatch = true;

        if (isMatch) {
           tab.classList.add('active', 'text-[#5D4037]', 'border-[#5D4037]');
           tab.classList.remove('text-[#5D4037]/50', 'border-transparent');
        }
      });
      load();
    };

    [qEl, periodEl, refreshEl, dateFromEl, dateToEl].forEach(function(el) {
      if (el) el.addEventListener("input", load);
      if (el) el.addEventListener("change", load);
    });

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

  function initVoucherManagement() {
    var searchEl = document.getElementById("voucher-search");
    var tableBodyEl = document.getElementById("voucher-table-body");
    var formEl = document.getElementById("voucher-form");
    var currentFilter = 'ALL';

    function load() {
      if (!window.ApiService || !ApiService.Voucher) return;
      
      var query = searchEl ? searchEl.value : "";
      return ApiService.Voucher.getSellerVouchers(query, currentFilter).then(function(vouchers) {
        setText("voucher-count", (vouchers || []).filter(v => v.status === 'ACTIVE').length);
        
        var html = (vouchers || []).map(function(v) {
          var typeLabel = v.discountType === 'PERCENT' ? 'Phần trăm' : 'Số tiền cố định';
          var valueDisplay = v.discountType === 'PERCENT' ? v.discountValue + '%' : vnd(v.discountValue);
          var statusInfo = getVoucherStatusInfo(v.status);
          var dateRange = formatDate(v.startDate) + " - " + formatDate(v.endDate);
          
          return (
            '<tr class="hover:bg-[#FAF5E8]/30 transition-colors">' +
            '<td class="px-6 py-5">' +
              '<div class="font-black text-[#5D4037]">' + esc(v.code) + '</div>' +
              '<div class="text-[10px] font-bold text-[#5D4037]/50">' + esc(v.name) + '</div>' +
            '</td>' +
            '<td class="px-6 py-5 text-xs font-bold text-[#5D4037]/70">' + typeLabel + '</td>' +
            '<td class="px-6 py-5 text-right text-xs font-black text-[#ea580c]">' + valueDisplay + '</td>' +
            '<td class="px-6 py-5 text-[10px] font-bold text-[#5D4037]/60">' + dateRange + '</td>' +
            '<td class="px-6 py-5 text-center text-xs font-black text-[#5D4037]">' + v.usedCount + '/' + v.usageLimit + '</td>' +
            '<td class="px-6 py-5">' +
              '<span class="status-pill" style="background-color: ' + statusInfo.bg + '; color: ' + statusInfo.color + '">' + statusInfo.label + '</span>' +
            '</td>' +
            '<td class="px-6 py-5 text-center">' +
              '<div class="flex items-center justify-center gap-3">' +
                '<button onclick="deleteVoucher(' + v.id + ')" class="text-rose-400 hover:text-rose-600 transition"><svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path></svg></button>' +
              '</div>' +
            '</td>' +
            '</tr>'
          );
        }).join("");

        setHtml("voucher-table-body", html || '<tr><td colspan="7" class="px-6 py-10 text-center font-bold text-[#5D4037]/40">Không tìm thấy mã giảm giá nào</td></tr>');
      });
    }

    function getVoucherStatusInfo(status) {
      switch (status) {
        case 'ACTIVE': return { label: 'Đang chạy', bg: '#ecfdf5', color: '#059669' };
        case 'EXPIRED': return { label: 'Hết hạn', bg: '#fef2f2', color: '#dc2626' };
        case 'DISABLED': return { label: 'Đã tắt', bg: '#f3f4f6', color: '#4b5563' };
        case 'EXHAUSTED': return { label: 'Hết lượt', bg: '#fffbeb', color: '#d97706' };
        default: return { label: status, bg: '#f3f4f6', color: '#4b5563' };
      }
    }

    function formatDate(d) {
      if (!d) return "";
      var date = new Date(d);
      return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
    }

    window.filterVouchers = function(status) {
      currentFilter = status;
      document.querySelectorAll('.voucher-tab').forEach(function(tab) {
        tab.classList.remove('active', 'text-[#5D4037]', 'border-[#5D4037]');
        tab.classList.add('text-[#5D4037]/50', 'border-transparent');
      });
      event.target.classList.add('active', 'text-[#5D4037]', 'border-[#5D4037]');
      event.target.classList.remove('text-[#5D4037]/50', 'border-transparent');
      load();
    };

    window.openCreateVoucherModal = function() {
      document.getElementById("voucher-modal").classList.remove("hidden");
    };

    window.closeVoucherModal = function() {
      document.getElementById("voucher-modal").classList.add("hidden");
      if (formEl) formEl.reset();
    };

    window.deleteVoucher = function(id) {
      if (confirm("Bạn có chắc chắn muốn xóa mã giảm giá này?")) {
        ApiService.Voucher.delete(id).then(function() {
          BookomToast.success("Đã xóa mã giảm giá thành công");
          load();
        }).catch(function(err) {
          BookomToast.error("Lỗi xóa voucher: " + err.message);
        });
      }
    };

    if (searchEl) searchEl.addEventListener("input", load);
    if (formEl) {
      formEl.addEventListener("submit", function(e) {
        e.preventDefault();
        var formData = new FormData(formEl);
        var data = {};
        formData.forEach((value, key) => {
          if (['discountValue', 'minOrderAmount', 'maxDiscountAmount', 'usageLimit'].includes(key)) {
            data[key] = value ? Number(value) : null;
          } else {
            data[key] = value;
          }
        });
        
        ApiService.Voucher.create(data).then(function() {
          BookomToast.success("Đã tạo mã giảm giá mới thành công");
          closeVoucherModal();
          load();
        }).catch(function(err) {
          BookomToast.error("Lỗi tạo voucher: " + err.message);
        });
      });
    }

    window.loadVouchers = load;
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
    initSellerAnalytics: initSellerAnalytics,
    initVoucherManagement: initVoucherManagement
  };
})();
