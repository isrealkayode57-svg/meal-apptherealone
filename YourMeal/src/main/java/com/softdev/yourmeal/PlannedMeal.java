package com.softdev.yourmeal;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/*
 * One meal that a user has put on a specific day in the planner.
 * It can come from their saved meals or be typed in as a new meal;
 * either way only the name is stored.
 */
@Entity
@Table(name = "planned_meals")
public class PlannedMeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private LocalDate planDate;

    @Column(nullable = false, length = 20)
    private String mealType;

    @Column(nullable = false, length = 150)
    private String mealName;

    protected PlannedMeal() {
    }

    public PlannedMeal(
            AppUser user,
            LocalDate planDate,
            String mealType,
            String mealName) {

        this.user = user;
        this.planDate = planDate;
        this.mealType = mealType;
        this.mealName = mealName;
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public LocalDate getPlanDate() {
        return planDate;
    }

    public String getMealType() {
        return mealType;
    }

    public String getMealName() {
        return mealName;
    }
}
