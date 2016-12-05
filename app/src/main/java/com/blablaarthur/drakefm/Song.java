package com.blablaarthur.drakefm;

/**
 * Created by Артур on 05.12.2016.
 */

public class Song {
    public String Id;
    public String Artist;
    public String Title;
    public String Path;

    public Song(String id, String artist, String title, String path){
        Id = id;
        Artist = artist;
        Title = title;
        Path = path;
    }
}
