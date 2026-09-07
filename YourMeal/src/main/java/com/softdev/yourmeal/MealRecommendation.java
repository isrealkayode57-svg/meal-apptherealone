package com.softdev.yourmeal;

import java.util.List;

public record MealRecommendation(
        String name,
        String reason,
        String tags,
        List<String> ingredients,
        String recipe
) {
}
