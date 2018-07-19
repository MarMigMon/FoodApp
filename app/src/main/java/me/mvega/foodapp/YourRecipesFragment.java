package me.mvega.foodapp;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import me.mvega.foodapp.model.Recipe;

public class YourRecipesFragment extends Fragment {

    ProfileRecipesAdapter profileRecipesAdapter;
    ArrayList<Recipe> recipes;
    RecyclerView rvRecipes;
    private SwipeRefreshLayout swipeContainer;
    FragmentCommunication profileListenerFragment;

    // implement interface
    public interface FragmentCommunication {
        void respond(Recipe recipe);
    }

    // The onCreateView method is called when Fragment should create its View object hierarchy,
    // either dynamically or via XML layout inflation.
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        // Defines the xml file for the fragment
        return inflater.inflate(R.layout.fragment_feed, parent, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FragmentCommunication) {
            profileListenerFragment = (FragmentCommunication) context;}
//        } else {
//            throw new ClassCastException(context.toString() + " must implement FeedFragment.FragmentCommunication");
//        }
    }

    // This event is triggered soon after onCreateView().
    // Any view setup should occur here.  E.g., view lookups and attaching view listeners.
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        // Setup any handles to view objects here
        // EditText etFoo = (EditText) view.findViewById(R.id.etFoo);

        // find the Recycler View
        rvRecipes = view.findViewById(R.id.rvRecipes);
        // initialize the ArrayList (data source)
        recipes = new ArrayList<>();
        // construct the adapter from this data source
        profileRecipesAdapter = new ProfileRecipesAdapter(recipes);
        // RecyclerView setup (layout manager, use adapter)
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        rvRecipes.setLayoutManager(layoutManager);
        //set the adapter
        rvRecipes.setAdapter(profileRecipesAdapter);

        profileRecipesAdapter.setProfileListener(new RecipeAdapter.AdapterCommunication() {
            @Override
            public void respond(Recipe recipe) {
                profileListenerFragment.respond(recipe);
            }
        });

        loadYourRecipes();

        // Lookup the swipe container view
        swipeContainer = view.findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                loadYourRecipes();
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }

    public static YourRecipesFragment newInstance() {
        YourRecipesFragment fragmentYourRecipes = new YourRecipesFragment();
        fragmentYourRecipes.setArguments(new Bundle());
        return fragmentYourRecipes;
    }

    private void loadYourRecipes() {
        final Recipe.Query recipeQuery = new Recipe.Query();
        recipeQuery.fromUser(ParseUser.getCurrentUser()); // TODO update Query to reflect what the "Your Recipes" Fragment should be loading

        recipeQuery.findInBackground(new FindCallback<Recipe>() {
            @Override
            public void done(List<Recipe> newRecipes, ParseException e) {
                if (e == null) {
                    // Remember to CLEAR OUT old items before appending in the new ones
                    profileRecipesAdapter.clear();
                    // ...the data has come back, add new items to your adapter...
                    profileRecipesAdapter.addAll(newRecipes);
                    // Now we call setRefreshing(false) to signal refresh has finished
                    swipeContainer.setRefreshing(false);

                } else {
                    e.printStackTrace();
                }
            }
        });
    }
}

