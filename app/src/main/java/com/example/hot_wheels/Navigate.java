package com.example.hot_wheels;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class Navigate extends FragmentActivity implements OnMapReadyCallback {
    //Bluetooth_Setting:---------------------------------
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private BluetoothAdapter mBluetoothAdapter = null;
    private String mConnectedDeviceName = null;
    private String mConnectedDeviceAddress = null;
    private BluetoothChatService mChatService = null;
    private StringBuffer mOutStringBuffer;
    public static String EXTRA_ADDRESS = "device_address";
    boolean parsing_finished = false;
    //Bluetooth_Setting:---------------------------------

    //Text View:------------------------------------------
    TextView car_lat_long, dis, b_angle, h_angle;

    //Text View:------------------------------------------


    // String Related Settings:-----------------------------------

    private static final String START = "~1@!#";
    private static final String STOP = "~0@!#";

    private static final char START_PACKET = '~';
    private static final char END_PACKET = '#';
    private String input_packet_string;

    //MAPS related---------------------------------------------------------------
    Button start_trip_btn, end_trip_btn;
    private GoogleMap mMap;
    private Marker destination_marker;
    private Marker source_marker;
    private ArrayList<String> coordinate_list;
    private boolean start_trip = false;
    private double dest_latitude = 0.0;
    private double destn_longitude = 0.0;
    private String src_lat = "0.0";
    private String src_long = "0.0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigate);
        //Map Related---------------------------------
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //Map Related---------------------------------

        Intent intent = getIntent();
        mConnectedDeviceAddress = intent.getStringExtra(MainActivity.EXTRA_ADDRESS);
        //Bluetooth_Setting:---------------------------------
        mConnectedDeviceAddress = intent.getStringExtra(MainActivity.EXTRA_ADDRESS);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        start_trip_btn = findViewById(R.id.start_trip_btn);
        end_trip_btn = findViewById(R.id.end_trip_btn);
        start_trip_btn.setEnabled(true);
        end_trip_btn.setEnabled(false);

        //Text view init--------------------
        car_lat_long = findViewById(R.id.car_location_id);
        //car_long=findViewById(R.id.car_long_id);
        dis = findViewById(R.id.distance_id);
        //bs=findViewById(R.id.back_sensor_id);
        //rs=findViewById(R.id.right_sensor_id);
        //ls=findViewById(R.id.left_sensor_id);
        b_angle = findViewById(R.id.car_bearing_id);
        h_angle = findViewById(R.id.car_heading_id);

        //Text view init------------------
        coordinate_list = new ArrayList<>();
        start_trip = false;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
        mBluetoothAdapter.cancelDiscovery();
    }


    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            //notify_user(" ","Bluetooth OFF.",true);
            finish();
        } else if (mChatService == null) {
            setupChat();
        }
    }

    private void setupChat() {
        mChatService = new BluetoothChatService(this, mHandler);
        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
        connectDevice(false);//Insecure Connection
    }

    private void parse_msg(String readMessage) {
        Log.i("parse_msg : ", "PARSED_PACKET: " + readMessage);
/*
            *Log.i("parse_msg : ","PARSED_PACKET: "+readMessage);
            ls_val.setText("LS: "+readMessage.substring(readMessage.indexOf('L')+1,readMessage.indexOf('R')));
            fs_val.setText("FS: "+readMessage.substring(readMessage.indexOf('F')+1,readMessage.indexOf('B')));
            rs_val.setText("RS: "+readMessage.substring(readMessage.indexOf('R')+1,readMessage.indexOf('F')));
            bs_val.setText("BS: "+readMessage.substring(readMessage.indexOf('B')+1,readMessage.indexOf('@')));
            lat_val.setText("Lat: "+readMessage.substring(readMessage.indexOf('@')+1,readMessage.indexOf('!')));
            long_val.setText("Long: "+readMessage.substring(readMessage.indexOf('!')+1,readMessage.indexOf('C')));
            compass_val.setText("Compass Angle: "+readMessage.substring(readMessage.indexOf('C')+1,readMessage.indexOf('^')));
            bearing_angle_tv.setText("Bearing Angle: "+readMessage.substring(readMessage.indexOf('^')+1,readMessage.indexOf('$')));
            distance_tv.setText("Distance: "+readMessage.substring(readMessage.indexOf('$')+1,readMessage.indexOf('#')));*/

        dis.setText("Distance: " + readMessage.substring(readMessage.indexOf('$') + 1, readMessage.indexOf('#')));
        h_angle.setText("Heading Angle: " + readMessage.substring(readMessage.indexOf('C') + 1, readMessage.indexOf('^')));
        b_angle.setText("Compass Angle: " + readMessage.substring(readMessage.indexOf('^') + 1, readMessage.indexOf('$')));
        src_lat = readMessage.substring(readMessage.indexOf('@') + 1, readMessage.indexOf('!'));
        src_long = readMessage.substring(readMessage.indexOf('!') + 1, readMessage.indexOf('C'));
        car_lat_long.setText("Lat: " + src_lat + " Long: " + src_long);
        if (Double.parseDouble(src_lat) != 36.000000) {
            parsing_finished = true;
            Log.i("parse_msg() ", "GPS Location Valid ");
        } else {
            parsing_finished = false;
            Log.i("parse_msg() ", "GPS Location Invalid ");
        }

    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        Log.i("sendMessage() ", message);
        if (mChatService.getState() == BluetoothChatService.STATE_CONNECTED && message.length() > 0) {
            Log.i("LOG: ", Integer.toString(message.length()));
            byte[] send = message.getBytes();
            mChatService.write(send);
        } else {
            Log.i("ERROR: ", "BT Not Connected!.Try Again");
            return;
        }

    }


    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;

                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    // Log.i("MSG FROM AQUIB***: ",readMessage+" "+readBuf.length);
                    for (int i = 0; i < readBuf.length; i++) {
                        byte rx_char = readBuf[i];
                        if (rx_char == START_PACKET) {
                            input_packet_string = "";
                            input_packet_string = Character.toString((char) rx_char);
                        } else if (rx_char == END_PACKET) {
                            input_packet_string += Character.toString((char) rx_char);
                            parse_msg(input_packet_string);
                            input_packet_string = "";

                        } else {
                            input_packet_string += Character.toString((char) rx_char);
                        }
                    }
                    break;


                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    break;
                case Constants.MESSAGE_TOAST:
                    break;
            }

            return false;
        }
    });

    private void connectDevice(boolean secure) {

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mConnectedDeviceAddress);
        mChatService.connect(device, secure);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.i("Map_Marker: ", (Double.parseDouble(src_lat) + "," + Double.parseDouble(src_long)));

        LatLng hot_wheels = new LatLng(37.3356906, -121.8874445);//default location
        //mMap.addMarker(new MarkerOptions().position(hot_wheels).title("University"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hot_wheels, 17f));
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                if (parsing_finished && null == source_marker) {
                    LatLng hot_wheels_str_pt = new LatLng(Double.parseDouble(src_lat), Double.parseDouble(src_long));
                    source_marker = mMap.addMarker(new MarkerOptions().position(hot_wheels_str_pt).title("Hot Wheels Start Point").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hot_wheels_str_pt, 17f));
                }
                if (null != destination_marker) {
                    destination_marker.remove();
                }
                if (!start_trip && parsing_finished) {
                    destination_marker = mMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    destination_marker.setTitle("Destination");
                }

                Log.i("Destination Position: ", latLng.toString());
            }
        });
    }


    public void start_trip_click(View view) {

        if (null != destination_marker && !start_trip) {

            dest_latitude = destination_marker.getPosition().latitude;
            destn_longitude = destination_marker.getPosition().longitude;
            //~1@!#
            String start_pck = "~1@" + Double.toString(dest_latitude) + "!" + Double.toString(destn_longitude) + "#";
            //coordinate_list.add(destination_marker.getPosition().toString());
            Log.i("start_trip_click: ", start_pck);
            sendMessage(start_pck);
            start_trip_btn.setEnabled(false);
            end_trip_btn.setEnabled(true);
            start_trip = true;
        } else {
            Toast.makeText(getApplicationContext(), "Destination Not Selected!", Toast.LENGTH_SHORT).show();
        }
    }

    public void end_trip_click(View view) {

        dest_latitude = destination_marker.getPosition().latitude;
        destn_longitude = destination_marker.getPosition().longitude;
        //~0@!#
        String stop_pck = "~0@" + Double.toString(dest_latitude) + "!" + Double.toString(destn_longitude) + "#";
        //coordinate_list.add(destination_marker.getPosition().toString());
        Log.i("end_trip_click: ", stop_pck);
        sendMessage(stop_pck);
        start_trip_btn.setEnabled(true);
        end_trip_btn.setEnabled(false);
        start_trip = false;
        source_marker.remove();
        source_marker = null;
        destination_marker.remove();
        //destination_marker = null;
        //mMap.clear();


    }
}
