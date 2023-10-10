package com.example.uploadimages;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int UPDATE_INTERVAL = 5000; // 5 seconds

    FusedLocationProviderClient locationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;
    private Location currentLocation;

    private int LOCATION_PERMISSION = 100;

    Button buttonGetLocation, buttonStopLocation;
    TextView textViewLatitude, textViewLongitude;
    private Button btn;
    private ImageView iv;
    private TextView tv;
    private ProgressBar progressBar;

    private final int GALLERY = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        askForPermissions();

        btn = findViewById(R.id.btnUpload);
        iv = findViewById(R.id.imageView);
        tv = findViewById(R.id.message);
        progressBar = findViewById(R.id.progressBar);
        tv.setText("");

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");

                startActivityForResult(Intent.createChooser(intent, "Select Image"), GALLERY);
            }
        });
        buttonGetLocation = (Button)findViewById(R.id.buttonGetLocation);
        buttonStopLocation = (Button)findViewById(R.id.buttonStopLocation);

        textViewLatitude = (TextView)findViewById(R.id.textViewLatitude);
        textViewLongitude = (TextView)findViewById(R.id.textViewLongitude);

        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
                if(locationAvailability.isLocationAvailable()){
                    Log.i(TAG,"Location is available");
                }else {
                    Log.i(TAG,"Location is unavailable");
                }
            }

            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Log.i(TAG,"Location result is available");
            }
        };


        buttonGetLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startGettingLocation();
            }
        });

        buttonStopLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopLocationRequests();
            }
        });


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == this.RESULT_CANCELED) {
            return;
        }
        if (requestCode == GALLERY) {
            if (data != null) {
                Uri uri = data.getData();
                try {

                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    iv.setImageBitmap(bitmap);

                    uploadImageUsingRetrofit(bitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Failed to select image!", Toast.LENGTH_SHORT).show();
                    tv.setText("Failed to select image!");
                    tv.setTextColor(Color.parseColor("#FF0000"));
                }
            }
        }
    }

    private void uploadImageUsingRetrofit(Bitmap bitmap){

        progressBar.setVisibility(View.VISIBLE);
        tv.setText("");
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        String image = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
        String name = String.valueOf(Calendar.getInstance().getTimeInMillis());

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(MyImageInterface.IMAGEURL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        MyImageInterface myImageInterface = retrofit.create(MyImageInterface.class);

        Call<String> call = myImageInterface.getImageData(name,image);

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful()) {
                    if (response.body() != null) {

                        Toast.makeText(MainActivity.this, "Image Uploaded Successfully!!", Toast.LENGTH_SHORT).show();
                        tv.setText("Image Uploaded Successfully!!");
                        tv.setTextColor(Color.parseColor("#008000"));

                    } else {
                        tv.setText("No response from the server");
                        tv.setTextColor(Color.parseColor("#FF0000"));
                    }
                }else{
                    tv.setText("Response not successful "+response.toString());
                    tv.setTextColor(Color.parseColor("#FF0000"));
                    Toast.makeText(getApplicationContext(), "Response not successful "+response.toString(), Toast.LENGTH_SHORT).show();

                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), "Error occurred!", Toast.LENGTH_SHORT).show();
                tv.setText("Error occurred during upload");
                tv.setTextColor(Color.parseColor("#FF0000"));

            }
        });
    }


    private void askForPermissions(){
        Dexter.withActivity(this)
                .withPermissions(

                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            Toast.makeText(getApplicationContext(), "All permissions are granted by user!", Toast.LENGTH_SHORT).show();
                        }

                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // show alert dialog navigating to Settings

                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).
                withErrorListener(new PermissionRequestErrorListener() {
                    @Override
                    public void onError(DexterError error) {
                        Toast.makeText(getApplicationContext(), "Some Error! ", Toast.LENGTH_SHORT).show();
                    }
                })
                .onSameThread()
                .check();
    }

    private void startGettingLocation() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            locationProviderClient.requestLocationUpdates(locationRequest,locationCallback, MainActivity.this.getMainLooper());
            locationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    currentLocation = location;
                    textViewLatitude.setText(""+currentLocation.getLatitude());
                    textViewLongitude.setText(""+currentLocation.getLongitude());
                }
            });

            locationProviderClient.getLastLocation().addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.i(TAG, "Exception while getting the location: "+e.getMessage());
                }
            });


        }else {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                Toast.makeText(MainActivity.this, "Permission needed", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[] { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        LOCATION_PERMISSION);
            }
        }
    }

    private void stopLocationRequests(){
        locationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        startGettingLocation();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationRequests();

    }
}