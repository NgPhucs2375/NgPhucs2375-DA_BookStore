(function () {
  function navItems(role) {
    if (role === "seller") {
      return [
        { key: "seller-dashboard", href: "../Seller/Seller_Dashboard.html", label: "Tong quan" },
        { key: "seller-orders", href: "../Seller/Seller_Orders.html", label: "Don hang" },
        { key: "seller-inventory", href: "../Seller/Inventory_Management.html", label: "Kho hang" },
        { key: "seller-analytics", href: "../Seller/Seller_Analytics.html", label: "Doanh thu" }
      ];
    }

    return [
      { key: "admin-dashboard", href: "Admin.html", label: "Tong quan san" },
      { key: "admin-users", href: "Admin_Users.html", label: "Nguoi dung" },
      { key: "admin-shops", href: "Admin_Shops.html", label: "Gian hang" },
      { key: "admin-books", href: "Admin_Books.html", label: "Kiem duyet sach" }
    ];
  }

  function sidebar(cfg) {
    var list = navItems(cfg.role)
      .map(function (item) {
        var active = item.key === cfg.page;
        return (
          '<a href="' +
          item.href +
          '" class="flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-extrabold tracking-wide transition ' +
          (active
            ? 'bg-brand-biscuit text-white shadow-md'
            : 'text-brand-cream/90 hover:bg-white/10 hover:text-white') +
          '">' +
          '<span class="h-2 w-2 rounded-full ' +
          (active ? 'bg-white' : 'bg-brand-cream/50') +
          '"></span>' +
          item.label +
          '</a>'
        );
      })
      .join("");

    return (
      '<aside class="hidden md:flex md:w-72 md:flex-col bg-brand-dark border-r border-black/10">' +
      '<div class="h-20 px-6 flex items-center border-b border-white/10">' +
      '<a href="' +
      (cfg.role === "seller" ? "../Seller/Seller_Dashboard.html" : "Admin.html") +
      '" class="text-2xl font-black tracking-tight text-white">BOOKOM <span class="text-brand-peach text-sm align-middle">' +
      (cfg.role === "seller" ? "SELLER" : "ADMIN") +
      "</span></a>" +
      "</div>" +
      '<nav class="p-4 space-y-2">' +
      list +
      "</nav>" +
      "</aside>"
    );
  }

  function header(cfg) {
    return (
      '<header class="h-20 bg-white border-b border-brand-accent px-6 md:px-8 flex items-center justify-between">' +
      '<div>' +
      '<h1 class="text-xl md:text-2xl font-black text-brand-dark">' + cfg.title + "</h1>" +
      '<p class="text-xs md:text-sm text-brand-dark/70 font-bold">' + (cfg.subtitle || "") + "</p>" +
      "</div>" +
      '<div class="flex items-center gap-3">' +
      '<a href="../Main/index.html" class="hidden sm:inline-flex text-sm font-black text-brand-dark/70 hover:text-brand-dark">Ve trang mua sam</a>' +
      '<div class="h-10 w-10 rounded-full bg-brand-biscuit text-white flex items-center justify-center text-xs font-black shadow-sm">AD</div>' +
      "</div>" +
      "</header>"
    );
  }

  window.BookomPanelShell = {
    init: function (cfg) {
      var sidebarHost = document.getElementById("panel-sidebar");
      var headerHost = document.getElementById("panel-header");
      if (!sidebarHost || !headerHost) return;
      sidebarHost.outerHTML = sidebar(cfg);
      headerHost.outerHTML = header(cfg);
    }
  };
})();
