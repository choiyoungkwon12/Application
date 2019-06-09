package com.example.myapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPolyline;
import net.daum.mf.map.api.MapReverseGeoCoder;
import net.daum.mf.map.api.MapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;



public class CreateCourseActivity extends AppCompatActivity implements MapView.CurrentLocationEventListener, MapReverseGeoCoder.ReverseGeoCodingResultListener, MapView.POIItemEventListener, MapView.MapViewEventListener {

    private static final String LOG_TAG = "CreateCourseActivity";
    private static final String TAG = "CreateCourseActivity";
    private MapView mapView = null;

    static Vector<RecordPoint> recordVector = new Vector<>();
    private MapPolyline polyline = new MapPolyline();
    private long startTime = 0;
    private Button create_button, create_start_button, create_end_button, create_exit_button, save_course_button, drive_button, drive_start_button, drive_end_button, target_select_button, select_finish_button;
    private boolean driving = false;
    static String IP_ADDRESS = "cpbike.dothome.co.kr/cpbike";
    static float speed=0, distance=0;
    static String time="";
    private TextView time_textview, speed_text, moving_distance_text, remaining_distance_text, connection_status_textview, count_text;
    private ListView target_list;
    private int pointNum=0;
    private HashMap<Integer, Boolean> pOIItemSelectedStatus = new HashMap<>();
    static int selectedNum=0;
    private MapPolyline selectedPolyline = new MapPolyline();
    static Vector<String> targetIDvector = new Vector<>();
    private String selectedTarget="";
    static Vector<RecordPoint> selectedTargetRecord = new Vector<>();
    private HashMap<String, Integer> targetRecordHashMap = new HashMap<>();
    static Context mContext;
    static String userID="";
    static int tableNum=0;
    private DriveRecordDialog driveRecordDialog;
    private float sumSpeed = 0;
    private boolean deleteDriveTable = false;
    private float AverageSpeed = 0;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION};


    static Vector<PolylinePoint> targetRecordVector = new Vector<>();
    static Vector<PolylinePoint> postUserRecordVector = new Vector<>();
    static Vector<PolylinePoint> polylineVector = new Vector<>();

    private BluetoothManager mBtManager = null;
    private ConnectionInfo mConnectionInfo = null;      // Remembers connection info when BT connection is made
    private TransactionBuilder mTransactionBuilder = null;
    private TransactionReceiver mTransactionReceiver = null;

    private Vector<MapPoint> selectedPolylineVector = new Vector<>();
    private boolean targeting =false;
    private Set<String> keys;


    private BtHandler mHandler;
    static boolean checkUsedCourse = false;
    private int handleCount = 0;
    static Vector<TargetRecord> targetRecord = new Vector<>();
    private int primaryColor = Color.argb(255, 255, 0, 0);
    private int selectedColor = Color.argb(255, 0, 0, 255);
    private float courseDistance = 0;
    private int lodingCount = 0;
    private int escapeCheckCount = 0;
    private boolean creatingCheck = false;
    private HashMap<String, Integer> targetingHash = new HashMap<>();
    private boolean countOverlapCheck = false;
    private MapPoint[] selectPolylinePointArr;
    private int pointPosition = 0;
    private float minX=0, minY=0, maxX=0, maxY=0;
    static boolean checkUserTarget = false;
    private boolean gpsEnable = false;
    private ProgressDialog pd;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreate.Start");



        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();

        Intent intent = getIntent();

        showProgress("로딩중");

        setContentView(R.layout.activity_create_course);
        mapView = (MapView) findViewById(R.id.map_view);
        mapView.setCurrentLocationEventListener(this);
        if (!checkLocationServicesStatus()) {

            showDialogForLocationServiceSetting();
        }else {

            checkRunTimePermission();
        }

        mapView.setMapViewEventListener(this);
        mapView.setPOIItemEventListener(this);

        mapView.setZoomLevel(4, true);

        create_button = (Button) findViewById(R.id.create_button);
        drive_button = (Button) findViewById(R.id.drive_button);
        create_start_button = (Button) findViewById(R.id.create_start_button);
        create_end_button = (Button) findViewById(R.id.create_end_button);
        create_exit_button = (Button) findViewById(R.id.create_exit_button);
        save_course_button = (Button) findViewById(R.id.save_course_button);
        drive_start_button = (Button) findViewById(R.id.drive_start_button);
        drive_end_button = (Button) findViewById(R.id.drive_end_button);
        target_select_button = (Button) findViewById(R.id.target_select_button);
        select_finish_button = (Button) findViewById(R.id.select_finish_button);
        target_list = (ListView) findViewById(R.id.target_list);

        count_text = (TextView) findViewById(R.id.count_text);
        time_textview = (TextView) findViewById(R.id.time_textview);
        speed_text = (TextView) findViewById(R.id.speed_text);
        moving_distance_text = (TextView) findViewById(R.id.moving_distance_text);
        remaining_distance_text = (TextView) findViewById(R.id.remaining_distance_text);
        connection_status_textview = (TextView) findViewById(R.id.connection_status_textview);

        create_button.setVisibility(View.VISIBLE);
        drive_button.setVisibility(View.VISIBLE);
        drive_button.setEnabled(false); // 코스 선택 미완료 상태인 초기 코스 주행 버튼 비활성화(영권)

        mapView.setMapTilePersistentCacheEnabled(true);
        initialize();

        userID = intent.getExtras().getString("id");
        mHandler = new BtHandler();
        OutputCourse getData = new OutputCourse();
        getData.execute("http://" + IP_ADDRESS + "/OutputCourse.php");


    }
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        if ( permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면

            boolean check_result = true;


            // 모든 퍼미션을 허용했는지 체크합니다.

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }


            if ( check_result ) {
                Log.d("@@@", "start");
                //위치 값을 가져올 수 있음
                mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);
            }
            else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {

                    Toast.makeText(this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
                    finish();


                }else { //

                    Toast.makeText(this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_LONG).show();

                }
            }

        }
    }

    void checkRunTimePermission(){

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED ) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)


            // 3.  위치 값을 가져올 수 있음
            mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);


        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {

                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);


            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }

    }



    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
                        checkRunTimePermission();
                        return;
                    }
                }

                break;
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    public void showProgress(String str) {
        if( pd == null ) {
            pd = new ProgressDialog(this);
            pd.setCancelable(false);
        }

        pd.setMessage(str);
        pd.show();
    }
    public void hideProgress() {
        if (pd != null && pd.isShowing()) {
            pd.dismiss();
        }


    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
        mapView.setShowCurrentLocationMarker(false);
        finalize();
    }


    @Override
    public void onReverseGeoCoderFoundAddress(MapReverseGeoCoder mapReverseGeoCoder, String s) {

    }

    @Override
    public void onReverseGeoCoderFailedToFindAddress(MapReverseGeoCoder mapReverseGeoCoder) {

    }

    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint currentLocation, float accuracyInMeters) {
        LocationManager manager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
        if(manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            hideProgress();
        }

        String time = getTimeOut();
        if(driving == true) {


            MapPoint.GeoCoordinate mapPointGeo = currentLocation.getMapPointGeoCoord();
            float x = (float) (Math.round(mapPointGeo.latitude*100000000)/100000000.0);
            float y = (float) (Math.round(mapPointGeo.longitude*100000000)/100000000.0);



            if(!creatingCheck) {
                if(minX<=x&&x<=maxX && minY<=y&&y<=maxY) { // 선택된 폴리라인의 경로에 있을 시 수행
                    escapeCheckCount = 0; // 이탈 경고 횟수 초기화
                    if(speed > 200)
                        speed = polylineVector.get(pointNum-1).speed;

                    polylineVector.add(new PolylinePoint(pointNum, x, y, time, distance, speed));
                    pointNum++;


                    Log.i("onCurrentLocationUpdate", "pointNum : " + Integer.toString(pointNum));
                } else {
                    escapeCheckCount++; // 이탈 횟수 + 1
                    Log.i("이탈횟수", Integer.toString(escapeCheckCount));
                    // if(escapeCheck > 7) // 이탈 횟수 3회 초과 시 강제 종료
                    // Toast.makeText(this, "이탈 경고 횟수 : " + escapeCheck, Toast.LENGTH_SHORT).show(); // 사용자에게 이탈 횟수 알림
                    // if(deleteDriveTable) // 테이블 생성했을 시 삭제
//                     deleteRecordTable(tableNum, userID);
                }
            } else {
                pointNum++;
                polyline.addPoint(MapPoint.mapPointWithGeoCoord(x, y));
                if(speed > 200)
                    speed = polylineVector.get(pointNum-1).speed;
                polylineVector.add(new PolylinePoint(pointNum, x, y, time, distance, speed));

                Log.i("onCurrentLocationUpdate", "time : " + time);
                Log.i("onCurrentLocationUpdate", Integer.toString(polylineVector.size()));
                mapView.addPolyline(polyline);
                Log.i("onCurrentLocationUpdate", "update");
            }
            Log.i("onCurrentLocationUpdate", "time : " + time);









        }
     /*   if(targeting) {
            boolean dd = false;
            for(String key : keys) {

                String[] str = key.split(":");
                String[] str1 = getTimeOut().split(":");
                for(int i=0; i<str.length; i++) {
                    if(Integer.parseInt(str[i])<Integer.parseInt(str1[i])) {
                        dd =true;
                    } else if(!dd) {
                        mapView.removeAllPOIItems();
                        MapPOIItem targetCurrentPoint = new MapPOIItem();
                        Log.i("pointnum", Integer.toString(targetRecord.get(key)));
                        Log.i("time", key);
                        Log.i("vector size", Integer.toString(selectedPolylineVector.size()));
                        targetCurrentPoint.setMapPoint(selectedPolylineVector.get(targetRecord.get(key)));
                        targetCurrentPoint.setMarkerType(MapPOIItem.MarkerType.BluePin);
                        mapView.addPOIItem(targetCurrentPoint);
                        dd=false;

                    }
                }

            }
        }*/


    }

    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {

    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {

    }

    public void onClickCreate(View view) {
        creatingCheck = true;
        create_button.setVisibility(View.GONE);
        drive_button.setVisibility(View.GONE);
        create_start_button.setVisibility(View.VISIBLE);

        moving_distance_text.setVisibility(View.VISIBLE);
        speed_text.setVisibility(View.VISIBLE);
        time_textview.setVisibility(View.VISIBLE);

        mapView.removeAllPolylines();
        mapView.removeAllPOIItems();

        CreateCourseTable createCourseTable = new CreateCourseTable();
        createCourseTable.execute("http://" + IP_ADDRESS + "/CheckTable.php");
    }



    public void onClickCreateStart(View view) {
        create_start_button.setVisibility(View.GONE);
        create_end_button.setVisibility(View.VISIBLE);

        TimerTask tt = new TimerTask() {
            int count = 5;
            @Override
            public void run() {
                count_text.setText(Integer.toString(count));
                count--;
                if(count == 0)
                    cancel();
            }
        };

        Timer timer = new Timer();
        timer.schedule(tt, 0, 1000);

        startTime = SystemClock.elapsedRealtime();
        System.out.println(startTime);
        myTimer.sendEmptyMessage(0);
        driving = true;
        count_text.setVisibility(View.GONE);


    }


    public void onClickCreateEnd(View view) {

        create_end_button.setVisibility(View.GONE);
        create_exit_button.setVisibility(View.VISIBLE);
        save_course_button.setVisibility(View.VISIBLE);
        time_textview.setVisibility(View.GONE);
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
        mapView.setShowCurrentLocationMarker(false);

        Iterator<PolylinePoint> it = polylineVector.iterator();
        int count=1;
        while(it.hasNext()) {
            MapPOIItem tempMapPIItem = new MapPOIItem();

            tempMapPIItem.setMapPoint(it.next().getMapPoint());
            tempMapPIItem.setMarkerType(MapPOIItem.MarkerType.CustomImage);
            tempMapPIItem.setItemName(Integer.toString(count));
            tempMapPIItem.setCustomImageAnchorPointOffset(new MapPOIItem.ImageOffset(7,0));
            tempMapPIItem.setCustomImageResourceId(R.drawable.target_marker);
            tempMapPIItem.setDraggable(true);
            count++;
            mapView.addPOIItem(tempMapPIItem);
        }



        AverageSpeed = Math.round(sumSpeed / handleCount * 100) / 100;
        time = getTimeOut();

        myTimer.removeMessages(0);
        mapView.fitMapViewAreaToShowPolyline(polyline);
    }
    public void onClickSaveCourse(View view) {

        insertData(tableNum, userID, polylineVector);

        insertRanking(tableNum, userID, speed, distance, time);

        create_exit_button.setVisibility(View.GONE);
        save_course_button.setVisibility(View.GONE);

        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void onClickCreateExit(View view) {

        create_exit_button.setVisibility(View.GONE);
        save_course_button.setVisibility(View.GONE);
        deleteTable(tableNum, userID);

        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void onClickDrive(View view) {
        if(selectedNum==0) {
            Toast.makeText(this, "선택된 코스 없음", Toast.LENGTH_SHORT).show();
        } else {
            double tempX = selectedPolyline.getPoint(0).getMapPointGeoCoord().latitude;
            double tempY = selectedPolyline.getPoint(0).getMapPointGeoCoord().longitude;

            MapPoint geoPoint = mapView.getMapCenterPoint();
            float x = (float) (Math.round(geoPoint.getMapPointGeoCoord().latitude*100000000)/100000000.0);
            float y = (float) (Math.round(geoPoint.getMapPointGeoCoord().longitude*100000000)/100000000.0);

            if(tempX-0.0001 <= x && x <= tempX+0.0001 && tempY-0.0001 <= y && y <= tempY+0.0001) {
                tableNum = selectedNum;
                drive_button.setVisibility(View.GONE);
                create_button.setVisibility(View.GONE);
                target_select_button.setVisibility(View.VISIBLE);
                moving_distance_text.setVisibility(View.VISIBLE);
                speed_text.setVisibility(View.VISIBLE);
                time_textview.setVisibility(View.VISIBLE);
                remaining_distance_text.setVisibility(View.VISIBLE);

                RankingCheck rankingCheck = new RankingCheck();
                rankingCheck.execute("http://" + IP_ADDRESS + "/rankingCheck.php");

                mapView.removeAllPolylines();  // 190602
                mapView.removeAllPOIItems();

                mapView.addPolyline(selectedPolyline);

                Log.i("point개수", Integer.toString(selectedPolyline.getPointCount()));
                Log.i("pointTag", Integer.toString(selectedPolyline.getTag()));
            } else {
                Toast.makeText(this, "코스 시작 위치가 아님", Toast.LENGTH_SHORT).show();
            }

        }
    }

    public void onClickTargetSelect(View view) {
        select_finish_button.setVisibility(View.VISIBLE);
        target_select_button.setVisibility(View.GONE);
        drive_start_button.setVisibility(View.GONE);
        target_list.setVisibility(View.VISIBLE);

        List<String> list = new Vector<>();

        Iterator<TargetRecord> it = targetRecord.iterator();

        while(it.hasNext()) {
            TargetRecord temp = it.next();
            list.add(temp.id + "   "+temp.speed + "   " +temp.time);

        }
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, list);
        target_list.setAdapter(adapter);

        Log.i("onClickTargetSelect","ID개수" + list.size());

        target_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String str = (String) adapterView.getItemAtPosition(position);
                Log.i("position", Integer.toString(position));
                String[] strArr = str.split("   ");
                selectedTarget = "record" + tableNum + strArr[0];
                Log.i("onClickTargetSelect","targetName : " + selectedTarget);
            }
        });
    }

    public void onClickSelectFinish(View view) {
        select_finish_button.setVisibility(View.GONE);
        drive_start_button.setVisibility(View.VISIBLE);
        target_list.setVisibility(View.GONE);

        mapView.removeAllPolylines();

        selectedPolyline = new MapPolyline();

        getTargetRecord(selectedTarget);

        Iterator<TargetRecord> it = targetRecord.iterator();
        String selectedTargetID = selectedTarget.replace("record" + tableNum, "").trim();

        while (it.hasNext()) {
            TargetRecord temp = it.next();
            if (temp.id.equals(userID)) { // 코스 주행 기록 중 사용자 아이디 있을 시
                checkUsedCourse = true;
            }

        }
        if (selectedTargetID.equals(userID))  // 비교하는 대상이 본인 아이디 일 시
            checkUserTarget = true;
        if(checkUsedCourse && !checkUserTarget) {
            getPostUserRecord("record" + tableNum + userID);
        }

    }


    public void getPostUserRecord(String target) {
        int pointNum;
        float x, y, distance, speed;
        String time;

        try {
            OutputPostUserRecord request = new OutputPostUserRecord("http://" + IP_ADDRESS + "/OutputData.php");
            String result = request.PhPtest(target);

            JSONObject jObject = new JSONObject(result);
            JSONArray results = jObject.getJSONArray("results");

            for (int i = 0; i < results.length(); i++) {
                JSONObject temp = results.getJSONObject(i);
                pointNum = Integer.parseInt("" + temp.get("pointNum"));
                x = Float.parseFloat("" + temp.get("x"));
                y = Float.parseFloat("" + temp.get("y"));
                time = "" + temp.get("time");
                distance = Float.parseFloat("" + temp.get("distance"));
                speed = Float.parseFloat("" + temp.get("speed"));
                postUserRecordVector.add(new PolylinePoint(pointNum, x, y, time, distance, speed));
                Log.i("getPostUserRecord", Integer.toString(i));
            }

        } catch(MalformedURLException e){
            e.printStackTrace();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

    }
    public void getTargetRecord(String target) {
        int pointNum;
        float x, y, distance, speed;
        String time;

        try {
            OutputPostUserRecord request = new OutputPostUserRecord("http://" + IP_ADDRESS + "/OutputData.php");
            String result = request.PhPtest(target);

            JSONObject jObject = new JSONObject(result);
            JSONArray results = jObject.getJSONArray("results");

            for (int i = 0; i < results.length(); i++) {
                JSONObject temp = results.getJSONObject(i);
                pointNum = Integer.parseInt("" + temp.get("pointNum"));
                x = Float.parseFloat("" + temp.get("x"));
                y = Float.parseFloat("" + temp.get("y"));
                time = "" + temp.get("time");
                distance = Float.parseFloat("" + temp.get("distance"));
                speed = Float.parseFloat("" + temp.get("speed"));
                targetRecordVector.add(new PolylinePoint(pointNum, x, y, time, distance, speed));
                selectedPolyline.addPoint(MapPoint.mapPointWithGeoCoord(x, y));
                targetingHash.put(time, pointNum);
                Log.i("getTargetRecord", Integer.toString(i));
            }
            mapView.addPolyline(selectedPolyline);
        } catch(MalformedURLException e){
            e.printStackTrace();
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

    }



    public void onClickDriveStart(View view) {

        drive_start_button.setVisibility(View.GONE);
        drive_end_button.setVisibility(View.VISIBLE);
        Iterator<PolylinePoint> it = targetRecordVector.iterator();
        boolean first = false;
        while(it.hasNext()) {
            PolylinePoint temp = it.next();
            if(!first) {
                minX = temp.x;
                minY = temp.y;
                maxX = temp.x;
                maxY = temp.y;
                first = true;
            } else {
                if(minX>temp.x) {
                    minX = temp.x;
                }
                if(maxX<temp.x) {
                    maxX = temp.x;
                }
                if(minY>temp.y) {
                    minY = temp.y;
                }
                if(maxY<temp.y) {
                    maxY = temp.y;
                }
            }
        }
        Log.i("minX,maxX,minY,maxY",Float.toString(minX) + " " + maxX + " " + minY + " "+maxY);


        startTime = SystemClock.elapsedRealtime();
        myTimer.sendEmptyMessage(0);

        keys = targetingHash.keySet();
        selectPolylinePointArr =  selectedPolyline.getMapPoints();
        targeting = true;
        if(!checkUsedCourse) {
            try {
                CreateTable request = new CreateTable("http://" + IP_ADDRESS + "/CreateRecordTable.php");
                String result = request.PhPtest(tableNum);
                if(result.equals("1")){
                    Log.i("onClickDriveStart", "테이블 생성 성공");
                    deleteDriveTable = true;
                }
                else{
                    Log.i("onClickDriveStart", "테이블 생성 실패");
                }
            }catch (MalformedURLException e){
                e.printStackTrace();
            }
        }
        driving = true;

    }

    public void onClickDriveEnd(View view) {
        drive_end_button.setVisibility(View.GONE);
        time_textview.setVisibility(View.GONE);
        mapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
        mapView.setShowCurrentLocationMarker(false);

        speed = Math.round(sumSpeed / handleCount * 100) / 100;
        time = getTimeOut();

        myTimer.removeMessages(0);

        driveRecordDialog = new DriveRecordDialog(this, saveListener, exitListener);
        driveRecordDialog.show();
    }
    private View.OnClickListener saveListener = new View.OnClickListener() {
        public void onClick(View v) {
            insertRecordData(tableNum, userID, polylineVector);

            if(checkUsedCourse)
                changeRanking(tableNum, userID, speed, distance, time);
            else
                insertRanking(tableNum, userID, speed, distance, time);

            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            finish();

        }
    };
    public void changeRanking(int tableNum, String id, float speed, float distance, String time) {
        try {
            ChangeRanking request = new ChangeRanking("http://" + IP_ADDRESS + "/changeRanking.php");
            String result = request.PhPtest(tableNum, id, speed, distance, time);
            if(result.equals("1")){
                Log.i("ChangeRanking", "랭킹 데이터 수정 성공");
            }
            else{
                Log.i("ChangeRanking", "랭킹 데이터 수정 실패");
            }
        }catch (MalformedURLException e){
            e.printStackTrace();
        }
    }

    private View.OnClickListener exitListener = new View.OnClickListener() {
        public void onClick(View v) {


            if(deleteDriveTable)
                deleteRecordTable(tableNum, userID);
            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            finish();

        }
    };

    private void insertRanking(int courseNum, String id, float speed, float distance, String time) {
        try {
            InsertRanking request = new InsertRanking("http://" + IP_ADDRESS + "/insertRanking.php");
            String result = request.PhPtest(courseNum, id, speed, distance, time);
            if(result.equals("1")){
                Log.i("insertRanking", "랭킹 입력 성공");
            }
            else{
                Log.i("insertRanking", "랭킹 입력 실패");
            }
        }catch (MalformedURLException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {
        MapPolyline tempPolyline = new MapPolyline();
        MapPolyline tempSelectedPolyline = new MapPolyline();


        for(MapPolyline temp : mapView.getPolylines()) {
            if(temp.getTag() != Integer.parseInt(mapPOIItem.getItemName())) {

                MapPolyline selectedPolyline1 = new MapPolyline();
                selectedNum = temp.getTag(); // tempNum <- 선택된 poiitemtag

                selectedPolyline1.addPoints(temp.getMapPoints());
                mapView.removePolyline(mapView.findPolylineByTag(selectedNum));
                selectedPolyline1.setLineColor(primaryColor);
                selectedPolyline1.setTag(selectedNum);

                mapView.addPolyline(selectedPolyline1);
            }
        }




        selectedNum = Integer.parseInt(mapPOIItem.getItemName());
        tempPolyline = mapView.findPolylineByTag(selectedNum);
        tempSelectedPolyline.addPoints(tempPolyline.getMapPoints());

        mapView.removePolyline(mapView.findPolylineByTag(selectedNum));
        tempSelectedPolyline.setLineColor(selectedColor);
        tempSelectedPolyline.setTag(selectedNum);
        selectedPolyline = tempSelectedPolyline;
        mapView.addPolyline(tempSelectedPolyline);

        for(MapPoint mp : tempSelectedPolyline.getMapPoints()) {
            selectedPolylineVector.add(mp);
        }


        Toast.makeText(this, "코스 선택 완료", Toast.LENGTH_SHORT).show();


        Log.i("폴리라인 갯수",Integer.toString(mapView.getPolylines().length));

        drive_button.setEnabled(true); // 코스 주행 버튼 활성화(영권)




    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {

    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {

    }

    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) { // 생성 코스 수정

        int item = Integer.parseInt(mapPOIItem.getItemName());
        MapPoint.GeoCoordinate temp = mapPoint.getMapPointGeoCoord();
        float x = (float) (Math.round(temp.latitude*100000000)/100000000.0);
        float y = (float) (Math.round(temp.longitude*100000000)/100000000.0);
        PolylinePoint tempItem = polylineVector.get(item-1);
        PolylinePoint tempPoint = new PolylinePoint(item, x, y, tempItem.time, tempItem.distance, tempItem.speed);
        polylineVector.remove(item-1);
        polylineVector.add(item-1, tempPoint);
        Log.i("Polyline",Integer.toString(polyline.getPointCount()));
        mapView.removePolyline(polyline);
        polyline = new MapPolyline();
        Iterator<PolylinePoint> it = polylineVector.iterator();

        while(it.hasNext())
            polyline.addPoint(it.next().getMapPoint());

        mapView.addPolyline(polyline);
    }

    @Override
    public void onMapViewInitialized(MapView mapView) {

    }

    @Override
    public void onMapViewCenterPointMoved(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewZoomLevelChanged(MapView mapView, int i) {

    }

    @Override
    public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDoubleTapped(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint) {

    }

    private void insertData(int tableNum, String id, Vector<PolylinePoint> vector) {
        try {
            InsertData request = new InsertData("http://" + IP_ADDRESS + "/InsertData.php");
            String result = request.PhPtest(tableNum, id, vector);
            if(result.equals("1")){
                Log.i("insertData", "데이터 저장 성공");
            }
            else{
                Log.i("insertData", "데이터 저장 실패");
            }
        }catch (MalformedURLException e){
            e.printStackTrace();
        }
    }
    private void insertRecordData(int tableNum, String id, Vector<PolylinePoint> vector) {
        try {
            InsertRecordData request = new InsertRecordData("http://" + IP_ADDRESS + "/InsertRecordData.php");
            String result = request.PhPtest(tableNum, id, vector);
            if(result.equals("1")){
                Log.i("insertCourseData", "기록 데이터 저장 성공");
            }
            else{
                Log.i("insertCourseData", "기록 데이터 저장 실패");
            }
        }catch (MalformedURLException e){
            e.printStackTrace();
        }
    }

    private void deleteTable(int tableNum, String id) {
        try {
            DeleteTable request = new DeleteTable("http://" + IP_ADDRESS + "/DeleteTable.php");
            String result = request.PhPtest(tableNum, id);
            if(result.equals("1")){
                Log.i("deleteTable", "테이블 삭제 성공");
            }
            else{
                Log.i("deleteTable", "테이블 삭제 실패");
            }
        }catch (MalformedURLException e){
            e.printStackTrace();
        }
    }
    private void deleteRecordTable(int tableNum, String id) {
        try {
            DeleteTable request = new DeleteTable("http://" + IP_ADDRESS + "/DeleteRecordTable.php");
            String result = request.PhPtest(tableNum, id);
            if(result.equals("1")){
                Log.i("deleteRecordTable", "기록 테이블 삭제 성공");
            }
            else{
                Log.i("deleteRecordTable", "기록 테이블 삭제 실패");
            }
        }catch (MalformedURLException e){
            e.printStackTrace();
        }
    }


    class OutputCourse extends AsyncTask<String, Integer, String> {

        int point_num;
        float x;
        float y;



        @Override
        protected String doInBackground(String... params) {

            StringBuilder jsonHtml = new StringBuilder();
            try {
                URL phpUrl = new URL(params[0]);
                HttpURLConnection conn = (HttpURLConnection) phpUrl.openConnection();

                if (conn != null) {
                    conn.setConnectTimeout(10000);
                    conn.setUseCaches(false);

                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                        while (true) {
                            String line = br.readLine();
                            if (line == null)
                                break;
                            jsonHtml.append(line + "\n");
                        }
                        br.close();
                    }
                    conn.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return jsonHtml.toString();
        }

        @Override
        protected void onPostExecute(String str) {
            String[] arr = str.split("&&");

            for(int j=0; j<arr.length; j++) {

                MapPolyline polylineTemp = new MapPolyline();

                try {
                    JSONObject jObject = new JSONObject(arr[j]);
                    JSONArray results = jObject.getJSONArray("results");
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject temp = results.getJSONObject(i);

                        if(temp==null)
                            continue;


                        x = Float.parseFloat("" + temp.get("x"));
                        y = Float.parseFloat("" + temp.get("y"));

                        polylineTemp.addPoint(MapPoint.mapPointWithGeoCoord(x, y));

                        if(i==0) {

                            MapPOIItem tempMapPIItem = new MapPOIItem();
                            tempMapPIItem.setMapPoint(MapPoint.mapPointWithGeoCoord(x, y));
                            tempMapPIItem.setItemName(Integer.toString(j+1));

                            tempMapPIItem.setMarkerType(MapPOIItem.MarkerType.BluePin);
                            mapView.addPOIItem(tempMapPIItem);
                            polylineTemp.setTag(j+1);
                            polylineTemp.setLineColor(primaryColor);
                            mapView.addPolyline(polylineTemp);

                        }
                    }



                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            for(MapPolyline temp1 : mapView.getPolylines())
                pOIItemSelectedStatus.put(temp1.getTag(), false);


        }
    }

    Handler myTimer = new Handler(){
        public void handleMessage(Message msg){
            time_textview.setText(getTimeOut());
            myTimer.sendEmptyMessage(0);
            String st=getTimeOut();


            if(targeting&&keys.contains(st)) {

                mapView.removeAllPOIItems();

                MapPOIItem targetCurrentPoint = new MapPOIItem();
                targetCurrentPoint.setMarkerType(MapPOIItem.MarkerType.CustomImage);
                targetCurrentPoint.setCustomImageAnchorPointOffset(new MapPOIItem.ImageOffset(7,0));
                targetCurrentPoint.setCustomImageResourceId(R.drawable.revise_marker);
                targetCurrentPoint.setMapPoint(selectedPolyline.getPoint(targetingHash.get(st)-1));
                targetCurrentPoint.setItemName("f");
                targetCurrentPoint.setTag(3);
                mapView.addPOIItem(targetCurrentPoint);




            }
        }
    };

    String getTimeOut(){
        long now = SystemClock.elapsedRealtime();
        long outTime = now - startTime;
        String easy_outTime = String.format("%02d:%02d:%02d", outTime/1000 / 60, (outTime/1000)%60,(outTime%1000)/10);
        return easy_outTime;
    }

    private void initialize() {
        // Make instances
        mConnectionInfo = ConnectionInfo.getInstance(mContext);

        mHandler = new BtHandler();
        mBtManager = BluetoothManager.getInstance(mContext, mHandler);
        if(mBtManager != null)
            mBtManager.setHandler(mHandler);

        // Get local Bluetooth adapter
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            return;
        }
    }

    public void finalize() {
        // Stop the bluetooth session
        if (mBtManager != null) {
            //mBtManager.stop();
            mBtManager.setHandler(null);
        }
        mBtManager = null;
        mContext = null;
        mConnectionInfo = null;
    }


    class BtHandler extends Handler {////
        public void handleMessage(Message msg) {
            switch(msg.what) {
                // Received packets from remote
                case BluetoothManager.MESSAGE_READ:
                    Log.d(TAG, "BT - MESSAGE_READ: ");

                    byte[] readBuf = (byte[]) msg.obj;
                    int readCount = msg.arg1;
                    if(msg.arg1 > 0) {
                        String strMsg = new String(readBuf, 0, msg.arg1);
                        Log.d("str-----------", strMsg + "--------------");

                        String str[] = strMsg.split("bt");
                        speed = Float.parseFloat(str[1].replace("km/h","").trim());
                        sumSpeed += speed;
                        handleCount++;
                        if(str[0].contains("km"))
                            distance += Float.parseFloat(str[0].replace("km","").trim()) * 1000.0d;
                        else
                            distance += Float.parseFloat(str[0].replace("m","").trim());

                        Log.i("distance",Float.toString(distance));
                        moving_distance_text.setText("거리 " + str[0]);
                        speed_text.setText("속도  " + str[1]);


                        break;
                    }
                case BluetoothManager.MESSAGE_TOAST:
                    Log.d(TAG, "BT - MESSAGE_TOAST: ");

                    Toast.makeText(mContext,
                            msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;

            }   // End of switch(msg.what)
            super.handleMessage(msg);
        }
    }
}