package com.example.bookstore.service;

import com.example.bookstore.model.Category;
import com.example.bookstore.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    // Lấy danh sách tất cả thể loại
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // Thêm một thể loại mới
    public Category addCategory(Category category) {
        return categoryRepository.save(category);
    }
}