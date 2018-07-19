package me.mvega.foodapp;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.parse.FindCallback;
import com.parse.ParseException;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.mvega.foodapp.model.Recipe;

public class FeedFragment extends Fragment {

    RecipeAdapter recipeAdapter;
    ArrayList<Recipe> recipes;
    @BindView(R.id.rvRecipes) RecyclerView rvRecipes;
    @BindView(R.id.swipeContainer) SwipeRefreshLayout swipeContainer;
    FragmentCommunication listenerFragment;

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
            listenerFragment = (FragmentCommunication) context;
        } else {
            throw new ClassCastException(context.toString() + " must implement FeedFragment.FragmentCommunication");
        }
    }

    // This event is triggered soon after onCreateView().
    // Any view setup should occur here.  E.g., view lookups and attaching view listeners.
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        ButterKnife.bind(this, view);
        // initialize the ArrayList (data source)
        recipes = new ArrayList<>();
        // construct the adapter from this data source
        recipeAdapter = new RecipeAdapter(recipes);
        // RecyclerView setup (layout manager, use adapter)
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        rvRecipes.setLayoutManager(linearLayoutManager);
        //set the adapter
        rvRecipes.setAdapter(recipeAdapter);

        recipeAdapter.setListener(new RecipeAdapter.AdapterCommunication() {
            @Override
            public void respond(Recipe recipe) {
                listenerFragment.respond(recipe);
            }
        });

        RecyclerView.ItemDecoration itemDecoration = new
                DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        rvRecipes.addItemDecoration(itemDecoration);

        loadTopRecipes();
        setSwipeContainer();


    }

    public void setSwipeContainer() {
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                loadTopRecipes();
            }
        });
        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }

    public static FeedFragment newInstance() {
        FeedFragment fragmentFeed = new FeedFragment();
        fragmentFeed.setArguments(new Bundle());
        return fragmentFeed;
    }

    private void loadTopRecipes() {
        final Recipe.Query recipeQuery = new Recipe.Query();
        recipeQuery.getTop().withUser().newestFirst();

        recipeQuery.findInBackground(new FindCallback<Recipe>() {
            @Override
            public void done(List<Recipe> newRecipes, ParseException e) {
                if (e == null) {
                    // Remember to CLEAR OUT old items before appending in the new ones
                    recipeAdapter.clear();
                    // ...the data has come back, add new items to your adapter...
                    recipeAdapter.addAll(newRecipes);
                    // Now we call setRefreshing(false) to signal refresh has finished
                    swipeContainer.setRefreshing(false);

                } else {
                    e.printStackTrace();
                }
            }
        });
    }


}
