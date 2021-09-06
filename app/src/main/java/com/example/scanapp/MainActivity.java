package com.example.scanapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button btnSend;
    TextView textLat;
    TextView textLon;
    TextView textCellId1;
    TextView textCellId2;
    TextView textCellId3;
    TextView textCellId4;
    TextView textRssi;

    //GPS
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;

    //CellInfo
    List<CellInfo> cellInfoList;
    TelephonyManager telephonyManager;
    String listGSM = "";
    String listLTE = "";
    String listRssi = "";



    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSend = findViewById(R.id.btn_start);
        textLat = findViewById(R.id.text_lat);
        textLon = findViewById(R.id.text_lon);
        textCellId1 = findViewById(R.id.text_cell_id_1);
        textCellId2 = findViewById(R.id.text_cell_id_2);
        textCellId3 = findViewById(R.id.text_cell_id_3);
        textCellId4 = findViewById(R.id.text_cell_id_4);
        textRssi = findViewById(R.id.text_rssi);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference().child("Data from ScanApp");

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);


        //GPS
        //noinspection deprecation
        locationRequest = new LocationRequest();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                updateValues(location);
            }
        };


        btnSend.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {


                updateGPS();
                infoCell();


                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);

                String latitude = textLat.getText().toString();
                String longitude = textLon.getText().toString();


                HashMap<String, String> coordinates = new HashMap<>();
                coordinates.put("Lat: ", latitude);
                coordinates.put("Lon: ", longitude);
                myRef.setValue(coordinates);
            }

        });


    }


    private void infoCell() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String list1= "";

        cellInfoList = telephonyManager.getAllCellInfo();
        for(int i=0; i<cellInfoList.size(); ++i)
        {
            CellInfo info = cellInfoList.get(i);

            if (info instanceof CellInfoGsm) {
                //listGSM+="Site_"+i +"\r\n";
                // listGSM+="Registered:  "+info.isRegistered();

                CellIdentityGsm cellid = ((CellInfoGsm) info).getCellIdentity();
                list1 += "cellID: " + cellid.getCid() + "\r\n";

                for(int k=0;k<5;k++) {
                    CellSignalStrengthGsm gsm = ((CellInfoGsm) info).getCellSignalStrength();
                    list1 += " " + gsm.getDbm() + ",";


                }
                textCellId1.setText(list1);

                Toast.makeText(MainActivity.this,"Stacji GSM jest:" +cellInfoList.size(),Toast.LENGTH_SHORT).show();


            }

            //listGSM+="dbm "+gsm.getDbm();

            else if (info instanceof CellInfoLte) {
                listLTE += "Site_" + i + "\r\n";
                listLTE += "Registered:  " + info.isRegistered();
                CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
                CellIdentityLte cellid = ((CellInfoLte) info).getCellIdentity();
                listLTE += "cellId" + cellid.getCi();
                listLTE += "dbm " + lte.getDbm();

            }
        }
        }





/*
    @SuppressLint("SetTextI18n")
    private void infoLTECell(){


        String listLTE = "";

        if(cellInfoLteList!=null){

            for(int i=0; i<cellInfoLteList.size(); ++i){

                CellInfo infoLte = cellInfoLteList.get(i);

                if(infoLte instanceof CellInfoLte) {
                    listLTE += "Site " + (i+1) +": ";
                    listLTE += "Registered: " + infoLte.isRegistered()+ ", ";
                    CellSignalStrengthLte lte = ((CellInfoLte) infoLte).getCellSignalStrength();
                    listLTE += "RSSI:  " + lte.getDbm()+", ";
                    CellIdentityLte identityLte = ((CellInfoLte) infoLte).getCellIdentity();
                    if(identityLte.getCi()==2147483647) {
                        listLTE += "CellID: " + "unavailable" + "\r\n";
                    }
                    else {
                        listLTE += "CellID:  " + identityLte.getCi() + "\r\n";
                    }

                }

            }
        }

       // textLte.setText(String.valueOf("Stacji LTE jest: "+" "+cellInfoLteList.size())+"\r\n"  +listLTE);


    }*/

    private void updateGPS()
    {
        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(MainActivity.this);

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {

                    updateValues(location);
                }
            });
        }
        else{
            if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            }

        }
    }

    private void updateValues(Location location)
    {
        textLat.setText(String.valueOf(location.getLatitude()));
        textLon.setText(String.valueOf(location.getLongitude()));

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case 100:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    updateGPS();

                }
                else
                {
                    Toast.makeText(MainActivity.this,"Permission denied",Toast.LENGTH_LONG).show();
                }
        }
    }
}