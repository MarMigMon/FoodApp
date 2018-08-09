package me.mvega.foodapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.ecloud.pulltozoomview.PullToZoomScrollViewEx;
import com.parse.FindCallback;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.mvega.foodapp.model.Notification;
import me.mvega.foodapp.model.Recipe;

import static me.mvega.foodapp.MainActivity.currentUser;

public class RecipeFragment extends Fragment {
    private static ParseUser user = currentUser;
    private RecipeUserCommunication recipeUserListener;
    Recipe recipe;
    private String recipeId;
    private int stepCount = 0;
    private static final String KEY_FAVORITE = "favorites";

    @BindView(R.id.tvName) TextView tvName;
    @BindView(R.id.tvUsername) TextView tvUsername;
    @BindView(R.id.recipeRatingBar) RatingBar recipeRating;
    @BindView(R.id.userRatingBar) RatingBar yourRating;
    @BindView(R.id.userRatingMessage) TextView userRatingMessage;
    @BindView(R.id.tvNumRatings) TextView tvNumRatings;
    @BindView(R.id.tvType) TextView tvType;
    @BindView(R.id.tvDescription) TextView tvDescription;
    @BindView(R.id.tvPrepTime) TextView tvPrepTime;
    @BindView(R.id.tvYield) TextView tvYield;
    @BindView(R.id.tvIngredients) TextView tvIngredients;
    @BindView(R.id.tvInstructions) TextView tvInstructions;
    @BindView(R.id.instructionsLayout) RelativeLayout instructionsLayout;
    @BindView(R.id.ivImage) ImageView ivImage;
    @BindView(R.id.btPlay) ImageButton btPlay;
    @BindView(R.id.btFavorite) ImageButton btFavorite;
    @BindView(R.id.pbLoading) ProgressBar pbLoading;

    //    MarkerUpdatesReceiver markerUpdatesReceiver;

    // implement interface
    public interface RecipeUserCommunication {
        void respond(ParseUser notificationUser);

        void startEdit(Recipe recipe);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof RecipeUserCommunication) {
            recipeUserListener = (RecipeUserCommunication) context;
        } else {
            throw new ClassCastException(context.toString() + " must implement RecipeFragment.RecipeUserCommunication");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("recipe", recipe);
        outState.putParcelable("user", user);
    }

    // The onCreateView method is called when Fragment should create its View object hierarchy either dynamically or via XML layout inflation.
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreateView(inflater, parent, savedInstanceState);

        // Prevents app crashing when switching orientations
        if (savedInstanceState != null) {
            recipe = savedInstanceState.getParcelable("recipe");
            user = savedInstanceState.getParcelable("user");
        }

