package me.mvega.foodapp;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.parse.CountCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.mvega.foodapp.model.Recipe;

public class ProfileFragment extends Fragment implements YourRecipesFragment.YourRecipesFragmentCommunication {

    ProfileFragmentCommunication yourRecipesListenerFragment;
    ParseUser user = ParseUser.getCurrentUser();
    @BindView(R.id.ivProfile) ImageView ivProfile;
    @BindView(R.id.tvUsername) TextView tvUsername;
    @BindView(R.id.tvContributed) TextView tvContributed;
    @BindView(R.id.tvCompleted) TextView tvCompleted;
    @BindView(R.id.tvReviewed) TextView tvReviewed;
    @BindView(R.id.profileTabs) TabLayout tabLayout;
    @BindView(R.id.tvDescription) TextView tvDescription;

    public interface ProfileFragmentCommunication {
        void respond(Recipe recipe);
    }

    // The onCreateView method is called when Fragment should create its View object hierarchy,
    // either dynamically or via XML layout inflation.
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, parent, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ProfileFragmentCommunication) {
            yourRecipesListenerFragment = (ProfileFragmentCommunication) context;
        }
    }


    // This event is triggered soon after onCreateView().
    // Any view setup should occur here.  E.g., view lookups and attaching view listeners.
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        ButterKnife.bind(this, view);

        setUserName();
        setUserDescription();
        setUserContributions();
        setUserReviewed();

        tvCompleted.setText("0"); // TODO get user's # of completed recipes

        setProfileImage();
        setTabs();
    }

    private void setUserContributions() {
        final Recipe.Query recipeQuery = new Recipe.Query();
        recipeQuery.fromUser(user).withUser().include("recipesRated");
        recipeQuery.countInBackground(new CountCallback() {
            @Override
            public void done(int count, ParseException e) {
                if (e == null) {
                    tvContributed.setText(Integer.toString(count));
                } else {
                    e.printStackTrace();
                    tvContributed.setText("0");
                }
            }
        });
    }

    private void setUserReviewed() {
        HashMap<String, Number> recipesRated = (HashMap<String, Number>) user.get("recipesRated");
        if (recipesRated == null) {
            recipesRated = new HashMap<>();
        }
        tvReviewed.setText(Integer.toString(recipesRated.size()));
    }

    private void setUserName() {
        String userName = (String) user.get("Name");
        tvUsername.setText(userName);
    }

    private void setUserDescription() {
        String userDescription = (String) user.get("description");
        if (userDescription != null) {
            tvDescription.setText(userDescription);
        } else {
            tvDescription.setText("Hello there!");
        }
    }

    private void setProfileImage() {
        ParseFile profileImage = user.getParseFile("image");
        if (profileImage != null) {
            String imageUrl = profileImage.getUrl();
            Glide.with(getContext()).load(imageUrl).apply(RequestOptions.circleCropTransform()).into(ivProfile);
        } else Glide.with(getContext()).load(R.drawable.image_placeholder).apply(RequestOptions.circleCropTransform()).into(ivProfile);
    }

    private void setTabs() {
        showYourRecipes(); // Automatically selects Your Recipes tab to start profile screen

        final TabLayout.Tab yourRecipes = tabLayout.newTab().setText("Your Recipes");
        final TabLayout.Tab favorites = tabLayout.newTab().setText("Favorites");
        tabLayout.addTab(yourRecipes, 0, true);
        tabLayout.addTab(favorites, 1, false);

        // handle tab selection
        tabLayout.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.equals(yourRecipes)) {
                    showYourRecipes();
                } else if (tab.equals(favorites)){
                    showFavorites();
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }
        });
    }

    public void showYourRecipes() {
        replaceFragment(YourRecipesFragment.newInstance());
    }

    public void respond(Recipe recipe) {
        yourRecipesListenerFragment.respond(recipe);
    }

    public void showFavorites() {
        replaceFragment(FavoritesFragment.newInstance());
    }

    public static ProfileFragment newInstance() {
        ProfileFragment fragmentProfile = new ProfileFragment();
        fragmentProfile.setArguments(new Bundle());
        return fragmentProfile;
    }

    public void replaceFragment(Fragment f) {
        final FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.userRecipes, f).commit();
    }
}
