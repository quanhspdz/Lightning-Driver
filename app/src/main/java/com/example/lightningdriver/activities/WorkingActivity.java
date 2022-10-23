package com.example.lightningdriver.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;

import com.example.lightningdriver.R;
import com.example.lightningdriver.models.CurrentPosition;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.Objects;

public class WorkingActivity extends AppCompatActivity implements OnMapReadyCallback {

    GoogleMap map;
    Marker currentLocationMarker;

    LatLng UET;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 123;
    private static final int ACCESS_COARSE_LOCATION_REQUEST_CODE = 234;

    public final String markerIconName = "lightning_circle";

    LocationListener locationListener;
    LocationManager locationManager;
    private LocationRequest locationRequest;

    LatLng lastLocation;

    LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_working);

        init();
    }

    private void init() {
        UET = new LatLng(21.038902482537342, 105.78296809797327); //Dai hoc Cong Nghe Lat Lng

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                updateLocationMarker(Objects.requireNonNull(locationResult.getLastLocation()));
                updateLocationOnFirebase(Objects.requireNonNull(locationResult.getLastLocation()));
            }
        };

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.fragment_maps);
        mapFragment.getMapAsync(this);
    }

    private void updateLocationOnFirebase(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (latLng.equals(lastLocation)) {
            return;
        }

        CurrentPosition currentPosition = new CurrentPosition(
                Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid(),
                latLng.toString(),
                Calendar.getInstance().getTime().toString()
        );

        FirebaseDatabase.getInstance().getReference().child("CurrentPosition")
                .child("Driver")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(currentPosition);

        lastLocation = latLng;
    }

    private void updateLocationMarker(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (latLng.equals(lastLocation)) {
            return;
        }

        if (map != null) {
            currentLocationMarker.remove();

            currentLocationMarker = map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("You are here!")
                    .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(markerIconName, 120, 120))));

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
        }
    }

    private void markCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            requestPermission();
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                        if (map != null) {
                            if (currentLocationMarker != null) {
                                currentLocationMarker.remove();
                            }
                            currentLocationMarker = map.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title("You are here!")
                                    //.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                                    .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(markerIconName, 120, 120))));
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
                        } else {
                            Toast.makeText(WorkingActivity.this, "Map is null", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    public Bitmap resizeMapIcons(String iconName, int width, int height) {
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(iconName, "drawable", getPackageName()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return resizedBitmap;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                ACCESS_FINE_LOCATION_REQUEST_CODE
        );

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                ACCESS_COARSE_LOCATION_REQUEST_CODE
        );
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        requestPermission();
        markCurrentLocation();
        startLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            requestPermission();
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

}