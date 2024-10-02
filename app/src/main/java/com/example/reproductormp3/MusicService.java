package com.example.reproductormp3;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MusicService extends Service {
    private static final String CHANNEL_ID = "MusicChannel";
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private MediaSessionCompat mediaSession;
    private List<Song> songList;
    private String currentSongTitle = "";
    private String currentSongArtist = "";
    private int currentSongIndex = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // Registra el BroadcastReceiver para cambios en la red
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, filter);
        filter.addAction("com.example.reproductormp3.SONG_CHANGED");
        registerReceiver(networkChangeReceiver, filter);

        mediaSession = new MediaSessionCompat(this, "TAG");

        mediaSession = new MediaSessionCompat(this, "TAG");
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Llama a la API para obtener la lista de canciones
        ApiService apiService = ApiClient.getRetrofitInstance().create(ApiService.class);
        apiService.getPlaylist().enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    songList = response.body();  // Guarda la lista de canciones en el servicio
                } else {
                    Toast.makeText(MusicService.this, "Error al descargar la lista de reproducción.", Toast.LENGTH_SHORT).show();
                    stopSelf();
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                Toast.makeText(MusicService.this, "No se pudo conectar al servidor.", Toast.LENGTH_SHORT).show();
                stopSelf();
            }
        });
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if ("PLAY".equals(action)) {
            int newSongIndex = intent.getIntExtra("song_index", -1);
            if (newSongIndex != currentSongIndex) {
                // Si es una nueva canción, actualiza el índice y reproduce
                currentSongIndex = newSongIndex;
                playMusic();
            }
        }

        if (intent != null) {
            List<Song> receivedSongList = (List<Song>) intent.getSerializableExtra("song_list");
            if (receivedSongList != null && !receivedSongList.isEmpty()) {
                songList = receivedSongList;  // Actualiza la lista de canciones en el servicio
            }

            if ("SET_SONG_INDEX".equals(action)) {
                if (intent.hasExtra("song_index")) {
                    int newSongIndex = intent.getIntExtra("song_index", currentSongIndex);
                    setCurrentSongIndex(newSongIndex); // Llama al método para actualizar el índice
                }
            }

            if (action != null && songList != null) {  // Asegúrate de que `songList` no sea null
                switch (action) {
                    case "PLAY":
                        if (!isPlaying) {
                            if (intent.hasExtra("song_index")) {
                                currentSongIndex = intent.getIntExtra("song_index", currentSongIndex);
                            }
                            playMusic();
                            Log.d("MusicService", "(PLAY) El ID de la canción ha cambiado a: " + currentSongIndex);
                        }
                        break;
                    case "PAUSE":
                        pauseMusic();
                        break;
                    case "STOP":
                        stopMusic();
                        break;
                    case "NEXT":
                        if (currentSongIndex < songList.size() - 1) {
                            currentSongIndex++;
                            Log.d("MusicService", "(NEXT) El ID de la canción ha cambiado a: " + currentSongIndex);
                            playMusic();  // Reproduce la siguiente canción
                        } else {
                            Toast.makeText(this, "No hay más canciones.", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "PREVIOUS":

                        if (currentSongIndex > 0) {
                            currentSongIndex--;  // Decrementa el índice para ir a la canción anterior
                            Log.d("MusicService", "(PREVIOUS) El ID de la canción ha cambiado a: " + currentSongIndex);
                            playMusic();  // Llama a la función playMusic para reproducir la canción anterior
                        } else {
                            Toast.makeText(this, "No hay canciones anteriores.", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        }
        return START_STICKY;
    }

    public void setCurrentSongIndex(int index) {
        if (index < 0 || index >= songList.size()) return; // Verifica que el índice esté en el rango válido
        if (currentSongIndex != index) { // Solo actualiza si es un cambio
            currentSongIndex = index; // Actualiza el índice
            playMusic(); // Reproduce la nueva canción
        }
    }


    private void playMusic() {
        if (songList == null || songList.isEmpty()) {
            Toast.makeText(this, "No hay canciones disponibles para reproducir.", Toast.LENGTH_SHORT).show();
            return;
        }

        Song currentSong = songList.get(currentSongIndex);

        Log.d("MusicService", "(PLAY)El ID de la canción ha cambiado a: " + currentSongIndex);

        // Si el MediaPlayer ya está preparado y en pausa, solo reanuda la reproducción
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            showNotification(currentSongTitle, currentSongArtist, true);
            return;  // Salimos del método aquí si solo estamos reanudando la canción
        }

        if (mediaPlayer != null) {
            mediaPlayer.reset();  // Reinicia el MediaPlayer si ya fue inicializado
        } else {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
        }

        try {
            mediaPlayer.setDataSource(currentSong.getUrl());

            // Listener para el buffering progresivo
            mediaPlayer.setOnBufferingUpdateListener((mp, percent) -> {
                if (percent < 10 && mediaPlayer.isPlaying()) {
                    // Si el buffer está por debajo del 10%, pausa la música
                    pauseMusic();
                    Toast.makeText(this, "Buffering lento, pausa la reproducción...", Toast.LENGTH_SHORT).show();
                } else if (percent > 10 && !mediaPlayer.isPlaying()) {
                    // Si el buffer vuelve a estar por encima del 20%, reanuda la música
                    mediaPlayer.start();
                    Toast.makeText(this, "Buffer suficiente, reanudando reproducción.", Toast.LENGTH_SHORT).show();
                }
            });

            // Listener para cuando el MediaPlayer esté listo para empezar a reproducir
            mediaPlayer.setOnPreparedListener(mp -> {
                mediaPlayer.start();
                isPlaying = true;
                currentSongTitle = currentSong.getTitle();
                currentSongArtist = currentSong.getAuthor();
                showNotification(currentSongTitle, currentSongArtist, true);

                // Broadcast para actualizar el UI o cualquier otro componente
                Intent intent = new Intent("com.example.reproductormp3.SONG_CHANGED");
                intent.putExtra("song_index", currentSongIndex);
                sendBroadcast(intent);
            });

            // Listener para el evento de finalización de la canción
            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;  // La canción ha terminado de reproducirse
                showNotification(currentSongTitle, currentSongArtist, false);  // Cambia a estado de pausa
            });

            mediaPlayer.prepareAsync(); // Prepara la canción de manera asíncrona
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();  // Si hay un error, detiene el servicio
        }
    }




    private void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            showNotification(currentSongTitle, currentSongArtist, false);  // Usa las variables guardadas
        }
    }


    private void stopMusic() {
        Log.d("MusicService", "(STOP) El ID de la canción ha cambiado a: " + currentSongIndex);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            isPlaying = false;
            stopForeground(true);  // Quitar la notificación y detener el servicio
            stopSelf();
        }
    }

    private void showNotification(String title, String artist, boolean isPlaying) {
        // Intent para abrir la MainActivity al tocar la notificación
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Intent para el botón de Play/Pause en la notificación
        Intent playPauseIntent = new Intent(this, MusicService.class);
        playPauseIntent.setAction(isPlaying ? "PAUSE" : "PLAY");
        PendingIntent playPausePendingIntent = PendingIntent.getService(
                this,
                1,
                playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Intent para el botón de Siguiente
        PendingIntent nextPendingIntent = getActionIntent("NEXT");

        // Intent para el botón de Anterior
        PendingIntent previousPendingIntent = getActionIntent("PREVIOUS");

        // Construcción de la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle(title != null ? title : currentSongTitle)  // Usa el título guardado si no se pasa
                .setContentText(artist != null ? artist : currentSongArtist)  // Usa el artista guardado si no se pasa
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(R.drawable.ic_previous, "Previous", previousPendingIntent)  // Botón "Anterior"
                .addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play, "Play/Pause", playPausePendingIntent)  // Botón "Play/Pause"
                .addAction(R.drawable.ic_next, "Next", nextPendingIntent)  // Botón "Siguiente"
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSession.getSessionToken()));

        // Inicia el servicio en primer plano con la notificación
        startForeground(1, builder.build());
    }

    private PendingIntent getActionIntent(String action) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }


    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();

        if (networkChangeReceiver != null) {
            unregisterReceiver(networkChangeReceiver);  // Desregistrar el BroadcastReceiver para evitar fugas
        }

        mediaSession.release();

    }

    private BroadcastReceiver networkChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isConnected = isNetworkAvailable();
            if (isConnected && !isPlaying && mediaPlayer != null) {
                // Reanuda la música si está en pausa y la conexión se ha restablecido
                mediaPlayer.start();
                isPlaying = true;
                Toast.makeText(context, "Conexión a internet restablecida, la reproducción ha sido reanudada.", Toast.LENGTH_SHORT).show();
                showNotification(currentSongTitle, currentSongArtist, true);  // Actualiza la notificación
            } else if (!isConnected && mediaPlayer != null && mediaPlayer.isPlaying()) {
                // Pausa la música si la conexión a internet se pierde
                pauseMusic();
                Toast.makeText(context, "Conexión a internet perdida, la reproducción ha sido pausada.", Toast.LENGTH_SHORT).show();
            }
        }
    };


    public boolean isNetworkAvailable() {
        // Obtén el ConnectivityManager del sistema
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Obtén la red activa actual
        Network network = connectivityManager.getActiveNetwork();

        // Obtén las capacidades de la red (como Internet)
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);

        // Verifica si la red tiene capacidad para conectarse a Internet
        if (networkCapabilities != null) {
            return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else {
            return false;  // No hay red o no es capaz de conectarse a Internet
        }
    }


}
