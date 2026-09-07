package com.softdev.yourmeal;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class MealRecommendationService {

    private final HackClubMealRecommendationClient hackClubMealRecommendationClient;

    public MealRecommendationService(HackClubMealRecommendationClient hackClubMealRecommendationClient) {
        this.hackClubMealRecommendationClient = hackClubMealRecommendationClient;
    }

    public MealRecommendationResult recommendMeals(DietaryProfile profile) {
        List<String> restrictions = describeRestrictions(profile);

        return hackClubMealRecommendationClient.recommendMeals(profile, restrictions);
    }

    public GroceryListResult recommendIngredients(List<String> savedMealNames) {
        return hackClubMealRecommendationClient.recommendIngredients(savedMealNames);
    }

    public List<String> describeRestrictions(DietaryProfile profile) {
        List<String> restrictions = new ArrayList<>();

        addIf(restrictions, profile.isVegetarian(), "Vegetarian");
        addIf(restrictions, profile.isVegan(), "Vegan");
        addIf(restrictions, profile.isGlutenFree(), "Gluten-free");
        addIf(restrictions, profile.isDairyFree(), "Dairy-free");
        addIf(restrictions, profile.isEggFree(), "Egg-free");
        addIf(restrictions, profile.isPeanutFree(), "Peanut-free");
        addIf(restrictions, profile.isNutFree(), "Tree nut-free");
        addIf(restrictions, profile.isShellfishFree(), "Shellfish-free");
        addIf(restrictions, profile.isSoyFree(), "Soy-free");
        addIf(restrictions, profile.isFishFree(), "Fish-free");
        addIf(restrictions, profile.isDiabetes(), "Diabetes-aware");
        addIf(restrictions, profile.isHBP(), "Blood pressure-aware");
        addIf(restrictions, profile.isKidneyDisease(), "Kidney disease-aware");
        addIf(restrictions, profile.isIBS(), "IBS-aware");
        addIf(restrictions, profile.isCeliac(), "Celiac-aware");

        return restrictions;
    }

    private void addIf(List<String> restrictions, boolean condition, String label) {
        if (condition) {
            restrictions.add(label);
        }
    }
}
