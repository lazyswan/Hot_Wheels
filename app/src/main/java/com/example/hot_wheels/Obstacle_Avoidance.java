
package com.example.hot_wheels;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class Obstacle_Avoidance extends AppCompatActivity {
    //Bluetooth_Setting:---------------------------------
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private BluetoothAdapter mBluetoothAdapter = null;
    private String mConnectedDeviceName = null;
    private String mConnectedDeviceAddress = null;
    private BluetoothChatService mChatService = null;
    private StringBuffer mOutStringBuffer;
    //Bluetooth_Setting:---------------------------------
    private static final String START = "~1@!#";
    private static final String STOP = "~0@!#";
    private static final String TAG = "Communicate_Activity";
    private static final String GEO = "GEO: ";
    private static final String FS = "FS: ";
    private static final String RS = "RS: ";
    private static final String LS = "LS: ";
    private static final String BS = "BS: ";

    private static final char START_PACKET = '~';
    private static final char END_PACKET   = '#';

    String input_packet_string;
    byte[] writeBuf;
    boolean start_conversion=true;
    //int rx_buff_ptr;
    Button start_btn,stop_btn;
    TextView mode_details_tv,fs_val,ls_val,rs_val,bs_val,lat_val,long_val;
   public static String EXTRA_ADDRESS = "device_address";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_obstacle__avoidance);
        Intent intent = getIntent();
        mConnectedDeviceAddress = intent.getStringExtra(MainActivity.EXTRA_ADDRESS);
        //Bluetooth_Setting:---------------------------------
        mConnectedDeviceAddress = intent.getStringExtra(MainActivity.EXTRA_ADDRESS);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //Bluetooth_Setting:---------------------------------
        init_listeners();
    }

    private void init_listeners() {


        start_btn=findViewById(R.id.start_button);
        stop_btn=findViewById(R.id.stop_button);
        mode_details_tv=findViewById(R.id.status_tv);
        fs_val=findViewById(R.id.fs_tv);
        ls_val=findViewById(R.id.ls_tv);
        rs_val=findViewById(R.id.rs_tv);
        bs_val=findViewById(R.id.bs_tv);
        long_val=findViewById(R.id.long_id);
        lat_val=findViewById(R.id.lat_id);


    }

//Bluetooth and Thread Communication Code-------------------------------------
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
            notify_user(" ","Bluetooth OFF.",true);
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

    private void connectDevice(boolean secure) {

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mConnectedDeviceAddress);
        mChatService.connect(device, secure);//Thread activity.
    }

    Handler mHandler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            notify_user("Status: "," Connected to "+mConnectedDeviceName,false);
                            break;

                        case BluetoothChatService.STATE_CONNECTING:
                            notify_user("Status: ", "Connecting...",false);
                            break;

                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            notify_user(" ", "Unable to Connect.Try Again",false);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    notify_user("Status: ","Message Sent",false);
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
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    break;
                case Constants.MESSAGE_TOAST:
                    break;
            }

            return false;
        }
    });

    private void parse_msg(String readMessage) {
        Log.i("parse_msg : ","PARSED_PACKET: "+readMessage);
            ls_val.setText("LS: "+readMessage.substring(readMessage.indexOf('L')+1,readMessage.indexOf('R')));
            fs_val.setText("FS: "+readMessage.substring(readMessage.indexOf('F')+1,readMessage.indexOf('B')));
            rs_val.setText("RS: "+readMessage.substring(readMessage.indexOf('R')+1,readMessage.indexOf('F')));
            bs_val.setText("BS: "+readMessage.substring(readMessage.indexOf('B')+1,readMessage.indexOf('@')));
            lat_val.setText("Lat: "+readMessage.substring(readMessage.indexOf('@')+1,readMessage.indexOf('!')));
            long_val.setText("Long: "+readMessage.substring(readMessage.indexOf('!')+1,readMessage.indexOf('#')));
       // geo_val.setText("Lat: "+readMessage.substring(readMessage.indexOf('!')+1,readMessage.indexOf('#'))+
              //  " Long: "+readMessage.substring(readMessage.indexOf('@')+1,readMessage.indexOf('!')));
        //bs_val.setText("BS: "+readMessage.substring(readMessage.indexOf('B'+1),readMessage.indexOf('#')));
    }

//@ !
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            notify_user("Status: ","Not Connected!.Try Again",false);
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //editText.setText(mOutStringBuffer);
        }
    }
//Bluetooth and Thread Communication Code-------------------------------------

    public void start_btn_click(View view) {

        sendMessage(START);//send 1
    }

    public void stop_btn_click(View view) {
        //String stop_cmd = STOP;
        sendMessage(STOP);//send 0
        Log.i("SEND: ", "BUTTON PRESS");
    }

    public void exit_btn_click(View view) {
        if (mChatService != null) {
            mChatService.stop();
        }
        mBluetoothAdapter.cancelDiscovery();
        //  mBluetoothAdapter.disable();//TurnOFF Bluetooth
        //recievedMsgList.clear();
        finish();
    }

   public void notify_user(String prefix,String message,boolean toast) {
        if (toast) {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        }
        //mode_details_tv.setText(prefix + message);

    }


}

