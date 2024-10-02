package com.example.reproductormp3;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.Serializable;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SongAdapter.OnItemClickListener {

    private TextView songTitle;
    private TextView songAuthor;
    private ImageButton btnPlay;
    private ImageButton btnPause;
    private ImageButton btnStop;
    private ImageButton btnNext;
    private ImageButton btnPrevious;

    private RecyclerView recyclerView;
    private SongAdapter songAdapter;  // Adaptador del RecyclerView
    private List<Song> songList;
    private int currentSongIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializa los elementos de la UI
        songTitle = findViewById(R.id.song_title);
        songAuthor = findViewById(R.id.song_author);
        btnPlay = findViewById(R.id.btn_play);
        btnPause = findViewById(R.id.btn_pause);
        btnStop = findViewById(R.id.btn_stop);
        btnNext = findViewById(R.id.btn_next);
        btnPrevious = findViewById(R.id.btn_previous);

        // Inicializa y configura el RecyclerView
        recyclerView = findViewById(R.id.recyclerView_songs); // Instancia del RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this)); // Configuración del layout

        enableButtons(false); // Desactiva los botones inicialmente

        IntentFilter filter = new IntentFilter("com.example.reproductormp3.SONG_CHANGED");
        registerReceiver(songChangedReceiver, filter);

        // Obtiene la lista de canciones desde la API
        ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
        apiService.getPlaylist().enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    songList = response.body(); // Guarda la lista de canciones
                    enableButtons(true); // Habilita los botones

                    // Configura el RecyclerView con el adaptador
                    songAdapter = new SongAdapter(songList, position -> {
                        // Maneja el clic en un elemento de la lista
                        currentSongIndex = position; // Actualiza el índice de la canción actual
                        updateUI(currentSongIndex); // Actualiza la UI con la canción seleccionada

                        // Envía el índice de la canción seleccionada al servicio
                        Intent intent = new Intent(MainActivity.this, MusicService.class);
                        intent.setAction("PLAY");
                        intent.putExtra("song_index", currentSongIndex); // Envía el índice actual
                        startService(intent); // Inicia o actualiza el servicio con el nuevo índice

                        Log.d("MusicService", "(PLAY) El ID de la canción ha cambiado a: " + currentSongIndex);
                    });
                    recyclerView.setAdapter(songAdapter); // Asigna el adaptador al RecyclerView

                    // Enviar la lista de canciones al MusicService
                    Intent serviceIntent = new Intent(MainActivity.this, MusicService.class);
                    serviceIntent.putExtra("song_list", (Serializable) songList); // Pasa la lista de canciones
                    startService(serviceIntent);

                    // Actualiza la UI para mostrar la primera canción
                    updateUI(currentSongIndex); // Muestra la primera canción
                } else {
                    showErrorMessage("Error al descargar la lista de reproducción.");
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                showErrorMessage("No se pudo conectar al servidor.");
            }
        });

        // Configura los click listeners para los botones
        btnPlay.setOnClickListener(v -> {
            if (!songList.isEmpty()) {
                if (currentSongIndex < songList.size()) {
                    sendServiceCommand("PLAY"); // Envía comando PLAY al MusicService
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

    @Override
    public void onItemClick(int position) {
        // Maneja el clic en la canción
        Song clickedSong = songList.get(position);
        // Aquí puedes iniciar la reproducción de la canción seleccionada
        Toast.makeText(this, "Canción seleccionada: " + clickedSong.getTitle(), Toast.LENGTH_SHORT).show();
    }
}
