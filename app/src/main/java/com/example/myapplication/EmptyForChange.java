package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


public class EmptyForChange extends AppCompatActivity {


    private static final String TAG = "EmptyForChange";
    private String id = "";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Log.i(TAG, id);
        id = intent.getExtras().getString("id");
        intent = new Intent(getApplicationContext(), CreateCourseActivity.class);
        intent.putExtra("id", id);
        startActivity(intent);
        finish();
    }




}