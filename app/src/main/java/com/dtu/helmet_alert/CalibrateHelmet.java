package com.dtu.helmet_alert;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.dtu.helmet_alert.bluetooth.HelmetServiceBT;

/**
 * Created by ce on 22-07-2016.
 */
public class CalibrateHelmet extends Fragment{

    private String TAG = "CalibrateHelmet";
    private Button connect_b;
    private ImageView helmetImage, arrow_cw, arrow_ccw, crossCheck;
    private ListView messageListView;
    private ArrayAdapter<String> listAdapter;

    private BluetoothDevice mHelmetDevice = null;
    private BluetoothAdapter mBtAdapter = null;

    private OLD_BluetoothService mCustomBTService;

    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private boolean mScanning;

    private double acc_x = 0;

    View root;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.calibrate_helmet, container, false);


        connect_b = (Button) root.findViewById(R.id.calibrate_helmet_connectButton);
        helmetImage = (ImageView) root.findViewById(R.id.calibrate_helmet_helmet);
        arrow_ccw = (ImageView) root.findViewById(R.id.calibrate_helmet_arrow_ccw);
        arrow_cw = (ImageView) root.findViewById(R.id.calibrate_helmet_arrow_cw);
        crossCheck = (ImageView) root.findViewById(R.id.calibrate_helmet_checkCross);

        messageListView = (ListView) root.findViewById(R.id.calibrate_helmet_accDataList);

        listAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.activity_list_item);
        messageListView.setAdapter(listAdapter);
        messageListView.setDivider(null);

        arrow_cw.setVisibility(View.INVISIBLE);
        arrow_ccw.setVisibility(View.INVISIBLE);
        

        mCustomBTService = new OLD_BluetoothService();

        connect_b.setText("Connect to helmet");

        // Handler Disconnect & Connect button
        connect_b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick- helmet");
                if (!mBtAdapter.isEnabled()) {
                    Log.d(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (connect_b.getText().equals("Connect to helmet")) {

                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices

                        Intent newIntent = new Intent(getActivity(), DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        getActivity().stopService(new Intent(getActivity().getBaseContext(), HelmetServiceBT.class));
                        helmetImage.setImageResource(R.drawable.helmet_side_bw);
                        arrow_cw.setVisibility(View.INVISIBLE);
                        arrow_ccw.setVisibility(View.INVISIBLE);
                        connect_b.setText("Connect to helmet");
                        crossCheck.setImageDrawable(getActivity().getDrawable(R.drawable.cross));

                    }
                }

            }
        });

        // Broadcast reciever
        getActivity().registerReceiver(accDataUpdateReceiver, accDataUpdateIntentFilter());



        // BT
         // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = bluetoothManager.getAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(getActivity(), "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }
        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);

                    BluetoothDevice temp_device;
                    temp_device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    if (temp_device.getName().equals("HelmetAlert_H")){
                        mHelmetDevice = temp_device;

                        // Store address of device
                        SharedPreferences prefs = getActivity().getSharedPreferences("com.dtu.susie_bike_app", Context.MODE_PRIVATE);
                        MyApplication.helmetAddress = deviceAddress;
                        prefs.edit().putString(MyApplication.prefsHelmetAddress,MyApplication.helmetAddress).commit();
                        updateImages();
                        connect_b.setText("Disconnect");

                        Log.d(TAG,"Connecting to: " + mHelmetDevice.getName()+" with address: "+mHelmetDevice.getAddress());

                        final Intent intent = new Intent(getContext(), HelmetServiceBT.class);
                        intent.putExtra(HelmetServiceBT.EXTRAS_DEVICE_NAME, mHelmetDevice.getName());
                        intent.putExtra(HelmetServiceBT.EXTRAS_DEVICE_ADDRESS, mHelmetDevice.getAddress());
                        intent.putExtra(HelmetServiceBT.EXTRAS_STATE,MyApplication.STATE_CALIBRATE);
                        getActivity().startService(intent);

                        //getActivity().startService(new Intent(getContext(), OLD_BluetoothService.class));

                    }
                    else {
                        Toast.makeText(getActivity(), "Not a helmet. Try again", Toast.LENGTH_SHORT).show();
                    }

                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(getActivity(), "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();

                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }


    public void updateImages(){

        //check if connected to helmet
        if (MyApplication.helmetAddress.length()>1){
            helmetImage.setImageResource(R.drawable.helmet_side);
        }
        else{
            helmetImage.setImageResource(R.drawable.helmet_side_bw);
        }
        // check accelerometer data and show arrows and check/cross marker
        //TODO determine the limits for a optimal helmet placement
        //TODO acc_x interval [-0.17g , 0.17g] (10 degrees from horizontal placement)

        //calc degree rotation from x_axis acceleration
        double radians = Math.acos(acc_x);
        double degrees = Math.toDegrees(radians);
        helmetImage.setRotation((float)(90-degrees));

        //Log.d(TAG,"new acc_x value: "+acc_x);

        if (acc_x<-0.17){
            //Log.d(TAG,"For langt fremme. tip op");
            arrow_ccw.setVisibility(View.INVISIBLE);
            arrow_cw.setVisibility(View.VISIBLE);
            crossCheck.setImageDrawable(getActivity().getDrawable(R.drawable.cross));
        }
        if (acc_x>0.17){
            //Log.d(TAG,"For langt tilbage. tip frem");
            arrow_cw.setVisibility(View.INVISIBLE);
            arrow_ccw.setVisibility(View.VISIBLE);
            crossCheck.setImageDrawable(getActivity().getDrawable(R.drawable.cross));
        }

        if (acc_x<0.17 && acc_x>-0.17){
            arrow_ccw.setVisibility(View.INVISIBLE);
            arrow_ccw.setVisibility(View.INVISIBLE);
            crossCheck.setImageDrawable(getActivity().getDrawable(R.drawable.check));
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroy();
        getActivity().stopService(new Intent(getActivity().getBaseContext(), OLD_BluetoothService.class));
        getActivity().unregisterReceiver(accDataUpdateReceiver);
    }

    private static IntentFilter accDataUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(HelmetServiceBT.NEW_ACC_DATA);
        return intentFilter;
    }

    private final BroadcastReceiver accDataUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (HelmetServiceBT.NEW_ACC_DATA.equals(action)) {

                //update acc_x value
                acc_x = intent.getDoubleExtra(HelmetServiceBT.ACC_X_DATA_VALUE,0);

                acc_x=acc_x*-1.0;
                // update UI
                updateImages();

            }

        }
    };
}