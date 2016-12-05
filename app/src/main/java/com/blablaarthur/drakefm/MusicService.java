package com.blablaarthur.drakefm;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.IOException;

/**
 * Created by Артур on 05.12.2016.
 */

public class MusicService extends Service {

    final static String PLAY = "com.blablaarthur.drakefm.action.PLAY";
    final static String PAUSE = "com.blablaarthur.drakefm.action.PAUSE";


    public static int mPlayPosition = 0;
    public static int pausedPosition = -1;
    private int pausedTime = 0;

    public static boolean isInBackground;

    public static int queue = 1; //1 - forward, 2 - repeat, 3 - shuffle

    public static MediaPlayer mediaPlayer;
    private Intent seekIntent;
    private BroadcastReceiver seekChangingBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSeekPos(intent);
        }
    };

    //Seekbar variables
    int mediaPosition;
    int mediaMax;
    private final Handler handler = new Handler();
    public static final String BROADCAST_SEEK = "BROADCAST_SEEK";

    //Call segment
    private boolean isPausedInCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;


    @Override
    public void onCreate(){
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Intent intent = new Intent(MainActivity.BROADCAST_ACTION);
                sendBroadcast(intent);
            }
        });

        mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                //if(!mediaPlayer.isPlaying())
                //    play();
            }
        });

        //Set up intent for seek broadcast
        seekIntent = new Intent(BROADCAST_SEEK);

        //For calls
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener(){
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state){
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if(mediaPlayer != null){
                            pause();
                            isPausedInCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if(mediaPlayer != null){
                            if(isPausedInCall){
                                isPausedInCall = false;
                                play();
                            }
                        }
                }
            }
        };

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    public int onStartCommand(Intent intent, int flags, int startId){
        String action = intent.getAction();
        if (action.equals(PLAY)) {
            play();
        } else if (action.equals(PAUSE)) {
            pause();
        }

        setupHandler();

        registerReceiver(seekChangingBroadcastReceiver, new IntentFilter(
                MainActivity.BROADCAST_SEEK_CHANGED));

        return START_NOT_STICKY;
    }

    public void play(){
        if(mPlayPosition != pausedPosition) {
            mediaPlayer.reset();
            try {
                Log.d("A_R_T", "PLAY!");
                mediaPlayer.setDataSource(MainActivity.songsAdapter.getItem(mPlayPosition).Path);
                mediaPlayer.prepare();
            } catch (IOException e) {
                Log.d("A_R_T", e.toString());
            }
            mediaPlayer.start();
        }
        else {
            mediaPlayer.seekTo(pausedTime);
            mediaPlayer.start();
            pausedTime = 0;
        }
    }

    public void pause(){
        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            pausedPosition = mPlayPosition;
            pausedTime = mediaPlayer.getCurrentPosition();
        }
    }

    public void setupHandler(){
        handler.removeCallbacks(sendUpdatesToUI);
        handler.postDelayed(sendUpdatesToUI, 1000);
    }

    private Runnable sendUpdatesToUI = new Runnable() {
        @Override
        public void run() {
            LogMediaPosition();
            handler.postDelayed(this, 1000);
        }
    };

    public void LogMediaPosition(){
        if(mediaPlayer.isPlaying()){
            mediaPosition = mediaPlayer.getCurrentPosition();
            mediaMax = mediaPlayer.getDuration();
            seekIntent.putExtra("current", String.valueOf(mediaPosition));
            seekIntent.putExtra("mediamax", String.valueOf(mediaMax));
            sendBroadcast(seekIntent);
        }
    }

    public void updateSeekPos(Intent intent){
        int seekPos = intent.getIntExtra("seekpos", 0);
        if(mediaPlayer.isPlaying()){
            handler.removeCallbacks(sendUpdatesToUI);
            mediaPlayer.seekTo(seekPos);
            mediaPlayer.start();
            setupHandler();
        }
        else{
            pausedTime = seekPos;
        }
    }

    public void onDestroy(){
        super.onDestroy();

        if(mediaPlayer != null){
            if(mediaPlayer.isPlaying()){
                mediaPlayer.stop();
            }
            mediaPlayer.release();
        }

        if(phoneStateListener != null){
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        handler.removeCallbacks(sendUpdatesToUI);
        unregisterReceiver(seekChangingBroadcastReceiver);


    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
