package com.example.bookstore.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity //danh dau day la 1 bang trong DB
@Table(name="books")
@Data // tu dong tao Getter,Setter,toString,equals,hashCode
@NoArgsConstructor // Auto tạo Constructor không tham số
@AllArgsConstructor // Auto tao Constructor co tham so

public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto tang id
    private Long id;
    @Column(nullable = false)
    private String title;
    private String author;
    private String description;
    private Double price;
    private Integer stockQuantity;


}
