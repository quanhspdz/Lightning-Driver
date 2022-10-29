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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lightningdriver.R;
import com.example.lightningdriver.models.Driver;
import com.example.lightningdriver.models.Passenger;
import com.example.lightningdriver.models.Trip;
import com.example.lightningdriver.models.Vehicle;
import com.example.lightningdriver.tools.Const;
import com.example.lightningdriver.tools.DecodeTool;
import com.example.services.MyLocationService;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class PickUpActivity extends AppCompatActivity implements OnMapReadyCallback {
    TextView textPassengerName, textPickUp, textMoney, textPaymentMethod, textStatus;
    RelativeLayout layoutCall, layoutChat, layoutTripInfo, layoutBottom;
    AppCompatButton buttonArrived;
    CircleImageView imgFocusOnMe;

    public static GoogleMap map;
    public static Marker currentLocationMarker;
    public static boolean isRunning = false;
    public static boolean bottomLayoutIsVisible = false;
    public static boolean focusOnMe = true;

    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 123;
    private static final int ACCESS_COARSE_LOCATION_REQUEST_CODE = 234;
    public static String markerIconName = "motor_marker_icon";
    public static final String taxiMarker = "taxi_marker";
    public static final String motorMarker = "motor_marker_icon";
    private static final String pickUpMarkerName = "pick_up_marker";
    private static final String desMarkerName = "des_marker";
    public static int driverMarkerSize = 160;
    public static int zoomToDriver = 17;

    MyLocationService mLocationService;
    private FusedLocationProviderClient fusedLocationClient;

    Intent mServiceIntent;
    LatLng UET;
    String tripId;
    boolean workingIsEnable = false;
    public static Driver driver;
    public static Vehicle vehicle;
    public static Trip trip;
    public static Passenger passenger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_up);

        init();
        getTripInfo();
        listener();
    }

    private void listener() {
        textStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bottomLayoutIsVisible) {
                    bottomLayoutIsVisible = false;
                    layoutTripInfo.setVisibility(View.GONE);
                } else {
                    bottomLayoutIsVisible = true;
                    layoutTripInfo.setVisibility(View.VISIBLE);
                }
            }
        });

        imgFocusOnMe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (focusOnMe) {
                    focusOnMe = false;
                    imgFocusOnMe.setImageResource(R.drawable.unfocus);
                } else {
                    focusOnMe = true;
                    imgFocusOnMe.setImageResource(R.drawable.focus);
                }
            }
        });
    }

    private void getTripInfo() {
        Intent intent = getIntent();
        tripId = intent.getStringExtra("tripId");
        if (tripId != null) {
            FirebaseDatabase.getInstance().getReference().child("Trips")
                    .child(tripId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            trip = snapshot.getValue(Trip.class);
                            if (trip != null) {
                                loadTripInfo(trip);
                                markPickUpAndDropOff(trip);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
        }
    }

    private void loadTripInfo(Trip trip) {
        textPickUp.setText(trip.getPickUpName());
        textMoney.setText(trip.getCost());

        FirebaseDatabase.getInstance().getReference().child("Passengers")
                .child(trip.getPassengerId())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        passenger = snapshot.getValue(Passenger.class);
                        if (passenger != null) {
                            textPassengerName.setText(passenger.getName());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    public void init() {
        textMoney = findViewById(R.id.text_money);
        textPassengerName = findViewById(R.id.text_passengerName);
        textPickUp = findViewById(R.id.text_pick_up_location);
        textPaymentMethod = findViewById(R.id.text_paymentMethod);
        textStatus = findViewById(R.id.text_status);

        layoutCall = findViewById(R.id.layout_call);
        layoutChat = findViewById(R.id.layout_chat);
        layoutTripInfo = findViewById(R.id.layout_tripInfo);
        layoutBottom = findViewById(R.id.layout_bottom);

        buttonArrived = findViewById(R.id.button_arrived);

        imgFocusOnMe = findViewById(R.id.img_focusOnMe);

        UET = new LatLng(21.038902482537342, 105.78296809797327); //Dai hoc Cong Nghe Lat Lng

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.fragment_maps);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setRotateGesturesEnabled(false);

        map.getUiSettings().setMyLocationButtonEnabled(true);

        markCurrentLocation();
    }

    private void markCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

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
                            Toast.makeText(PickUpActivity.this, "Map is null", Toast.LENGTH_SHORT).show();
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


    private void markPickUpAndDropOff(Trip trip) {
        LatLng pickup = DecodeTool.getLatLngFromString(trip.getPickUpLocation());
        LatLng dropOff = DecodeTool.getLatLngFromString(trip.getDropOffLocation());

        map.addMarker(new MarkerOptions()
                .position(pickup)
                .title("Pick-up")
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(pickUpMarkerName, driverMarkerSize, driverMarkerSize))));

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(pickup, zoomToDriver));

        map.addMarker(new MarkerOptions()
                .position(dropOff)
                .title("Drop-off")
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(desMarkerName, driverMarkerSize, driverMarkerSize))));

    }

    private void startServiceFunc(){
        mLocationService = new MyLocationService();
        mServiceIntent = new Intent(this, mLocationService.getClass());
        if (!isMyServiceRunning(mLocationService.getClass(), this)) {
            startService(mServiceIntent);
            Toast.makeText(this, "Service start successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Service is already running", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopServiceFunc(){
        mLocationService = new MyLocationService();
        mServiceIntent = new Intent(this, mLocationService.getClass());
        if (isMyServiceRunning(mLocationService.getClass(), this)) {
            stopService(mServiceIntent);
            Toast.makeText(this, "Service stopped!!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Service is already stopped!!", Toast.LENGTH_SHORT).show();
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
    protected void onStart() {
        super.onStart();
        isRunning = true;

        if (!(isMyServiceRunning(MyLocationService.class, this))) {
            startServiceFunc();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        isRunning = false;
    }
}