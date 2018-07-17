package me.mvega.foodapp.model;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.util.Date;

@ParseClassName("Recipe")
public class Recipe extends ParseObject {
    private static final String KEY_NAME = "recipeName";
    private static final String KEY_INGREDIENTS = "ingredients";
    private static final String KEY_INSTRUCTIONS = "instructions";
    private static final String KEY_IMAGE = "image";
    private static final String KEY_USER = "user";

    public String getName() {
        return getString(KEY_NAME);
    }
    public void setName(String name) {
        put(KEY_NAME, name);
    }

    public String getIngredients() {
        return getString(KEY_INGREDIENTS);
    }
    public void setIngredients(String ingredients) {
        put(KEY_INGREDIENTS, ingredients);
    }

    public String getInstructions() {
        return getString(KEY_INSTRUCTIONS);
    }
    public void setInstructions(String instructions) {
        put(KEY_INSTRUCTIONS, instructions);
    }

    public ParseFile getImage() {
        return getParseFile(KEY_IMAGE);
    }
    public void setImage(ParseFile image) {
        put(KEY_IMAGE, image);
    }

    public ParseUser getUser() {
        return getParseUser(KEY_USER);
    }
    public void setUser(ParseUser user) {
        put(KEY_USER, user);
    }

    public Date getTimestamp() {
        return getCreatedAt();
    }

    public static class Query extends ParseQuery<Recipe> {
        public Query() {
            super(Recipe.class);
        }

        public Query newestFirst() {
            orderByDescending("createdAt");
            return this;
        }

        public Query getTop() {
            setLimit(20);
            return this;
        }

        public Query withUser() {
            include("user");
            return this;
        }
    }
}
