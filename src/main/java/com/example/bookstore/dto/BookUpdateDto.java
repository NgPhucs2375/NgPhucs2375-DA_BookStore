package com.example.bookstore.dto;

import lombok.Data;

@Data
public class BookUpdateDto {
    private String title;
    private String author;
    private String description;
    private Double price;
    private Integer stockQuantity;
    private String publisher;
    private String publishYear;
    private Long categoryId;
}