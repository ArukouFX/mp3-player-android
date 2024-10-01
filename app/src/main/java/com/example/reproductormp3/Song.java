package com.example.reproductormp3;

import java.io.Serializable;

public class Song implements Serializable {
    private String title;
    private String author;
    private String url;
    private String duration;

    public Song(String title, String author, String url, String duration) {
        this.title = title;
        this.author = author;
        this.url = url;
        this.duration = duration;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getUrl() {
        return url;
    }

    public String getDuration() {
        return duration;
    }
}
