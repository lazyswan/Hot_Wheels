package com.example.hot_wheels;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Obstacle_Avoidance extends AppCompatActivity {
    //Bluetooth_Setting:---------------------------------
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final String TAG = "Communicate_Activity";
    private BluetoothAdapter mBluetoothAdapter = null;
    private String mConnectedDeviceName = null;
    private String mConnectedDeviceAddress = null;
    private BluetoothChatService mChatService = null;
    private StringBuffer mOutStringBuffer;
    //Bluetooth_Setting:---------------------------------
    byte[] readBuf;
    Button start_btn,stop_btn;
    TextView mode_details_tv;
    //private String mConnectedDeviceAddress = null;
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
        mode_details_tv=findViewById(R.id.tv_mode_details);
        //rx_data=findViewById(R.id.rx_data);
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
                    // construct a string from the buffer
                    //String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    notify_user("Status: ","Message Sent",false);
                    //rx_data.setText("Rx_Data: ");
                    break;
                case Constants.MESSAGE_READ:
                    readBuf = (byte[]) msg.obj;
                    //readBuf.toString();
                   String readMessage = new String(readBuf, 0, msg.arg1);
                    //notify_user(" ",readMessage,false);
                    //rx_data.setText("Rx_Data: "+readMessage);
                    // construct a string from the valid bytes in the buffer

                    //notify_user(" ","Incoming Message Ignored",false);
                    // mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
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
        String start_cmd ="~1@!#";
        sendMessage(start_cmd);//send 1
    }

    public void stop_btn_click(View view) {
        String stop_cmd = "~0@!#";
        sendMessage(stop_cmd);//send 0
    }

    public void exit_btn_click(View view) {
        if (mChatService != null) {
            mChatService.stop();
        }
        mBluetoothAdapter.cancelDiscovery();
        //  mBluetoothAdapter.disable();//TurnOFF Bluetooth
        finish();
    }

   public void notify_user(String prefix,String message,boolean toast) {
        if (toast) {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        }
        mode_details_tv.setText(prefix + message);

    }

    public void log_btn(View view) {
        Intent newintent = new Intent(this, Log_screen.class);
        newintent.putExtra(EXTRA_ADDRESS,mConnectedDeviceAddress);
        startActivity(newintent);
    }
}
