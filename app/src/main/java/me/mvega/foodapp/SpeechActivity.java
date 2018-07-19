package me.mvega.foodapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseFile;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;
import me.mvega.foodapp.model.Recipe;

public class SpeechActivity extends AppCompatActivity implements
        RecognitionListener {

    private static final String MENU_SEARCH = "say start or stop";

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private Recipe recipe;
    private ParseFile audioFile;
    private SpeechRecognizer recognizer;
    private MediaPlayer player;
    private Boolean isPaused;

    @BindView(R.id.btStart) Button btStart;
    @BindView(R.id.btStop) Button btStop;
    @BindView(R.id.tvName) TextView tvName;
    @BindView(R.id.tvInstructions) TextView tvInstructions;
    @BindView(R.id.tvIngredients) TextView tvIngredients;
    @BindView(R.id.pbLoading) ProgressBar pbLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speech);

        ButterKnife.bind(this);

        // Check if user has given permission to record audio
        checkPermissions();

        new SetupTask(this).execute();

        player = new MediaPlayer();

        // Set views
        recipe = getIntent().getParcelableExtra("recipe");
        audioFile = recipe.getMedia();
        tvName.setText(recipe.getName());
        tvIngredients.setText(recipe.getIngredients());
        tvInstructions.setText(recipe.getInstructions());

        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beginRecipe();
            }
        });

        btStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishRecipe();
            }
        });

    }

    private void beginRecipe() {
        // If audio file exists, start player and speech recognition
        if (audioFile != null) {
            toggleVisibility(btStop);
            toggleVisibility(btStart);
            pbLoading.setVisibility(ProgressBar.VISIBLE);
            startPlayer();

            Toast.makeText(SpeechActivity.this, "Listening for start or stop", Toast.LENGTH_SHORT).show();
            startRecognition(MENU_SEARCH);
        } else {
            Toast.makeText(SpeechActivity.this, "Audio file not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void finishRecipe() {
        // Stop speech recognition and player and reset to start button
        recognizer.stop();
        stopPlayer();
        toggleVisibility(btStop);
        toggleVisibility(btStart);
    }

    public void toggleVisibility(View view) {
        view.setVisibility((view.getVisibility() == View.VISIBLE)
                ? View.INVISIBLE
                : View.VISIBLE);
    }

    private void stopPlayer() {
        if (player.isPlaying()) {
            try {
                player.reset();
                player.prepare();
                player.stop();
                player.release();
                player=null;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private void startPlayer() {
        try {
            String audioFileURL = audioFile.getUrl();
            player.setDataSource(audioFileURL);
            player.prepare();
            player.start();
            pbLoading.setVisibility(ProgressBar.INVISIBLE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkPermissions() {
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
    }

    private static class SetupTask extends AsyncTask<Void, Void, Exception> {
        WeakReference<SpeechActivity> activityReference;
        SetupTask(SpeechActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }
        @Override
        protected Exception doInBackground(Void... params) {
            try {
                Assets assets = new Assets(activityReference.get());
                File assetDir = assets.syncAssets();
                activityReference.get().setupRecognizer(assetDir);
            } catch (IOException e) {
                return e;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Exception result) {
            if (result != null) {
                Toast.makeText(activityReference.get(),"Failed to init recognizer " + result, Toast.LENGTH_LONG);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                new SetupTask(this).execute();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
    }

    /**
     * After end of speech, stop recognizer and get a final result
     */
    @Override
    public void onEndOfSpeech() {
        Log.d("Speech recognition", "Calling end of speech");
        recognizer.stop();
    }

    private void startRecognition(String searchName) {
        recognizer.startListening(searchName);
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .getRecognizer();

        recognizer.addListener(this);

        // Create grammar-based search for selection between demos
        File menuGrammar = new File(assetsDir, "menu.gram");
        recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);
    }

    @Override
    public void onError(Exception error) {
        Log.d("Speech recognition", error.toString());
    }

    // Called after recognizer is stopped
    @Override
    public void onResult(Hypothesis hypothesis) {
        int length = player.getCurrentPosition();
        isPaused = !player.isPlaying() && length > 1;
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            if (text.equals("start") && isPaused) {
                Toast.makeText(this, "Start", Toast.LENGTH_SHORT).show();
                player.seekTo(length);
                player.start();
            } else if (text.equals("stop") && !isPaused) {
                Toast.makeText(this, "Stop", Toast.LENGTH_SHORT).show();
                player.pause();
            }
        }

        startRecognition(MENU_SEARCH);
    }

    /**
     * Unused methods
     */
    // Called when speech begins
    @Override
    public void onBeginningOfSpeech() {
    }

    // Called if we set a time out on recognizer's start listening method
    @Override
    public void onTimeout() {
    }
}
