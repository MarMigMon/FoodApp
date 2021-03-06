package me.mvega.foodapp;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.mvega.foodapp.model.Recipe;

import static android.support.constraint.Constraints.TAG;

public class RecipesCompletedFragment extends Fragment {

    private ProfileRecipesAdapter profileRecipesAdapter;
    private YourRecipesFragment.YourRecipesFragmentCommunication profileListenerFragment;

    @BindView(R.id.rvRecipes)
    RecyclerView rvRecipes;
    @BindView(R.id.swipeContainer)
    SwipeRefreshLayout swipeContainer;
    @BindView(R.id.pbLoading)
    ProgressBar pbLoading;
    ParseUser user;
    @BindView(R.id.ivPlaceholder)
    ImageView ivPlaceholder;
    @BindView(R.id.tvNoRecipes)
    TextView tvNoRecipes;
    Context context;

    // The onCreateView method is called when Fragment should create its View object hierarchy,
    // either dynamically or via XML layout inflation.
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        super.onCreateView(inflater, parent, savedInstanceState);

        if (savedInstanceState != null) {
            user = savedInstanceState.getParcelable("user");
        }

        onAttachToParentFragment(getParentFragment());
        // Defines the xml file for the fragment
        return inflater.inflate(R.layout.tab_profile, parent, false);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("user", user);
    }

    private void onAttachToParentFragment(Fragment childFragment) {
        try {
            profileListenerFragment = (YourRecipesFragment.YourRecipesFragmentCommunication) childFragment;

        } catch (ClassCastException e) {
            throw new ClassCastException(
                    childFragment.toString() + " must implement OnPlayerSelectionSetListener");
        }
    }

    // This event is triggered soon after onCreateView().
    // Any view setup should occur here.  E.g., view lookups and attaching view listeners.
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        ButterKnife.bind(this, view);
        ivPlaceholder.setVisibility(View.INVISIBLE);
        tvNoRecipes.setVisibility(View.INVISIBLE);
        pbLoading.setVisibility(ProgressBar.VISIBLE);

        // find the Recycler View
        RecyclerView rvRecipes = view.findViewById(R.id.rvRecipes);
        // initialize the ArrayList (data source)
        ArrayList<Recipe> recipes = new ArrayList<>();
        // construct the adapter from this data source
        profileRecipesAdapter = new ProfileRecipesAdapter(recipes);
        // RecyclerView setup (layout manager, use adapter)
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        rvRecipes.setLayoutManager(layoutManager);
        //set the adapter
        rvRecipes.setAdapter(profileRecipesAdapter);
        rvRecipes.addItemDecoration(new SpacesItemDecoration(32));

        profileRecipesAdapter.setProfileListener(new ProfileRecipesAdapter.ProfileAdapterCommunication() {
            @Override
            public void respond(Recipe recipe) {
                profileListenerFragment.respond(recipe);
            }

            @Override
            public void showDeleteDialog(Recipe recipe) {

            }
        });

        loadCompleted();

        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.detach(RecipesCompletedFragment.this).attach(RecipesCompletedFragment.this).commit();
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                loadCompleted();
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }

    private void loadCompleted() {
        ArrayList<String> userCompleted = (ArrayList<String>) user.get("recipesCompleted");
        if (userCompleted == null) {
            userCompleted = new ArrayList<>();
        }
        final List<ParseQuery<Recipe>> queries = new ArrayList<>();
        Recipe.Query completedQuery = new Recipe.Query();

        for (int i = 0; i < userCompleted.size(); i++) {
            final Recipe.Query recipeQuery = new Recipe.Query();
            recipeQuery.is(userCompleted.get(i));
            queries.add(recipeQuery);
        }

        if (!queries.isEmpty()) {
            completedQuery.or(queries).include("user.username").findInBackground(new FindCallback<Recipe>() {
                @Override
                public void done(List<Recipe> newRecipes, ParseException e) {
                    if (e == null) {
                        // Remember to CLEAR OUT old items before appending in the new ones
                        profileRecipesAdapter.clear();
                        // ...the data has come back, add new items to your adapter...
                        profileRecipesAdapter.addAll(newRecipes);
                        // Now we call setRefreshing(false) to signal refresh has finished
                        swipeContainer.setRefreshing(false);
                        pbLoading.setVisibility(ProgressBar.INVISIBLE);
                    } else {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            pbLoading.setVisibility(ProgressBar.GONE);
            rvRecipes.setVisibility(View.GONE);

            ivPlaceholder.setVisibility(View.VISIBLE);
            tvNoRecipes.setVisibility(View.VISIBLE);
            // Remember to CLEAR OUT old items before appending in the new ones
            profileRecipesAdapter.clear();
            // Now we call setRefreshing(false) to signal refresh has finished
            swipeContainer.setRefreshing(false);
        }
    }
}

