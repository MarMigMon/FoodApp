package me.mvega.foodapp.model;

import android.widget.CheckBox;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@ParseClassName("Recipe")
public class Recipe extends ParseObject {
    public static final String KEY_NAME = "recipeName";
    public static final String KEY_TYPE = "type";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_INGREDIENTS = "ingredients";
    private static final String KEY_INSTRUCTIONS = "instructions";
    private static final String KEY_YIELD = "yield";
    private static final String KEY_IMAGE = "image";
    private static final String KEY_USER = "user";
    public static final String KEY_RATING = "rating";
    public static final String KEY_PREP_TIME = "prepTime";
    private static final String KEY_MEDIA = "media";
    private static final String KEY_USERS_WHO_FAVORITED = "usersWhoFavorited";
    private static final String KEY_STEPS = "steps";
    private static final String KEY_OBJECT_ID = "objectId";
    private static final String KEY_VIEWS = "views";
    private static final String KEY_USER_RATINGS = "userRatings";
    public static int lowestRating;

    public List<String> getSteps() {
        return getList(KEY_STEPS);
    }
    public void setSteps(ArrayList<String> steps) {
        put(KEY_STEPS, steps);
    }

    public ParseFile getMedia() {
        return getParseFile(KEY_MEDIA);
    }
    public void setMedia(ParseFile media) {
        put(KEY_MEDIA, media);
    }

    public String getName() {
        return getString(KEY_NAME);
    }
    public void setName(String name) {
        put(KEY_NAME, name);
    }

    public String getType() {
        return getString(KEY_TYPE);
    }
    public void setType(String type) {
        put(KEY_TYPE, type);
    }

    public String getDescription() {
        return getString(KEY_DESCRIPTION);
    }
    public void setDescription(String description) {
        put(KEY_DESCRIPTION, description);
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

    public String getYield() {
        return getString(KEY_YIELD);
    }
    public void setYield(String yield) {
        put(KEY_YIELD, yield);
    }

    public String getPrepTime() {
        return getString(KEY_PREP_TIME);
    }
    public void setPrepTime(String prepTime) {
        put(KEY_PREP_TIME, prepTime);
    }

    public ParseFile getImage() {
        return getParseFile(KEY_IMAGE);
    }
    public void setImage(ParseFile image) {
        put(KEY_IMAGE, image);
    }

    public Number getRating() {
        return getNumber(KEY_RATING);
    }
    public void updateRating() {
        HashMap<String, Number> userRatings = (HashMap<String, Number>) get(KEY_USER_RATINGS);
        if (userRatings != null) {
            Number recipeRating = 0f;
            for (Number userRating : userRatings.values()) {
                recipeRating = recipeRating.doubleValue() + userRating.doubleValue();
            }
            recipeRating = recipeRating.doubleValue() / userRatings.size();
            put(KEY_RATING, recipeRating);
        }
    }

    public Number getUserRating(ParseUser user) {
        HashMap<String, Number> userRatings = (HashMap<String, Number>) get(KEY_USER_RATINGS);
        if (userRatings == null) {
            userRatings = new HashMap<>();
        }
        Number rating = userRatings.get(user.getObjectId());
        return (rating == null) ? 0.0 : rating;
    }
    public void setUserRating(ParseUser user, Number rating) {
        HashMap<String, Number> userRatings = (HashMap<String, Number>) get(KEY_USER_RATINGS);
        if (userRatings == null) {
            userRatings = new HashMap<>();
        }
        userRatings.put(user.getObjectId(), rating);
        put(KEY_USER_RATINGS, userRatings);
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

    public Integer getViews() {
        return getInt(KEY_VIEWS);
    }
    public void setViews(Integer views) {
        put(KEY_VIEWS, views);
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

        public Query containsQuery(String key, String query) {
            whereFullText(KEY_NAME, query);
            return this;
        }

        public Query fromUser(ParseUser user) {
            whereEqualTo(KEY_USER, user);
            return this;
        }

        public Query is(String objectId) {
            whereEqualTo(KEY_OBJECT_ID, objectId);
            return this;
        }

        public Query runOrQueries(List<ParseQuery<Recipe>> queries) {
            or(queries);
            return this;
        }

        public void findLowestRating(CheckBox[] checkBoxes) {
            Boolean checked = false;
            ArrayList<Integer> numbers = new ArrayList<>();
            for (CheckBox item: checkBoxes) {
                if (item.isChecked()) {
                    int value = Integer.valueOf(item.getText().toString().substring(0, 1));
                    numbers.add(value);
                    checked = true;
                }
            }
            if (checked) {
                lowestRating = Collections.min(numbers);
            }
        }

        public ArrayList<ParseQuery<Recipe>> addTypeQueries(CheckBox[] checkBoxes) {
            Boolean checked = false;
            ArrayList<ParseQuery<Recipe>> queries = new ArrayList<>();

            for (CheckBox item: checkBoxes) {
                if (item.isChecked()) {
                    ParseQuery query = new ParseQuery("Recipe");
                    query.whereGreaterThanOrEqualTo(KEY_RATING, lowestRating).whereEqualTo(KEY_TYPE, item.getText().toString());
                    queries.add(query);
                    checked = true;
                }
            }

            if (!checked) {
                ParseQuery query = new ParseQuery("Recipe");
                query.whereGreaterThanOrEqualTo(KEY_RATING, lowestRating);
                queries.add(query);
            }

            return queries;
        }

        public ParseQuery<Recipe> addMaxPrepTime(String maxPrepTimeEntered) {
            ParseQuery maxPrepTimeQuery = new ParseQuery("Recipe");
            maxPrepTimeQuery.whereEqualTo(Recipe.KEY_PREP_TIME, maxPrepTimeEntered);
            return maxPrepTimeQuery;
        }

    }
}
