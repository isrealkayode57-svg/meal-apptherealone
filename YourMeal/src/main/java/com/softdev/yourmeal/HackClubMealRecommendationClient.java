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

    private static final String CHAT_COMPLETIONS_PATH =
            "/proxy/v1/chat/completions";

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

    // ============================================================
    // MEAL RECOMMENDATIONS
    // ============================================================

    public MealRecommendationResult recommendMeals(
            DietaryProfile profile,
            List<String> restrictions) {

        if (apiKey == null || apiKey.isBlank()) {

            return new MealRecommendationResult(
                    List.of(),
                    "HACKCLUB_API_KEY is missing. Set it before starting the app.");
        }

        try {

            String systemPrompt = """
                    You are a meal recommendation assistant for a nutrition app.

                    Recommend meals that respect EVERY allergy, dietary restriction,
                    and health consideration supplied by the user.

                    Create fresh meal ideas based on the user's profile.

                    For every meal provide:

                    - name
                    - reason
                    - tags
                    - ingredients
                    - recipe

                    Ingredients MUST be an array of individual ingredients.

                    Recipe MUST be an array of simple step-by-step cooking instructions.

                    Do not provide medical advice.

                    Do not claim that food can treat, cure, or prevent disease.

                    Keep reasons short, practical, and user-friendly.

                    NEVER include an ingredient that violates one of the user's
                    listed restrictions.

                    Respond with ONLY valid JSON.

                    Do NOT use markdown.
                    Do NOT use code fences.
                    Do NOT include any text before or after the JSON.

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
                    """;

            String userPrompt =
                    buildProfilePrompt(
                            profile,
                            restrictions);

            String requestBody =
                    objectMapper.writeValueAsString(
                            buildChatRequest(
                                    systemPrompt,
                                    userPrompt));

            String responseJson =
                    restClient.post()
                            .uri(CHAT_COMPLETIONS_PATH)
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + apiKey)
                            .header(
                                    HttpHeaders.ACCEPT,
                                    MediaType.APPLICATION_JSON_VALUE)
                            .contentType(
                                    MediaType.APPLICATION_JSON)
                            .body(requestBody)
                            .retrieve()
                            .body(String.class);

            if (responseJson == null
                    || responseJson.isBlank()) {

                return new MealRecommendationResult(
                        List.of(),
                        "Hack Club AI returned an empty response.");
            }

            JsonNode response =
                    objectMapper.readTree(responseJson);

            String jsonText =
                    extractMessageContent(response);

            if (jsonText == null
                    || jsonText.isBlank()) {

                return new MealRecommendationResult(
                        List.of(),
                        "Hack Club AI returned no meal text.");
            }

            JsonNode payload =
                    parseJsonPayload(jsonText);

            JsonNode recommendationsNode =
                    payload.path("recommendations");

            if (!recommendationsNode.isArray()) {

                return new MealRecommendationResult(
                        List.of(),
                        "Hack Club AI returned an invalid meal recommendation format.");
            }

            List<MealRecommendation> meals =
                    new ArrayList<>();

            for (JsonNode mealNode : recommendationsNode) {

                String name =
                        mealNode
                                .path("name")
                                .asText("")
                                .trim();

                String reason =
                        mealNode
                                .path("reason")
                                .asText("")
                                .trim();

                String tags =
                        mealNode
                                .path("tags")
                                .asText("")
                                .trim();

                List<String> ingredients =
                        readStringList(
                                mealNode.path("ingredients"));

                List<String> instructions =
                        readStringList(
                                mealNode.path("recipe"));

                /*
                 * Some models may accidentally return "instructions"
                 * instead of "recipe". Support both.
                 */
                if (instructions.isEmpty()) {

                    instructions =
                            readStringList(
                                    mealNode.path("instructions"));
                }

                if (!name.isBlank()) {

                    meals.add(
                            new MealRecommendation(
                                    name,
                                    reason,
                                    tags,
                                    ingredients,
                                    instructions));
                }
            }

            if (meals.isEmpty()) {

                return new MealRecommendationResult(
                        List.of(),
                        "Hack Club AI returned a response, but it did not contain any meals.");
            }

            return new MealRecommendationResult(
                    meals,
                    "");

        } catch (RestClientResponseException ex) {

            return new MealRecommendationResult(
                    List.of(),
                    "Hack Club AI API error: "
                            + extractApiErrorMessage(
                                    ex.getResponseBodyAsString()));

        } catch (Exception ex) {

            return new MealRecommendationResult(
                    List.of(),
                    "Hack Club AI response could not be parsed: "
                            + safeMessage(ex));
        }
    }

    // ============================================================
    // GROCERY LIST
    // ============================================================

    public GroceryListResult recommendIngredients(
            List<String> savedMealNames) {

        if (savedMealNames == null
                || savedMealNames.isEmpty()) {

            return new GroceryListResult(
                    List.of(),
                    "Save meals before updating your grocery list.");
        }

        if (apiKey == null
                || apiKey.isBlank()) {

            return new GroceryListResult(
                    List.of(),
                    "HACKCLUB_API_KEY is missing. Set it before starting the app.");
        }

        try {

            String systemPrompt = """
                    You create grocery lists for a meal planning app.

                    Infer the practical ingredients needed for the saved meals.

                    Combine duplicate ingredients.

                    Keep each grocery item short and practical.

                    Return ingredients ONLY.

                    Do not return cooking instructions.

                    Respond with ONLY valid JSON.

                    Do NOT use markdown.
                    Do NOT use code fences.

                    Use exactly this structure:

                    {
                      "ingredients": [
                        "item 1",
                        "item 2"
                      ]
                    }
                    """;

            String userPrompt =
                    buildIngredientsPrompt(
                            savedMealNames);

            String responseJson =
                    restClient.post()
                            .uri(CHAT_COMPLETIONS_PATH)
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer " + apiKey)
                            .header(
                                    HttpHeaders.ACCEPT,
                                    MediaType.APPLICATION_JSON_VALUE)
                            .contentType(
                                    MediaType.APPLICATION_JSON)
                            .body(
                                    objectMapper.writeValueAsString(
                                            buildChatRequest(
                                                    systemPrompt,
                                                    userPrompt)))
                            .retrieve()
                            .body(String.class);

            if (responseJson == null
                    || responseJson.isBlank()) {

                return new GroceryListResult(
                        List.of(),
                        "Hack Club AI returned an empty grocery response.");
            }

            JsonNode response =
                    objectMapper.readTree(responseJson);

            String jsonText =
                    extractMessageContent(response);

            if (jsonText == null
                    || jsonText.isBlank()) {

                return new GroceryListResult(
                        List.of(),
                        "Hack Club AI returned no grocery list text.");
            }

            JsonNode payload =
                    parseJsonPayload(jsonText);

            List<String> ingredients =
                    readStringList(
                            payload.path("ingredients"));

            if (ingredients.isEmpty()) {

                return new GroceryListResult(
                        List.of(),
                        "Hack Club AI returned a response, but it did not contain ingredients.");
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

        } catch (Exception ex) {

            return new GroceryListResult(
                    List.of(),
                    "Hack Club AI response could not be parsed: "
                            + safeMessage(ex));
        }
    }

    // ============================================================
    // REQUEST BUILDER
    // ============================================================

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

    // ============================================================
    // PROFILE PROMPT
    // ============================================================

    private String buildProfilePrompt(
            DietaryProfile profile,
            List<String> restrictions) {

        String goal =
                profile.getGoal() == null
                        || profile.getGoal().isBlank()
                                ? "balanced nutrition"
                                : profile.getGoal();

        String restrictionText =
                restrictions == null
                        || restrictions.isEmpty()
                                ? "No restrictions selected."
                                : String.join(
                                        ", ",
                                        restrictions);

        return """
                Suggest 4 varied meals for this user.

                Goal:
                %s

                Restrictions and health considerations:
                %s

                Every meal MUST include:

                - name
                - reason
                - tags
                - ingredients
                - recipe

                Ingredients must be individual items.

                Recipe must contain simple step-by-step cooking instructions.

                Make the meals practical for a normal home kitchen.

                Do not repeat the same meal four times.

                Return ONLY the JSON object requested by the system prompt.
                """
                .formatted(
                        goal,
                        restrictionText);
    }

    // ============================================================
    // GROCERY PROMPT
    // ============================================================

    private String buildIngredientsPrompt(
            List<String> savedMealNames) {

        return """
                Build a grocery list for these saved meals:

                %s

                Return 8 to 15 common ingredient items.

                Combine duplicate ingredients.

                Return ONLY the JSON object requested by the system prompt.
                """
                .formatted(
                        String.join(
                                ", ",
                                savedMealNames));
    }

    // ============================================================
    // RESPONSE PARSING
    // ============================================================

    private String extractMessageContent(
            JsonNode response) {

        JsonNode choices =
                response.path("choices");

        if (!choices.isArray()
                || choices.isEmpty()) {

            return "";
        }

        JsonNode content =
                choices
                        .path(0)
                        .path("message")
                        .path("content");

        if (content.isTextual()) {
            return content.asText();
        }

        /*
         * Some API/model combinations can return content
         * as an array of content parts.
         */
        if (content.isArray()) {

            StringBuilder builder =
                    new StringBuilder();

            for (JsonNode part : content) {

                if (part.has("text")) {

                    builder.append(
                            part.path("text")
                                    .asText(""));
                }
            }

            return builder.toString();
        }

        return "";
    }

    private List<String> readStringList(
            JsonNode node) {

        List<String> values =
                new ArrayList<>();

        if (node == null
                || !node.isArray()) {

            return values;
        }

        for (JsonNode item : node) {

            String value =
                    item.asText("")
                            .trim();

            if (!value.isBlank()) {
                values.add(value);
            }
        }

        return values;
    }

    /**
     * Removes markdown code fences and extracts the JSON object
     * if the AI accidentally adds surrounding text.
     */
    private JsonNode parseJsonPayload(
            String text) throws Exception {

        String trimmed =
                text.trim();

        if (trimmed.startsWith("```")) {

            int firstNewline =
                    trimmed.indexOf('\n');

            if (firstNewline >= 0) {

                trimmed =
                        trimmed.substring(
                                firstNewline + 1);
            }

            if (trimmed.endsWith("```")) {

                trimmed =
                        trimmed.substring(
                                0,
                                trimmed.length() - 3);
            }

            trimmed =
                    trimmed.trim();
        }

        /*
         * First try the response exactly as returned.
         */
        try {

            return objectMapper.readTree(
                    trimmed);

        } catch (Exception ignored) {
        }

        /*
         * If the model added text around the JSON,
         * locate the first { and final }.
         */
        int start =
                trimmed.indexOf('{');

        int end =
                trimmed.lastIndexOf('}');

        if (start >= 0
                && end > start) {

            String possibleJson =
                    trimmed.substring(
                            start,
                            end + 1);

            return objectMapper.readTree(
                    possibleJson);
        }

        throw new IllegalArgumentException(
                "No valid JSON object was found in the AI response.");
    }

    // ============================================================
    // API ERROR
    // ============================================================

    private String extractApiErrorMessage(
            String responseBody) {

        if (responseBody == null
                || responseBody.isBlank()) {

            return "No response body from Hack Club AI.";
        }

        try {

            JsonNode root =
                    objectMapper.readTree(
                            responseBody);

            String message =
                    root.path("error")
                            .path("message")
                            .asText("");

            if (!message.isBlank()) {
                return message;
            }

        } catch (Exception ignored) {
        }

        return responseBody;
    }

    // ============================================================
    // SAFE ERROR MESSAGE
    // ============================================================

    private String safeMessage(
            Exception ex) {

        if (ex.getMessage() == null
                || ex.getMessage().isBlank()) {

            return ex.getClass()
                    .getSimpleName();
        }

        return ex.getMessage();
    }
}
