package com.softdev.yourmeal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class GeminiMealRecommendationClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiMealRecommendationClient(
            ObjectMapper objectMapper,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini-3.6-flash}") String model) {
        this.restClient = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com/v1beta").build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public MealRecommendationResult recommendMeals(DietaryProfile profile, List<String> restrictions) {
        if (apiKey == null || apiKey.isBlank()) {
            return new MealRecommendationResult(List.of(), "GEMINI_API_KEY is missing. Set it before starting the app.");
        }

        try {
            String requestJson = objectMapper.writeValueAsString(buildRequest(profile, restrictions));
            String responseJson = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestJson)
                    .retrieve()
                    .body(String.class);

            String jsonText = extractGeneratedText(objectMapper.readTree(responseJson));
            if (jsonText.isBlank()) {
                return new MealRecommendationResult(List.of(), "Gemini returned no meal text.");
            }

            JsonNode recommendations = objectMapper.readTree(jsonText).path("recommendations");
            List<MealRecommendation> meals = new ArrayList<>();
            for (JsonNode meal : recommendations) {
                meals.add(new MealRecommendation(
                        meal.path("name").asText(),
                        meal.path("reason").asText(),
                        meal.path("tags").asText()));
            }

            if (meals.isEmpty()) {
                return new MealRecommendationResult(List.of(), "Gemini returned a response, but it did not include meal recommendations.");
            }

            return new MealRecommendationResult(meals, "");
        } catch (RestClientResponseException ex) {
            return new MealRecommendationResult(List.of(), "Gemini API error: " + extractApiErrorMessage(ex.getResponseBodyAsString()));
        } catch (RuntimeException ex) {
            return new MealRecommendationResult(List.of(), "Gemini request failed: " + ex.getMessage());
        } catch (Exception ex) {
            return new MealRecommendationResult(List.of(), "Gemini response could not be parsed: " + ex.getMessage());
        }
    }

    public GroceryListResult recommendIngredients(List<String> savedMealNames) {
        if (savedMealNames == null || savedMealNames.isEmpty()) {
            return new GroceryListResult(List.of(), "Save meals before updating your grocery list.");
        }

        if (apiKey == null || apiKey.isBlank()) {
            return new GroceryListResult(List.of(), "GEMINI_API_KEY is missing. Set it before starting the app.");
        }

        try {
            String requestJson = objectMapper.writeValueAsString(buildIngredientsRequest(savedMealNames));
            String responseJson = restClient.post()
                    .uri("/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestJson)
                    .retrieve()
                    .body(String.class);

            String jsonText = extractGeneratedText(objectMapper.readTree(responseJson));
            if (jsonText.isBlank()) {
                return new GroceryListResult(List.of(), "Gemini returned no grocery list text.");
            }

            JsonNode ingredientsNode = objectMapper.readTree(jsonText).path("ingredients");
            List<String> ingredients = new ArrayList<>();
            for (JsonNode ingredient : ingredientsNode) {
                String ingredientText = ingredient.asText();
                if (!ingredientText.isBlank()) {
                    ingredients.add(ingredientText);
                }
            }

            if (ingredients.isEmpty()) {
                return new GroceryListResult(List.of(), "Gemini returned a response, but it did not include ingredients.");
            }

            return new GroceryListResult(ingredients, "");
        } catch (RestClientResponseException ex) {
            return new GroceryListResult(List.of(), "Gemini API error: " + extractApiErrorMessage(ex.getResponseBodyAsString()));
        } catch (RuntimeException ex) {
            return new GroceryListResult(List.of(), "Gemini request failed: " + ex.getMessage());
        } catch (Exception ex) {
            return new GroceryListResult(List.of(), "Gemini response could not be parsed: " + ex.getMessage());
        }
    }

    private Map<String, Object> buildRequest(DietaryProfile profile, List<String> restrictions) {
        return Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", """
                        You are a meal recommendation assistant for a nutrition app.
                        Recommend meals that respect every allergy, dietary restriction, and health condition supplied.
                        Create fresh meal ideas from the profile instead of choosing from a fixed list.
                        Do not provide medical advice or claim to treat disease.
                        Keep reasons short, practical, and user-friendly.
                        Return only meals that are safe for the listed restrictions.
                        """))),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", buildProfilePrompt(profile, restrictions))))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseJsonSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "recommendations", Map.of(
                                                "type", "array",
                                                "items", Map.of(
                                                        "type", "object",
                                                        "properties", Map.of(
                                                                "name", Map.of("type", "string"),
                                                                "reason", Map.of("type", "string"),
                                                                "tags", Map.of("type", "string")),
                                                        "required", List.of("name", "reason", "tags")))),
                                "required", List.of("recommendations"))));
    }

    private Map<String, Object> buildIngredientsRequest(List<String> savedMealNames) {
        return Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", """
                        You create grocery lists for a meal planning app.
                        Infer practical ingredients needed for the saved meals.
                        Combine duplicate ingredients and keep each item short.
                        Return ingredients only, not cooking instructions.
                        """))),
                "contents", List.of(Map.of("parts", List.of(Map.of("text", buildIngredientsPrompt(savedMealNames))))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseJsonSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "ingredients", Map.of(
                                                "type", "array",
                                                "items", Map.of("type", "string"))),
                                "required", List.of("ingredients"))));
    }

    private String buildProfilePrompt(DietaryProfile profile, List<String> restrictions) {
        String goal = profile.getGoal() == null || profile.getGoal().isBlank()
                ? "balancedNutrition"
                : profile.getGoal();
        String restrictionText = restrictions.isEmpty()
                ? "No restrictions selected."
                : String.join(", ", restrictions);

        return """
                Suggest meals for this user profile.
                Goal: %s
                Restrictions and health considerations: %s
                Output 3 or 4 varied meals. Tags should be comma-separated short labels.
                """.formatted(goal, restrictionText);
    }

    private String buildIngredientsPrompt(List<String> savedMealNames) {
        return """
                Build a grocery list for these saved meals:
                %s
                Return 8 to 15 common ingredient items.
                """.formatted(String.join(", ", savedMealNames));
    }

    private String extractGeneratedText(JsonNode response) {
        JsonNode candidates = response.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return "";
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            return "";
        }

        return parts.get(0).path("text").asText("");
    }

    private String extractApiErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "No response body from Gemini.";
        }

        try {
            JsonNode message = objectMapper.readTree(responseBody).path("error").path("message");
            if (!message.isMissingNode() && !message.asText().isBlank()) {
                return message.asText();
            }
        } catch (Exception ignored) {
        }

        return responseBody;
    }
}
