package com.example.snaptoksample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String ALL_VIDEOS_API="http://15.207.150.183/API/index.php?p=videoTestAPI";
    //Views
    private PlayerView videoPlayer;
    private SimpleExoPlayer player;
    private TextView videoId;
    private Button reloadBtn;
    private ProgressBar progressBar;
    //Videos list
    ArrayList<String> videoListIds = new ArrayList<>();
    //Control play state of video
    private boolean playWhenReady = true;
    private int currentWindow = 0;
    private long playbackPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Init views
        videoPlayer = findViewById(R.id.videoPlayer);
        videoId = findViewById(R.id.videoId);
        reloadBtn = findViewById(R.id.reloadBtn);
        progressBar = findViewById(R.id.progressBar);

        reloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadVideoData();
                reloadBtn.setVisibility(View.GONE);
                playWhenReady = true;
                currentWindow = 0;
                playbackPosition = 0;

            }
        });
    }

    //Control lifecycle of activity
    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT >= 24) {
            loadVideoData();
        }
    }
    /*@Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT < 24 || player == null)) {
            loadVideoData();
        }
    }*/
    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT < 24) {
            releasePlayer();
        }
    }
    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT >= 24) {
            releasePlayer();
        }
    }

    //Utility functions to set up exo player.
    private void releasePlayer() {
        if (player != null) {
            playWhenReady = player.getPlayWhenReady();
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            player.release();
            player = null;
        }
    }
    private void initializeWithVideoList(ArrayList<String> videoList){
        Uri video = Uri.parse(videoList.get(1));
        videoId.setText(videoList.get(0));
        player = new SimpleExoPlayer.Builder(this).build();
        videoPlayer.setPlayer(player);
        MediaSource mediaSource = buildMediaSource(video);
        player.setPlayWhenReady(playWhenReady);
        player.seekTo(currentWindow, playbackPosition);
        player.prepare(mediaSource, false, false);
        player.addListener(new Player.EventListener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                switch (state){
                    case Player.STATE_BUFFERING:
                        progressBar.setVisibility(View.VISIBLE); break;
                    case Player.STATE_READY:
                        progressBar.setVisibility(View.GONE); break;
                    case Player.STATE_ENDED:
                        reloadBtn.setVisibility(View.VISIBLE); break;
                }
            }
        });
    }
    private MediaSource buildMediaSource(Uri uri){
        DataSource.Factory factory =
                new DefaultDataSourceFactory(this, "exoplayer-codelab");
        return new ProgressiveMediaSource.Factory(factory)
                .createMediaSource(uri);
    }

    //Data is loaded and player is initialized.
    private void loadVideoData(){
        //We create a volley request
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, ALL_VIDEOS_API, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String data = response.toString();
                        ArrayList<String> videoList = createVideoList(data);
                        initializeWithVideoList(videoList);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.i("TAG", Objects.requireNonNull(error.getMessage()));
            }
        });
        requestQueue.add(request);
    }

    //These are the utility functions which load data from web.
    private ArrayList<String> createVideoList(String data){
        ArrayList<String> videosList = new ArrayList<>();
        try {
            JSONObject object = new JSONObject(data);
            String code = object.getString("code");
            if(code.equals("200")){
                Log.i("TAG", "Video loading successful!");
                JSONArray allVideos = object.getJSONArray("msg");
                for(int i=0;i<allVideos.length();i++){
                    JSONObject video = allVideos.getJSONObject(i);
                    String id = video.getString("id");
                    videosList.add(id);
                    String videoUrl = video.getString("mp4Video");
                    videosList.add(format(videoUrl));
                    //videoListIds.add(id);
                }
            }
            else{
                Log.i("TAG", "Some error occurred!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return videosList;
    }
    private String format(@NonNull String string){
        StringBuilder formattedString = new StringBuilder();
        for(int i=0;i<string.length();i++){
            if(string.charAt(i) == '\\') { continue; }
            formattedString.append(string.charAt(i));
        }
        return formattedString.toString();
    }

}