        // Defines the xml file for the fragment
        View mainView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_recipe, null);

        // Sets up the "PullToZoom" views
        PullToZoomScrollViewEx pullToZoom = mainView.findViewById(R.id.pullToZoomScroll);
        View zoomView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_recipe_image, null);
        View contentView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_recipe_content, null);
        View headView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_recipe_head, null);

        // Adds toolbar to headView if the user owns the recipe
        if (recipe.getUser().getObjectId().equals(ParseUser.getCurrentUser().getObjectId())) {
            TextView btnEditRecipe = contentView.findViewById(R.id.btnEditRecipe);
            btnEditRecipe.setVisibility(View.VISIBLE);
            btnEditRecipe.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    recipeUserListener.startEdit(recipe);
                }
            });
        } else {
            contentView.findViewById(R.id.btnEditRecipe).setVisibility(View.GONE);
        }

        // Adds "PullToZoom" views to the main view
        pullToZoom.setHeaderView(headView);
        pullToZoom.setZoomView(zoomView);
        pullToZoom.setScrollContentView(contentView);

        // Sets zoom display metrics
        DisplayMetrics localDisplayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(localDisplayMetrics);
        int mScreenWidth = localDisplayMetrics.widthPixels;
        LinearLayout.LayoutParams localObject = new LinearLayout.LayoutParams(mScreenWidth, (int) (9.0F * (mScreenWidth / 16.0F)));
        pullToZoom.setHeaderLayoutParams(localObject);

        return mainView;
    }

    // This event is triggered soon after onCreateView().
    // Any view setup should occur here.  E.g., view lookups and attaching view listeners.
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {

        user = ParseUser.getCurrentUser();

        ButterKnife.bind(this, view);

        pbLoading.setVisibility(View.VISIBLE);

        ArrayList<String> steps = (ArrayList<String>) recipe.getSteps();
        recipeId = recipe.getObjectId();

        tvName.setText(recipe.getName());
        tvUsername.setText("@" + recipe.getUser().getUsername());
        tvType.setText(recipe.getType());
        tvDescription.setText(recipe.getDescription());
        tvPrepTime.setText(recipe.getPrepTimeString());
        tvYield.setText(recipe.getYield());
        tvIngredients.setText(TextUtils.join("\n", recipe.getIngredients()));
        setInstructions(steps);

        btPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beginRecipe();
            }
        });

        // Checks whether the user has favorited the recipe
        ArrayList<String> userFavorites = (ArrayList<String>) user.get("favorites");
        if (userFavorites != null) {
            if (userFavorites.contains(recipeId)) {
                // fills in the favorite icon if the user previously favorited the recipe
                btFavorite.setSelected(true);
            }
        }

        btFavorite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Set the button's appearance
                btFavorite.setSelected(!btFavorite.isSelected());

                if (btFavorite.isSelected()) {
                    user.addAll(KEY_FAVORITE, Collections.singletonList(recipe.getObjectId()));
                    user.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null) e.printStackTrace();
                        }
                    });

                    Notification likeNotification = new Notification();
                    likeNotification.setActiveUser(user);
                    likeNotification.setRecipe(recipe);
                    likeNotification.setRecipeUser(recipe.getUser());
                    likeNotification.setFavorite(true);
                    likeNotification.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Log.d("Notification", "created!");
                            } else {
                                e.printStackTrace();
                            }
                        }
                    });
//                    markerUpdatesReceiver = new MarkerUpdatesReceiver(this);
//                    IntentFilter intentFilter = new IntentFilter("com.parse.push.intent.RECEIVE");
//                    registerReceiver(markerUpdatesReceiver, intentFilter);

                } else {
                    user.removeAll(KEY_FAVORITE, Collections.singletonList(recipe.getObjectId()));
                    user.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null) e.printStackTrace();
                        }
                    });

                    ParseQuery<ParseObject> query = ParseQuery.getQuery("Notification");
                    query.whereEqualTo("recipe", recipe);
                    query.whereEqualTo("favorite", true);
                    query.whereEqualTo("activeUser", user);
                    query.findInBackground(new FindCallback<ParseObject>() {
                        @Override
                        public void done(List<ParseObject> notifications, ParseException e) {
                            if (e == null) {
                                for (ParseObject notification : notifications) {
                                    notification.deleteInBackground();
                                }
                            } else {
                                e.printStackTrace();
                            }
                        }
                    });
