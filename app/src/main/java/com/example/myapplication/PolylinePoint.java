package com.example.myapplication;

import net.daum.mf.map.api.MapPoint;

public class PolylinePoint {
    int pointNum;
    float x, y, distance, speed;
    String time;

    PolylinePoint(int pointNum, float x, float y, String time, float distance, float speed) {
        this.pointNum = pointNum;
        this.x = x;
        this.y = y;
        this.time = time;
        this.distance = distance;
        this.speed = speed;
    }
    MapPoint getMapPoint() {
        return MapPoint.mapPointWithGeoCoord(x, y);
    }

}
