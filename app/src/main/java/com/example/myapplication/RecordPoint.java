package com.example.myapplication;

public class RecordPoint {
    int pointNum;
    String time;
    float distance;
    float speed;

    RecordPoint(int pointNum, String time, float distance, float speed) {
        this.pointNum = pointNum;
        this.time = time;
        this.distance = distance;
        this.speed = speed;
    }
    RecordPoint(int pointNum, String time) {
        this.pointNum = pointNum;
        this.time = time;

    }
}
