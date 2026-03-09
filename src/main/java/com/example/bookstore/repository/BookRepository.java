package com.example.bookstore.repository;
import com.example.bookstore.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository

public interface BookRepository extends JpaRepository<Book, Long>{
    // Ví dụ: Tìm sách theo tiêu đề để hỗ trợ gợi ý sản phẩm
    List<Book> findByTitleContaining(String title);
}
