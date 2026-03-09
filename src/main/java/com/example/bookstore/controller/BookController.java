package com.example.bookstore.controller;

import com.example.bookstore.model.Book;
import com.example.bookstore.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // Bao cho SB biet class nay chuyen dung de tao API tra ve du lieu(thuong la dinh dang JSON) chu khong phai tra ve giao dien HTML
@RequestMapping("/api/books") // Dat "dia chi goc" cho toan bo cac API trong class nay
public class BookController {


    @Autowired // Day chinh la co che Dependency Injection quen thuoc
    // (tuong tu nhu cach lam viec voi interface trong cac project .NET). Spring Boot se tu dong "tiem" BookService vao de dung ma khong can thiet phai viet
    //API lay danh sach toan bo sach
    private BookService bookService;
    @GetMapping // Bao hieu rang ham getAllBooks() se duoc chay khi co ai do truy cap vao dia chi goc bang phuong thuc Get(nhu khi go link tren trinh duyet)
    public List<Book> getAllBooks(){
        return bookService.getAllBook();
        // ham findAll() : duoc Spring Boot tu dong cung cap
    }

    // --- API add new book ---
    // @RequestBody : khi gui 1 cuc dl Json chua thong tin sach SB auto "nan" JSON do thanh 1 Doi tuong "Object" Book in Java tinh nang nay same [FromBody] trong .Net API
    @PostMapping
    public Book createBook(@RequestBody Book book){
        return bookService.addBook(book);
    }

    //API take one book by id
    //Dau ngoac nhon {id} nghia la gia tri nay se thay doi theo tren Url (vd: /api/books/1)
    @GetMapping("/{id}")
    public Book getBookById(@PathVariable Long id){
        return bookService.getBookbyId(id);
    }

    //API delete book by id
    @DeleteMapping("/{id}")
    public void deleteBook(@PathVariable Long id){
        bookService.deleteBoook(id);
    }
    //API update
    @PutMapping("/{id}")
    public Book updateBook(@PathVariable Long id, @RequestBody Book bookDetails) {
        return bookService.updateBook(id, bookDetails);
    }
}
