package com.blablaarthur.drakefm;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.Inflater;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener{

    ListView musicListView;
    SeekBar musicProgress;
    ImageView playQueue;
    //ImageView playPause;
    //ImageView previousTrack;
    //ImageView nextTrack;
    TextView currentSong;
    RelativeLayout relativeLayout;

    private GestureDetectorCompat gestureDetector;


    final static int PERMISSION_REQUEST_CODE = 555;
    final static String BROADCAST_ACTION = "BROADCAST_ACTION";
    public static final String BROADCAST_SEEK_CHANGED = "SEEK_CHANGED";
    static int playing = 0;
    static boolean keepplaying = false;
    private int seekMax;

    BroadcastReceiver br;
    BroadcastReceiver seekbr = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSeekBar(intent);
            if(MusicService.isInBackground)
                sendNotification();
        }
    };

    List<Song> songs = new ArrayList<Song>(0);
    static SongAdapter songsAdapter = null;
    NotificationManager nm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        setContentView(R.layout.touchable);

        musicListView = (ListView) findViewById(R.id.musicListView);


        musicProgress = (SeekBar) findViewById(R.id.musicProgress);
        playQueue = (ImageView) findViewById(R.id.playQueue);
//        playPause = (ImageView) findViewById(R.id.playPause);
//        previousTrack = (ImageView) findViewById(R.id.previousTrack);
//        nextTrack = (ImageView) findViewById(R.id.nextTrack);
        currentSong = (TextView) findViewById(R.id.currentSong);
        relativeLayout = (RelativeLayout) findViewById(R.id.activity_main);

        gestureDetector = new GestureDetectorCompat(this,this);
        gestureDetector.setOnDoubleTapListener(this);

        songsAdapter = new SongAdapter(this, songs);
        musicListView.setAdapter(songsAdapter);

        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);


        if(MusicService.mediaPlayer != null){
            if(MusicService.mediaPlayer.isPlaying()){
                playing = 1;
                //playPause.setImageResource(R.drawable.ic_pause_black_24dp);
                musicProgress.setProgress(MusicService.mediaPlayer.getCurrentPosition());
                musicProgress.setMax(MusicService.mediaPlayer.getDuration());
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE
                    },
                    PERMISSION_REQUEST_CODE);
        }
        else{
            getSongs();
        }

        musicListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showSongTitle(position);
                keepplaying = true;
                MusicService.mPlayPosition = position;
                PlayPause();
            }
        });

        musicProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    int seekPos = seekBar.getProgress();
                    Intent seekChangedIntent = new Intent(BROADCAST_SEEK_CHANGED);
                    seekChangedIntent.putExtra("seekpos", seekPos);
                    sendBroadcast(seekChangedIntent);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                MusicService.pausedPosition = -1;
                switch (MusicService.queue){
                    case 1:
                        if(MusicService.mPlayPosition != songsAdapter.getCount() - 1) {
                            MusicService.mPlayPosition += 1;
                            keepplaying = true;
                        }
                        break;
                    case 2:
                        if(MusicService.mPlayPosition != songsAdapter.getCount() - 1)
                            MusicService.mPlayPosition += 1;
                        else
                            MusicService.mPlayPosition = 0;
                        keepplaying = true;
                        break;
                    case 3:
                        MusicService.mPlayPosition = getRandomSongPosition();
                        keepplaying = true;
                        break;
                }
                PlayPause();
                musicProgress.setProgress(0);
                showSongTitle(MusicService.mPlayPosition);
            }
        };

        registerReceiver(br, new IntentFilter(BROADCAST_ACTION));
        registerReceiver(seekbr, new IntentFilter(MusicService.BROADCAST_SEEK));


    }

    private void updateSeekBar(Intent serviceIntent){
        int seekProgress = Integer.parseInt(serviceIntent.getStringExtra("current"));
        seekMax = Integer.parseInt(serviceIntent.getStringExtra("mediamax"));
        musicProgress.setMax(seekMax);
        musicProgress.setProgress(seekProgress);
    }

    //public void ChangePlayQueue(View view){
    public void ChangePlayQueue(){
        switch (MusicService.queue){
            case 1:
                MusicService.queue = 2;
                playQueue.setImageResource(R.drawable.ic_loop_black_24dp);
                break;
            case 2:
                MusicService.queue = 3;
                playQueue.setImageResource(R.drawable.ic_shuffle_black_24dp);
                break;
            case 3:
                MusicService.queue = 1;
                playQueue.setImageResource(R.drawable.ic_trending_flat_black_24dp);
                break;
        }
    }

    public void ChangePlayQueueReverse(){
        switch (MusicService.queue){
            case 1:
                MusicService.queue = 3;
                playQueue.setImageResource(R.drawable.ic_shuffle_black_24dp);
                break;
            case 2:
                MusicService.queue = 1;
                playQueue.setImageResource(R.drawable.ic_trending_flat_black_24dp);
                break;
            case 3:
                MusicService.queue = 2;
                playQueue.setImageResource(R.drawable.ic_loop_black_24dp);
                break;
        }
    }

    //public void PlayPause(View view)
    public void PlayPause(){
        Intent serviceIntent = new Intent(this, MusicService.class);
        if(keepplaying && playing == 1)
            playing = 0;

        switch (playing){
            case 0:
                playing = 1;
                //playPause.setImageResource(R.drawable.ic_pause_black_24dp);
                keepplaying = false;
                serviceIntent.setAction(MusicService.PLAY);
                startService(serviceIntent);

                break;
            case 1:
                playing = 0;
                //playPause.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                keepplaying = false;
                serviceIntent.setAction(MusicService.PAUSE);
                startService(serviceIntent);

                break;
        }
    }

