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

    /*
     * Gemini model name.
     */
    private static final String MODEL = "gemini-3.6-flash";

    public GeminiMealRecommendationClient(
            ObjectMapper objectMapper,
            @Value("${GEMINI_API_KEY:}") String apiKey
    ) {

        this.restClient = RestClient.builder()
                .baseUrl(
                        "https://generativelanguage.googleapis.com/v1beta"
                )
                .build();

        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }


    /*
     * ============================================================
     * MEAL RECOMMENDATIONS
     * ============================================================
     */

    public MealRecommendationResult recommendMeals(
            DietaryProfile profile,
            List<String> restrictions
    ) {

        if (apiKey == null || apiKey.isBlank()) {

            return new MealRecommendationResult(
                    List.of(),
                    "GEMINI_API_KEY is missing. Set it before starting the app."
            );
        }

        try {

            String requestJson =
                    objectMapper.writeValueAsString(
                            buildRequest(
                                    profile,
                                    restrictions
                            )
                    );

            String responseJson =
                    restClient.post()
                            .uri(
                                    "/models/{model}:generateContent",
                                    MODEL
                            )
                            .header(
                                    "x-goog-api-key",
                                    apiKey
                            )
                            .header(
                                    HttpHeaders.ACCEPT,
                                    MediaType.APPLICATION_JSON_VALUE
                            )
                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )
                            .body(requestJson)
                            .retrieve()
                            .body(String.class);


            String jsonText =
                    extractGeneratedText(
                            objectMapper.readTree(responseJson)
                    );


            if (jsonText.isBlank()) {

                return new MealRecommendationResult(
                        List.of(),
                        "Gemini returned no meal text."
                );
            }


            JsonNode recommendations =
                    objectMapper
                            .readTree(jsonText)
                            .path("recommendations");


            List<MealRecommendation> meals =
                    new ArrayList<>();


            if (!recommendations.isArray()) {

                return new MealRecommendationResult(
                        List.of(),
                        "Gemini did not return a valid recommendations list."
                );
            }


            for (JsonNode meal : recommendations) {

                String name =
                        meal.path("name")
                                .asText("");

                String reason =
                        meal.path("reason")
                                .asText("");

                String tags =
                        meal.path("tags")
                                .asText("");


                /*
                 * INGREDIENTS
                 */

                List<String> ingredients =
                        new ArrayList<>();

                JsonNode ingredientsNode =
                        meal.path("ingredients");


                if (ingredientsNode.isArray()) {

                    for (JsonNode ingredient :
                            ingredientsNode) {

                        String ingredientText =
                                ingredient.asText("");

                        if (!ingredientText.isBlank()) {

                            ingredients.add(
                                    ingredientText
                            );
                        }
                    }
                }


                /*
                 * INSTRUCTIONS
                 */

                List<String> instructions =
                        new ArrayList<>();

                JsonNode instructionsNode =
                        meal.path("instructions");


                if (instructionsNode.isArray()) {

                    for (JsonNode instruction :
                            instructionsNode) {

                        String instructionText =
                                instruction.asText("");

                        if (!instructionText.isBlank()) {

                            instructions.add(
                                    instructionText
                            );
                        }
                    }
                }


                /*
                 * Only add valid meals.
                 */

                if (!name.isBlank()) {

                    meals.add(
                            new MealRecommendation(
                                    name,
                                    reason,
                                    tags,
                                    ingredients,
                                    instructions
                            )
                    );
                }
            }


            if (meals.isEmpty()) {

                return new MealRecommendationResult(
                        List.of(),
                        "Gemini returned a response, but it did not include meal recommendations."
                );
            }


            return new MealRecommendationResult(
                    meals,
                    ""
            );


        } catch (RestClientResponseException ex) {

            return new MealRecommendationResult(
                    List.of(),
                    "Gemini API error: "
                            + extractApiErrorMessage(
                                    ex.getResponseBodyAsString()
                            )
            );


        } catch (RuntimeException ex) {

            return new MealRecommendationResult(
                    List.of(),
                    "Gemini request failed: "
                            + ex.getMessage()
            );


        } catch (Exception ex) {

            return new MealRecommendationResult(
                    List.of(),
                    "Gemini response could not be parsed: "
                            + ex.getMessage()
            );
        }
    }


    /*
     * ============================================================
     * GROCERY INGREDIENT RECOMMENDATIONS
     * ============================================================
     */

    public GroceryListResult recommendIngredients(
            List<String> savedMealNames
    ) {

        if (savedMealNames == null
                || savedMealNames.isEmpty()) {

            return new GroceryListResult(
                    List.of(),
                    "Save meals before updating your grocery list."
            );
        }


        if (apiKey == null || apiKey.isBlank()) {

            return new GroceryListResult(
                    List.of(),
                    "GEMINI_API_KEY is missing. Set it before starting the app."
            );
        }


        try {

            String requestJson =
                    objectMapper.writeValueAsString(
                            buildIngredientsRequest(
                                    savedMealNames
                            )
                    );


            String responseJson =
                    restClient.post()
                            .uri(
                                    "/models/{model}:generateContent",
                                    MODEL
                            )
                            .header(
                                    "x-goog-api-key",
                                    apiKey
                            )
                            .header(
                                    HttpHeaders.ACCEPT,
                                    MediaType.APPLICATION_JSON_VALUE
                            )
                            .contentType(
                                    MediaType.APPLICATION_JSON
                            )
                            .body(requestJson)
                            .retrieve()
                            .body(String.class);


            String jsonText =
                    extractGeneratedText(
                            objectMapper.readTree(responseJson)
                    );


            if (jsonText.isBlank()) {

                return new GroceryListResult(
                        List.of(),
                        "Gemini returned no grocery list text."
                );
            }


            JsonNode ingredientsNode =
                    objectMapper
                            .readTree(jsonText)
                            .path("ingredients");


            List<String> ingredients =
                    new ArrayList<>();


            if (ingredientsNode.isArray()) {

                for (JsonNode ingredient :
                        ingredientsNode) {

                    String ingredientText =
                            ingredient.asText("");

                    if (!ingredientText.isBlank()) {

                        ingredients.add(
                                ingredientText
                        );
                    }
                }
            }


            if (ingredients.isEmpty()) {

                return new GroceryListResult(
                        List.of(),
                        "Gemini returned a response, but it did not include ingredients."
                );
            }


            return new GroceryListResult(
                    ingredients,
                    ""
            );


        } catch (RestClientResponseException ex) {

            return new GroceryListResult(
                    List.of(),
                    "Gemini API error: "
                            + extractApiErrorMessage(
                                    ex.getResponseBodyAsString()
                            )
            );


        } catch (RuntimeException ex) {

            return new GroceryListResult(
                    List.of(),
                    "Gemini request failed: "
                            + ex.getMessage()
            );


        } catch (Exception ex) {

            return new GroceryListResult(
                    List.of(),
                    "Gemini response could not be parsed: "
                            + ex.getMessage()
            );
        }
    }


    /*
     * ============================================================
     * GEMINI MEAL REQUEST
     * ============================================================
     */

    private Map<String, Object> buildRequest(
            DietaryProfile profile,
            List<String> restrictions
    ) {

        return Map.of(

                "systemInstruction",

                Map.of(
                        "parts",
                        List.of(
                                Map.of(
                                        "text",
                                        """
                                        You are a meal recommendation assistant for a nutrition app.

                                        Recommend meals that respect EVERY allergy,
                                        dietary restriction, and health condition supplied
                                        by the user.

                                        Create fresh meal ideas instead of choosing from
                                        a fixed list.

                                        For every meal provide:

                                        1. The meal name.
                                        2. A short reason why it fits the user's profile.
                                        3. Short comma-separated tags.
                                        4. A complete ingredient list with useful quantities.
                                        5. Clear step-by-step cooking instructions.

                                        Recipes should be practical for a normal home kitchen.

                                        Do not provide medical advice.

                                        Do not claim that food treats or cures diseases.

                                        If the user has allergies or dietary restrictions,
                                        make sure the recipe does not contain those foods.

                                        Return ONLY JSON matching the requested schema.
                                        """
                                )
                        )
                ),


                "contents",

                List.of(
                        Map.of(
                                "parts",
                                List.of(
                                        Map.of(
                                                "text",
                                                buildProfilePrompt(
                                                        profile,
                                                        restrictions
                                                )
                                        )
                                )
                        )
                ),


                "generationConfig",

                Map.of(

                        "responseMimeType",
                        "application/json",

                        "responseJsonSchema",

                        Map.of(

                                "type",
                                "object",

                                "properties",

                                Map.of(

                                        "recommendations",

                                        Map.of(

                                                "type",
                                                "array",

                                                "items",

                                                Map.of(

                                                        "type",
                                                        "object",

                                                        "properties",

                                                        Map.of(

                                                                "name",
                                                                Map.of(
                                                                        "type",
                                                                        "string"
                                                                ),

                                                                "reason",
                                                                Map.of(
                                                                        "type",
                                                                        "string"
                                                                ),

                                                                "tags",
                                                                Map.of(
                                                                        "type",
                                                                        "string"
                                                                ),

                                                                "ingredients",
                                                                Map.of(
                                                                        "type",
                                                                        "array",

                                                                        "items",
                                                                        Map.of(
                                                                                "type",
                                                                                "string"
                                                                        )
                                                                ),

                                                                "instructions",
                                                                Map.of(
                                                                        "type",
                                                                        "array",

                                                                        "items",
                                                                        Map.of(
                                                                                "type",
                                                                                "string"
                                                                        )
                                                                )
                                                        ),

                                                        "required",

                                                        List.of(
                                                                "name",
                                                                "reason",
                                                                "tags",
                                                                "ingredients",
                                                                "instructions"
                                                        )
                                                )
                                        )
                                ),

                                "required",
                                List.of(
                                        "recommendations"
                                )
                        )
                )
        );
    }


    /*
     * ============================================================
     * INGREDIENT REQUEST
     * ============================================================
     */

    private Map<String, Object> buildIngredientsRequest(
            List<String> savedMealNames
    ) {

        return Map.of(

                "systemInstruction",

                Map.of(
                        "parts",
                        List.of(
                                Map.of(
                                        "text",
                                        """
                                        You create grocery lists for a meal planning app.

                                        Infer practical ingredients needed for the saved meals.

                                        Combine duplicate ingredients.

                                        Keep each item short.

                                        Return ingredients only.
                                        Do not return cooking instructions.

                                        Return ONLY JSON matching the requested schema.
                                        """
                                )
                        )
                ),


                "contents",

                List.of(
                        Map.of(
                                "parts",
                                List.of(
                                        Map.of(
                                                "text",
                                                buildIngredientsPrompt(
                                                        savedMealNames
                                                )
                                        )
                                )
                        )
                ),


                "generationConfig",

                Map.of(

                        "responseMimeType",
                        "application/json",

                        "responseJsonSchema",

                        Map.of(

                                "type",
                                "object",

                                "properties",

                                Map.of(

                                        "ingredients",

                                        Map.of(

                                                "type",
                                                "array",

                                                "items",
                                                Map.of(
                                                        "type",
                                                        "string"
                                                )
                                        )
                                ),

                                "required",
                                List.of(
                                        "ingredients"
                                )
                        )
                )
        );
    }


    /*
     * ============================================================
     * PROFILE PROMPT
     * ============================================================
     */

    private String buildProfilePrompt(
            DietaryProfile profile,
            List<String> restrictions
    ) {

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
                                restrictions
                        );


        return """
                Suggest meals for this user profile.

                Goal:
                %s

                Restrictions and health considerations:
                %s

                Output 3 or 4 varied meals.

                For EACH meal provide:

                - meal name
                - short reason
                - comma-separated tags
                - ingredients with quantities
                - step-by-step cooking instructions

                Make the recipes realistic and practical.
                """.formatted(
                        goal,
                        restrictionText
                );
    }


    /*
     * ============================================================
     * GROCERY PROMPT
     * ============================================================
     */

    private String buildIngredientsPrompt(
            List<String> savedMealNames
    ) {

        return """
                Build a grocery list for these saved meals:

                %s

                Return 8 to 15 common ingredient items.
                """.formatted(
                        String.join(
                                ", ",
                                savedMealNames
                        )
                );
    }


    /*
     * ============================================================
     * EXTRACT GEMINI TEXT
     * ============================================================
     */

    private String extractGeneratedText(
            JsonNode response
    ) {

        JsonNode candidates =
                response.path("candidates");


        if (!candidates.isArray()
                || candidates.isEmpty()) {

            return "";
        }


        JsonNode parts =
                candidates
                        .get(0)
                        .path("content")
                        .path("parts");


        if (!parts.isArray()
                || parts.isEmpty()) {

            return "";
        }


        return parts
                .get(0)
                .path("text")
                .asText("");
    }


    /*
     * ============================================================
     * GEMINI ERROR MESSAGE
     * ============================================================
     */

    private String extractApiErrorMessage(
            String responseBody
    ) {

        if (responseBody == null
                || responseBody.isBlank()) {

            return "No response body from Gemini.";
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
