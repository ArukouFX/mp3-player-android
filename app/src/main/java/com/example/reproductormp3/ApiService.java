package com.example.reproductormp3;

import retrofit2.Call;
import retrofit2.http.GET;
import java.util.List;

public interface ApiService {
    @GET("mobile2/musicas/list.json")
    Call<List<Song>> getPlaylist();
}
