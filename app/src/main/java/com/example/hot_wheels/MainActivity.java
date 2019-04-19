package com.example.hot_wheels;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    public static String EXTRA_ADDRESS = "device_address";
    private static final String TAG = "MainActivity";
    Button search_btn,on_off_btn;
    TextView status;
    ListView pair_list,new_device_list;
    DeviceListAdapter mDeviceListAdapter;
    //public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    private ArrayList<BluetoothDevice> mNewDevicesArrayAdapter;
    ArrayList<BluetoothDevice> pairedDevicesArrayAdapter;
    BluetoothDevice mBTDevice=null;
    BluetoothAdapter bluetoothAdapter=null;


    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (action.equals(bluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, bluetoothAdapter.ERROR);
                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE OFF");
                        notify_user("Bluetooth OFF");
                        break;
                     case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                         notify_user("Bluetooth ON");
                        break;
                            }
            }
        }
    };

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device);
                    mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mNewDevicesArrayAdapter);
                    new_device_list.setAdapter(mDeviceListAdapter);
                }
                if(device.getBondState() == BluetoothDevice.BOND_BONDED){
                    pairedDevicesArrayAdapter.add(device);
                    mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, pairedDevicesArrayAdapter);
                    pair_list.setAdapter(mDeviceListAdapter);

                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //setProgressBarIndeterminateVisibility(false);
                //notify_user("Finished Searching.Select Device");

            }
        }
    };









    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init_listeners();
        initialise();

    }

    private void initialise() {
        if(!bluetoothAdapter.isEnabled()){
            search_btn.setEnabled(false);
        }
        else{
            search_btn.setEnabled(true);
        }

        status.setText("");
        pair_list.setAdapter(null);
        new_device_list.setAdapter(null);

    }

    private void init_listeners() {
        Log.d(TAG, "initialise_listeners: listeners_init.");
        search_btn=(Button) findViewById(R.id.btn_search);
        on_off_btn=(Button) findViewById(R.id.btn_on_off);
        status=(TextView) findViewById(R.id.tv_status);
        pair_list=(ListView)findViewById(R.id.list_pair);
        new_device_list=(ListView)findViewById(R.id.list_new);
        //mBTDevices = new ArrayList<>();
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();//1
        pairedDevicesArrayAdapter =new ArrayList<>();
        mNewDevicesArrayAdapter = new ArrayList<>();
        //pair_list.setAdapter(pairedDevicesArrayAdapter);
        pair_list.setOnItemClickListener(mDeviceClickListener);
        //new_device_list.setAdapter(pairedDevicesArrayAdapter);
        new_device_list.setOnItemClickListener(mDeviceClickListener);

        // Enabling Disabling Bluetooth
        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver1, BTIntent);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        //Adding Paired Devices to List
        // Get a set of currently paired devices



    }

    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            bluetoothAdapter.cancelDiscovery();

            mBTDevice= (BluetoothDevice) av.getAdapter().getItem(arg2);

            notify_user(mBTDevice.getName());

            String device_name=mBTDevice.getName();
            String device_address=mBTDevice.getAddress();
            Log.d(TAG, "Calling New Activity" );
            initialise();

            Intent newintent = new Intent(MainActivity.this, Menu_Select.class);
            newintent.putExtra(EXTRA_ADDRESS,device_address);
            startActivity(newintent);


        }
    };


    private void checkBTPermissions() {
        Log.d(TAG, "checkBTPermissions: Checking permissions");
        int permission_check = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        permission_check += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
        if (permission_check != 0) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
        }
        else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: called.");
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mReceiver);
        //unregisterReceiver(mBroadcastReceiver3);
        bluetoothAdapter.cancelDiscovery();
    }

    public void btn_on_off(View view) {
        Log.d(TAG, "btn_on_off: Button pressed.");
        initialise();
        enable_bt();//2
        checkBTPermissions();//3
    }

    private void enable_bt() {
        Log.d(TAG, "enable_bt");
        if(bluetoothAdapter==null){
            Log.i(TAG,"Device does not have bluetooth capabilities");
        }

        if(!bluetoothAdapter.isEnabled()){
            Log.d(TAG, "enableDisableBT: enabling BT.");

            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);
            search_btn.setEnabled(true);

        }
        else if(bluetoothAdapter.isEnabled()){
            Log.d(TAG, "enableDisableBT: disabling BT.");
            bluetoothAdapter.disable();
            search_btn.setEnabled(false);
        }




    }

    public void btnt_search(View view) {
        Log.d(TAG, "btnt_search: Button pressed.");
        pair_list.setAdapter(null);
        new_device_list.setAdapter(null);
        mNewDevicesArrayAdapter.clear();
        pairedDevicesArrayAdapter.clear();

        doDiscovery();
    }

    private void doDiscovery() {
        Log.d(TAG, "doDiscovery: Discovering Devices.");
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        bluetoothAdapter.startDiscovery();
    }


    public void notify_user(String s){
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_SHORT).show();
        status.setText("Status: "+s);
    }
}
