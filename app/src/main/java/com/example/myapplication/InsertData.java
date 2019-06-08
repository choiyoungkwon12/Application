package com.example.myapplication;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;

public class InsertData {
    private URL url;

    public InsertData(String url) throws MalformedURLException { this.url = new URL(url); }

    private String readStream(InputStream in) throws IOException {
        StringBuilder jsonHtml = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        String line = null;

        while((line = reader.readLine()) != null)
            jsonHtml.append(line);

        reader.close();
        return jsonHtml.toString();
    }

    public String PhPtest(int tableNum, String id, Vector<PolylinePoint> vector) {
        Iterator<PolylinePoint> it = vector.iterator();
        String courseData = "";
        String postData = "course="+ Integer.toString(tableNum) + "&id="+ id + "&data=";

        boolean start = true;
        try {
            while(it.hasNext()) {
                PolylinePoint point = it.next();
                if(start == true) {
                    postData += "(" + Integer.toString(point.pointNum) + "," + Float.toString(point.x) + "," + Float.toString(point.y) + "," + strChange(point.time) +","+ Float.toString(point.distance) + "," + Float.toString(point.speed) + ")";
                    courseData += "(" + Integer.toString(point.pointNum) + "," + Float.toString(point.x) + "," + Float.toString(point.y) + ")";
                    start = false;
                } else {
                    postData += ", ("  + Integer.toString(point.pointNum) + ","+ Float.toString(point.x) + "," + Float.toString(point.y) + "," + strChange(point.time) +","+ Float.toString(point.distance) + "," + Float.toString(point.speed) + ")";
                    courseData += ", (" + Integer.toString(point.pointNum) + "," + Float.toString(point.x) + "," + Float.toString(point.y) + ")";
                }
            }
            postData += "&courseData=" + courseData;
            Log.i("InsertData", postData);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(postData.getBytes("UTF-8"));
            outputStream.flush();
            outputStream.close();
            String result = readStream(conn.getInputStream());
            conn.disconnect();
            return result;
        }
        catch (Exception e) {
            Log.i("PHPRequest", "request was failed.");
            return null;
        }
    }
    public String strChange(String str){
        return "\"" + str + "\"";
    }
}


