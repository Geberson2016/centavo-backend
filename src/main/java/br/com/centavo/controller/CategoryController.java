//package br.com.centavo.controller;
//
//import br.com.centavo.repository.CategoryDAO;
//import br.com.centavo.entity.Category;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/categories")
//public class CategoryController {
//
//    private final CategoryDAO categoryDAO;
//
//    public CategoryController(CategoryDAO categoryDAO) {
//        this.categoryDAO = categoryDAO;
//    }
//
//    @GetMapping
//    public List<Category> getAllCategories() {
//        return categoryDAO.findAll();
//    }
//
//    @PostMapping
//    public void createCategory(@RequestBody Category category) {
//        categoryDAO.save(category);
//    }
//}