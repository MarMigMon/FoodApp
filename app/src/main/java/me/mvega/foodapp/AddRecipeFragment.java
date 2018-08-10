package me.mvega.foodapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.media.ExifInterface;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.SaveCallback;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.mvega.foodapp.model.Recipe;

import static me.mvega.foodapp.MainActivity.currentUser;

public class AddRecipeFragment extends Fragment implements AddRecipePageOne.PageOneFragmentCommunication, AddRecipePageTwo.PageTwoFragmentCommunication {

//    // listener
//    private NewRecipeCommunication newRecipeListener;

    // Global Views
    @BindView(R.id.scrollView)
    ScrollView scrollView;
    @BindView(R.id.pbLoading)
    ProgressBar pbLoading;
    @BindView(R.id.vpCreation)
    DynamicViewPager vpCreation;

    // Pager Fragments
    AddRecipePageOne pageOne;
    Bundle pageOneBundle = new Bundle();
    AddRecipePageTwo pageTwo;
    Bundle pageTwoBundle = new Bundle();

    private static final int MAX_SIZE = 720;
    private ParseFile imageFile;

    // Submitting edited recipe
    private Recipe editedRecipe;
    private Context context;
    private MainActivity mainActivity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        mainActivity = (MainActivity) context;
    }

    private static final String KEY_RECIPE = "recipe";
    private static final String KEY_EDITING = "editing";
    private static final String KEY_NAME = "name";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_YIELD = "yield";
    private static final String KEY_PREP_TIME = "prepTime";
    private static final String KEY_PREP_TIME_TEXT = "prepTimeText";
    private static final String KEY_TYPE_TEXT = "typeText";
    private static final String KEY_STEPS = "steps";
    private static final String KEY_INGREDIENTS = "ingredients";
    // True if a recipe is being edited
    private Boolean editing = false;

    // Recipe fields
    private String name;
    private String description;
    private Number yield;
    private Number prepTime;
    private String prepTimeText;
    private String typeText;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreateView(inflater, parent, savedInstanceState);
        return inflater.inflate(R.layout.fragment_add_recipe, parent, false);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_EDITING, editing);
        outState.putParcelable(KEY_RECIPE, editedRecipe);
        super.onSaveInstanceState(outState);
    }

    public static AddRecipeFragment newInstance(Recipe recipe, Boolean editing) {
        Bundle args = new Bundle();
        args.putParcelable(KEY_RECIPE, recipe);
        args.putBoolean(KEY_EDITING, editing);
        AddRecipeFragment fragment = new AddRecipeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    // This event is triggered soon after onCreateView().
    // Any view setup should occur here.  E.g., view lookups and attaching view listeners.
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        ButterKnife.bind(this, view);

        if (savedInstanceState != null) {
            editing = savedInstanceState.getBoolean(KEY_EDITING, false);
            editedRecipe = savedInstanceState.getParcelable(KEY_RECIPE);
        } else {
            if (getArguments() != null) {
                editing = getArguments().getBoolean(KEY_EDITING);
                editedRecipe = getArguments().getParcelable(KEY_RECIPE);
            }
        }

        vpCreation.setAdapter(new RecipePagerAdapter(getChildFragmentManager()));

        pageOneBundle.putBoolean(KEY_EDITING, editing);
        pageOneBundle.putParcelable(KEY_RECIPE, editedRecipe);
        pageOne = AddRecipePageOne.newInstance(pageOneBundle);

        pageTwoBundle.putBoolean(KEY_EDITING, editing);
        pageTwoBundle.putParcelable(KEY_RECIPE, editedRecipe);
        pageTwo = AddRecipePageTwo.newInstance(pageTwoBundle);

        vpCreation.setCurrentItem(0);
    }

    @Override
    public void next(Bundle bundle) {
        name = bundle.getString(KEY_NAME);
        description = bundle.getString(KEY_DESCRIPTION);
        yield = bundle.getInt(KEY_YIELD);
        prepTime = bundle.getInt(KEY_PREP_TIME);
        prepTimeText = bundle.getString(KEY_PREP_TIME_TEXT);
        typeText = bundle.getString(KEY_TYPE_TEXT);
        vpCreation.setCurrentItem(1);
    }

    @Override
    public void back(Bundle bundle) {
        vpCreation.setCurrentItem(0);
    }

    /**
     * Scrolls scrollview to maintain position of button
     */
    public void scrollDownTextField(boolean reverse, int distance) {
        if (reverse) {
            scrollView.scrollBy(0, -distance);
        } else {
            scrollView.scrollBy(0, distance);
        }
    }

    @Override
    public Bitmap getImage(String imagePath) {
        return setSelectedPhoto(new File(imagePath));
    }

    @Override
    public void submit(Bundle bundle) throws IllegalArgumentException {
        final Recipe recipe;
        final Bundle args = getArguments();
        Recipe oldRecipe = null;
        if (args != null) {
            oldRecipe = getArguments().getParcelable(KEY_RECIPE);
        }
        final boolean newRecipe;

        // checks if this submission is an edit or a new recipe
        if (oldRecipe == null) {
            recipe = new Recipe();
            newRecipe = true;
        } else {
            recipe = oldRecipe;
            newRecipe = false;
        }

        setFields(bundle, recipe);
        pbLoading.setVisibility(ProgressBar.VISIBLE);

        if (newRecipe) {
            // Recipe user and rating are automatically filled in Parse
            recipe.setUser(currentUser);
            recipe.setRating(0);
            recipe.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        Toast.makeText(context, "Recipe successfully created!", Toast.LENGTH_LONG).show();
                        pbLoading.setVisibility(ProgressBar.INVISIBLE);
                        mainActivity.bottomNavigationView.setSelectedItemId(R.id.tab_feed);
                        FragmentManager fm = mainActivity.getSupportFragmentManager();
                        FragmentTransaction ft = fm.beginTransaction();
                        ft.replace(R.id.frameLayout, new FeedFragment());
                        ft.commit();
                        // Resets the pages for the next recipe
                        vpCreation.setCurrentItem(0);


                    } else {
                        Toast.makeText(context, "Recipe creation failed!", Toast.LENGTH_LONG).show();
                        pbLoading.setVisibility(ProgressBar.INVISIBLE);
                        e.printStackTrace();
                    }
                }
            });
        } else {
            recipe.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        Toast.makeText(context, "Recipe successfully edited!", Toast.LENGTH_LONG).show();
                        pbLoading.setVisibility(ProgressBar.INVISIBLE);
                        editing = false;
                        mainActivity.bottomNavigationView.setSelectedItemId(R.id.tab_feed);
                        FragmentManager fm = mainActivity.getSupportFragmentManager();
                        FragmentTransaction ft = fm.beginTransaction();
                        ft.replace(R.id.frameLayout, new FeedFragment());

                        ft.commit();
                        // Resets the pages for the next recipe
                        vpCreation.setCurrentItem(0);


                    } else {
                        Toast.makeText(context, "Recipe edit failed!", Toast.LENGTH_LONG).show();
                        pbLoading.setVisibility(ProgressBar.INVISIBLE);
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void setFields(Bundle bundle, Recipe recipe) {
        ArrayList<String> steps = bundle.getStringArrayList(KEY_STEPS);
        ArrayList<String> ingredients = bundle.getStringArrayList(KEY_INGREDIENTS);

        // Checks to ensure every required field is filled out
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Please enter a name for your recipe.");
        } else {
            recipe.setName(name);
        }
        if (description.isEmpty()) {
            throw new IllegalArgumentException("Please enter a description for your recipe.");
        } else {
            recipe.setDescription(description);
        }
        try {
            Number yieldNumber = yield;
            if (yieldNumber.doubleValue() == 1) {
                String yield = String.valueOf(yieldNumber) + " serving";
                recipe.setYield(yield);
            } else {
                String yield = String.valueOf(yieldNumber) + " servings";
                recipe.setYield(yield);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter a number of servings for your recipe.");
        }
        try {
            recipe.setPrepTime(prepTime);

            if (prepTimeText.equals("hours")) {
                recipe.setPrepTimeMinutes((int) prepTime * 60);
            } else {
                recipe.setPrepTime(prepTime);
            }

            // Makes prep time period plural or singular based on prep time value
            if (prepTime.doubleValue() == 1) {
                recipe.setPrepTimePeriod(prepTimeText.substring(0, prepTimeText.length() - 1));
            } else {
                recipe.setPrepTimePeriod(prepTimeText);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter an amount of time for your recipe.");
        }

        if (typeText.isEmpty()) {
            throw new IllegalArgumentException("Please select a type from the type drop-down.");
        } else {
            recipe.setType(typeText);
        }
        if (ingredients.isEmpty()) {
            throw new IllegalArgumentException("Please enter some ingredients for your recipe.");
        } else {
            recipe.setIngredients(ingredients);
        }
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Please enter some instructions for your recipe.");
        } else {
            recipe.setSteps(steps);
        }
        if (imageFile != null) {
            recipe.setImage(imageFile);
        }
    }

    private Bitmap setSelectedPhoto(File file) {
        Bitmap rawTakenImage = null;

        // Tries to appropriately rotates image
        try {
            rawTakenImage = decodeFile(file);

            android.support.media.ExifInterface ei = new android.support.media.ExifInterface(file.getAbsolutePath());
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rawTakenImage = rotateImage(rawTakenImage, 90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rawTakenImage = rotateImage(rawTakenImage, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    rawTakenImage = rotateImage(rawTakenImage, 270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    break;
            }
            return getResizedBitmap(rawTakenImage);
        } catch (IOException e) {
            e.printStackTrace();
            return getResizedBitmap(rawTakenImage);
        }
    }

    private Bitmap getResizedBitmap(Bitmap image) {
        int width = image.getWidth();
        if (width > MAX_SIZE) {
            int height = image.getHeight();
            float bitmapRatio = (float) width / (float) height;
            width = MAX_SIZE;
            height = (int) (MAX_SIZE / bitmapRatio);
            image = Bitmap.createScaledBitmap(image, width, height, true);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 80, out);
        byte[] data = out.toByteArray();
        Bitmap b = BitmapFactory.decodeStream(new ByteArrayInputStream(data));
        imageFile = new ParseFile("recipeImage", data, "image/jpeg");
        imageFile.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
        return b;
    }

    private Bitmap decodeFile(File f) throws IOException {
        Bitmap b;

        //Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;

        FileInputStream fis = new FileInputStream(f);
        BitmapFactory.decodeStream(fis, null, o);
        fis.close();

        int scale = 1;
        if (o.outHeight > MAX_SIZE || o.outWidth > MAX_SIZE) {
            scale = (int) Math.pow(2, (int) Math.ceil(Math.log(MAX_SIZE /
                    (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
        }

        //Decode with inSampleSize
        o.inSampleSize = scale;
        o.inJustDecodeBounds = false;
        o.inPreferredConfig = Bitmap.Config.RGB_565;
        fis = new FileInputStream(f);
        b = BitmapFactory.decodeStream(fis, null, o);
        fis.close();

        return b;
    }

    private static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    public class RecipePagerAdapter extends FragmentPagerAdapter {
        private int NUM_ITEMS = 2;
        private int mCurrentPosition = -1;

        public RecipePagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return pageOne;
                case 1:
                    return pageTwo;
                default:
                    return null;
            }
        }

        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            if (position != mCurrentPosition) {
                Fragment fragment = (Fragment) object;
                DynamicViewPager pager = (DynamicViewPager) container;
                if (fragment != null && fragment.getView() != null) {
                    mCurrentPosition = position;
                    pager.measureCurrentView(fragment.getView());
                }
            }
        }
    }
}