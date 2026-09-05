package com.softdev.yourmeal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GroceryIngredientRepository
        extends JpaRepository<GroceryIngredient, Long> {

    List<GroceryIngredient> findByUser(AppUser user);

    boolean existsByUserAndNormalizedName(
            AppUser user,
            String normalizedName
    );
}
