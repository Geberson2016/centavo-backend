package br.com.centavo.controller;

import br.com.centavo.api.CategoriesApi;
import br.com.centavo.dto.CategoryRequest;
import br.com.centavo.dto.CategoryResponse;
import br.com.centavo.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Paths, validação e content types vêm da interface gerada a partir do openapi.yaml.
@RestController
public class CategoryController implements CategoriesApi {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Override
    public ResponseEntity<CategoryResponse> createCategory(CategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<List<CategoryResponse>> listCategories() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @Override
    public ResponseEntity<CategoryResponse> getCategory(Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }
}
