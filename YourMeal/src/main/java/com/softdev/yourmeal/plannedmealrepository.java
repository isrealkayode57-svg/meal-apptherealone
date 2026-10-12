package com.softdev.yourmeal;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlannedMealRepository extends JpaRepository<PlannedMeal, Long> {

    List<PlannedMeal> findByUserAndPlanDateBetweenOrderByPlanDateAscIdAsc(
            AppUser user,
            LocalDate start,
            LocalDate end);

    List<PlannedMeal> findTop12ByUserAndPlanDateGreaterThanEqualOrderByPlanDateAscIdAsc(
            AppUser user,
            LocalDate from);
}
