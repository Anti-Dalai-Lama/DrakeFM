package com.blablaarthur.drakefm;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Артур on 05.12.2016.
 */

class SongAdapter extends ArrayAdapter<Song> {

    List<Song> songs;
    //ItemFilter mFilter = new ItemFilter();
    Context c;
    LayoutInflater songsInflater;

    public SongAdapter(Context context, List<Song> songs) {
        super(context, R.layout.song_list_element, songs);
        c = context;
        this.songs = songs;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(songsInflater == null) {
            songsInflater = LayoutInflater.from(getContext());
        }
        View myView = songsInflater.inflate(R.layout.song_list_element, parent, false);

        Song item = getItem(position);
        TextView title = (TextView) myView.findViewById(R.id.songTitle);
        TextView artist = (TextView) myView.findViewById(R.id.songArtist);

        title.setText(item.Title);
        artist.setText(item.Artist);

        return myView;

    }

//    public int getId(){
//        int maxId = 0;
//        for(Note note: notes){
//            if(note.Id > maxId)
//                maxId = note.Id;
//        }
//        return maxId+1;
//    }

//    public Song getItemById(int id){
//        for(Song note: songs)
//            if (note.Id == id) {
//                return note;
//            }
//        return null;
//    }

    @Override
    public void add(Song object) {
        songs.add(object);
    }

    @Nullable
    @Override
    public Song getItem(int position) {
        return songs.get(position);
    }


    @Override
    public int getCount() {
        return songs.size();
    }

}
