package com.softdev.yourmeal;

import jakarta.persistence.*;

@Entity
@Table(name = "saved_meals")
public class SavedMeals {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    private String mealNames;

    @Lob
    private String ingredients;

    @Lob
    private String instructions;

    protected SavedMeals() {
    }

    public SavedMeals(
            AppUser user,
            String mealNames,
            String ingredients,
            String instructions) {

        this.user = user;
        this.mealNames = mealNames;
        this.ingredients = ingredients;
        this.instructions = instructions;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public String getMealNames() {
        return mealNames;
    }

    public String getIngredients() {
        return ingredients;
    }

    public String getInstructions() {
        return instructions;
    }
}
