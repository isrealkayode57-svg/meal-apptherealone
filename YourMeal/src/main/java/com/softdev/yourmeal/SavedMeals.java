package com.softdev.yourmeal;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "saved_meals")
public class SavedMeals {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private AppUser user;

    @Column(nullable = false)
    private String mealNames;

    @ElementCollection
    @CollectionTable(
            name = "saved_meal_ingredients",
            joinColumns = @JoinColumn(
                    name = "saved_meal_id"
            )
    )
    @OrderColumn(name = "ingredient_order")
    @Column(name = "ingredient")
    private List<String> ingredients =
            new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "saved_meal_instructions",
            joinColumns = @JoinColumn(
                    name = "saved_meal_id"
            )
    )
    @OrderColumn(name = "instruction_order")
    @Column(name = "instruction")
    private List<String> instructions =
            new ArrayList<>();

    protected SavedMeals() {
    }

    /*
     * Constructor for older code that only
     * provides a meal name.
     */
    public SavedMeals(
            AppUser user,
            String mealNames) {

        this.user = user;
        this.mealNames = mealNames;

        this.ingredients =
                new ArrayList<>();

        this.instructions =
                new ArrayList<>();
    }

    /*
     * Constructor used when saving a complete
     * AI recommendation.
     */
    public SavedMeals(
            AppUser user,
            String mealNames,
            List<String> ingredients,
            List<String> instructions) {

        this.user = user;
        this.mealNames = mealNames;

        this.ingredients =
                ingredients == null
                        ? new ArrayList<>()
                        : new ArrayList<>(ingredients);

        this.instructions =
                instructions == null
                        ? new ArrayList<>()
                        : new ArrayList<>(instructions);
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

    public List<String> getIngredients() {
        return ingredients;
    }

    public List<String> getInstructions() {
        return instructions;
    }
}
