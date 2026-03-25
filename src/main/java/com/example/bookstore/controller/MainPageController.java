package com.example.bookstore.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainPageController {

    @GetMapping("/")
    public String home() {
        return "main/index";
    }

    @GetMapping("/main/index.html")
    public String homeHtmlAlias() {
        return "main/index";
    }

    @GetMapping("/main/discovery")
    public String discovery() {
        return "main/Discovery_Page";
    }

    @GetMapping("/main/Discovery_Page.html")
    public String discoveryHtmlAlias() {
        return "main/Discovery_Page";
    }

    @GetMapping("/main/auth")
    public String auth() {
        return "main/Auth_Page";
    }

    @GetMapping("/main/Auth_Page.html")
    public String authHtmlAlias() {
        return "main/Auth_Page";
    }

    @GetMapping("/main/cart")
    public String cart() {
        return "main/Cart_Page";
    }

    @GetMapping("/main/Cart_Page.html")
    public String cartHtmlAlias() {
        return "main/Cart_Page";
    }

    @GetMapping("/main/contact")
    public String contact() {
        return "main/Contact_us";
    }

    @GetMapping("/main/Contact_us.html")
    public String contactHtmlAlias() {
        return "main/Contact_us";
    }

    @GetMapping("/main/checkout")
    public String checkout() {
        return "main/Checkout_Page";
    }

    @GetMapping("/main/Checkout_Page.html")
    public String checkoutHtmlAlias() {
        return "main/Checkout_Page";
    }

    @GetMapping("/main/order-details")
    public String orderDetails() {
        return "main/Order_Details";
    }

    @GetMapping("/main/Order_Details.html")
    public String orderDetailsHtmlAlias() {
        return "main/Order_Details";
    }

    @GetMapping("/main/order-success")
    public String orderSuccess() {
        return "main/Order_Success";
    }

    @GetMapping("/main/Order_Success.html")
    public String orderSuccessHtmlAlias() {
        return "main/Order_Success";
    }

    @GetMapping("/main/search")
    public String searchResult() {
        return "main/Search_Result";
    }

    @GetMapping("/main/Search_Result.html")
    public String searchResultHtmlAlias() {
        return "main/Search_Result";
    }

    @GetMapping("/main/flash-sale")
    public String flashSale() {
        return "main/Flash_Sale";
    }

    @GetMapping("/main/Flash_Sale.html")
    public String flashSaleHtmlAlias() {
        return "main/Flash_Sale";
    }

    @GetMapping("/buyer/dashboard")
    public String buyerDashboard() {
        return "buyer/Buyer_DashBoard";
    }
}
