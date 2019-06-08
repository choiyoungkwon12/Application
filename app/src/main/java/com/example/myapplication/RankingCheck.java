package com.example.myapplication;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

class RankingCheck extends AsyncTask<String, Integer, String> {


    @Override
    protected String doInBackground(String... params) {
        StringBuilder jsonHtml = new StringBuilder();
        try {
            URL phpUrl = new URL(params[0]);
            HttpURLConnection conn = (HttpURLConnection)phpUrl.openConnection();

            if ( conn != null ) {
                conn.setConnectTimeout(10000);
                conn.setUseCaches(false);

                if ( conn.getResponseCode() == HttpURLConnection.HTTP_OK ) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    while ( true ) {
                        String line = br.readLine();
                        if ( line == null )
                            break;
                        jsonHtml.append(line + "\n");
                    }
                    br.close();
                }
                conn.disconnect();
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return jsonHtml.toString();
    }

    @Override
    protected void onPostExecute(String str) {
        int courseNum;
        String id, time;
        float speed, distance;

        try {
            // PHP에서 받아온 JSON 데이터를 JSON오브젝트로 변환
            JSONObject jObject = new JSONObject(str);
            // results라는 key는 JSON배열로 되어있다.
            JSONArray results = jObject.getJSONArray("results");

            for (int i=0; i < results.length(); i++) {

                JSONObject temp = results.getJSONObject(i);
                courseNum = Integer.parseInt("" + temp.get("courseNum"));
                id = "" + temp.get("ID");
                speed = Float.parseFloat("" + temp.get("speed"));
                distance = Float.parseFloat("" + temp.get("distance"));
                time = "" + temp.get("time");

                if(courseNum == CreateCourseActivity.tableNum) {
                    TargetRecord targetRecord = new TargetRecord(courseNum, id, time, speed, distance);
                    CreateCourseActivity.targetRecord.add(targetRecord);
                }

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}