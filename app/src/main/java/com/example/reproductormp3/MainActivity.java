package com.example.reproductormp3;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView songTitle;
    private TextView songAuthor;
    private ImageButton btnPlay;
    private ImageButton btnPause;
    private ImageButton btnStop;
    private ImageButton btnNext;
    private ImageButton btnPrevious;

    private List<Song> songList;
    private int currentSongIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songTitle = findViewById(R.id.song_title);
        songAuthor = findViewById(R.id.song_author);
        btnPlay = findViewById(R.id.btn_play);
        btnPause = findViewById(R.id.btn_pause);
        btnStop = findViewById(R.id.btn_stop);
        btnNext = findViewById(R.id.btn_next);
        btnPrevious = findViewById(R.id.btn_previous);

        enableButtons(false);

        IntentFilter filter = new IntentFilter("com.example.reproductormp3.SONG_CHANGED");
        registerReceiver(songChangedReceiver, filter);

        ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
        apiService.getPlaylist().enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    songList = response.body();
                    updateUI(currentSongIndex);
                    enableButtons(true);

                    // Enviar la lista de canciones al MusicService
                    Intent serviceIntent = new Intent(MainActivity.this, MusicService.class);
                    serviceIntent.putExtra("song_list", (Serializable) songList); // Pasa la lista de canciones
                    startService(serviceIntent);
                } else {
                    showErrorMessage("Error al descargar la lista de reproducción.");
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                showErrorMessage("No se pudo conectar al servidor.");
            }
        });

        btnPlay.setOnClickListener(v -> {
            if (!songList.isEmpty()) {
                if (currentSongIndex < songList.size()) {
                    sendServiceCommand("PLAY");
                } else {
                    Toast.makeText(this, "No hay canciones disponibles para reproducir.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No hay canciones disponibles para reproducir.", Toast.LENGTH_SHORT).show();
            }
        });

        btnPause.setOnClickListener(v -> sendServiceCommand("PAUSE"));
        btnStop.setOnClickListener(v -> sendServiceCommand("STOP"));
        btnNext.setOnClickListener(v -> sendServiceCommand("NEXT"));
        btnPrevious.setOnClickListener(v -> sendServiceCommand("PREVIOUS"));
    }

    private final BroadcastReceiver songChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int newSongIndex = intent.getIntExtra("song_index", 0);
            currentSongIndex = newSongIndex;
            updateUI(currentSongIndex);
        }
    };

    private void sendServiceCommand(String action) {
        // Verifica que la lista no esté vacía antes de enviar un comando al servicio
        if (!songList.isEmpty()) {
            Intent serviceIntent = new Intent(MainActivity.this, MusicService.class);
            serviceIntent.setAction(action);
            serviceIntent.putExtra("song_index", currentSongIndex);
            startService(serviceIntent);
        } else {
            Toast.makeText(this, "No hay canciones cargadas", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI(int index) {
        Song currentSong = songList.get(index);
        songTitle.setText(currentSong.getTitle());
        songAuthor.setText(currentSong.getAuthor());
    }


    private void enableButtons(boolean enabled) {
        btnPlay.setEnabled(enabled);
        btnPause.setEnabled(enabled);
        btnStop.setEnabled(enabled);
        btnNext.setEnabled(enabled);
        btnPrevious.setEnabled(enabled);
    }

    private void showErrorMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }


}