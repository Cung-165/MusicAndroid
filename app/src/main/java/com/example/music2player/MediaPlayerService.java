package com.example.music2player;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MediaPlayerService extends Service {
    MediaPlayer mediaPlayer;


    List<AudioFile> playList;
    int currentTrackIndex = 0;
    ServiceController binder = new ServiceController();

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("MediaPlayService", "onBind");
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MediaPlayService", "onCreate");

        playList = new ArrayList<>();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d("MediaPlayService", "onCompletion");
                binder.next();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public class ServiceController extends Binder {

        public int play(int index) {
            currentTrackIndex = index;
            AudioFile audioFile = playList.get(currentTrackIndex);
            try {
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.setDataSource(audioFile.getFilePath());
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return mediaPlayer.getDuration();
        }
        public void playseekTo(int time){
            mediaPlayer.seekTo(time);
        }
        public void autoNext(){
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    next();
                }
            });
        }
        public int getTimeSB(){
            return mediaPlayer.getCurrentPosition();
        }
        public void play() {
            mediaPlayer.start();
        }

        public void pause() {
            mediaPlayer.pause();
        }

        public void next() {
            currentTrackIndex++;
            if (currentTrackIndex >= playList.size())
                currentTrackIndex = 0;
            play(currentTrackIndex);
        }

        public void back() {
            currentTrackIndex--;
            if (currentTrackIndex < 0)
                currentTrackIndex = playList.size() - 1;
            play(currentTrackIndex);
        }

        public void setPlayList(List<AudioFile> data) {
            MediaPlayerService.this.playList = data;
        }

        public List<AudioFile> getPlayList() {
            return playList;
        }

        public AudioFile getCurrentTrack() {
            return playList.get(currentTrackIndex);
        }

    }

}
