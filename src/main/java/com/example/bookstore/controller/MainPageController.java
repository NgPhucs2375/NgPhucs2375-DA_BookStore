package com.example.bookstore.controller;

import com.example.bookstore.model.Book;
import com.example.bookstore.model.Category;
import com.example.bookstore.model.SellerShop;
import com.example.bookstore.model.enums.ApprovalStatus;
import com.example.bookstore.repository.BookRepository;
import com.example.bookstore.repository.CategoryRepository;
import com.example.bookstore.repository.SellerShopRepository;
import com.example.bookstore.service.SellerShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
public class MainPageController {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private SellerShopService sellerShopService;

    @Autowired
    private SellerShopRepository sellerShopRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping("/")
    public String home(Model model) {
        // Load approved books for display (limit to 20)
        Pageable pageable = PageRequest.of(0, 20);
        model.addAttribute("books", bookRepository.findByApprovalStatus(ApprovalStatus.APPROVED, pageable).getContent());
        return "main/index";
    }

    @GetMapping("/main/index.html")
    public String homeHtmlAlias() {
        return "main/index";
    }

    @GetMapping("/main/discovery")
    public String discovery(Model model) {
        // Load approved books for discovery page
        Pageable pageable = PageRequest.of(0, 20);
        model.addAttribute("books", bookRepository.findByApprovalStatus(ApprovalStatus.APPROVED, pageable).getContent());
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

    @GetMapping("/main/payment-result")
    public String paymentResult() {
        return "main/Payment_Result";
    }

    @GetMapping("/main/Payment_Result.html")
    public String paymentResultHtmlAlias() {
        return "main/Payment_Result";
    }

    @GetMapping("/buyer/dashboard")
    public String buyerDashboard() {
        return "buyer/Buyer_Profile_Dashboard";
    }

    /**
     * Public shop page - hiển thị thông tin shop và danh sách sản phẩm
     * Dữ liệu được đồng bộ từ database thay vì mock data
     */
    @GetMapping("/shop/{slug}")
    public String viewShop(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "popular") String sort,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            Model model) {
        // 1. Lấy thông tin shop từ database
        SellerShop shop = sellerShopRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cửa hàng."));

        // 2. Chỉ hiển thị shop đã được duyệt
        if (shop.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cửa hàng này hiện không hoạt động.");
        }

        // 3. Lấy danh sách sách APPROVED của shop (có phân trang)
        Sort sorting;
        switch (sort) {
            case "newest":
                sorting = Sort.by(Sort.Direction.DESC, "id");
                break;
            case "price_asc":
                sorting = Sort.by(Sort.Direction.ASC, "price");
                break;
            case "price_desc":
                sorting = Sort.by(Sort.Direction.DESC, "price");
                break;
            case "bestselling":
                sorting = Sort.by(Sort.Direction.DESC, "id"); // fallback, sẽ dùng query riêng
                break;
            default: // popular
                sorting = Sort.by(Sort.Direction.DESC, "id");
                break;
        }
        Pageable pageable = PageRequest.of(page, size, sorting);

        // Lấy sách APPROVED của shop với filter
        Page<Book> booksPage;
        if (category != null || minPrice != null || maxPrice != null) {
            // Sử dụng search query với filter
            List<Long> sellerIdList = List.of(shop.getSeller().getId());
            booksPage = bookRepository.searchApprovedBooks(
                    null,                                                    // q
                    category != null ? List.of(category) : null,             // categoryIds
                    sellerIdList,                                            // sellerIds
                    null,                                                    // author
                    minPrice,                                                // minPrice
                    maxPrice,                                                // maxPrice
                    null,                                                    // minRating
                    null,                                                    // inStock
                    null,                                                    // publishYearFrom
                    null,                                                    // publishYearTo
                    ApprovalStatus.APPROVED,                                 // status
                    pageable                                                 // pageable
            );
        } else {
            booksPage = bookRepository.findBySellerAndApprovalStatus(
                    shop.getSeller(), ApprovalStatus.APPROVED, pageable);
        }
        // 4. Lấy danh sách categories
        List<Category> categories = categoryRepository.findAll();

        // 5. Đếm số lượng sách
        int bookCount = (int) booksPage.getTotalElements();

        // 6. Tính thời gian tham gia
        String joinDuration = "Mới";
        if (shop.getCreatedAt() != null) {
            long years = ChronoUnit.YEARS.between(shop.getCreatedAt(), LocalDateTime.now());
            if (years > 0) {
                joinDuration = years + " năm";
            } else {
                long months = ChronoUnit.MONTHS.between(shop.getCreatedAt(), LocalDateTime.now());
                joinDuration = (months > 0 ? months + " tháng" : "Mới");
            }
        }

        // 7. Đưa dữ liệu vào model
        model.addAttribute("shop", shop);
        model.addAttribute("books", booksPage.getContent());
        model.addAttribute("bookCount", bookCount);
        model.addAttribute("categories", categories);
        model.addAttribute("joinDuration", joinDuration);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", booksPage.getTotalPages());
        model.addAttribute("totalElements", booksPage.getTotalElements());
        model.addAttribute("currentSort", sort);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);

        return "seller/Shop_Seller";
    }
}
