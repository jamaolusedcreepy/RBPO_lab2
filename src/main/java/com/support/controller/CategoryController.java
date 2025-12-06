package com.support.controller;

import com.support.domain.Category;
import com.support.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    // Создание категории
    @PostMapping
    public ResponseEntity<?> createCategory(@Valid @RequestBody Category category) {
        if (categoryRepository.existsByName(category.getName())) {
            return ResponseEntity.badRequest()
                    .body("Error: Category name already exists!");
        }
        Category savedCategory = categoryRepository.save(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCategory);
    }

    // Получение всех категорий
    @GetMapping
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // Получение категории по ID
    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(@PathVariable Long id) {
        Optional<Category> category = categoryRepository.findById(id);
        return category.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Обновление категории
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(@PathVariable Long id, @Valid @RequestBody Category categoryDetails) {
        return categoryRepository.findById(id)
                .map(category -> {
                    if (!category.getName().equals(categoryDetails.getName()) &&
                            categoryRepository.existsByName(categoryDetails.getName())) {
                        return ResponseEntity.badRequest()
                                .body("Error: Category name already exists!");
                    }
                    category.setName(categoryDetails.getName());
                    category.setDescription(categoryDetails.getDescription());
                    Category updatedCategory = categoryRepository.save(category);
                    return ResponseEntity.ok(updatedCategory);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Удаление категории
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        return categoryRepository.findById(id)
                .map(category -> {
                    categoryRepository.delete(category);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}