//                    // avoid memory leaks
//                    if (markerUpdatesReceiver != null) {
//                        unregisterReceiver(markerUpdatesReceiver);
//                    }
                }
            }
        });

        ParseFile image = recipe.getImage();
        if (image != null) {
            String imageUrl = image.getUrl();
            Glide.with(getContext()).load(imageUrl).into(ivImage);
        } else {
            Glide.with(getContext()).load(R.drawable.image_placeholder).into(ivImage);
        }
        pbLoading.setVisibility(View.INVISIBLE);

        float rating = recipe.getRating().floatValue();
        recipeRating.setRating(rating);

        int numRatings = recipe.getNumRatings();
        if (numRatings == 1) {
            tvNumRatings.setText(Integer.toString(numRatings) + " Rating");
        } else {
            tvNumRatings.setText(Integer.toString(numRatings) + " Ratings");
        }


        yourRating.setRating(recipe.getUserRating(user).floatValue());
        HashMap<String, Number> userRatings = (HashMap<String, Number>) recipe.get(Recipe.KEY_USER_RATINGS);
        if (userRatings != null) {
            Number userRating = userRatings.get(user.getObjectId());
            if (userRating != null) {
                if (userRating.doubleValue() == 1) {
                    userRatingMessage.setText("You rated this recipe " + userRating.toString() + " star!");
                } else {
                    userRatingMessage.setText("You rated this recipe " + userRating.toString() + " stars!");
                }
            }
        }

        yourRating.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    showRatingDialog();
                    Notification rateNotification = new Notification();
                    rateNotification.setActiveUser(user);
                    rateNotification.setRecipe(recipe);
                    rateNotification.setRecipeUser(recipe.getUser());
                    rateNotification.setRate(true);
                    rateNotification.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Log.d("Notification", "created!");
                            } else {
                                e.printStackTrace();
                            }
                        }
                    });
                }
                return true;
            }
        });

        tvUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ParseUser notificationUser = recipe.getUser();
                recipeUserListener.respond(notificationUser);
            }
        });
    }

    private void setInstructions(ArrayList<String> steps) {

        while (stepCount < steps.size()) {
            TextView step = new TextView(getContext());

            if (stepCount > 0) {
                // Set layout params
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
                params.addRule(RelativeLayout.BELOW, stepCount);
                step.setLayoutParams(params);
            }

            String stepText = (stepCount + 1) + ". " + steps.get(stepCount);
            step.setText(stepText);

            stepCount += 1;
            step.setId(stepCount);

            // Add step
            instructionsLayout.addView(step);
        }
    }

    private void beginRecipe() {
        Intent i = new Intent(getContext(), SpeechActivity.class);
        i.putExtra("recipe", recipe);
        startActivity(i);
    }

    private void showRatingDialog() {
        // Create builder using dialog layout
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View dialog = getLayoutInflater().inflate(R.layout.dialog_rating, null);
        final RatingBar userRating = dialog.findViewById(R.id.rbDialog);

        // Creates the rating dialog box with the previously input user rating (0 if never rated)
        userRating.setRating(recipe.getUserRating(user).floatValue());

        builder.setView(dialog);

        // Add cancel option and message
        builder.setCancelable(true);
        builder.setMessage(Html.fromHtml("What would you like to rate <b>" + recipe.getName() + "</b>?"));

        // Create alert dialog
        AlertDialog alertDialog = builder.create();

        // Configure dialog button (OK)
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        updateRating(userRating.getRating());
                        dialog.dismiss();
                    }
                });

        // Configure dialog button (CANCEL)
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "CANCEL",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        dialog.dismiss();

                    }
                });

        // Display the dialog
        alertDialog.show();
    }

    private void updateRating(Number rating) {
        // updates user's rating in recipe object
        recipe.setUserRating(user, rating);

        // updates user's rating in user object
        HashMap<String, Number> recipesRated = (HashMap<String, Number>) user.get("recipesRated");
        if (recipesRated == null) {
            recipesRated = new HashMap<>();
        }
        recipesRated.put(recipeId, rating);
        user.put("recipesRated", recipesRated);

        // updates recipe rating
        recipe.updateRating();

        recipe.saveInBackground();
        user.saveInBackground();

        // updates recipe rating on rating bars
        recipeRating.setRating(recipe.getRating().floatValue());
        yourRating.setRating(rating.floatValue());
        int numRatings = recipe.getNumRatings();
        if (numRatings == 1) {
            tvNumRatings.setText(Integer.toString(numRatings) + " Rating");
        } else {
            tvNumRatings.setText(Integer.toString(numRatings) + " Ratings");
        }
        if (rating.doubleValue() == 1) {
            userRatingMessage.setText("You rated this recipe " + rating.toString() + " star!");
        } else {
            userRatingMessage.setText("You rated this recipe " + rating.toString() + " stars!");
        }
    }
}