//    public void GoToPrevious(View view){
    public void GoToPrevious(){
        switch (MusicService.queue){
            case 1:
                if(MusicService.mPlayPosition != 0)
                    MusicService.mPlayPosition -= 1;
                else
                    MusicService.mPlayPosition = songsAdapter.getCount() - 1;
                break;
            case 2:
                if(MusicService.mPlayPosition != 0)
                    MusicService.mPlayPosition -= 1;
                else
                    MusicService.mPlayPosition = songsAdapter.getCount() - 1;
                break;
            case 3:
                MusicService.mPlayPosition = getRandomSongPosition();
                break;
        }

        keepplaying = true;
        musicProgress.setProgress(0);
        PlayPause();
        showSongTitle(MusicService.mPlayPosition);
    }

    //public void GoToNext(View view){
    public void GoToNext(){
        switch (MusicService.queue){
            case 1:
                if(MusicService.mPlayPosition != songsAdapter.getCount() - 1)
                    MusicService.mPlayPosition += 1;
                else
                    MusicService.mPlayPosition = 0;
                break;
            case 2:
                if(MusicService.mPlayPosition != songsAdapter.getCount() - 1)
                    MusicService.mPlayPosition += 1;
                else
                    MusicService.mPlayPosition = 0;
                break;
            case 3:
                MusicService.mPlayPosition = getRandomSongPosition();
                break;
        }
        keepplaying = true;
        musicProgress.setProgress(0);
        PlayPause();
        showSongTitle(MusicService.mPlayPosition);
    }

    public static int getRandomSongPosition(){
        Random rn = new Random();
        int range = songsAdapter.getCount() - 1 - 1;
        return rn.nextInt(range);
    }

    public String showSongTitle(int position){
        Song current = songsAdapter.getItem(position);
        String song = current.Artist + " - " + current.Title;
        if(song.length() > 35){
            song = song.substring(0, 35) + "...";
        }
        currentSong.setText(song);
        return song;
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getSongs();
                Toast.makeText(this,"Music uploaded", Toast.LENGTH_LONG).show();
            }
            else if (grantResults[0] == PackageManager.PERMISSION_DENIED){
                Toast.makeText(this,"Can't load your music", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void getSongs(){
        songs.clear();
        String[] columns = {MediaStore.Audio.AudioColumns._ID, MediaStore.Audio.AudioColumns.ARTIST, MediaStore.Audio.AudioColumns.TITLE, MediaStore.Audio.AudioColumns.DATA};
        String where = MediaStore.Audio.AudioColumns.IS_MUSIC + " <> 0";
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, columns, where, null, null);

        while (cursor.moveToNext()) {
            Song n = new Song(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3));
            songs.add(n);
        }
        songsAdapter.notifyDataSetChanged();

        if(songsAdapter.getCount() != 0)
            showSongTitle(MusicService.mPlayPosition);
    }

    @Override
    public void onPause() {
        super.onPause();
        MusicService.isInBackground = true;
        sendNotification();
    }

    @Override
    public void onResume(){
        super.onResume();
        MusicService.isInBackground = false;
    }

    public void sendNotification(){
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this)
                .setContentIntent(pIntent)
                .setSmallIcon(R.drawable.ic_audiotrack_white_24dp)
                .setContentTitle(showSongTitle(MusicService.mPlayPosition))
                .setTicker("Your music is playing")
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setProgress(MusicService.mediaPlayer.getDuration(),
                        MusicService.mediaPlayer.getCurrentPosition(), false);

        Notification n = notification.build();
        nm.notify(555, n);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        Log.d("A_R_T", "onShowPress");
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.d("A_R_T", "onSingleTapUp");
        PlayPause();
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }


    PopupWindow popup;
    LayoutInflater layoutInflater;

    @Override
    public void onLongPress(MotionEvent e) { //show Drake
        Log.d("A_R_T", "onLongPress");
        layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        ViewGroup container = (ViewGroup) layoutInflater.inflate(R.layout.drake, null);

        popup = new PopupWindow(container, 900, 550, true);
        popup.showAtLocation(relativeLayout, Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);

        new Handler().postDelayed(new Runnable(){
            public void run() {
                if (popup != null) {
                    popup.dismiss();
                    popup = null;
                }
            }}, 2 *1000);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if(e1.getX() < e2.getX() && e2.getX()-e1.getX() > Math.abs(e1.getY() - e2.getY())){
            Log.d("A_R_T", "Right");
            GoToPrevious();
        }
        else if (e1.getX() > e2.getX() && e1.getX()-e2.getX() > Math.abs(e1.getY() - e2.getY())){
            Log.d("A_R_T", "Left");
            GoToNext();
        }
        else if (e1.getY() > e2.getY() && e1.getY()-e2.getY() > Math.abs(e1.getX() - e2.getX())){
            Log.d("A_R_T", "Top");
            ChangePlayQueue();
        }
        else if (e1.getY() < e2.getY() && e2.getY()-e1.getY() > Math.abs(e1.getX() - e2.getX())){
            Log.d("A_R_T", "Bottom");
            ChangePlayQueueReverse();
        }
        return false;
    }


    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        Log.d("A_R_T", "onDoubleTapEvent");
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }
}
