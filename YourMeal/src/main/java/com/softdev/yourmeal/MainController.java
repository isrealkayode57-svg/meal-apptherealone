package com.softdev.yourmeal;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MainController {

    private final AppUserRepository appUserRepository;
    private final DietaryProfileRepository dietaryProfileRepository;
    private final SavedMealsRepository savedMealsRepository;
    private final GroceryIngredientRepository groceryIngredientRepository;
    private final MealRecommendationService mealRecommendationService;
    private final SQLeditor sqleditor;

    public MainController(
            AppUserRepository appUserRepository,
            DietaryProfileRepository dietaryProfileRepository,
            MealRecommendationService mealRecommendationService,
            SQLeditor sqleditor,
            SavedMealsRepository savedMealsRepository,
            GroceryIngredientRepository groceryIngredientRepository) {

        this.appUserRepository = appUserRepository;
        this.dietaryProfileRepository = dietaryProfileRepository;
        this.mealRecommendationService = mealRecommendationService;
        this.sqleditor = sqleditor;
        this.savedMealsRepository = savedMealsRepository;
        this.groceryIngredientRepository = groceryIngredientRepository;
    }

    // ============================================================
    // HOME
    // ============================================================

    @GetMapping("/")
    public String index() {
        return "index";
    }

    // ============================================================
    // DIETARY SELECTION
    // ============================================================

    @GetMapping("/selection")
    public String selection(
            HttpSession session,
            Model model) {

        AppUser user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        DietaryProfile profile =
                dietaryProfileRepository.findByUser(user)
                        .orElseGet(() ->
                                dietaryProfileRepository.save(
                                        new DietaryProfile(user)));

        model.addAttribute("name", user.getName());
        model.addAttribute("profile", profile);

        return "selection";
    }

    @PostMapping("/selection")
    public String saveDietaryProfile(
            @RequestParam(defaultValue = "false")
            boolean vegetarian,

            @RequestParam(defaultValue = "false")
            boolean vegan,

            @RequestParam(defaultValue = "false")
            boolean dairy,

            @RequestParam(defaultValue = "false")
            boolean egg,

            @RequestParam(defaultValue = "false")
            boolean gluten,

            @RequestParam(defaultValue = "false")
            boolean peanuts,

            @RequestParam(defaultValue = "false")
            boolean shellfish,

            @RequestParam(defaultValue = "false")
            boolean soy,

            @RequestParam(defaultValue = "false")
            boolean nuts,

            @RequestParam(defaultValue = "false")
            boolean fish,

            @RequestParam(defaultValue = "false")
            boolean diabetes,

            @RequestParam(defaultValue = "false")
            boolean HBP,

            @RequestParam(defaultValue = "false")
            boolean kidney,

            @RequestParam(defaultValue = "false")
            boolean IBS,

            @RequestParam(defaultValue = "false")
            boolean celiac,

            @RequestParam(defaultValue = "")
            String goal,

            HttpSession session) {

        AppUser user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        DietaryProfile profile =
                dietaryProfileRepository
                        .findByUser(user)
                        .orElseGet(() ->
                                new DietaryProfile(user));

        profile.setVegetarian(vegetarian);
        profile.setVegan(vegan);
        profile.setGlutenFree(gluten);
        profile.setDairyFree(dairy);
        profile.setNutFree(nuts);
        profile.setEggFree(egg);
        profile.setPeanutFree(peanuts);
        profile.setShellfishFree(shellfish);
        profile.setSoyFree(soy);
        profile.setFishFree(fish);
        profile.setDiabetes(diabetes);
        profile.setHBP(HBP);
        profile.setKidneyDisease(kidney);
        profile.setIBS(IBS);
        profile.setCeliacDisease(celiac);
        profile.setGoal(goal);

        dietaryProfileRepository.save(profile);

        return "redirect:/dashboard/dashboard";
    }

    // ============================================================
    // ADMIN USERS
    // ============================================================

    @GetMapping("/admin/users")
    public String users(
            HttpSession session,
            Model model) {

        AppUser user = getLoggedInUser(session);

        if (user == null
                || !"admin@gmail.com".equals(user.getEmail())) {

            return "redirect:/";
        }

        model.addAttribute(
                "users",
                appUserRepository.findAll());

        return "admin/users";
    }

    @PostMapping("/admin/users/delete")
    public String deleteUser(
            @RequestParam Long userId) {

        sqleditor.deleteUserById(userId);

        return "redirect:/admin/users";
    }

    @GetMapping("/admin/adminSelection")
    public String adminSelection(
            HttpSession session,
            Model model) {

        AppUser user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/";
        }

        model.addAttribute(
                "name",
                user.getName());

        return "admin/adminSelection";
    }

    // ============================================================
    // ERROR
    // ============================================================

    @GetMapping("/error")
    public String error() {
        return "error";
    }

    // ============================================================
    // DASHBOARD
    // ============================================================

    @GetMapping("/dashboard/dashboard")
    public String dashboard(
            HttpSession session,
            Model model) {

        AppUser user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/";
        }

        model.addAttribute(
                "name",
                user.getName());

        DietaryProfile profile =
                dietaryProfileRepository
                        .findByUser(user)
                        .orElseGet(() ->
                                dietaryProfileRepository.save(
                                        new DietaryProfile(user)));

        List<String> restrictions =
                mealRecommendationService
                        .describeRestrictions(profile);

        List<SavedMeals> existingMeals =
                savedMealsRepository.findByUser(user);

        for (SavedMeals meal : existingMeals) {

            if (meal.getMealNames() == null) {
                sqleditor.deleteNullMeals(
                        meal.getId());
            }
        }

        List<SavedMeals> savedMeals =
                savedMealsRepository.findByUser(user);

        model.addAttribute(
                "savedMeals",
                savedMeals);

        model.addAttribute(
                "safeMeals",
                0);

        model.addAttribute(
                "restrictionsCount",
                restrictions.size());

        model.addAttribute(
                "weeklyCost",
                "TBD");

        model.addAttribute(
                "savedMealsCount",
                savedMeals.size());

        model.addAttribute(
                "restrictions",
                restrictions);

        return "dashboard/dashboard";
    }

    // ============================================================
    // MEALS PAGE
    // ============================================================

    @GetMapping("/dashboard/meals")
    public String meals(
            HttpSession session,
            Model model) {

        AppUser user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/";
        }

        List<SavedMeals> savedMeals =
                savedMealsRepository.findByUser(user);

        model.addAttribute(
                "savedMeals",
                savedMeals);

        model.addAttribute(
                "hasSavedMeals",
                !savedMeals.isEmpty());

        model.addAttribute(
                "showRecommendations",
                false);

        model.addAttribute(
                "hasRecommendations",
                false);

        model.addAttribute(
                "recommendations",
                List.of());

        model.addAttribute(
                "recommendationStatus",
                "");

        return "dashboard/meals";
    }

    // ============================================================
    // GENERATE MEAL RECOMMENDATIONS
    // ============================================================

    @PostMapping("/dashboard/meals/recommend")
    public String recommendMeals(
            HttpSession session,
            Model model) {

        AppUser user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/";
        }

        DietaryProfile profile =
                dietaryProfileRepository
                        .findByUser(user)
                        .orElseGet(() ->
                                dietaryProfileRepository.save(
                                        new DietaryProfile(user)));

        MealRecommendationResult recommendationResult =
                mealRecommendationService
                        .recommendMeals(profile);

        List<MealRecommendation> recommendations =
                recommendationResult.meals();

        List<SavedMeals> savedMeals =
                savedMealsRepository.findByUser(user);

        model.addAttribute(
                "savedMeals",
                savedMeals);

        model.addAttribute(
                "hasSavedMeals",
                !savedMeals.isEmpty());

        model.addAttribute(
                "recommendations",
                recommendations);

        model.addAttribute(
                "hasRecommendations",
                recommendationResult.hasMeals());

        model.addAttribute(
                "recommendationStatus",
                recommendationResult.statusMessage());

        model.addAttribute(
                "showRecommendations",
                true);

        return "dashboard/meals";
    }

    // ============================================================
    // SAVE SELECTED MEALS
    //
    // IMPORTANT:
    // This is intentionally NOT POST /dashboard/meals.
    //
    // GET  /dashboard/meals       -> display page
    // POST /dashboard/meals/save  -> save meals
    // ============================================================

    @PostMapping("/dashboard/meals/save")
    public String saveMeals(
            HttpSession session,

            @RequestParam(
                    name = "mealIndex",
                    required = false)
            List<Integer> mealIndexes,

            @RequestParam(
                    name = "mealName",
                    required = false)
            List<String> mealNames,

            @RequestParam(
                    name = "mealIngredients",
                    required = false)
            List<String> mealIngredients,

            @RequestParam(
                    name = "mealInstructions",
                    required = false)
            List<String> mealInstructions) {

        AppUser user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        /*
         * Nothing selected.
         */
        if (mealIndexes == null
                || mealIndexes.isEmpty()
                || mealNames == null
                || mealNames.isEmpty()) {

            return "redirect:/dashboard/meals";
        }

        for (Integer index : mealIndexes) {

            if (index == null) {
                continue;
            }

            if (index < 0
                    || index >= mealNames.size()) {

                continue;
            }

            String mealName =
                    mealNames.get(index);

            if (mealName == null
                    || mealName.isBlank()) {

                continue;
            }

            List<String> ingredients =
                    new ArrayList<>();

            if (mealIngredients != null
                    && index < mealIngredients.size()) {

                ingredients =
                        splitMealData(
                                mealIngredients.get(index));
            }

            List<String> instructions =
                    new ArrayList<>();

            if (mealInstructions != null
                    && index < mealInstructions.size()) {

                instructions =
                        splitMealData(
                                mealInstructions.get(index));
            }

            SavedMeals savedMeal =
                    new SavedMeals(
                            user,
                            mealName.trim(),
                            ingredients,
                            instructions);

            savedMealsRepository.save(savedMeal);
        }

        /*
         * After saving, go back to the normal GET page.
         */
        return "redirect:/dashboard/meals";
    }

    // ============================================================
    // SPLIT INGREDIENTS / INSTRUCTIONS
    // ============================================================

    private List<String> splitMealData(
            String data) {

        List<String> result =
                new ArrayList<>();

        if (data == null
                || data.isBlank()) {

            return result;
        }

        String[] pieces =
                data.split(
                        "\\|\\|\\|",
                        -1);

        for (String piece : pieces) {

            if (piece != null
                    && !piece.trim().isBlank()) {

                result.add(
                        piece.trim());
            }
        }

        return result;
    }

    // ============================================================
    // DASHBOARD POST
    // ============================================================

    @PostMapping("/dashboard/dashboard")
    public String sendToDashboard(
            HttpSession session) {

        AppUser user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/";
        }

        return "redirect:/dashboard/dashboard";
    }

    // ============================================================
    // PLANNER
    // ============================================================

    @GetMapping("/dashboard/planner")
    public String planner(
            HttpSession session,
            Model model) {

        AppUser user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/";
        }

        return "dashboard/planner";
    }

    // ============================================================
    // GROCERY
    // ============================================================

    @GetMapping("/dashboard/grocery")
    public String grocery(
            HttpSession session,
            Model model) {

        AppUser user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/";
        }

        addGroceryBaseModel(
                user,
                model);

        addGroceryIngredientsModel(
                user,
                model);

        model.addAttribute(
                "groceryStatus",
                "Click update to add missing ingredients from your saved meals.");

        return "dashboard/grocery";
    }

    @PostMapping("/dashboard/grocery/update")
    public String updateGrocery(
            HttpSession session,
            Model model) {

        AppUser user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/";
        }

        List<String> savedMealNames =
                addGroceryBaseModel(
                        user,
                        model);

        GroceryListResult groceryListResult =
                mealRecommendationService
                        .recommendIngredients(
                                savedMealNames);

        int newIngredients =
                saveNewIngredients(
                        user,
                        groceryListResult.ingredients());

        addGroceryIngredientsModel(
                user,
                model);

        model.addAttribute(
                "groceryStatus",
                groceryListResult.hasIngredients()
                        ? groceryUpdateStatus(
                                newIngredients)
                        : groceryListResult.statusMessage());

        return "dashboard/grocery";
    }

    // ============================================================
    // GROCERY HELPERS
    // ============================================================

    private List<String> addGroceryBaseModel(
            AppUser user,
            Model model) {

        List<SavedMeals> savedMeals =
                savedMealsRepository
                        .findByUser(user);

        List<String> savedMealNames =
                new ArrayList<>();

        for (SavedMeals meal : savedMeals) {

            if (meal.getMealNames() != null
                    && !meal.getMealNames()
                            .isBlank()) {

                savedMealNames.add(
                        meal.getMealNames());
            }
        }

        model.addAttribute(
                "savedMeals",
                savedMeals);

        model.addAttribute(
                "hasSavedMeals",
                !savedMealNames.isEmpty());

        return savedMealNames;
    }

    private void addGroceryIngredientsModel(
            AppUser user,
            Model model) {

        List<GroceryIngredient> groceryIngredients =
                groceryIngredientRepository
                        .findByUser(user);

        model.addAttribute(
                "groceryIngredients",
                groceryIngredients);

        model.addAttribute(
                "hasGroceryIngredients",
                !groceryIngredients.isEmpty());
    }

    private int saveNewIngredients(
            AppUser user,
            List<String> ingredients) {

        int newIngredients = 0;

        if (ingredients == null) {
            return 0;
        }

        for (String ingredient : ingredients) {

            String normalizedIngredient =
                    normalizeIngredient(
                            ingredient);

            if (!normalizedIngredient.isBlank()
                    && !groceryIngredientRepository
                            .existsByUserAndNormalizedName(
                                    user,
                                    normalizedIngredient)) {

                groceryIngredientRepository.save(
                        new GroceryIngredient(
                                user,
                                ingredient.trim(),
                                normalizedIngredient));

                newIngredients++;
            }
        }

        return newIngredients;
    }

    private String normalizeIngredient(
            String ingredient) {

        if (ingredient == null) {
            return "";
        }

        return ingredient
                .trim()
                .toLowerCase();
    }

    private String groceryUpdateStatus(
            int newIngredients) {

        if (newIngredients == 0) {
            return "No new ingredients were added.";
        }

        return newIngredients
                + " new ingredient(s) added.";
    }

    // ============================================================
    // AUTHENTICATED USER
    // ============================================================

    private AppUser getLoggedInUser(
            HttpSession session) {

        Object userId =
                session.getAttribute("userId");

        if (!(userId instanceof Long id)) {
            return null;
        }

        return appUserRepository
                .findById(id)
                .orElse(null);
    }
}
