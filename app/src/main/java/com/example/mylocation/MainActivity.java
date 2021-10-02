package com.example.mylocation;

import androidx.annotation.NonNull;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;

import android.annotation.SuppressLint;
import android.app.AlertDialog;

import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private SupportMapFragment supportMapFragment;
    private FusedLocationProviderClient client;
    private RelativeLayout relativeLayout;
    private final int REQUEST_CHECK_CODE = 8989;
    private GoogleApiClient googleApiClient;
    private String uri;
    private Place place;
    private EditText searchLoc;
    private View bottomSheet;
    private BottomSheetBehavior bottomSheetBehavior;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));

        initValues();
        checkLocationPermission();

        try {
            if (isConnected())
                checkLocationPermission();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

    }

    private void initValues() {
        Log.d("TAG", "In initValues");
        relativeLayout = findViewById(R.id.activity_main);
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.google_map);
        client = LocationServices.getFusedLocationProviderClient(this);
        searchLoc = findViewById(R.id.searchLoc);

        //Initialize places
        // Enter you API KEY in second param
        Places.initialize(getApplicationContext(), "");
        searchLoc.setFocusable(false);
    }

    private void checkLocationPermission() {
        Log.d("TAG", "In checkLocationPermission");
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            checkLocationOnOff();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("ShowToast")
    private void checkLocationOnOff() {
        Log.d("TAG", "In checkLocationOnOff");

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API).addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).build();
            googleApiClient.connect();
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(30 * 1000);
            locationRequest.setFastestInterval(5 * 1000);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);

            // **************************
            builder.setAlwaysShow(true); // this is the key ingredient
            // **************************

            PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi
                    .checkLocationSettings(googleApiClient, builder.build());
            result.setResultCallback(result1 -> {
                final Status status = result1.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location
                        // requests here.
                        try {
                            if (isConnected())
                                getCurrentLocation();
                            else {
                                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                                bottomSheet.setVisibility(View.GONE);
                                Snackbar.make(relativeLayout,
                                        "You are not connected to internet", Snackbar.LENGTH_SHORT)
                                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                        .setBackgroundTint(getColor(R.color.black))
                                        .setTextColor(getColor(R.color.white))
                                        .show();
                            }
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be
                        // fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling
                            // startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_CODE);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have
                        // no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            });
        }
    }

    public boolean isConnected() throws InterruptedException, IOException {
        Log.d("TAG", "In isConnected");
        String command = "ping -c 1 google.com";
        return Runtime.getRuntime().exec(command).waitFor() == 0;
    }

    @SuppressLint("ShowToast")
    private void getCurrentLocation() {
        Log.d("TAG", "In getCurrentLocation");
            @SuppressLint("MissingPermission")
            Task<Location> task = client.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null);
            task.addOnSuccessListener(location -> {
                if(location!= null){

                    supportMapFragment.getMapAsync(googleMap -> {
                        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
                        MarkerOptions options = new MarkerOptions().position(latLng).title("Your Location");
                        options.visible(true).draggable(false);
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));
                        googleMap.addMarker(options);

                        uri = "http://maps.google.com/maps?saddr=" +location.getLatitude()+","+location.getLongitude();

                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        bottomSheet.setVisibility(View.GONE);
                        Snackbar.make(relativeLayout,
                                "Location added successfully", Snackbar.LENGTH_SHORT)
                                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                .setBackgroundTint(getColor(R.color.black))
                                .setTextColor(getColor(R.color.white))
                                .show();
                    });

                }else {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    bottomSheet.setVisibility(View.GONE);
                    Snackbar.make(relativeLayout,
                            "Failed to get location", Snackbar.LENGTH_SHORT)
                            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                            .setBackgroundTint(getColor(R.color.black))
                            .setTextColor(getColor(R.color.white))
                            .show();
                    Log.d("TAG", "Location fetch error - ");
                }
            }).addOnFailureListener(e -> {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                bottomSheet.setVisibility(View.GONE);
                Snackbar.make(relativeLayout,
                        "Failed to get location" + e.getMessage(), Snackbar.LENGTH_SHORT)
                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                        .setBackgroundTint(getColor(R.color.black))
                        .setTextColor(getColor(R.color.white))
                        .show();
                Log.d("TAG", "Location fetch error - " + e.getMessage());
            });
    }

    @SuppressLint("ShowToast")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        Log.d("TAG", "In onActivityResult");
        if (requestCode==REQUEST_CHECK_CODE){
            if (resultCode==RESULT_OK){
                try {
                    if (isConnected()) {
                        getCurrentLocation();
                    }else{
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        bottomSheet.setVisibility(View.GONE);
                        Snackbar.make(relativeLayout,
                                "You are not connected to internet", Snackbar.LENGTH_SHORT)
                                .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                                .setBackgroundTint(getColor(R.color.black))
                                .setTextColor(getColor(R.color.white))
                                .show();
                    }
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                bottomSheet.setVisibility(View.GONE);
                Snackbar.make(relativeLayout,
                        "Location not turned on", Snackbar.LENGTH_SHORT)
                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                        .setBackgroundTint(getColor(R.color.black))
                        .setTextColor(getColor(R.color.white))
                        .show();
            }
        }else if (requestCode==100){

            if (resultCode==RESULT_OK){
                //When Success
                //Initialize place
                assert data != null;
                place = Autocomplete.getPlaceFromIntent(data);
                //Set City
                LatLng latLng = new LatLng(Objects.requireNonNull(place.getLatLng()).latitude, place.getLatLng().longitude);
                MarkerOptions options = new MarkerOptions().position(latLng).title("Your Location");
                options.visible(true).draggable(false);

                supportMapFragment.getMapAsync(googleMap -> {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));
                    googleMap.addMarker(options);
                });

                uri = "http://maps.google.com/maps?saddr=" +place.getLatLng().latitude+","+place.getLatLng().longitude;

                Snackbar.make(relativeLayout,
                        "Location added successfully", Snackbar.LENGTH_SHORT)
                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                        .setBackgroundTint(getColor(R.color.black))
                        .setTextColor(getColor(R.color.white))
                        .show();
            }else if (resultCode == AutocompleteActivity.RESULT_ERROR){
                //When error
                //Initialize status
                assert data != null;
                Status status = Autocomplete.getStatusFromIntent(data);
                //Display toast
                Snackbar.make(relativeLayout,
                        "Failed to add location " + status.getStatusMessage(), Snackbar.LENGTH_SHORT)
                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                        .setBackgroundTint(getColor(R.color.black))
                        .setTextColor(getColor(R.color.white))
                        .show();
                Log.d("TAG", "Search Location error - " + status.getStatusMessage());
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressLint("ShowToast")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d("TAG", "In onRequestPermissionsResult");
        if(requestCode == 44){
            if (grantResults.length > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                checkLocationOnOff();
            }else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                bottomSheet.setVisibility(View.GONE);
                Snackbar.make(relativeLayout,
                        "Location permission is required", Snackbar.LENGTH_SHORT)
                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                        .setBackgroundTint(getColor(R.color.black))
                        .setTextColor(getColor(R.color.white))
                        .show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bottomSheet = findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true).setTitle("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    MainActivity.super.onBackPressed();
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onConnected(@Nullable @org.jetbrains.annotations.Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull @NotNull ConnectionResult connectionResult) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @SuppressLint("ShowToast")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId()==R.id.shareLoc){
            if (uri!=null){
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "This is my location");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, uri);
                startActivity(Intent.createChooser(sharingIntent, "Share via"));
            }else {
                Snackbar.make(relativeLayout,
                        "Failed to get location", Snackbar.LENGTH_SHORT)
                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                        .setBackgroundTint(getColor(R.color.black))
                        .setTextColor(getColor(R.color.white))
                        .show();
            }

        }
        return super.onOptionsItemSelected(item);
    }

}