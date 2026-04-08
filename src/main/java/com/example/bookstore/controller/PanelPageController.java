// PanelPageController : dùng để điều hướng các trang admin và seller, mỗi phương thức sẽ trả về một view tương ứng với trang đó, đồng thời truyền vào model các thuộc tính như pageTitle, pageSubtitle và activeMenu để hiển thị thông tin trên giao diện và đánh dấu menu đang hoạt động.
package com.example.bookstore.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PanelPageController {

    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        model.addAttribute("pageTitle", "Tong quan san");
        model.addAttribute("pageSubtitle", "Dieu huong bang Thymeleaf Fragments");
        model.addAttribute("activeMenu", "admin-dashboard");
        return "admin/Admin";
    }

    @GetMapping("/admin/users")
    public String adminUsers(Model model) {
        model.addAttribute("pageTitle", "Quan ly nguoi dung");
        model.addAttribute("pageSubtitle", "Loc theo ten, vai tro, trang thai");
        model.addAttribute("activeMenu", "admin-users");
        return "admin/Admin_Users";
    }

    @GetMapping("/admin/books")
    public String adminBooks(Model model) {
        model.addAttribute("pageTitle", "Kiem duyet sach");
        model.addAttribute("pageSubtitle", "Loc theo ten, danh muc, ton kho");
        model.addAttribute("activeMenu", "admin-books");
        return "admin/Admin_Books";
    }

    @GetMapping("/admin/shops")
    public String adminShops(Model model) {
        model.addAttribute("pageTitle", "Xet duyet gian hang");
        model.addAttribute("pageSubtitle", "Tim kiem theo ten shop, chu shop, MST");
        model.addAttribute("activeMenu", "admin-shops");
        return "admin/Admin_Shops";
    }

    @GetMapping("/seller/dashboard")
    public String sellerDashboard(Model model) {
        model.addAttribute("pageTitle", "Tong quan nha ban");
        model.addAttribute("pageSubtitle", "Theo doi tong quan don hang va kho");
        model.addAttribute("activeMenu", "seller-dashboard");
        return "seller/Seller_Dashboard";
    }

    @GetMapping("/seller/orders")
    public String sellerOrders(Model model) {
        model.addAttribute("pageTitle", "Quan ly don hang");
        model.addAttribute("pageSubtitle", "Loc theo ma don va trang thai");
        model.addAttribute("activeMenu", "seller-orders");
        return "seller/Seller_Orders";
    }

    @GetMapping("/seller/inventory")
    public String sellerInventory(Model model) {
        model.addAttribute("pageTitle", "Quan ly kho hang");
        model.addAttribute("pageSubtitle", "Loc theo ten sach, danh muc, ton kho");
        model.addAttribute("activeMenu", "seller-inventory");
        return "seller/Inventory_Management";
    }

    @GetMapping("/seller/analytics")
    public String sellerAnalytics(Model model) {
        model.addAttribute("pageTitle", "Phan tich doanh thu");
        model.addAttribute("pageSubtitle", "Bieu do doanh thu va trang thai don");
        model.addAttribute("activeMenu", "seller-analytics");
        return "seller/Seller_Analytics";
    }

    @GetMapping("/seller/shop")
    public String sellerShop(Model model) {
        model.addAttribute("pageTitle", "Ho so gian hang");
        model.addAttribute("pageSubtitle", "Cap nhat thong tin shop va trang thai hoat dong");
        model.addAttribute("activeMenu", "seller-shop");
        return "seller/Shop_Seller";
    }

    @GetMapping("/seller/product-detail")
    public String sellerProductDetail() {
        return "seller/Seller_Product_Detail";
    }
}
