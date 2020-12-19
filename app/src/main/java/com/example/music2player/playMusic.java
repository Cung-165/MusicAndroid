package com.example.music2player;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class playMusic extends AppCompatActivity {
    int pos;
    ImageButton btn_play, btn_back, btn_next, btn_list, btn_like, btn_reply, btn_set,bt_upload,out;
    SeekBar seekBar;
    TextView tv_title, tv_timeCurrent, tv_timeEnd;
    List<AudioFile> data;
    Animation animation;
    ImageView dis;

    Boolean stt = true;
    Boolean isSeverBound = false;
    MediaPlayerService.ServiceController serviceController;
    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("MainActivity", "onServiceConnected");

            isSeverBound = true;
            serviceController = (MediaPlayerService.ServiceController) service;

            List<AudioFile> playList = serviceController.getPlayList();
            if (playList.size() > 0){
                data.clear();
                data.addAll(playList);


                tv_title.setText(data.get(pos).getFilename());
                tv_timeEnd.setText(data.get(pos).getDurationString());
                seekBar.setMax(data.get(pos).getDuration());
                serviceController.play(pos);

                UptimeSong(pos);
            } else {
               loadAudioFile();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isSeverBound = false;
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_play_music);
        out =findViewById(R.id.backOut);
        tv_timeCurrent = findViewById(R.id.tv_timeCurrent);
        tv_title = findViewById(R.id.tv_title);
        tv_timeEnd = findViewById(R.id.tv_timeEnd);
        btn_back = findViewById(R.id.btn_back);
        btn_play = findViewById(R.id.btn_play);
        btn_next = findViewById(R.id.btn_next);
        btn_list = findViewById(R.id.btn_list);
        btn_like = findViewById(R.id.btn_like);
        btn_reply = findViewById(R.id.btn_reply);
        btn_set = findViewById(R.id.btn_set);
        seekBar = findViewById(R.id.seekBar);
        bt_upload=findViewById(R.id.upload);
        tv_title = findViewById(R.id.tv_title);
        animation= AnimationUtils.loadAnimation(this,R.anim.rotate);
        dis=findViewById(R.id.disc);

        data=new ArrayList<>();

        Intent intent=getIntent();
        Bundle bundle=intent.getBundleExtra("all");
        data = bundle.getParcelableArrayList("listMusics");
        pos=bundle.getInt("positon");

        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stt = !stt;
                if (stt==false) {
                    btn_play.setImageResource(R.drawable.start);
                    serviceController.pause();
                } else {
                    btn_play.setImageResource(R.drawable.pause);
                    serviceController.play();
                }
                dis.startAnimation(animation);
            }
        });
        btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serviceController.back();
                pos--;
                if(pos<0){
                    pos=data.size()-1;
                }
                tv_title.setText(data.get(pos).getFilename());
                tv_timeEnd.setText(data.get(pos).getDurationString());
                seekBar.setMax(data.get(pos).getDuration());
            }
        });
        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serviceController.next();
                pos++;
                if(pos>=data.size()){
                    pos=0;
                }
                tv_title.setText(data.get(pos).getFilename());
                tv_timeEnd.setText(data.get(pos).getDurationString());
                seekBar.setMax(data.get(pos).getDuration());
                UptimeSong(pos);
            }
        });
        out.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                serviceController.playseekTo(seekBar.getProgress());
            }
        });
    }

    private void UptimeSong(final int p) {
        final Handler handler=new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat time=new SimpleDateFormat("mm:ss");
                tv_timeCurrent.setText(time.format(serviceController.getTimeSB()));
                seekBar.setProgress(serviceController.getTimeSB());
                serviceController.autoNext();
                handler.postDelayed(this,500);

            }
        },100);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, MediaPlayerService.class);
        startService(intent);

        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadAudioFile();
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();
        }
    }

    private void loadAudioFile() {
        serviceController.setPlayList(data);
    }
}
