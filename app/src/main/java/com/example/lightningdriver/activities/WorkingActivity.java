package com.example.lightningdriver.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lightningdriver.R;
import com.example.lightningdriver.models.Driver;
import com.example.lightningdriver.models.Vehicle;
import com.example.lightningdriver.services.MyLocationService;
import com.google.android.gms.location.FusedLocationProviderClient;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class WorkingActivity extends AppCompatActivity implements OnMapReadyCallback {

    AppCompatButton buttonEnableConnection;
    TextView textAvailableStatus;
    CircleImageView imgProfile, imgVehicle;
    TextView textVehicleName, textPlateNumber;

    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 123;
    private static final int ACCESS_COARSE_LOCATION_REQUEST_CODE = 234;
    public static String markerIconName = "motor_marker_icon";
    public static final String taxiMarker = "taxi_marker";
    public static final String motorMarker = "motor_marker_icon";
    public static int driverMarkerSize = 160;
    public static int zoomToDriver = 17;

    public static GoogleMap map;
    public static Marker currentLocationMarker;
    static WorkingActivity instance;

    public static WorkingActivity getInstance() {
        return instance;
    }
    public static boolean isRunning = false;

    MyLocationService mLocationService;
    private FusedLocationProviderClient fusedLocationClient;

    Intent mServiceIntent;
    LatLng UET;
    boolean workingIsEnable = false;
    public static Driver driver;
    public static Vehicle vehicle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_working);

        init();
        loadDriverInfo();
        listener();
    }

    private void loadDriverInfo() {
        FirebaseDatabase.getInstance().getReference().child("Drivers")
                .child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        driver = snapshot.getValue(Driver.class);
                        if (driver != null) {
                            setDriverInfoView(driver);
                            loadVehicleInfo(driver.getVehicleId());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void loadVehicleInfo(String vehicleId) {
        FirebaseDatabase.getInstance().getReference().child("Vehicles")
                .child(vehicleId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        vehicle = snapshot.getValue(Vehicle.class);
                        if (vehicle != null) {
                            setVehicleInfoView(vehicle);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void setVehicleInfoView(Vehicle vehicle) {
        if (vehicle.getType().equals("car")) {
            markerIconName = taxiMarker;
            Picasso.get().load(vehicle.getVehicleImageUrl()).placeholder(R.drawable.shipper).resize(500, 500).centerCrop().into(imgVehicle);
        } else if (vehicle.getType().equals("motorbike")) {
            markerIconName = motorMarker;
            Picasso.get().load(vehicle.getVehicleImageUrl()).placeholder(R.drawable.car).resize(500, 500).centerCrop().into(imgVehicle);
        }

        if (currentLocationMarker != null) {
            currentLocationMarker.setIcon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(markerIconName, driverMarkerSize, driverMarkerSize)));
        }

        textVehicleName.setText(vehicle.getName());
        textPlateNumber.setText(vehicle.getPlateNumber());
    }

    private void setDriverInfoView(Driver driver) {
        Picasso.get().load(driver.getDriverImageUrl()).placeholder(R.drawable.user_blue).resize(1000, 1000).into(imgProfile);
    }

    private void listener() {
        buttonEnableConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!workingIsEnable) {
                    buttonEnableConnection.setText("  Connected");
                    buttonEnableConnection.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_green_background));
                    textAvailableStatus.setText("You are online");
                    workingIsEnable = true;
                    startServiceFunc();
                } else {
                    buttonEnableConnection.setText("  Enable connection");
                    buttonEnableConnection.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_black_background));
                    textAvailableStatus.setText("You are offline");
                    workingIsEnable = false;
                    stopServiceFunc();
                }
            }
        });
    }

    private void init() {
        buttonEnableConnection = findViewById(R.id.buttonEnable);
        textAvailableStatus = findViewById(R.id.text_status);
        textPlateNumber = findViewById(R.id.text_plateNumber);
        textVehicleName = findViewById(R.id.text_vehicleName);
        imgProfile = findViewById(R.id.img_profile);
        imgVehicle = findViewById(R.id.imgVehicle);

        instance = this;
        driver = new Driver();

        UET = new LatLng(21.038902482537342, 105.78296809797327); //Dai hoc Cong Nghe Lat Lng

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.fragment_maps);
        mapFragment.getMapAsync(this);

        if (isMyServiceRunning(MyLocationService.class, getInstance())) {
            buttonEnableConnection.setText("  Connected");
            buttonEnableConnection.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_green_background));
            textAvailableStatus.setText("You are online");
            workingIsEnable = true;
        } else {
            buttonEnableConnection.setText("  Enable connection");
            buttonEnableConnection.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_black_background));
            textAvailableStatus.setText("You are offline");
            workingIsEnable = false;
        }
    }

    public void updateLocationMarker(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (map != null) {
            currentLocationMarker.remove();

            currentLocationMarker = map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("You are here!")
                    .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(markerIconName, 120, 120))));

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomToDriver));
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
                        Float bearing = location.getBearing();
                        if (map != null) {
                            if (currentLocationMarker != null)
                                currentLocationMarker.remove();

                            currentLocationMarker = map.addMarker(new MarkerOptions()
                                    .position(latLng)
                                    .title("You are here!")
                                    .anchor(0.5f, 0.5f)
                                    .rotation(bearing)
                                    .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(markerIconName, driverMarkerSize, driverMarkerSize))));

                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomToDriver));
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
        map.getUiSettings().setRotateGesturesEnabled(false);

        requestPermission();
        markCurrentLocation();
    }

    private void startServiceFunc(){
        mLocationService = new MyLocationService();
        mServiceIntent = new Intent(this, mLocationService.getClass());
        if (!isMyServiceRunning(mLocationService.getClass(), this)) {
            startService(mServiceIntent);
            //Toast.makeText(this, "Service start successfully", Toast.LENGTH_SHORT).show();
        } else {
            //Toast.makeText(this, "Service is already running", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopServiceFunc(){
        mLocationService = new MyLocationService();
        mServiceIntent = new Intent(this, mLocationService.getClass());
        if (isMyServiceRunning(mLocationService.getClass(), this)) {
            stopService(mServiceIntent);
            //Toast.makeText(this, "Service stopped!!", Toast.LENGTH_SHORT).show();
        } else {
            //Toast.makeText(this, "Service is already stopped!!", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isMyServiceRunning(Class<?> serviceClass, Activity mActivity) {
        ActivityManager manager = (ActivityManager) mActivity.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        isRunning = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isRunning = false;
    }
}