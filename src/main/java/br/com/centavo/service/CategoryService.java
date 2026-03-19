package br.com.centavo.service;

import br.com.centavo.dto.CategoryRequest;
import br.com.centavo.dto.CategoryResponse;
import br.com.centavo.entity.Category;
import br.com.centavo.entity.User;
import br.com.centavo.repository.CategoryRepository;
import br.com.centavo.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this. userRepository = userRepository;
    }

    public CategoryResponse createCategory (CategoryRequest categoryRequest) {
        User user = userRepository.findById(categoryRequest.userId())
                .orElseThrow(()-> new RuntimeException("Usuário não encontrado"));

        Category category = new Category(
                categoryRequest.name(),
                categoryRequest.type(),
                user,
                categoryRequest.budgetType()
        );

        Category savedCategory = categoryRepository.save(category);

        return new CategoryResponse(
                savedCategory.getId(),
                savedCategory.getName(),
                savedCategory.getType(),
                savedCategory.getUser().getId(),
                savedCategory.getBudgetType()
        );

    }

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll()
                .stream()
                .map(category -> new CategoryResponse(
                        category.getId(),
                        category.getName(),
                        category.getType(),
                        category.getUser().getId(),
                        category.getBudgetType()
                ))
                .toList();
    }

}
