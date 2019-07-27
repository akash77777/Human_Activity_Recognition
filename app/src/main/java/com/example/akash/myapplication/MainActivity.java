package com.example.akash.myapplication;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;

import android.widget.Toast;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.speech.tts.TextToSpeech;

import android.widget.TextView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;




public class MainActivity extends AppCompatActivity implements SensorEventListener, TextToSpeech.OnInitListener {

    private static final int N_SAMPLES = 200;
    private static List<Float> x;
    private static List<Float> y;
    private static List<Float> z;
    private TextView downstairsTextView;
    String message=" ";
    final int SEND_SMS_PERMISSION_REQUEST_CODE=1;


    private TextView collapseTextView;
    private TextView sittingTextView;
    private TextView standingTextView;
    private TextView upstairsTextView;
    private TextView walkingTextView;
    private TextToSpeech textToSpeech;
    private float[] results;
    private TensorFlowClassifier classifier;

    private String[] labels = {"Collapse", "Sitting", "Standing", "Walking"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        x = new ArrayList<>();
        y = new ArrayList<>();
        z = new ArrayList<>();

        collapseTextView = (TextView) findViewById(R.id.collapse_prob);
        sittingTextView = (TextView) findViewById(R.id.sitting_prob);
        standingTextView = (TextView) findViewById(R.id.standing_prob);
        walkingTextView = (TextView) findViewById(R.id.walking_prob);

        classifier = new TensorFlowClassifier(getApplicationContext());

        textToSpeech = new TextToSpeech(this, this);
        textToSpeech.setLanguage(Locale.US);
    }
    public boolean checkPermission(String permission)
    {
int check=ContextCompat.checkSelfPermission(this,permission);
return(check==PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onInit(int status) {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (results == null || results.length == 0) {
                    return;
                }
                float max = -1;
                int idx = -1;
                for (int i = 0; i < results.length; i++) {
                    if (results[i] > max) {
                        idx = i;
                        max = results[i];
                    }
                }

                textToSpeech.speak(labels[idx], TextToSpeech.QUEUE_ADD, null, Integer.toString(new Random().nextInt()));
            }
        }, 2000, 5000);
    }

    protected void onPause() {
        getSensorManager().unregisterListener(this);
        super.onPause();
    }

    protected void onResume() {
        super.onResume();
        getSensorManager().registerListener(this, getSensorManager().getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        activityPrediction();
        x.add(event.values[0]);
        y.add(event.values[1]);
        z.add(event.values[2]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void activityPrediction() {
        if (x.size() == N_SAMPLES && y.size() == N_SAMPLES && z.size() == N_SAMPLES) {
            List<Float> data = new ArrayList<>();
            data.addAll(x);
            data.addAll(y);
            data.addAll(z);

            results = classifier.predictProbabilities(toFloatArray(data));
            collapseTextView.setText(Float.toString(round(results[0], 2)));
            sittingTextView.setText(Float.toString(round(results[1], 2)));
            standingTextView.setText(Float.toString(round(results[2], 2)));
            walkingTextView.setText(Float.toString(round(results[3], 2)));
            if(results[1]*10>=5){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.SEND_SMS},SEND_SMS_PERMISSION_REQUEST_CODE);
            if(checkPermission(Manifest.permission.SEND_SMS))
            {
                LocationManager locationManager;
                LocationListener locationListener;
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                //locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0,MainActivity.th);
                locationListener = new LocationListener()
                {
                    @Override
                    public void onLocationChanged(Location location) {


                        message=location.getLatitude() + "," + location.getLongitude();
                    }


                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        Log.d("Latitude","status");

                    }

                    @Override
                    public void onProviderEnabled(String provider)
                    {
                        Log.d("Latitude","enable");
                    }

                    @Override
                    public void onProviderDisabled(String provider)
                    {
                        Log.d("Latitude","disable");
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                };
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission
                                (getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET}, 10);
                    }
                    return;
                }
                locationManager.requestLocationUpdates("gps", 1000, 5, locationListener);
                SmsManager smsManager = SmsManager.getDefault();
                String message1="I am at emergency in:"+"\\https://maps.app.goo.gl\\";
                smsManager.sendTextMessage("7097442266", null, message1, null, null);
                Toast.makeText(getApplicationContext(), "stop  htgh", Toast.LENGTH_LONG).show();;
            }
            }
            x.clear();
            y.clear();
            z.clear();
        }
    }

   //comment this for temporary use
    public void sms()
    {

        LocationManager locationManager;
            LocationListener locationListener;
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            //locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 0, 0,MainActivity.th);
            locationListener = new LocationListener()
            {
                @Override
                public void onLocationChanged(Location location) {


                    message=location.getLatitude() + "," + location.getLongitude();
                }


                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    Log.d("Latitude","status");

                }

                @Override
                public void onProviderEnabled(String provider)
                {
                    Log.d("Latitude","enable");
                }

                @Override
                public void onProviderDisabled(String provider)
                {
                    Log.d("Latitude","disable");
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            };
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission
                            (getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.INTERNET}, 10);
                }
                return;
            }
            locationManager.requestLocationUpdates("gps", 1000, 5, locationListener);


            Toast.makeText(getApplicationContext(), "stop", Toast.LENGTH_LONG).show();
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            SmsManager smsManager = SmsManager.getDefault();
            String message1="I am at emergency in:https://maps.app.goo.gl";
            smsManager.sendTextMessage("7097442266", null, message1, null, null);
            Toast.makeText(getApplicationContext(), "stop  htgh", Toast.LENGTH_LONG).show();
        }


    private float[] toFloatArray(List<Float> list) {
        int i = 0;
        float[] array = new float[list.size()];

        for (Float f : list) {
            array[i++] = (f != null ? f : Float.NaN);
        }
        return array;
    }

    private static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    private SensorManager getSensorManager() {
        return (SensorManager) getSystemService(SENSOR_SERVICE);
    }

}
