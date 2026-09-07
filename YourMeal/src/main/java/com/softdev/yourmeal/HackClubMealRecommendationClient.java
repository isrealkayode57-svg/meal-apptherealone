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

/**
 * Client for the Hack Club AI API.
 */
@Component
public class HackClubMealRecommendationClient {

    private static final String CHAT_COMPLETIONS_PATH = "/proxy/v1/chat/completions";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public HackClubMealRecommendationClient(
            ObjectMapper objectMapper,
            @Value("${hackclub.api-key:}") String apiKey,
            @Value("${hackclub.model:microsoft/phi-4}") String model) {

        this.restClient = RestClient.builder()
                .baseUrl("https://ai.hackclub.com")
                .build();

        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public MealRecommendationResult recommendMeals(
            DietaryProfile profile,
            List<String> restrictions) {

        if (apiKey == null || apiKey.isBlank()) {
            return new MealRecommendationResult(
                    List.of(),
                    "HACKCLUB_API_KEY is missing. Set it before starting the app.");
        }

        try {
            String responseJson = restClient.post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + apiKey)
                    .header(
                            HttpHeaders.ACCEPT,
                            MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(
                            buildChatRequest(
                                    """
                                    You are a meal recommendation assistant for a nutrition app.

                                    Recommend meals that respect every allergy, dietary restriction,
                                    and health condition supplied.

                                    Create fresh meal ideas from the user's profile.

                                    For every meal provide:
                                    - name
                                    - reason
                                    - tags
                                    - ingredients
                                    - recipe

                                    Ingredients must be a list of individual ingredients.

                                    Recipe must be a list of simple cooking steps.

                                    Do not provide medical advice or claim to treat disease.

                                    Keep reasons short, practical, and user-friendly.

                                    Return only meals that are safe for the listed restrictions.

                                    Respond with ONLY a JSON object.
                                    Do not use markdown.
                                    Do not use code fences.

                                    Use exactly this structure:

                                    {
                                      "recommendations": [
                                        {
                                          "name": "Meal name",
                                          "reason": "Short reason",
                                          "tags": "tag1,tag2",
                                          "ingredients": [
                                            "ingredient 1",
                                            "ingredient 2"
                                          ],
                                          "recipe": [
                                            "Step 1",
                                            "Step 2"
                                          ]
                                        }
                                      ]
                                    }
                                    """,
                                    buildProfilePrompt(
                                            profile,
                                            restrictions))))
                    .retrieve()
                    .body(String.class);

            String jsonText = extractMessageContent(
                    objectMapper.readTree(responseJson));

            if (jsonText.isBlank()) {
                return new MealRecommendationResult(
                        List.of(),
                        "Hack Club AI returned no meal text.");
            }

            JsonNode recommendations =
                    parseJsonPayload(jsonText).path("recommendations");

            List<MealRecommendation> meals = new ArrayList<>();

            for (JsonNode meal : recommendations) {

                String name = meal.path("name").asText("");
                String reason = meal.path("reason").asText("");
                String tags = meal.path("tags").asText("");

                List<String> ingredients = new ArrayList<>();

                JsonNode ingredientsNode = meal.path("ingredients");

                if (ingredientsNode.isArray()) {
                    for (JsonNode ingredient : ingredientsNode) {
                        String ingredientText = ingredient.asText("");

                        if (!ingredientText.isBlank()) {
                            ingredients.add(ingredientText);
                        }
                    }
                }

                List<String> recipe = new ArrayList<>();

                JsonNode recipeNode = meal.path("recipe");

                if (recipeNode.isArray()) {
                    for (JsonNode step : recipeNode) {
                        String stepText = step.asText("");

                        if (!stepText.isBlank()) {
                            recipe.add(stepText);
                        }
                    }
                }

                if (!name.isBlank()) {
                    meals.add(
                            new MealRecommendation(
                                    name,
                                    reason,
                                    tags,
                                    ingredients,
                                    recipe));
                }
            }

            if (meals.isEmpty()) {
                return new MealRecommendationResult(
                        List.of(),
                        "Hack Club AI returned a response, but it did not include meal recommendations.");
            }

            return new MealRecommendationResult(meals, "");

        } catch (RestClientResponseException ex) {

            return new MealRecommendationResult(
                    List.of(),
                    "Hack Club AI API error: "
                            + extractApiErrorMessage(
                                    ex.getResponseBodyAsString()));

        } catch (RuntimeException ex) {

            return new MealRecommendationResult(
                    List.of(),
                    "Hack Club AI request failed: "
                            + ex.getMessage());

        } catch (Exception ex) {

            return new MealRecommendationResult(
                    List.of(),
                    "Hack Club AI response could not be parsed: "
                            + ex.getMessage());
        }
    }

