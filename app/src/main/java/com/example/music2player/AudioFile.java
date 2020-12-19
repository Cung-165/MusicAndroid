package com.example.music2player;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AudioFile implements Parcelable {
    String filename;
    String filePath;
    int duration;
    public AudioFile(){}
    public AudioFile(String filename, String filePath, int duration) {
        this.filename = filename;
        this.filePath = filePath;
        this.duration = duration;
    }

    protected AudioFile(Parcel in) {
        filename = in.readString();
        filePath = in.readString();
        duration = in.readInt();
    }

    public static final Creator<AudioFile> CREATOR = new Creator<AudioFile>() {
        @Override
        public AudioFile createFromParcel(Parcel in) {
            return new AudioFile(in);
        }

        @Override
        public AudioFile[] newArray(int size) {
            return new AudioFile[size];
        }
    };

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public String toString() {
        return this.filename;
    }

    public String getDurationString() {
        int fDuration = this.duration/1000;
        String strDouble = String.valueOf(fDuration/60) + ':' + String.valueOf(fDuration%60);
        return strDouble;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(filename);
        dest.writeString(filePath);
        dest.writeInt(duration);
    }

    public static class Login extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_login);

            final EditText user,pass;
            Button log,back;

            log=findViewById(R.id.Login);
            user=findViewById(R.id.user);
            pass=findViewById(R.id.password);
            back=findViewById(R.id.back);

            log.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String a=user.getText().toString();
                    String b=pass.getText().toString();
                    if(a.equals("admin") && b.equals("160520")){
                        Intent intent=new Intent();
                        setResult(RESULT_OK,intent);
                        finish();
                    }
                    else{
                        Toast.makeText(Login.this,"Login fail",Toast.LENGTH_SHORT).show();
                    }
                }
            });
            back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }
}
