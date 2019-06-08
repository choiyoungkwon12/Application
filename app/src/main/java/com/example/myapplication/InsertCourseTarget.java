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

public class InsertCourseTarget {
    private URL url;

    public InsertCourseTarget(String url) throws MalformedURLException { this.url = new URL(url); }

    private String readStream(InputStream in) throws IOException {
        StringBuilder jsonHtml = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        String line = null;

        while((line = reader.readLine()) != null)
            jsonHtml.append(line);

        reader.close();
        return jsonHtml.toString();
    }

    public String PhPtest(Vector<RecordPoint> vector) {
        Iterator<RecordPoint> it = vector.iterator();
        String postData = "course="+CreateCourseActivity.tableNum + "&tag="+ CreateCourseActivity.tableNum + CreateCourseActivity.userID + "&data=";
        String target = "";
        String data = "";
        boolean start = true;
        try {
            while(it.hasNext()) {
                RecordPoint point = it.next();
                if(start == true) {
                    data += "(" + Integer.toString(point.pointNum) + "," + "\"" + point.time + "\"," + Float.toString(point.distance) + "," + Float.toString(point.speed)+")";
                    target += "(" + Integer.toString(point.pointNum) + "," + "\"" + point.time + "\")";
                    start = false;
                } else {
                    data += ", ("+ Integer.toString(point.pointNum) + ","+ "\"" + point.time + "\"," + point.distance + "," + point.speed+")";
                    target += ", (" + Integer.toString(point.pointNum) + "," + "\"" + point.time + "\")";
                }
            }
            postData += data + "&target=" + target;
            Log.i("InsertRecordData",postData);
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
}


