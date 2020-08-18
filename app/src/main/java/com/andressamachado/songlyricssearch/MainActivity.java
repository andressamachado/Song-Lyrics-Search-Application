package com.andressamachado.songlyricssearch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    /**Key to be used to pass user`s mode preferences to the next activity*/
    public static String user_mode_pref_key = "style";

    /**Instances to be used to handle the shared preferences between activities*/
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor mEditor;

    /**XML layout elements declarations*/
    Toolbar toolbar;
    DrawerLayout drawer;
    View view;
    TextView appTitle;
    Switch modeSwitch;
    EditText artistFieldInput;
    EditText songFieldInput;
    Button searchButton;
    ImageButton googleButton;
    TextView needHelp;
    ListView searchedSongList;

    /**Used to hold the name of the artist or group to be searched. Data from the user input*/
    String artist = null;
    /**Used to hold the title of the song to be searched. Data from the user input*/
    String song = null;
    /**Used to hold the lyric found from web searched*/
    String lyrics;
    /**Holds every song searched before to avoid add repeated songs to the database*/
    ArrayList<LyricSearch> searchHistory = new ArrayList<>();

    /**Builds the alert dialog*/
    AlertDialog.Builder dialogBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_search_page_activity);

        initializeActivity();
        setClickListeners();
    }

    private void initializeActivity() {
        //Initialization of every view used in this activity
        toolbar = (Toolbar) findViewById(R.id.application_toolbar);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        view = (View) findViewById(R.id.song_lyrics_main);
        appTitle = (TextView) findViewById(R.id.application_name);
        artistFieldInput = (EditText) findViewById(R.id.artist_field_input);
        songFieldInput = (EditText) findViewById(R.id.song_title_field_input);
        searchButton = (Button) findViewById(R.id.search_button);
        googleButton = (ImageButton) findViewById(R.id.google_button);
        modeSwitch = (Switch) findViewById(R.id.switch_mode);
        searchedSongList = (ListView) findViewById(R.id.searched_song_list);
        needHelp = (TextView) findViewById(R.id.help_button);

        //Sets the toolbar title to empty string to not display the name of the project there as it
        //does not have space to display everything we have to display.
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        //Instance to handle the functionality of the drawer layout and framework action bar to
        // implement the recommended design for navigation drawer
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.open, R.string.close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        //Standard navigation menu for the application. It is what pops out from the side
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Sets initial background color to white
        view.setBackgroundColor(getResources().getColor(android.R.color.white));

        //Manage to hold the user`s style mode preference to be loaded next time the application is opened
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String modeOptionSaved = sharedPreferences.getString(user_mode_pref_key, "light");

        if (modeOptionSaved.equals("light")){
            //mode switch not checked
            modeSwitch.setChecked(false);
            //if user checks to use the light mode, load the light mode style
            setLightMode();
        } else {
            modeSwitch.setChecked(true);
            //if user checks to use the dark mode, load the dark mode style
            setDarkMode();
        }

        //builder fot the alert dialog
        dialogBuilder = new AlertDialog.Builder(this);

    }

    private void setClickListeners(){
        //Listener to the switch on the top right that selects the user`s mode preference
        modeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //Editor to manage the mode change made by the user
            mEditor = sharedPreferences.edit();

            //Performs changes in the layout of the activity
            if (isChecked) {
                mEditor.putString(user_mode_pref_key, "dark");
                setDarkMode();
            } else {
                mEditor.putString(user_mode_pref_key, "light");
                setLightMode();
            }

            mEditor.apply();
        });

        //Listener to the search button
        searchButton.setOnClickListener(v -> {
            performSearch();
        });

        //Listener to the text view need help
        needHelp.setOnClickListener(v -> {
            //displays a alert dialog with information about the application
            buildAndDisplayAlertDialog();
        });
    }

   @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        //Inflate the menu with a layout created for it
        inflater.inflate(R.menu.toolbar_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Instance to go to the next activity
        Intent browserIntent;
        //String holds the message to be displayed
        String message;

        //Look at your menu XML file. Put a case for every id in that file:
        switch(item.getItemId()) {
            case R.id.toolbar_linkedin:
                //Sets the intent to be passed as argument to the startActivity method
                browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/andressa-machado-59705792/"));
                //Goes to the API web page
                startActivity(browserIntent);
                break;
            case R.id.toolbar_github:
                //Sets the intent to be passed as argument to the startActivity method
                browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/andressamachado"));
                //Goes to the API web page
                startActivity(browserIntent);
                break;
            case R.id.toolbar_about:
                message = "This is the Song Lyrics Search activity written by Andressa Machado";
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                break;
        }

        return true;
    }

    private void buildAndDisplayAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //Inflate the custom dialog to the view
        View customAlertView = getLayoutInflater().inflate(R.layout.info_alert_dialog_layout, null, false);

        //sets title
        TextView title = customAlertView.findViewById(R.id.alert_title);
        title.setText(R.string.info_alert_dialog_title);

        //sets message
        TextView instructions = customAlertView.findViewById(R.id.alert_instructions);
        instructions.setText(R.string.info_alert_dialog_message);

        //sets positive button to close the dialog
        builder.setPositiveButton(R.string.alert_dialog_positive_btn, (click, arg) -> {
        }).setView(customAlertView).show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        //Look at your menu XML file. Put a case for every id in that file:
        switch(item.getItemId()) {
            case R.id.drawer_instructions:
                buildAndDisplayAlertDialog();
                break;
            case R.id.drawer_about_api:
                //Sets the intent to be passed as argument to the startActivity method
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://lyricsovh.docs.apiary.io/#"));
                //Goes to the API web page
                startActivity(browserIntent);
                break;
            case R.id.drawer_github:
                //Sets the intent to be passed as argument to the startActivity method
                browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/andressamachado"));
                //Goes to the API web page
                startActivity(browserIntent);
                break;
            case R.id.drawer_linkedin:
                //Sets the intent to be passed as argument to the startActivity method
                browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/andressa-machado-59705792/"));
                //Goes to the API web page
                startActivity(browserIntent);
                break;
        }

        //Close the navigation drawer
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.closeDrawer(GravityCompat.START);

        return false;
    }

    private void setDarkMode() {
        //Black background
        view.setBackgroundColor(Color.rgb(1, 0, 0));
        //new image to the list view
        searchedSongList.setBackgroundResource(R.drawable.darkmode);
        //title color to purple
        appTitle.setTextColor(Color.rgb(154, 98, 121));
        //black background for the Google image button
        googleButton.setBackgroundColor(Color.rgb(1, 0, 0));
    }

    private void setLightMode() {
        //White background
        view.setBackgroundColor(Color.WHITE);
        //Standard image for the list view background
        searchedSongList.setBackgroundResource(R.drawable.background);
        //App title to black
        appTitle.setTextColor(Color.BLACK);
        //Google image button background to white to matches the app background
        googleButton.setBackgroundColor(Color.WHITE);
    }



    /**
     * Method to encode input from the user to perform a successful search.
     * Uses the URLEncoder java class to perform the operation
     * @see java.net.URLEncoder
     * */
    private void encodeString() {
        //Try/catch to handle exceptions
        try {
            //put encoded values into variables
            artist = URLEncoder.encode(artistFieldInput.getText().toString(), "UTF-8");
            song = URLEncoder.encode(songFieldInput.getText().toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to request the song from the API
     * It receives a string containing an URL to be used in the search
     * */
    private void requestFromAPI(String requestURL) {
        //Instance of the Inner class SongQuery to perform the request
        SongQuery req = new SongQuery();
        req.execute(requestURL);
    }

    /**
     * Method to perform the song search from the API
     * @see "lyricsovh.docs.apiary.io/#"
     * */
    private void performSearch() {
        //encode the string entered to match the search pattern
        encodeString();
        //String with the url to be used in the search
        String requestURL = "https://api.lyrics.ovh/v1/" + artist + "/" + song;
        //Checks if the song exists in the API
        requestFromAPI(requestURL);
    }

    /**
     * Method to pass the lyric data to the next activity to be displayed
     * */
    private void sendLyricToNextActivity(String lyrics, long id, String songTitle, String artistName) {
        //Intent from this to the next activity
        Intent goToSongPage = new Intent(MainActivity.this, SongLyricActivity.class);
        //Puts data into the intent instance
        goToSongPage.putExtra("lyric", lyrics);
        goToSongPage.putExtra("id", id);
        goToSongPage.putExtra("song info", songTitle + " - " + artistName);

        //starts the new activity
        startActivity(goToSongPage);
    }


    /**
     * This inner class inherits from AsyncTask and it is responsible for the tasks that runs on a
     * background thread and whose result is published on the UI thread. An asynchronous task is
     * defined by 3 generic types, called Params, Progress and Result, and 4 steps,
     * called onPreExecute, doInBackground, onProgressUpdate and onPostExecute.
     *
     * @author Andressa Machado
     * @version 1.0
     * @see "android.os.AsyncTask<Params, Progress, Result>"
     * @since 2020/08/17
     */
    protected class SongQuery extends AsyncTask<String, Integer, String> {

        /**
         * Method invoked on the background thread immediately after onPreExecute() finishes executing.
         *
         *  @see java.io.InputStream and org.json.JSONObject
         * */
        @Override
        protected String doInBackground(String... strings) {
            InputStream response;

            //Try to get connected with the API and get a new Json object from this connection
            try {
                response = connectToAPI(strings[0]);

                String result = convertResponseToString(response);
                JSONObject lyricsObject = new JSONObject(result);

                //save the lyric got from the api to the lyric string
                lyrics = getLyricsFromJSON(lyricsObject);
                //Sets the progress bar update progress
                onProgressUpdate(50);

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * Method to connect the application with the API, using the URL of the API
         *
         * @see java.io.InputStream, java.net.HttpURLConnection, and  java.net.URL
         * */
        private InputStream connectToAPI(String string) throws IOException {
            InputStream response;

            URL url = new URL(string);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
                response = urlConnection.getErrorStream();
            } else {
                response = urlConnection.getInputStream();
            }

            return response;
        }

        /**
         * Method to read the result from the API and save it, returning the lyric as a string
         *
         * @see java.io.InputStream*/
        private String convertResponseToString(InputStream response) throws IOException {
            BufferedReader reader = new BufferedReader(new InputStreamReader(response, StandardCharsets.UTF_8), 8);
            StringBuilder sb = new StringBuilder();

            //While it is a line, continue reading and appending to the string
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            return sb.toString();
        }

        /**
         * Method to return the lyric tag found in the JSON object
         *
         * @see "javax.json"
         * */
        private String getLyricsFromJSON(JSONObject lyricsObject) throws JSONException {
            if (lyricsObject.has("lyrics")) {
                //debug purpose
                Log.i("SongLyricsSearch", "lyric found");

                return lyricsObject.getString("lyrics");
            } else {
                String error = lyricsObject.getString("error");
                Log.i("SongLyricsSearch", error);

                return null;
            }
        }

        /**
         *  Method to be invoked on the UI thread after the background computation finishes. The result of the
         *  background computation is passed to this step as a parameter.
         *  */
        public void onPostExecute(String fromDoInBackground) {
                //Song was found
                String currentArtist = artistFieldInput.getText().toString().toLowerCase();
                String currentSong = songFieldInput.getText().toString().toLowerCase();

                LyricSearch current;

                //boolean to control if the song was already searched before, so it is not added to the database nor the list
                boolean exists = false;

                for (int i = 0; i < searchHistory.size(); i++){
                    current = searchHistory.get(i);

                    if(current.getArtist().equalsIgnoreCase(currentArtist) && current.getSongTitle().equalsIgnoreCase(currentSong)){
                        exists = true;
                        break;
                    }
                }

                long newId = -1;

                //if the song was not searched before, it is added to the database and the array list
                if(!exists) {
                    //Creates a new search object and add it to the array list
                    LyricSearch currentSearch = new LyricSearch(newId, currentArtist, currentSong, false);
                    searchHistory.add(currentSearch);
                }

                sendLyricToNextActivity(lyrics, newId, currentSong, currentArtist);
        }
    }
}

