// Class này : dùng để bơm dữ liệu từ csv vào System
// Lưu ý : db hoàn toàn dùng từ file csv được lấy tưf dataset trên kaggle nên phần Price hoàn toàn là random dù tôi đã rất try tìm kiếm nhưng vẫn ko có giá sát thực tế được haizzz : ))

package com.example.bookstore;

import com.example.bookstore.model.Book;
import com.example.bookstore.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private BookRepository bookRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Dang don dep kho sach cu...");
        bookRepository.deleteAll();

        System.out.println("Dang nap du lieu sach tu file CSV Kaggle...");

        java.io.InputStream is = getClass().getResourceAsStream("/books_data.csv");
        if (is == null) {
            System.out.println("❌ BÁO ĐỘNG: Không tìm thấy file books_data.csv!");
            return;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Bỏ qua dòng tiêu đề: ISBN,Book-Title,Book-Author...
                }

                // Tách cột bằng Regex (để không bị lỗi nếu trong tên sách có dấu phẩy)
                String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");

                // File Kaggle có 8 cột, nên phải >= 8 mới lấy được Image-URL-L (cột số 7)
                if (data.length >= 8) {
                    try {
                        Book book = new Book();
                        book.setTitle(data[1].replace("\"", "").trim()); // Cột 1: Tên sách
                        book.setAuthor(data[2].replace("\"", "").trim()); // Cột 2: Tác giả

                        // Ghép Nhà xuất bản (Cột 6) và Năm (Cột 3) làm mô tả
                        String desc = "NXB: " + data[6].replace("\"", "").trim() +
                                " - Năm: " + data[3].replace("\"", "").trim();
                        if(desc.length() > 250) desc = desc.substring(0, 250);
                        book.setDescription(desc);

                        // Lấy link ảnh xịn nhất (Cột 7: Image-URL-L)
                        book.setImageUrl(data[7].replace("\"", "").trim());

                        // Phép thuật Random: Giá từ 50k đến 250k (làm tròn hàng ngàn cho đẹp)
                        double randomPrice = Math.round((Math.random() * 200000) + 50000) / 1000 * 1000;
                        book.setPrice(randomPrice);

                        // Phép thuật Random: Tồn kho từ 10 đến 100
                        book.setStockQuantity((int)(Math.random() * 90) + 10);

                        bookRepository.save(book);
                    } catch (Exception e) {
                        // Kệ mấy dòng bị lỗi cấu trúc, skip luôn
                    }
                }
            }
            System.out.println("✅ Nạp dữ liệu từ CSV Kaggle thành công rực rỡ! Anh mở web lên xem đi!");
        } catch (Exception e) {
            System.out.println("❌ Lỗi khi đọc file: " + e.getMessage());
        }
    }
}