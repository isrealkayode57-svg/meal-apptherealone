package com.softdev.yourmeal;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SQLeditor sqleditor;

    public MainController(AppUserRepository appUserRepository, DietaryProfileRepository dietaryProfileRepository, MealRecommendationService mealRecommendationService, SQLeditor sqleditor, SavedMealsRepository savedMealsRepository, GroceryIngredientRepository groceryIngredientRepository) {
        this.appUserRepository = appUserRepository;
        this.dietaryProfileRepository = dietaryProfileRepository;
        this.mealRecommendationService = mealRecommendationService;
        this.sqleditor = sqleditor;
        this.savedMealsRepository = savedMealsRepository;
        this.groceryIngredientRepository = groceryIngredientRepository;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
 

    @PostMapping("/login")
    public String loginUser(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {
        return appUserRepository.findByEmail(normalizeEmail(email))
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                .map(user -> {
                    session.setAttribute("userId", user.getId());
                    if (email.equals("admin@gmail.com")){ return "redirect:admin/adminSelection"; }
                    return "redirect:/dashboard/dashboard";
                })
                .orElseGet(() -> {
                    model.addAttribute("loginError", "Email or password is incorrect.");
                    return "login";
                });
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirm_password,
            HttpSession session,
            Model model) {
        String normalizedEmail = normalizeEmail(email);

        if (appUserRepository.findByEmail(normalizedEmail).isPresent()) {
            model.addAttribute("registerError", "An account with that email already exists.");
            return "register";
        }

        if (!password.equals(confirm_password)){
            model.addAttribute("registerError", "Passwords do not match.");
            return "register";
        }

        AppUser user = appUserRepository.save(new AppUser(
                normalizedEmail,
                passwordEncoder.encode(password), name));

        dietaryProfileRepository.save(new DietaryProfile(user));
        session.setAttribute("userId", user.getId());

        if(email.equals("admin@gmail.com")){ return "redirect:admin/adminSelection"; }

        return "redirect:/selection";
    }

    @GetMapping("/selection")
    public String selection(HttpSession session, Model model) {
        AppUser user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        DietaryProfile profile = dietaryProfileRepository.findByUser(user)
                .orElseGet(() -> dietaryProfileRepository.save(new DietaryProfile(user)));

        model.addAttribute("name", user.getName());
        model.addAttribute("profile", profile);
        return "selection";
    }

    @GetMapping("/admin/users")
    public String users(HttpSession session, Model model) {
        model.addAttribute("users", appUserRepository.findAll());
        AppUser user = getLoggedInUser(session);

        if (user == null || !user.getEmail().equals("admin@gmail.com")){
            return "redirect:/";
        }
        return "admin/users";
    }

    @GetMapping("/admin/adminSelection")
    public String adminSelection(HttpSession session, Model model){
        AppUser user = getLoggedInUser(session);
        model.addAttribute("name", user.getName());
        return "admin/adminSelection";
    }

    @PostMapping("/selection")
    public String saveDietaryProfile(
            @RequestParam(defaultValue = "false") boolean vegetarian,
            @RequestParam(defaultValue = "false") boolean vegan,
            @RequestParam(defaultValue = "false") boolean dairy,
            @RequestParam(defaultValue = "false") boolean egg,
            @RequestParam(defaultValue = "false") boolean gluten,
            @RequestParam(defaultValue = "false") boolean peanuts,
            @RequestParam(defaultValue = "false") boolean shellfish,
            @RequestParam(defaultValue = "false") boolean soy,
            @RequestParam(defaultValue = "false") boolean nuts,
            @RequestParam(defaultValue = "false") boolean fish,
            @RequestParam(defaultValue = "false") boolean diabetes,
            @RequestParam(defaultValue = "false") boolean HBP,
            @RequestParam(defaultValue = "false") boolean kidney,
            @RequestParam(defaultValue = "false") boolean IBS,
            @RequestParam(defaultValue = "false") boolean celiac,
            @RequestParam(defaultValue = "") String goal,
//            @RequestParam(defaultValue = "") String notes,
            HttpSession session) {
        AppUser user = getLoggedInUser(session);

        if (user == null) {
            return "redirect:/login";
        }

        DietaryProfile profile = dietaryProfileRepository.findByUser(user)
                .orElseGet(() -> new DietaryProfile(user));

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
//        profile.setNotes(notes);
        dietaryProfileRepository.save(profile);

        return "redirect:/dashboard/dashboard";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private AppUser getLoggedInUser(HttpSession session) {
        Object userId = session.getAttribute("userId");

        if (!(userId instanceof Long id)) {
            return null;
        }

        return appUserRepository.findById(id).orElse(null);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    @PostMapping("/admin/users/delete")
    public String deleteUser(@RequestParam Long userId) {
        sqleditor.deleteUserById(userId);
        return "redirect:/admin/users";
    }

    @GetMapping("/error")
    public String error() {
        return "error";
    }

    @GetMapping("/dashboard/dashboard")
    public String dashboard(HttpSession session, Model model){
        AppUser user = getLoggedInUser(session);

        if (user == null){
            return "redirect:/";
        }

        model.addAttribute("name", user.getName());
        DietaryProfile profile = dietaryProfileRepository.findByUser(user)
                .orElseGet(() -> dietaryProfileRepository.save(new DietaryProfile(user)));
        List<String> restrictions = mealRecommendationService.describeRestrictions(profile);

        for(SavedMeals meal: savedMealsRepository.findByUser(user)){
            if (meal.getMealNames() == null){
                sqleditor.deleteNullMeals(meal.getId());
            }
        }

        List<SavedMeals> savedMeals = savedMealsRepository.findByUser(user);
        model.addAttribute("savedMeals", savedMeals);

        model.addAttribute("safeMeals", 0);
        model.addAttribute("restrictionsCount", restrictions.size());
        model.addAttribute("weeklyCost", "TBD");
        model.addAttribute("savedMealsCount", savedMeals.size());
        model.addAttribute("restrictions", restrictions);

        return "dashboard/dashboard";
    }


    @GetMapping("/dashboard/meals")
    public String meals(HttpSession session, Model model){
        AppUser user = getLoggedInUser(session);

        if (user == null){
            return "redirect:/";
        }

        List<SavedMeals> savedMeals = savedMealsRepository.findByUser(user);
        model.addAttribute("savedMeals", savedMeals);
        model.addAttribute("hasSavedMeals", !savedMeals.isEmpty());
        model.addAttribute("showRecommendations", false);

        return "dashboard/meals";
    }

    @PostMapping("/dashboard/meals/recommend")
    public String recommendMeals(HttpSession session, Model model){
        AppUser user = getLoggedInUser(session);

        if (user == null){
            return "redirect:/";
        }

        DietaryProfile profile = dietaryProfileRepository.findByUser(user)
                .orElseGet(() -> dietaryProfileRepository.save(new DietaryProfile(user)));
        MealRecommendationResult recommendationResult = mealRecommendationService.recommendMeals(profile);
        List<MealRecommendation> recommendations = recommendationResult.meals();
        List<SavedMeals> savedMeals = savedMealsRepository.findByUser(user);

        model.addAttribute("savedMeals", savedMeals);
        model.addAttribute("hasSavedMeals", !savedMeals.isEmpty());
        model.addAttribute("recommendations", recommendations);
        model.addAttribute("hasRecommendations", recommendationResult.hasMeals());
        model.addAttribute("recommendationStatus", recommendationResult.statusMessage());
        model.addAttribute("showRecommendations", true);

        return "dashboard/meals";
    }

    @PostMapping("/dashboard/meals")
    public String saveMeals(HttpSession session, @RequestParam(name = "meal", required = false) List<String> meals){
        AppUser user = getLoggedInUser(session);

        if (user == null){
            return"redirect:/login";
        }

        if (meals != null) {
            for (String meal : meals) {
                savedMealsRepository.save(new SavedMeals(user, meal));
            }
        }

        return "redirect:/dashboard/meals";
    }

    @PostMapping("/dashboard/dashboard")
    public String sendToDashboard(HttpSession session){
        AppUser user = getLoggedInUser(session);
        if (user == null){
            return "redirect:/";
        }

        return "dashboard/dashboard";
    }
    @GetMapping("/dashboard/planner")
    public String planner(HttpSession session, Model model){

        AppUser user = getLoggedInUser(session);

        if(user == null){
            return "redirect:/";
        }


        return "dashboard/planner";
    }

    @GetMapping("/dashboard/grocery")
    public String grocery(HttpSession session, Model model){
        AppUser user = getLoggedInUser(session);

        if(user == null){
            return "redirect:/";
        }

        addGroceryBaseModel(user, model);
        addGroceryIngredientsModel(user, model);
        model.addAttribute("groceryStatus", "Click update to add missing ingredients from your saved meals.");

        return "dashboard/grocery";
    }

    @PostMapping("/dashboard/grocery/update")
    public String updateGrocery(HttpSession session, Model model){
        AppUser user = getLoggedInUser(session);

        if(user == null){
            return "redirect:/";
        }

        List<String> savedMealNames = addGroceryBaseModel(user, model);
        GroceryListResult groceryListResult = mealRecommendationService.recommendIngredients(savedMealNames);
        int newIngredients = saveNewIngredients(user, groceryListResult.ingredients());

        addGroceryIngredientsModel(user, model);
        model.addAttribute("groceryStatus", groceryListResult.hasIngredients()
                ? groceryUpdateStatus(newIngredients)
                : groceryListResult.statusMessage());

        return "dashboard/grocery";
    }

    private List<String> addGroceryBaseModel(AppUser user, Model model) {
        List<SavedMeals> savedMeals = savedMealsRepository.findByUser(user);
        List<String> savedMealNames = new ArrayList<>();

        for (SavedMeals meal : savedMeals) {
            if (meal.getMealNames() != null && !meal.getMealNames().isBlank()) {
                savedMealNames.add(meal.getMealNames());
            }
        }

        model.addAttribute("savedMeals", savedMeals);
        model.addAttribute("hasSavedMeals", !savedMealNames.isEmpty());
        return savedMealNames;
    }

    private void addGroceryIngredientsModel(AppUser user, Model model) {
        List<GroceryIngredient> groceryIngredients = groceryIngredientRepository.findByUser(user);
        model.addAttribute("groceryIngredients", groceryIngredients);
        model.addAttribute("hasGroceryIngredients", !groceryIngredients.isEmpty());
    }

    private int saveNewIngredients(AppUser user, List<String> ingredients) {
        int newIngredients = 0;

        for (String ingredient : ingredients) {
            String normalizedIngredient = normalizeIngredient(ingredient);

            if (!normalizedIngredient.isBlank()
                    && !groceryIngredientRepository.existsByUserAndNormalizedName(user, normalizedIngredient)) {
                groceryIngredientRepository.save(new GroceryIngredient(user, ingredient.trim(), normalizedIngredient));
                newIngredients++;
            }
        }

        return newIngredients;
    }

    private String normalizeIngredient(String ingredient) {
        if (ingredient == null) {
            return "";
        }

        return ingredient.trim().toLowerCase();
    }

    private String groceryUpdateStatus(int newIngredients) {
        if (newIngredients == 0) {
            return "No new ingredients were added.";
        }

        return newIngredients + " new ingredient(s) added.";
    }

}
