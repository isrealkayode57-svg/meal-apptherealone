package com.softdev.yourmeal;

import java.util.List;

public record MealRecommendationResult(
        List<MealRecommendation> meals,
        String statusMessage
) {

    public boolean hasMeals() {
        return meals != null && !meals.isEmpty();
    }
}