    public GroceryListResult recommendIngredients(
            List<String> savedMealNames) {

        if (savedMealNames == null || savedMealNames.isEmpty()) {
            return new GroceryListResult(
                    List.of(),
                    "Save meals before updating your grocery list.");
        }

        if (apiKey == null || apiKey.isBlank()) {
            return new GroceryListResult(
                    List.of(),
                    "HACKCLUB_API_KEY is missing. Set it before starting the app.");
        }

        try {

            String responseJson = restClient.post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "Bearer " + apiKey)
                    .header(
                            HttpHeaders.ACCEPT,
                            MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsString(
                            buildChatRequest(
                                    """
                                    You create grocery lists for a meal planning app.

                                    Infer practical ingredients needed for the saved meals.

                                    Combine duplicate ingredients.

                                    Keep each item short.

                                    Return ingredients only,
                                    not cooking instructions.

                                    Respond with ONLY a JSON object.

                                    Use exactly this structure:

                                    {
                                      "ingredients": [
                                        "item 1",
                                        "item 2"
                                      ]
                                    }
                                    """,
                                    buildIngredientsPrompt(
                                            savedMealNames))))
                    .retrieve()
                    .body(String.class);

            String jsonText = extractMessageContent(
                    objectMapper.readTree(responseJson));

            if (jsonText.isBlank()) {
                return new GroceryListResult(
                        List.of(),
                        "Hack Club AI returned no grocery list text.");
            }

            JsonNode ingredientsNode =
                    parseJsonPayload(jsonText).path("ingredients");

            List<String> ingredients = new ArrayList<>();

            for (JsonNode ingredient : ingredientsNode) {

                String ingredientText =
                        ingredient.asText("");

                if (!ingredientText.isBlank()) {
                    ingredients.add(ingredientText);
                }
            }

            if (ingredients.isEmpty()) {
                return new GroceryListResult(
                        List.of(),
                        "Hack Club AI returned a response, but it did not include ingredients.");
            }

            return new GroceryListResult(
                    ingredients,
                    "");

        } catch (RestClientResponseException ex) {

            return new GroceryListResult(
                    List.of(),
                    "Hack Club AI API error: "
                            + extractApiErrorMessage(
                                    ex.getResponseBodyAsString()));

        } catch (RuntimeException ex) {

            return new GroceryListResult(
                    List.of(),
                    "Hack Club AI request failed: "
                            + ex.getMessage());

        } catch (Exception ex) {

            return new GroceryListResult(
                    List.of(),
                    "Hack Club AI response could not be parsed: "
                            + ex.getMessage());
        }
    }

    private Map<String, Object> buildChatRequest(
            String systemPrompt,
            String userPrompt) {

        return Map.of(
                "model",
                model,

                "messages",
                List.of(
                        Map.of(
                                "role",
                                "system",
                                "content",
                                systemPrompt),

                        Map.of(
                                "role",
                                "user",
                                "content",
                                userPrompt)),

                "temperature",
                0.7);
    }

    private String buildProfilePrompt(
            DietaryProfile profile,
            List<String> restrictions) {

        String goal =
                profile.getGoal() == null
                        || profile.getGoal().isBlank()
                                ? "balancedNutrition"
                                : profile.getGoal();

        String restrictionText =
                restrictions == null
                        || restrictions.isEmpty()
                                ? "No restrictions selected."
                                : String.join(
                                        ", ",
                                        restrictions);

        return """
                Suggest meals for this user profile.

                Goal: %s

                Restrictions and health considerations: %s

                Output 3 or 4 varied meals.

                Every meal must include:
                - name
                - reason
                - tags
                - ingredients
                - recipe

                Ingredients should be individual items.

                Recipe should contain simple step-by-step cooking instructions.
                """
                .formatted(
                        goal,
                        restrictionText);
    }

    private String buildIngredientsPrompt(
            List<String> savedMealNames) {

        return """
                Build a grocery list for these saved meals:

                %s

                Return 8 to 15 common ingredient items.

                Combine duplicate ingredients.
                """
                .formatted(
                        String.join(
                                ", ",
                                savedMealNames));
    }

    /**
     * Pulls the assistant's reply out of an OpenAI-style
     * chat completion response.
     */
    private String extractMessageContent(
            JsonNode response) {

        return response
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText("");
    }

    /**
     * Removes markdown code fences if the AI accidentally
     * returns JSON inside them.
     */
    private JsonNode parseJsonPayload(
            String text) throws Exception {

        String trimmed =
                text.trim();

        if (trimmed.startsWith("```")) {

            int firstNewline =
                    trimmed.indexOf('\n');

            trimmed =
                    firstNewline >= 0
                            ? trimmed.substring(
                                    firstNewline + 1)
                            : "";

            if (trimmed.endsWith("```")) {

                trimmed =
                        trimmed.substring(
                                0,
                                trimmed.length() - 3);
            }

            trimmed =
                    trimmed.trim();
        }

        return objectMapper.readTree(
                trimmed);
    }

    private String extractApiErrorMessage(
            String responseBody) {

        if (responseBody == null
                || responseBody.isBlank()) {

            return "No response body from Hack Club AI.";
        }

        try {

            JsonNode message =
                    objectMapper
                            .readTree(responseBody)
                            .path("error")
                            .path("message");

            if (!message.isMissingNode()
                    && !message.asText().isBlank()) {

                return message.asText();
            }

        } catch (Exception ignored) {
        }

        return responseBody;
    }
}
