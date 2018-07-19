package me.mvega.foodapp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.mvega.foodapp.model.Recipe;

public class RecipeFragment extends Fragment {
    Recipe recipe;
    @BindView(R.id.tvName) TextView tvName;
    @BindView(R.id.ratingBar) RatingBar ratingBar;
    @BindView(R.id.tvType) TextView tvType;
    @BindView(R.id.tvDescription) TextView tvDescription;
    @BindView(R.id.tvPrepTime) TextView tvPrepTime;
    @BindView(R.id.tvYield) TextView tvYield;
    @BindView(R.id.tvIngredients) TextView tvIngredients;
    @BindView(R.id.tvInstructions) TextView tvInstructions;

    // The onCreateView method is called when Fragment should create its View object hierarchy either dynamically or via XML layout inflation.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Defines the xml file for the fragment
        View view = inflater.inflate(R.layout.fragment_recipe, parent, false);
        return view;
    }

    // This event is triggered soon after onCreateView().
    // Any view setup should occur here.  E.g., view lookups and attaching view listeners.
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ButterKnife.bind(this, view);

        tvName.setText(recipe.getName());
        tvType.setText(recipe.getType());
        tvDescription.setText(recipe.getDescription());
        tvPrepTime.setText(recipe.getPrepTime());
        tvYield.setText(recipe.getYield());
        tvIngredients.setText(recipe.getIngredients());
        tvInstructions.setText(recipe.getInstructions());

    }
}
