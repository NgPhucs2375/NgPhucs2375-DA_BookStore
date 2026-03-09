package com.example.bookstore;

import com.example.bookstore.model.Book;
import com.example.bookstore.repository.BookRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BookStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookStoreApplication.class, args);
    }

    @Bean
    CommandLineRunner start(BookRepository bookRepository){
        return args -> {
            //Tao thu 1 cuon sach
            Book book1 = new Book(null,"lap trinh Java Spring","Will","Sach hay cho dev",150000.0,50);
            // Luu vao SQL Server
            bookRepository.save(book1);
            System.out.println("--- Da save cuon sach dau tien vao SSMS ! ---");

        };
    }
}
