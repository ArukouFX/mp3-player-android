package com.example.reproductormp3;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private List<Song> songList;
    private OnItemClickListener listener;

    // Constructor que acepta la lista de canciones y el listener
    public SongAdapter(List<Song> songList, OnItemClickListener listener) {
        this.songList = songList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song currentSong = songList.get(position);
        holder.title.setText(currentSong.getTitle());
        holder.author.setText(currentSong.getAuthor());

        // Maneja el clic en el elemento de la lista
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(position); // Notifica el clic al listener
            }
        });
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    // ViewHolder interno que contiene las referencias a los elementos de la UI
    public static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView author;

        public SongViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.song_title);
            author = itemView.findViewById(R.id.song_author);
        }
    }

    // Interfaz para manejar clics
    public interface OnItemClickListener {
        void onItemClick(int position);
    }
}
