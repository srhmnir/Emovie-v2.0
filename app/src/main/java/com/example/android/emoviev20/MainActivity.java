package com.example.android.emoviev20;


import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    GridView gridView ;
    ArrayList<Movie> movieData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridView = (GridView) findViewById(R.id.gridView);

        FetchDataTask task = new FetchDataTask();
        task.execute();

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                            @Override
                                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                                Intent intent = new Intent(getApplicationContext(), GridItemDetail.class);
                                                Bundle bundle = new Bundle();
                                                bundle.putSerializable("data",movieData.get(position));
                                                intent.putExtras(bundle);
                                                startActivity(intent);
                                            }
                                        }
        );

    }


    public class FetchDataTask extends AsyncTask<Void, Void, ArrayList<Movie>> {

        String movieJsonStr;

        @Override
        protected void onPostExecute(ArrayList<Movie> data) {
            super.onPostExecute(data);
            gridView.setAdapter(new ImageListAdapter(getApplicationContext(), movieData));
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected ArrayList<Movie> doInBackground(Void... params) {
            isOnline();

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            try {
                // Construct the URL for the The Movie DB query

                final String MOVIE_BASE_URL =
                        "http://api.themoviedb.org/3/movie/popular";

                Uri builtUri = Uri.parse(MOVIE_BASE_URL).buildUpon()
                        .appendQueryParameter("api_key", "6b70074357ee3b0db71a86feba69a4bd")
                        .build();

                URL url = new URL(builtUri.toString());

                // Create the request to themoviedb, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                movieJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e("Movie", "Error " + e);
                // If the code didn't successfully get the movie data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("Movie", "Error closing stream" + e);
                    }
                }
            }

            try {
                return getMovieDataFromJson(movieJsonStr);
            } catch (JSONException e) {
                Log.e("Movie", e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }
        public boolean isOnline() {
            ConnectivityManager cm =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

            return cm.getActiveNetworkInfo() != null &&
                    cm.getActiveNetworkInfo().isConnectedOrConnecting();
        }
        private ArrayList<Movie> getMovieDataFromJson(String movieJsonStr) throws JSONException {

            movieData = new ArrayList<Movie>();

            JSONObject dataObject = new JSONObject(movieJsonStr);

            JSONArray resultsArray = dataObject.getJSONArray("results");

            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject element = resultsArray.getJSONObject(i);

                String posterPath = element.getString("poster_path");
                String movieName = element.getString("title");
                String overview = element.getString("overview");
                String releaseDate = element.getString("release_date");
                String rating = element.getString("vote_average");

                Movie m = new Movie(posterPath, movieName, overview, releaseDate,rating);

                movieData.add(i, m);
            }

            return movieData;
        }
    }
}