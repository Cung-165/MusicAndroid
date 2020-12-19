package com.example.music2player;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private boolean check_permission=false;
    private int po,duration;
    Uri uri;
    String songUri;
    String songname;
    ListView listView;
    TextView tv_title, tv_timeCurrent, tv_timeEnd;
    ImageButton bt_upload;



    List<AudioFile> data;
    ArrayAdapter<AudioFile> adapter;

    Boolean stt = false;
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
                adapter.notifyDataSetChanged();
                AudioFile audioFile = serviceController.getCurrentTrack();
                tv_title.setText(audioFile.getFilename());
            } else {
                loadAudioFile();
                receiveSong();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isSeverBound = false;
        }
    };

    private void receiveSong() {
        DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference("song");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds:dataSnapshot.getChildren()){
                    AudioFile songObj=ds.getValue(AudioFile.class);
                    data.add(songObj);
                }
                serviceController.setPlayList(data);
                adapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        tv_timeCurrent = findViewById(R.id.tv_timeCurrent);
        tv_title = findViewById(R.id.tv_title);
        tv_timeEnd = findViewById(R.id.tv_timeEnd);


        bt_upload=findViewById(R.id.upload);
        check_internet();
        data = new ArrayList<>();

        adapter = new ArrayAdapter<>(
                this,
                R.layout.song_layout,
                R.id.song_title,
                data
        );

        listView.setAdapter(adapter);



        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


                Intent intent=new Intent(MainActivity.this,playMusic.class);
                Bundle bundle=new Bundle();
                bundle.putInt("positon",position);

                bundle.putParcelableArrayList("listMusics", (ArrayList<? extends Parcelable>) data);
                intent.putExtra("all",bundle);
                startActivity(intent);

            }
        });


        bt_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this, AudioFile.Login.class);
                startActivityForResult(intent,165);

            }
        });
    }
    public void check_internet(){
        if(!checkConectIn()){
            AlertDialog.Builder builder=new AlertDialog.Builder(this);
            builder.setMessage("Not connect Internet");
            builder.show();
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
        }
    }
    private Boolean checkConectIn(){
        ConnectivityManager connectivityManager=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState()== NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState()==NetworkInfo.State.CONNECTED){
            return true;
        }
        else{
            return false;
        }
    }



    private void pickSong() {

        Intent intent=new Intent();
        intent.setType("audio/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode==1){
            if(resultCode==RESULT_OK){
                uri=data.getData();
                Cursor cursor=getApplicationContext().getContentResolver().query(uri,null,null,null,null);
                int indexname=cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                duration=findSongduration(uri);
                cursor.moveToFirst();
                songname=cursor.getString(indexname);

                cursor.close();

                uploadtoFirebase();
            }

        }
        if(requestCode==165){
            if (resultCode==RESULT_OK){

                if(valiadatePermision()){
                    pickSong();
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private int findSongduration(Uri uri) {
        int timeMilion=0;
        try{
            MediaMetadataRetriever retriever=new MediaMetadataRetriever();
            retriever.setDataSource(this,uri);
            String time=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            timeMilion=Integer.parseInt(time);

            retriever.release();
            return timeMilion;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void uploadtoFirebase() {
        StorageReference storageReference= FirebaseStorage.getInstance().getReference().child("Song").child(uri.getLastPathSegment());
        final ProgressDialog progressDialog=new ProgressDialog(this);
        progressDialog.show();

        storageReference.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri> uriTask=taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isComplete());
                Uri urisong=uriTask.getResult();
                songUri=urisong.toString();

                uploadDetail();
                progressDialog.dismiss();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this,"Fail",Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                double progess=(100.0*taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();
                int currentPro=(int) progess;
                progressDialog.setMessage("Upload: "+currentPro+"%");
            }
        });

    }

    private void uploadDetail() {

        AudioFile songObj=new AudioFile(songname,songUri,duration);
        FirebaseDatabase.getInstance().getReference("song").push().setValue(songObj).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(MainActivity.this,"Song uploaded + time: "+duration,Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this,"Fail",Toast.LENGTH_SHORT).show();
            }
        });
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
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        } else {
            String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
            String[] projection = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DURATION
            };

            Cursor cursor = getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    null
            );
            while (cursor.moveToNext()) {
                AudioFile audioFile = new AudioFile(
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)),
                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)),
                        cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                );
                data.add(audioFile);
            }
            cursor.close();
            serviceController.setPlayList(data);
            adapter.notifyDataSetChanged();
        }
    }

    private boolean valiadatePermision(){
        Dexter.withActivity(MainActivity.this).withPermission(Manifest.permission.READ_EXTERNAL_STORAGE).withListener(new PermissionListener() {
            @Override
            public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                check_permission=true;
            }

            @Override
            public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                check_permission=false;
            }

            @Override
            public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                permissionToken.continuePermissionRequest();
            }
        }).check();
        return check_permission;
    }
}
