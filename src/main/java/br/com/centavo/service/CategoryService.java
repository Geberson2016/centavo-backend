package br.com.centavo.service;

import br.com.centavo.dto.CategoryRequest;
import br.com.centavo.dto.CategoryResponse;
import br.com.centavo.entity.Category;
import br.com.centavo.entity.User;
import br.com.centavo.repository.CategoryRepository;
import br.com.centavo.repository.UserRepository;
import br.com.centavo.util.AuthUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final AuthUtils authUtils;

    public CategoryService(CategoryRepository categoryRepository, AuthUtils authUtils) {
        this.categoryRepository = categoryRepository;
        this.authUtils = authUtils;

    }

    public CategoryResponse createCategory (CategoryRequest categoryRequest) {
        User user = authUtils.getAuthenticatedUser();

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
        User user = authUtils.getAuthenticatedUser();

        return categoryRepository.findAllByUserId(user.getId())
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

    public CategoryResponse findById(Long id) {
        User user = authUtils.getAuthenticatedUser();

        Category category = categoryRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada"));

        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getUser().getId(),
                category.getBudgetType()
        );
    }

}
