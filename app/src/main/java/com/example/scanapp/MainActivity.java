package com.example.scanapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity {

    Button btnSend;
    TextView textLat;
    TextView textLon;


    //GPS
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;


    TextView textLteCid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSend = findViewById(R.id.btn_start);
        textLat = findViewById(R.id.text_loc_x);
        textLon = findViewById(R.id.text_loc_y);
        textLteCid = findViewById(R.id.text_lte_cdi);


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

/*
        String list = "";  //I'm just adding everything to a string to display, but you can do whatever
        String list1="";
        String list2="";

        //get cell info
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        List<CellInfo> infos = tel.getAllCellInfo();


        for (int i = 0; i<infos.size(); ++i)
        {
            try {
                CellInfo info = infos.get(i);
                //GSM

                if (info instanceof CellInfoGsm)
                {
                    list += "Sitegsm_"+i + "\r\n";
                    list += "Registered: " + info.isRegistered() + "\r\n";
                    CellSignalStrengthGsm gsm = ((CellInfoGsm) info).getCellSignalStrength();
                    CellIdentityGsm identityGsm = ((CellInfoGsm) info).getCellIdentity();
                    list += "cellID: "+ identityGsm.getCid() + "\r\n";
                    list += "dBm: " + gsm.getDbm() + "\r\n\r\n";

                    textLteCid.setText(String.valueOf(identityGsm.getCid()));

                    //call whatever you want from gsm / identitydGsm
                }

                //LTE

                 if (info instanceof CellInfoLte)  //if LTE connection
                {
                    list1 += "Sitelte_"+i + "\r\n";
                    list1 += "Registered: " + info.isRegistered() + "\r\n";
                    CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
                    CellIdentityLte identityLte = ((CellInfoLte) info).getCellIdentity();
                    //call whatever you want from lte / identityLte
                    list1 +="RSSI:  "+lte.getRssi();
                    list1 += "CellID" +identityLte.getCi();
                }

                //3G
                if (info instanceof CellInfoWcdma)  //if wcdma connection
                {
                    CellSignalStrengthWcdma wcdmaS = ((CellInfoWcdma) info).getCellSignalStrength();
                    CellIdentityWcdma wcdmaid = ((CellInfoWcdma)info).getCellIdentity();
                    list2 += "Sitewcdma_"+i + "\r\n";
                    list2 += "Registered: " + info.isRegistered() + "\r\n";
                    //call whatever you want from wcdmaS / wcdmaid

                }



            } catch (Exception ex) {
                Log.i("neighboring error 2: " ,ex.getMessage());
            }
        }
        //Log.d("Info display", list);//display everything.
        //Log.i("Info1 display", list1);//display everything.

        Log.d("Info2 display", list);//display everything.

        Toast.makeText(MainActivity.this, "aa"+list,
                Toast.LENGTH_LONG).show();




       final TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telephony.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            final GsmCellLocation location = (GsmCellLocation) telephony.getCellLocation();
            if (location != null) {
                textLteCid.setText(String.valueOf(location.getCid()));

            }
        }
        */



        btnSend.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {

                updateGPS();
                fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,null);






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