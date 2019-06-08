package com.example.myapplication;

public class TargetRecord {
    int courseNum;
    String id, time;
    float speed, distance;

    TargetRecord(int courseNum, String id, String time, float speed, float distance) {
        this.courseNum = courseNum;
        this.id = id;
        this.time = time;
        this.speed = speed;
        this.distance = distance;
    }
    TargetRecord(int courseNum, String time) {
        this.courseNum = courseNum;

        this.time = time;

    }
}
