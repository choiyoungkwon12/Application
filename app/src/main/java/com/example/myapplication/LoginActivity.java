package com.example.myapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;




public class LoginActivity extends AppCompatActivity {
    private final String TAG = "LoginActivity";
    private Button login_button, join_user_button;
    private TextView announcement;
    private EditText id_text, pw_text;
    private String IP_ADDRESS = "cpbike.dothome.co.kr/cpbike";
    private boolean loginCheck = false;
    private int button_id=0;
    private String pw;
    private String id;
    private Context mContext;
    private BluetoothManager mBtManager;
    private ConnectionInfo mConnectionInfo;
    private TransactionBuilder mTransactionBuilder;
    private BtHandler mBtHandler;
    private String mBtStatusString;
    private int mBtStatus = BluetoothManager.STATE_NONE;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        NetworkUtil.setNetworkPolicy();

        setContentView(R.layout.activity_login);

        login_button = (Button) findViewById(R.id.login_button);
        join_user_button = (Button) findViewById(R.id.join_user_button);
        id_text = (EditText) findViewById(R.id.id_text);
        pw_text = (EditText) findViewById(R.id.pw_text);
        announcement = (TextView) findViewById(R.id.announcement);

        mContext = getApplicationContext();

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);
        } else {
            setupBT();
        }
        initialize();
    }

    @Override
    public void onResume() {
        super.onResume();

        if(mBtManager != null) {
            mBtStatus = mBtManager.getState();
            if(mBtHandler != null)
                mBtManager.setHandler(mBtHandler);
        }
        showBtStatus();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        finalize();
    }

    public void onClickJoinUser(View view) {
        button_id = 2;
        CheckIDPW getData = new CheckIDPW();
        getData.execute("http://" + IP_ADDRESS + "/CheckIDPW.php");
    }

    public void onClickLogin(View view) {
        button_id = 1;
        CheckIDPW getData = new CheckIDPW();
        getData.execute("http://"+IP_ADDRESS+"/CheckIDPW.php");

    }
    class CheckIDPW extends AsyncTask<String, Integer, String> {

        String ID;
        String PW;

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
            id = id_text.getText().toString();
            pw = pw_text.getText().toString();
                try {
                    JSONObject jObject = new JSONObject(str);
                    JSONArray results = jObject.getJSONArray("results");
                    for (int i = 0; i < results.length(); i++) {

                        JSONObject temp = results.getJSONObject(i);
                        ID = "" + temp.get("ID");
                        PW = "" + temp.get("PW");
                        switch(button_id) {
                            case 1: {
                                if(id.equals(ID)&&pw.equals(PW)) {
                                    Toast.makeText(LoginActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(getApplicationContext(), EmptyForChange.class);
                                    intent.putExtra("id", id);
                                    startActivity(intent);

                                } else if(i==(results.length())) {
                                    Toast.makeText(LoginActivity.this, "ID가 없습니다.", Toast.LENGTH_SHORT).show();
                                }
                                break;
                            }
                            case 2: {
                                if(id.equals(ID)) {
                                    Toast.makeText(LoginActivity.this, "ID가 중복됨", Toast.LENGTH_SHORT).show();
                                    break;
                                } else if(i==(results.length()-1)){
                                    try {

                                        String idpw = "(\""+id+"\""+","+"\""+pw+"\")";
                                        Log.i("idpw", "" + idpw);
                                        CreateUser request = new CreateUser("http://" + IP_ADDRESS + "/CreateUser.php");
                                        String result = request.PhPtest(idpw);
                                        if(result.equals("1")){
                                            Toast.makeText(LoginActivity.this, "회원가입에 성공했습니다.", Toast.LENGTH_SHORT).show();
                                        }
                                        else{
                                            Toast.makeText(LoginActivity.this, "회원가입에 실패했습니다.", Toast.LENGTH_SHORT).show();
                                        }
                                    }catch (MalformedURLException e){
                                        e.printStackTrace();
                                    }
                                }
                                break;
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    public void setupBT() {
        // Initialize the BluetoothManager to perform bluetooth connections
        if(mBtManager == null)
            mBtManager = BluetoothManager.getInstance(this, mBtHandler);
        Intent intent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(intent, Constants.REQUEST_CONNECT_DEVICE);
    }
    private void showBtStatus() {
        switch (mBtStatus) {
            case BluetoothManager.STATE_NONE:
                mBtStatusString = "none";

                break;
            case BluetoothManager.STATE_LISTEN:
                mBtStatusString = "listening";
                break;
            case BluetoothManager.STATE_CONNECTING:
                mBtStatusString = "connecting";

                break;
            case BluetoothManager.STATE_CONNECTED:
                mBtStatusString = "connected";
                break;
        }
        String status = new String();
        status += "BT controller : ";
        status += mBtStatusString;
        Toast.makeText(this, status, Toast.LENGTH_LONG).show();
    }
    private void connectDevice(String address) {
        Log.d(TAG, "Service - connect to " + address);

        // Get the BluetoothDevice object
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter != null) {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            if(device != null && mBtManager != null) {
                mBtManager.connect(device);
            }
        }
    }
    private void connectDevice(BluetoothDevice device) {
        if(device != null && mBtManager != null) {
            mBtManager.connect(device);
        }
    }
    public void finalize() {
        // Stop the bluetooth session
        if (mBtManager != null) {
            mBtManager.stop();
            mBtManager.setHandler(null);
        }
        mBtManager = null;
        mContext = null;
        mConnectionInfo = null;
    }
    private void initialize() {
        // Make instances
        mBtHandler = new BtHandler();

        mConnectionInfo = ConnectionInfo.getInstance(mContext);

        // Get local Bluetooth adapter
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, Constants.REQUEST_ENABLE_BT);

        }
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult " + resultCode);

        switch(requestCode) {
            case Constants.REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled
                } else {
                    // User did not enable Bluetooth or an error occured
                    Log.e(TAG, "BT is not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                }
                break;
            case Constants.REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Attempt to connect to the device
                    if(address != null) {
                        if(mConnectionInfo != null)
                            mConnectionInfo.setDeviceAddress(address);
                        connectDevice(address);
                    }
                }
                break;
        }	// End of switch(requestCode)
    }
    class BtHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            switch(msg.what) {
                // Bluetooth state changed
                case BluetoothManager.MESSAGE_STATE_CHANGE:
                    // Bluetooth state Changed
                    Log.d(TAG, "Service - MESSAGE_STATE_CHANGE: " + msg.arg1);

                    switch (msg.arg1) {
                        case BluetoothManager.STATE_NONE:
                            mBtStatus = BluetoothManager.STATE_NONE;

                            showBtStatus();
                            break;
                        case BluetoothManager.STATE_LISTEN:
                            mBtStatus = BluetoothManager.STATE_LISTEN;
                            showBtStatus();
                            break;
                        case BluetoothManager.STATE_CONNECTING:
                            mBtStatus = BluetoothManager.STATE_CONNECTING;
                            showBtStatus();

                            break;
                        case BluetoothManager.STATE_CONNECTED:
                            mBtStatus = BluetoothManager.STATE_CONNECTED;
                            showBtStatus();

                            break;
                    }
                    break;

                // If you want to send data to remote
                case BluetoothManager.MESSAGE_WRITE:
                    break;

                // Received packets from remote
                case BluetoothManager.MESSAGE_READ:
                    Log.d(TAG, "BT - MESSAGE_READ: ");
                    byte[] readBuf = (byte[]) msg.obj;
                    int readCount = msg.arg1;
                    if(msg.arg1 > 0) {
                        String strMsg = new String(readBuf, 0, msg.arg1);
                        // parse string
                        if(strMsg.contains("b")) {
                            Intent intent = new Intent(LoginActivity.this, EmptyForChange.class);
                            startActivity(intent);
                        } else if(strMsg.contains("c")) {
                        }
                    }
                    break;

                case BluetoothManager.MESSAGE_DEVICE_NAME:
                    Log.d(TAG, "MESSAGE_DEVICE_NAME: ");

                    // save connected device's name and notify using toast
                    String deviceAddress = msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_DEVICE_ADDRESS);
                    String deviceName = msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_DEVICE_NAME);

                    if(deviceName != null && deviceAddress != null) {
                        // Remember device's address and name
                        mConnectionInfo.setDeviceAddress(deviceAddress);
                        mConnectionInfo.setDeviceName(deviceName);

                        Toast.makeText(mContext,
                                "Connected to " + deviceName, Toast.LENGTH_SHORT).show();

                    }
                    break;

                case BluetoothManager.MESSAGE_TOAST:
                    Log.d(TAG, "BT - MESSAGE_TOAST: ");

                    Toast.makeText(mContext,
                            msg.getData().getString(Constants.SERVICE_HANDLER_MSG_KEY_TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;

            }	// End of switch(msg.what)

            super.handleMessage(msg);
        }
    }	// End of class MainHandler
    private void sendMessageToRemote(String message) {
        sendMessageToDevice(message);
    }

    /**
     * Send message to device.
     * @param message		message to send
     */
    private void sendMessageToDevice(String message) {
        if(message == null || message.length() < 1)
            return;

        TransactionBuilder.Transaction transaction = mTransactionBuilder.makeTransaction();
        transaction.begin();
        transaction.setMessage(message);
        transaction.settingFinished();
        transaction.sendTransaction();
    }
}
