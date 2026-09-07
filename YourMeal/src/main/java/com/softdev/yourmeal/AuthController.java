
package com.softdev.yourmeal;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final AppUserRepository appUserRepository;

    public AuthController(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    // -------------------------
    // LOGIN PAGE
    // -------------------------

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // -------------------------
    // LOGIN
    // -------------------------

    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        AppUser user = appUserRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            model.addAttribute("loginError", "Invalid email or password.");
            return "login";
        }

        if (!user.getPasswordHash().equals(password)) {
            model.addAttribute("loginError", "Invalid email or password.");
            return "login";
        }

        // Save the logged-in user in the session
        session.setAttribute("userId", user.getId());

        return "redirect:/dashboard/dashboard";
    }

    // -------------------------
    // REGISTER PAGE
    // -------------------------

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    // -------------------------
    // REGISTER
    // -------------------------

    @PostMapping("/register")
    public String register(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam("confirm_password") String confirmPassword,
            Model model) {

        // Check passwords
        if (!password.equals(confirmPassword)) {
            model.addAttribute(
                    "registerError",
                    "Passwords do not match."
            );

            return "register";
        }

        // Check if email already exists
        if (appUserRepository.findByEmail(email).isPresent()) {
            model.addAttribute(
                    "registerError",
                    "An account with that email already exists."
            );

            return "register";
        }

        // Create and save user
        AppUser user = new AppUser(
                email,
                password,
                name
        );

        appUserRepository.save(user);

        // Send them to login after registering
        return "redirect:/login";
    }

    // -------------------------
    // LOGOUT
    // -------------------------

    @PostMapping("/logout")
    public String logout(HttpSession session) {

        session.invalidate();

        return "redirect:/login";
    }
}

