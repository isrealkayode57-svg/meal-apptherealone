package com.softdev.yourmeal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedMealsRepository
        extends JpaRepository<SavedMeals, Long> {

    List<SavedMeals> findByUser(AppUser user);

    boolean existsByUserAndMealNames(
            AppUser user,
            String mealNames);
}
```
