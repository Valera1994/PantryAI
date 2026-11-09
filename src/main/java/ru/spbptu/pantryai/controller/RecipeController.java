package ru.spbptu.pantryai.controller;

import ru.spbptu.pantryai.service.RecipeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping("/suggest")
    public Map<String, String> suggestRecipe(
            @RequestParam(required = false) String dishType,
            @RequestParam(required = false) String cuisine,
            @RequestParam(required = false) String preferences) {

        return Map.of(
                "recipe",
                recipeService.suggestRecipe(dishType, cuisine, preferences)
        );
    }
}
