package com.example.bookstore.service;

import com.example.bookstore.model.Book;
import com.example.bookstore.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service // Danh dau lai lop nay la Logic nghiep vu nhan yeu cau tu roi xu lys
public class BookService {
    @Autowired // keu SB tu dong "Tiem" dl Repository vao day
    private BookRepository bookRepository;

    // Function lay all book
    public List<Book> getAllBook(){
        // Sau nay co the them Logic o day (VD : Where : ....)
        return bookRepository.findAll();
    }

    //Function add new book
    public Book addBook(Book book){
        // nho Repository save new book into SSMS
        return bookRepository.save(book);
    }

    //Function get 1 book theo ID
    public Book getBookbyId(Long id){
        //Ham findById tra ve kieu Optional(co the co hoac khong co du lieu)
        //Dung orElse(null) nghia la : Neu khong tim thay sach thi tra ve null
        return bookRepository.findById(id).orElse(null);
    }

    //Function Delete 1 book by id
    public void deleteBoook(Long id){
        bookRepository.deleteById(id);
    }

    //Function Update info 1 book
    public Book updateBook(Long id,Book bookDetails){
        //1. Find old book in DB
        Book existingBook = bookRepository.findById(id).orElse(null);

        //2. Iffind, process force new db into
        if(existingBook != null){
            existingBook.setTitle(bookDetails.getTitle());
            existingBook.setAuthor(bookDetails.getAuthor());
            existingBook.setDescription(bookDetails.getDescription());
            existingBook.setPrice(bookDetails.getPrice());
            existingBook.setStockQuantity(bookDetails.getStockQuantity());
            //3. Save into DB
            return bookRepository.save(existingBook);
        }
        //return null if can't find id
        return null;
    }

        //=================================================================================
        // --- Tính năng mới cho multi-vendor
        //=================================================================================

//    S02
}
