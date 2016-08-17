package com.dtu.helmet_alert.home;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.dtu.helmet_alert.MyApplication;
import com.dtu.helmet_alert.R;
import com.dtu.helmet_alert.biking.BikeRide;
import com.dtu.helmet_alert.biking.MonitorBikeRide;
import com.dtu.helmet_alert.bluetooth.BluetoothService;

/**
 * Created by chris on 17-08-2016.
 */
public class Home_frag extends Fragment {

    private final static String TAG = Home_frag.class.getSimpleName();

    View root;
    Button emulateBikeRide_b;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root = inflater.inflate(R.layout.home_frag, container, false);

        emulateBikeRide_b = (Button) root.findViewById(R.id.home_emulate_bikeRide);

        emulateBikeRide_b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "onClick- Test.");
                if (emulateBikeRide_b.getText().equals("Emulate bike ride")){
                    MyApplication.ridingBike = true;
                    getActivity().startService(new Intent(getActivity().getBaseContext(), MonitorBikeRide.class));
                    emulateBikeRide_b.setText("Stop");
                }
                else if (emulateBikeRide_b.getText().equals("Stop")) {
                    MyApplication.ridingBike = false;
                    getActivity().stopService(new Intent(getActivity().getBaseContext(), MonitorBikeRide.class));
                    emulateBikeRide_b.setText("Emulate bike ride");

                }
            }
        });

        return root;
    }

}
