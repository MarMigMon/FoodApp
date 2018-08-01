package me.mvega.foodapp;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

public class SpeechCardAdapter extends FragmentPagerAdapter {
    ArrayList<String> steps;
    ArrayList<String> components;

    public SpeechCardAdapter(FragmentManager fragmentManager, ArrayList<String> steps, ArrayList<String> components) {
        super(fragmentManager);
        this.steps = steps;
        this.components = components;
    }

    // Returns total number of pages
    @Override
    public int getCount() {
        return steps.size();
    }

    // Returns the fragment to display for that page
    @Override
    public Fragment getItem(int step) {
        String currStep = steps.get(step);
        if (step > 0) {
            String ingredients = matchesIngredient(currStep);
            return SpeechCardFragmentPlain.newInstance(currStep, step, ingredients);
        } else {
            return SpeechCardFragment.newInstance(currStep);
        }
    }

    // Returns the page title for the top indicator
    @Override
    public CharSequence getPageTitle(int position) {
        return "Page " + position;
    }

    private String matchesIngredient(String currStep) {
        String[] stepSplit = currStep.replaceAll("[,./]", "").split(" ");
        String matchingIngredients = "";
        String[] filter = {"and", "or", "from", "tbsp", "TB", "cup", "into", "a", "to", "c", "the"};

        for (String component : components) {
            String[] componentSplit = component.replaceAll("[/,.0-9]", "").split(" ");
            for (String word : stepSplit) {
                Log.d("Filter", "Testing " + word);
                if (Arrays.asList(componentSplit).contains(word) && !matchingIngredients.contains(component) && !Arrays.asList(filter).contains(word)) {
                    Log.d("Filter", word);
                    matchingIngredients = matchingIngredients + component + "\n";
                }
            }
        }
        return matchingIngredients;
    }

}
