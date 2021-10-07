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
import android.os.CountDownTimer;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private  static final int MEASUREMENT_PERIOD = 1000;
    private static final int MAX_MEASUREMENT_PER_CELL = 100;


    Button btnSend;
    TextView textLat;
    TextView textLon;
    TextView textStationsCount;
    TextView text;

    //GPS
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;

    //CellInfo
    List<CellInfo> cellInfoList;
    TelephonyManager telephonyManager;

    Location currentLocation = null;
    Location measurementLocation = null;
    boolean measurementEnabled = false;
    List<MeasurementCell> measurementCells = new ArrayList<>();

    Timer timer;
    CountDownTimer countDownTimer;
    int downTimer;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSend = findViewById(R.id.btn_start);
        textLat = findViewById(R.id.text_lat);
        textLon = findViewById(R.id.text_lon);
        textStationsCount = findViewById(R.id.text_stations_count);
        text = findViewById(R.id.text);

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        //GPS
        //noinspection deprecation
        locationRequest = new LocationRequest();
        locationRequest.setInterval(400);
        locationRequest.setFastestInterval(400);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
                if (!measurementEnabled) {
                    updateLocationValues(currentLocation);
                }
            }
        };


        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            }

        }

        btnSend.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {

                if (measurementEnabled) {
                    measurementEnabled = false;
                    stopMeasurement();
                } else {
                    if(currentLocation == null) {
                        Toast.makeText(MainActivity.this, "No location", Toast.LENGTH_SHORT).show();
                    } else {
                        measurementEnabled = true;
                        startMeasurement();
                    }
                }

            }

        });

    }

    private void startMeasurement(){
        btnSend.setText("STOP");
        measurementLocation = currentLocation;
        textStationsCount.setText("Searching...");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        @SuppressLint("MissingPermission")
        List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
        for (CellInfo info : cellInfoList){
            if(info instanceof  CellInfoGsm){
                CellInfoGsm cellInfoGsm = (CellInfoGsm) info;
                //0 - omnidirectional antenna
                //2147483647 - UNAVAILABLE
                //65535 - Integer.MAX_VALUE=UNKNOWN_VALUE
                if(cellInfoGsm.getCellIdentity().getCid() != 65535 && cellInfoGsm.getCellIdentity().getCid() != 2147483647 && cellInfoGsm.getCellIdentity().getCid() != 0 )
                {
                    MeasurementCell measurementCell = new MeasurementCell();
                    measurementCell.cellId = cellInfoGsm.getCellIdentity().getCid();
                    measurementCells.add(measurementCell);

                }
            }
            if(info instanceof CellInfoLte){
                CellInfoLte cellInfoLte = (CellInfoLte) info;
                if(cellInfoLte.getCellIdentity().getCi() != 2147483647)
                {
                    MeasurementCell measurementCell = new MeasurementCell();
                    measurementCell.cellId = cellInfoLte.getCellIdentity().getCi();
                    measurementCells.add(measurementCell);
                }
            }

        }
        if(measurementCells.size()>0)
        {
            startTimer();
            collectMeasurementsForCells();
        }
        else
        {
            stopMeasurement();
        }

    }

    private void stopMeasurement(){
        btnSend.setText("START");
        measurementLocation = null;
        stopTimer();
        measurementCells.clear();
        textStationsCount.setText("");
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(timer != null){
            timer.cancel();
            timer = null;
        }
    }

    private void collectMeasurementsForCells(){
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {

                    TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    @SuppressLint("MissingPermission")
                    List<CellInfo> cellInfoList =telephonyManager.getAllCellInfo();
                    if(cellInfoList!=null)
                    {
                        for(int i=0;i<cellInfoList.size();i++)
                        {
                            CellInfo info = cellInfoList.get(i);
                            if(info instanceof CellInfoGsm){
                                CellInfoGsm cellInfoGsm = (CellInfoGsm) info;

                                if(measurementCells.stream().allMatch(m -> m.measurementsList.size() >= MAX_MEASUREMENT_PER_CELL ) || downTimer ==0 )
                                {
                                    if(timer!= null )
                                    {
                                        runOnUiThread(() -> {
                                            putDataToFirebase();
                                        });
                                        timer.cancel();
                                        timer = null;
                                    }
                                }
                                else {

                                    // dla kazdego cellid ->rssi
                                    // z tablicy measerementCells która zawiera wszytskie cellid dodajemy odpowiadające próki rssi przechowywane w measurementList

                                    measurementCells.stream().filter(m -> m.cellId == cellInfoGsm.getCellIdentity().getCid() && m.measurementsList.size() < MAX_MEASUREMENT_PER_CELL
                                            ).forEach(m -> m.measurementsList.add(cellInfoGsm.getCellSignalStrength().getDbm()));


                                    runOnUiThread(() -> {

                                        String measurementText = "";
                                        for (MeasurementCell m : measurementCells) {
                                            measurementText = measurementText + m.cellId + ": " + m.measurementsList.size() + "/" + MAX_MEASUREMENT_PER_CELL + "\n";

                                        }
                                        textStationsCount.setText(measurementText);
                                        if(downTimer == 1) {
                                            measurementCells.removeIf(m->m.measurementsList.size()<=50);
                                        }

                                    });

                                }

                            }
                            if(info instanceof CellInfoLte){
                                CellInfoLte cellInfoLte = (CellInfoLte) info;

                                if(measurementCells.stream().allMatch(m -> m.measurementsList.size() >= MAX_MEASUREMENT_PER_CELL ) || downTimer ==0 )
                                {
                                    if(timer!= null )
                                    {
                                        runOnUiThread(() -> {
                                            putDataToFirebase();
                                        });
                                        timer.cancel();
                                        timer = null;
                                    }
                                }
                                else {

                                    // dla kazdego cellid ->rssi
                                    // z tablicy measerementCells która zawiera wszytskie cellid dodajemy odpowiadające próki rssi przechowywane w measurementList

                                    measurementCells.stream().filter(m -> m.cellId == cellInfoLte.getCellIdentity().getCi() && m.measurementsList.size() < MAX_MEASUREMENT_PER_CELL
                                    ).forEach(m -> m.measurementsList.add(cellInfoLte.getCellSignalStrength().getDbm()));


                                    runOnUiThread(() -> {

                                        String measurementText = "";
                                        for (MeasurementCell m : measurementCells) {
                                            measurementText = measurementText + m.cellId + ": " + m.measurementsList.size() + "/" + MAX_MEASUREMENT_PER_CELL + "\n";

                                        }
                                        textStationsCount.setText(measurementText);
                                        if(downTimer == 1) {
                                            measurementCells.removeIf(m->m.measurementsList.size()<=50);
                                        }


                                    });

                                }

                            }

                        }
                    }

                }
                catch (Exception e){

                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error while getting cells info", Toast.LENGTH_SHORT).show());
                }
            }
        }, 400,MEASUREMENT_PERIOD);
    }

    private void putDataToFirebase(){

        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference().child("Measurements");

        String key = myRef.push().getKey();
        LocationMeasurement locationMeasurement = new LocationMeasurement();
        locationMeasurement.lat = measurementLocation.getLatitude();
        locationMeasurement.lon = measurementLocation.getLongitude();
        locationMeasurement.measurementAllCells = measurementCells;
        assert key != null;
        myRef.child(key).setValue(locationMeasurement);
        Toast.makeText(MainActivity.this, "Data sent to the Firebase ", Toast.LENGTH_SHORT).show();
        measurementEnabled = false;
        stopMeasurement();

    }


    private void updateLocationValues(Location location){
        textLat.setText(String.valueOf(location.getLatitude()));
        textLon.setText(String.valueOf(location.getLongitude()));
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 100:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);

                } else {
                    Toast.makeText(MainActivity.this, "Permission denied", Toast.LENGTH_LONG).show();
                }
        }
    }

    public void startTimer() {

        countDownTimer =  new CountDownTimer(100000, 1000) {


            @SuppressLint("SetTextI18n")
            public void onTick(long millisUntilFinished) {
                int total = (int) (millisUntilFinished / 1000);

                text.setText("Seconds remaining: " +total);
                downTimer = Integer.parseInt(String.valueOf(total));
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onFinish() {
                text.setText("Seconds remaining: 0");

            }
        }.start();
    }

    @SuppressLint("SetTextI18n")
    public void stopTimer(){

        countDownTimer.cancel();
        text.setText("RESET");
    }

}