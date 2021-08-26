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
    TextView textLte;
    TextView textGsm;



    //GPS
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSend = findViewById(R.id.btn_start);
        textLat = findViewById(R.id.text_lat);
        textLon = findViewById(R.id.text_lon);
        textGsm = findViewById(R.id.text_gsm_data);
        textLte = findViewById(R.id.text_lte_data);




        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference().child("Data from ScanApp");

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
                //infoGSMCell();
                infoLTECell();


                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);


                String latitude = textLat.getText().toString();
                String longitude = textLon.getText().toString();


                String cellid = "aa";

                HashMap<String, String> coordinates = new HashMap<>();
                coordinates.put("Lat: ", latitude);
                coordinates.put("Lon: ", longitude);
                coordinates.put("CellID ", cellid);
                myRef.child(cellid).setValue(coordinates);
            }

        });


    }



    @SuppressLint("SetTextI18n")
    private void infoLTECell(){

        List<CellInfo> cellInfoLteList;
        TelephonyManager telephonyManager;
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        cellInfoLteList = telephonyManager.getAllCellInfo();
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

        textLte.setText(String.valueOf("Stacji LTE jest: "+" "+cellInfoLteList.size())+"\r\n"  +listLTE);


    }

    @SuppressLint("SetTextI18n")
    private void infoGSMCell(){
        List<CellInfo> cellInfoGsmList;
        TelephonyManager telephonyManager;
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        cellInfoGsmList = telephonyManager.getAllCellInfo();

        String listGSM="";

        if(cellInfoGsmList!=null){
            for(int i=0; i<cellInfoGsmList.size(); ++i){

                CellInfo infoGSM = cellInfoGsmList.get(i);
                if(infoGSM instanceof CellInfoGsm) {
                    listGSM += "Site " + i + ": ";
                    listGSM += "Registered: " + infoGSM.isRegistered() + " ";
                    CellSignalStrengthGsm gsm = ((CellInfoGsm) infoGSM).getCellSignalStrength();
                    listGSM += "RSSI: " + gsm.getDbm() + " ";
                    CellIdentityGsm identityGsm = ((CellInfoGsm) infoGSM).getCellIdentity();
                    if(identityGsm.getCid()==2147483647) {
                        listGSM += "CellID " + "unavailable" + "\r\n";
                    }
                    else {
                        listGSM += "CellID  " + identityGsm.getCid() + "\r\n";
                    }
                }
            }
        }
        textGsm.setText(String.valueOf("Stacji GSM jest:  "+cellInfoGsmList.size())+"\r\n"+ listGSM);
    }



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