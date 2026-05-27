// PanelPageController : dùng để điều hướng các trang admin và seller, mỗi phương thức sẽ trả về một view tương ứng với trang đó, đồng thời truyền vào model các thuộc tính như pageTitle, pageSubtitle và activeMenu để hiển thị thông tin trên giao diện và đánh dấu menu đang hoạt động.
package com.example.bookstore.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PanelPageController {

    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        model.addAttribute("pageTitle", "Tổng quan hệ thống");
        model.addAttribute("pageSubtitle", "Theo dõi hoạt động kinh doanh toàn sàn");
        model.addAttribute("activeMenu", "admin-dashboard");
        return "admin/Admin";
    }

    @GetMapping("/admin/users")
    public String adminUsers(Model model) {
        model.addAttribute("pageTitle", "Quản lý người dùng");
        model.addAttribute("pageSubtitle", "Lọc theo tên, vai trò và trạng thái tài khoản");
        model.addAttribute("activeMenu", "admin-users");
        return "admin/Admin_Users";
    }

    @GetMapping("/admin/books")
    public String adminBooks(Model model) {
        model.addAttribute("pageTitle", "Kiểm duyệt sản phẩm");
        model.addAttribute("pageSubtitle", "Phê duyệt sách mới và quản lý nội dung");
        model.addAttribute("activeMenu", "admin-books");
        return "admin/Admin_Books";
    }

    @GetMapping("/admin/shops")
    public String adminShops(Model model) {
        model.addAttribute("pageTitle", "Xét duyệt gian hàng");
        model.addAttribute("pageSubtitle", "Quản lý đối tác và thông tin pháp lý");
        model.addAttribute("activeMenu", "admin-shops");
        return "admin/Admin_Shops";
    }

    @GetMapping("/admin/orders")
    public String adminOrders(Model model) {
        model.addAttribute("pageTitle", "Quản lý đơn hàng");
        model.addAttribute("pageSubtitle", "Xem toàn bộ đơn hàng trên hệ thống");
        model.addAttribute("activeMenu", "admin-orders");
        return "admin/Admin_Orders";
    }

    @GetMapping("/admin/categories")
    public String adminCategories(Model model) {
        model.addAttribute("pageTitle", "Quản lý danh mục");
        model.addAttribute("pageSubtitle", "Thêm, sửa và xóa danh mục sách trên sàn");
        model.addAttribute("activeMenu", "admin-categories");
        return "admin/Admin_Categories";
    }

    @GetMapping("/admin/coupons")
    public String adminCoupons(Model model) {
        model.addAttribute("pageTitle", "Quản lý khuyến mãi");
        model.addAttribute("pageSubtitle", "Tạo và quản lý các mã giảm giá trên toàn sàn");
        model.addAttribute("activeMenu", "admin-coupons");
        return "admin/Admin_Coupons";
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

    @GetMapping("/seller/vouchers")
    public String sellerVouchers(Model model) {
        model.addAttribute("pageTitle", "Quan ly khuyen mai");
        model.addAttribute("pageSubtitle", "Tao va quan ly ma giam gia cho cua hang");
        model.addAttribute("activeMenu", "seller-vouchers");
        return "seller/Seller_Vouchers";
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