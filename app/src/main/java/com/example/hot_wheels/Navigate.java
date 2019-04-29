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
    boolean parsing_finished=false;
    //Bluetooth_Setting:---------------------------------
    // String Related Settings:-----------------------------------

    private static final String START = "~1@!#";
    private static final String STOP = "~0@!#";

    private static final char START_PACKET = '~';
    private static final char END_PACKET   = '#';
    private String input_packet_string;

    //MAPS related---------------------------------------------------------------
    Button start_trip_btn,end_trip_btn;
    private GoogleMap mMap;
    private Marker destination_marker;
    private Marker source_marker;
    private ArrayList<String> coordinate_list;
    private boolean start_trip=false;
    private double dest_latitude=0.0;
    private double destn_longitude=0.0;
    private String src_lat="0.0";
    private String src_long="0.0";
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
        start_trip_btn=findViewById(R.id.start_trip_btn);
        end_trip_btn=findViewById(R.id.end_trip_btn);
        start_trip_btn.setEnabled(true);
        end_trip_btn.setEnabled(false);
        coordinate_list=new ArrayList<>();
        start_trip=false;

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
       // Log.i("parse_msg : ","PARSED_PACKET: "+readMessage);

        src_lat=readMessage.substring(readMessage.indexOf('@')+1,readMessage.indexOf('!'));
        src_long=readMessage.substring(readMessage.indexOf('!')+1,readMessage.indexOf('#'));
        Log.i("converted MSG : ",src_lat+":"+src_long);
        coordinate_list.add(src_lat+","+src_long);
        parsing_finished=true;
    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Log.i("ERROR: ","Not Connected!.Try Again");
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            //int len= message.length();
            Log.i("LOG: ",Integer.toString(message.length()));
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            //mOutStringBuffer.setLength(0);
            //editText.setText(mOutStringBuffer);
        }
    }


    Handler mHandler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            //notify_user("Status: "," Connected to "+mConnectedDeviceName,false);
                            //setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            //mConversationArrayAdapter.clear();

                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            //notify_user("Status: ", "Connecting...",false);
                            //setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            //notify_user(" ", "Unable to Connect.Try Again",false);
                            //setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    //String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    //notify_user("Status: ","Message Sent",false);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    // Log.i("MSG FROM AQUIB***: ",readMessage+" "+readBuf.length);
                    for(int i=0;i<readBuf.length;i++){
                        byte rx_char=readBuf[i];
                        if(rx_char == START_PACKET){
                            input_packet_string="";
                            input_packet_string=Character.toString((char)rx_char);
                        }
                        else if(rx_char == END_PACKET){
                            input_packet_string+=Character.toString((char)rx_char);
                            parse_msg(input_packet_string);
                            input_packet_string="";

                        }
                        else{
                            input_packet_string+=Character.toString((char)rx_char);
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



       // mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Car Location"));//Set the Marker to location of Car
        // Add a marker in Sydney and move the camera
        //double d_src_lat=0.0;
        //double d_src_long=0.0;
        //d_src_lat= Double.parseDouble(src_lat);
        //d_src_long=Double.parseDouble(src_long);
        Log.i("Map_Marker: ",(Double.parseDouble(src_lat)+","+Double.parseDouble(src_long)));

        LatLng hot_wheels = new LatLng(37.3356906,-121.8874445);
        //mMap.addMarker(new MarkerOptions().position(hot_wheels).title("University"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hot_wheels,17f));
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                if(null != source_marker){
                    source_marker.remove();
                }
                if(parsing_finished){
                    LatLng hot_wheels_str_pt = new LatLng(Double.parseDouble(src_lat),Double.parseDouble(src_long));
                    source_marker=mMap.addMarker(new MarkerOptions().position(hot_wheels_str_pt).title("Hot Wheels Start Point").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hot_wheels_str_pt,17f));
                    //mMap.clear();
                }

                if(null != destination_marker){
                    destination_marker.remove();
                }
                destination_marker=mMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                destination_marker.setTitle("Destination");
                Log.i("Destination Position: ",latLng.toString());
            }
        });
    }


    public void start_trip_click(View view) {

        if(null!=destination_marker && !start_trip){

            dest_latitude=destination_marker.getPosition().latitude;
            destn_longitude=destination_marker.getPosition().longitude;
            //~1@!#
            String start_pck="~1@"+Double.toString(dest_latitude)+"!"+Double.toString(destn_longitude)+"#";
            //coordinate_list.add(destination_marker.getPosition().toString());
            Log.i("start_trip_click: ",start_pck);
            sendMessage(start_pck);
            start_trip_btn.setEnabled(false);
            end_trip_btn.setEnabled(true);
        }
        else{
             Toast.makeText(getApplicationContext(), "Destination Not Selected!", Toast.LENGTH_SHORT).show();
        }
    }

    public void end_trip_click(View view) {

            dest_latitude=destination_marker.getPosition().latitude;
            destn_longitude=destination_marker.getPosition().longitude;
            //~0@!#
            String stop_pck="~0@"+Double.toString(dest_latitude)+"!"+Double.toString(destn_longitude)+"#";
            //coordinate_list.add(destination_marker.getPosition().toString());
            Log.i("end_trip_click: ",stop_pck);
            sendMessage(stop_pck);
            start_trip_btn.setEnabled(true);
            end_trip_btn.setEnabled(false);
            destination_marker.remove();
            destination_marker = null;


    }
}
