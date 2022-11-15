package com.example.lightningdriver.activities;

import static java.lang.Thread.sleep;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.lightningdriver.R;
import com.example.lightningdriver.models.CurrentPosition;
import com.example.lightningdriver.models.Driver;
import com.example.lightningdriver.models.Passenger;
import com.example.lightningdriver.models.Trip;
import com.example.lightningdriver.models.Vehicle;
import com.example.lightningdriver.tools.Const;
import com.example.lightningdriver.tools.DecodeTool;
import com.example.lightningdriver.services.MyLocationService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.PolyUtil;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class PickUpActivity extends AppCompatActivity implements OnMapReadyCallback {
    TextView textPassengerName, textPickUp, textMoney, textPaymentMethod, textStatus;
    RelativeLayout layoutCall, layoutChat, layoutTripInfo, layoutBottom;
    AppCompatButton buttonArrived, buttonCancel;
    CircleImageView imgFocusOnMe;
    ImageView imgDriver;
    RelativeLayout layoutStatus;

    public static GoogleMap map;
    public static Marker currentLocationMarker;
    public static boolean isRunning = false;
    public static boolean bottomLayoutIsVisible = true;
    public static boolean focusOnMe = false;

    private static final int ACCESS_FINE_LOCATION_REQUEST_CODE = 123;
    private static final int ACCESS_COARSE_LOCATION_REQUEST_CODE = 234;
    public static String markerIconName = "motor_marker_icon";
    public static final String taxiMarker = "taxi_marker";
    public static final String motorMarker = "motor_marker_icon";
    private static final String pickUpMarkerName = "pick_up_marker";
    private static final String desMarkerName = "flag";
    public static int driverMarkerSize = 160;
    public static int locationMarkerSize = 120;
    public static int zoomToDriver = 17;
    public static String MAPS_API_KEY;
    public static String GOONG_API_KEY;
    public static float polyWidth = 14;

    MyLocationService mLocationService;
    private FusedLocationProviderClient fusedLocationClient;

    Intent mServiceIntent;
    LatLng UET;
    String tripId;
    boolean workingIsEnable = false;
    boolean tripIsLoaded = false, driverPosIsLoaded = false, keyIsLoaded = false;
    public static Driver driver;
    public static Vehicle vehicle;
    public static Trip trip;
    public static Passenger passenger;
    public static CurrentPosition driverCurrentPosition;
    public static LatLng driverCurrentLatLng;

    private Polyline pickupPolyline, dropOffPolyLine;

    ProgressDialog progressDialog;

    boolean arrivedToPickUpPoint = false;
    boolean passengerIsReady = false;
    boolean arrivedToDropOff = false;
    boolean firstTimeLoadTrip = true;
    boolean pickUpIsDrawn = false, dropOffIsDrawn = false;

    private final int CALL_REQUEST_CODE = 123;
    private final int SMS_REQUEST_CODE = 234;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_up);

        init();
        progressDialog.show();
        getTripInfo();
        listener();
    }

    private void listener() {
        layoutStatus.setOnClickListener(new View.OnClickListener() {
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
                    if (tripIsLoaded && driverPosIsLoaded) {
                        if (!dropOffIsDrawn) {
                            zoomToPickUpRoute();
                        } else {
                            zoomToDropOff();
                        }
                    }
                    imgFocusOnMe.setImageResource(R.drawable.unfocus);
                } else {
                    focusOnMe = true;
                    imgFocusOnMe.setImageResource(R.drawable.focus);
                    zoomToDriver();
                }
            }
        });

        buttonArrived.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (trip != null) {
                    if (!arrivedToPickUpPoint) {
                        updateStatus(Const.driverArrivedPickUp);
                    } else if (!passengerIsReady) {
                        updateStatus(Const.onGoing);
                    } else if (!arrivedToDropOff) {
                        updateStatus(Const.arrivedDropOff);
                    } else {
                        updateStatus(Const.success);
                        Intent intent = new Intent(PickUpActivity.this, WorkingActivity.class);
                        Toast.makeText(PickUpActivity.this, "Done!", Toast.LENGTH_SHORT).show();
                        MyLocationService.isFindingTrip = true;
                        startActivity(intent);
                        finish();
                    }
                }
                else {
                    Toast.makeText(getApplicationContext(), "Trip is null", Toast.LENGTH_SHORT).show();
                }
            }
        });

        layoutCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (passenger != null) {
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CALL_PHONE)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(PickUpActivity.this,
                                new String[]{Manifest.permission.CALL_PHONE},
                                CALL_REQUEST_CODE);
                    } else {
                        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + passenger.getPhoneNumber()));
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Passenger is null!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        layoutChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (passenger != null) {
                    if (ContextCompat.checkSelfPermission(getApplicationContext(),android.Manifest.permission.SEND_SMS)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(PickUpActivity.this,
                                new String[]{Manifest.permission.SEND_SMS},
                                SMS_REQUEST_CODE);
                    } else {
                        String number = passenger.getPhoneNumber();
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", number,null));
                        //intent.putExtra("sms_body", "Hehe");
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(PickUpActivity.this, "passenger is null!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonCancel.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                cancelTrip();

                return false;
            }
        });
    }

    private void cancelTrip() {
        if (trip != null) {
            trip.setStatus(Const.cancelByDriver);
            FirebaseDatabase.getInstance().getReference()
                    .child("Trips")
                    .child(trip.getId())
                    .setValue(trip)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(PickUpActivity.this, "Your trip has been canceled!", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(PickUpActivity.this, WorkingActivity.class);
                            MyLocationService.rejectedTrips.put(trip.getId(), trip);
                            startActivity(intent);
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(PickUpActivity.this, "Can not cancel your trip, try again!", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    public void zoomToDropOff() {
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(DecodeTool.getLatLngFromString(trip.getDropOffLocation()))
                .include(DecodeTool.getLatLngFromString(trip.getPickUpLocation())).build();
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);

        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, point.x, 800, 250));
    }


    private void updateStatus(String status) {
        if (!status.equals(trip.getStatus())) {
            updateTripStatus(status);
        }

        if (status.equals(Const.driverArrivedPickUp)) {
            arrivedToPickUpPoint = true;
            if (trip.getVehicleType().equals(Const.car)) {
                textStatus.setText("Waiting for passenger to get in");
                buttonArrived.setText("Passenger got in");
            } else {
                textStatus.setText("Waiting for passenger to get on");
                buttonArrived.setText("Passenger got on");
            }
        } else if (status.equals(Const.onGoing)) {
            arrivedToPickUpPoint = true;
            passengerIsReady = true;
            textStatus.setText("Going to drop-off point");
            buttonArrived.setText("Arrived");

            if (!dropOffIsDrawn && pickUpIsDrawn) {
                pickupPolyline.remove();
                try {
                    direction(
                            DecodeTool.getLatLngFromString(trip.getPickUpLocation()),
                            DecodeTool.getLatLngFromString(trip.getDropOffLocation()),
                            "drop-off"
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
                dropOffIsDrawn = true;
            }
            textPickUp.setText(trip.getDropOffName());
        }
        else if (status.equals(Const.arrivedDropOff)) {
            arrivedToPickUpPoint = true;
            passengerIsReady = true;
            arrivedToDropOff = true;
            textStatus.setText("Waiting for payment");
            buttonArrived.setText("Done");

            if (!dropOffIsDrawn && pickUpIsDrawn) {
                pickupPolyline.remove();
                try {
                    direction(
                            DecodeTool.getLatLngFromString(trip.getPickUpLocation()),
                            DecodeTool.getLatLngFromString(trip.getDropOffLocation()),
                            "drop-off"
                    );
                } catch (IOException e) {
                    e.printStackTrace();
                }
                dropOffIsDrawn = true;
            }
            textPickUp.setText(trip.getDropOffName());
        }
    }

    private void updateTripStatus(String status) {
        trip.setStatus(status);
        if (status.equals(Const.success)) {
            trip.setEndTime(Calendar.getInstance().getTime().toString());
        }
        FirebaseDatabase.getInstance().getReference().child("Trips")
                .child(tripId)
                .setValue(trip);
    }

    private void getTripStatusUpdate(String tripId) {
        FirebaseDatabase.getInstance().getReference()
                .child("Trips")
                .child(tripId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Trip trip = snapshot.getValue(Trip.class);
                        if (trip != null) {
                            if (trip.getStatus().equals(Const.cancelByPassenger)) {
                                Intent intent = new Intent(PickUpActivity.this, WorkingActivity.class);
                                Toast.makeText(PickUpActivity.this, "Passenger has canceled this trip!", Toast.LENGTH_SHORT).show();
                                MyLocationService.isFindingTrip = true;
                                startActivity(intent);
                                finish();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    public void zoomToDriver() {
        if (driverCurrentLatLng != null)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(driverCurrentLatLng, zoomToDriver));
    }

    public void zoomToPickUpRoute() {
        LatLng destination = DecodeTool.getLatLngFromString(trip.getPickUpLocation());
        LatLng origin = DecodeTool.getLatLngFromString(driverCurrentPosition.getPosition());

        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(destination)
                .include(origin).build();
        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);

        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, point.x, 800, 250));
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
                                updateStatus(trip.getStatus());
                                getTripStatusUpdate(trip.getId());
                            }
                            tripIsLoaded = true;
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
        }
    }

    private void setVehicleIcon(Trip trip) {
        if (trip.getVehicleType().equals(Const.car)) {
            markerIconName = taxiMarker;
        } else {
            markerIconName = motorMarker;
        }
    }

    private void loadTripInfo(Trip trip) {
        if (trip.getStatus().equals(Const.driverArrivedPickUp) || trip.getStatus().equals(Const.waitingPickUp)) {
            textPickUp.setText(trip.getPickUpName());
        }
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

    private void loadDriverInfo() {
        FirebaseDatabase.getInstance().getReference().child("Drivers")
                .child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        driver = snapshot.getValue(Driver.class);
                        if (driver != null) {
                            Picasso.get().load(driver.getDriverImageUrl()).placeholder(R.drawable.user_blue)
                                    .centerCrop().resize(1000, 1000).into(imgDriver);
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
        imgDriver = findViewById(R.id.img_profile);
        layoutStatus = findViewById(R.id.layout_statusUpdate);
        buttonCancel = findViewById(R.id.buttonCancel);

        setUpFocusButton();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");

        UET = new LatLng(21.038902482537342, 105.78296809797327); //Dai hoc Cong Nghe Lat Lng

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.fragment_maps);
        mapFragment.getMapAsync(this);
    }

    private void setUpFocusButton() {
        if (focusOnMe) {
            imgFocusOnMe.setImageResource(R.drawable.focus);
        } else {
            imgFocusOnMe.setImageResource(R.drawable.unfocus);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setRotateGesturesEnabled(false);

        map.getUiSettings().setMyLocationButtonEnabled(true);

        markCurrentLocation();

        waitDataFullLoad();
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
                .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(pickUpMarkerName, locationMarkerSize, locationMarkerSize))));

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(pickup, zoomToDriver));

        map.addMarker(new MarkerOptions()
                .position(dropOff)
                .title("Drop-off")
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(desMarkerName, locationMarkerSize, locationMarkerSize))));

    }

    private void startServiceFunc(){
        mLocationService = new MyLocationService();
        mServiceIntent = new Intent(this, mLocationService.getClass());
        if (!isMyServiceRunning(mLocationService.getClass(), this)) {
            startService(mServiceIntent);
//            Toast.makeText(this, "Service start successfully", Toast.LENGTH_SHORT).show();
        } else {
//            Toast.makeText(this, "Service is already running", Toast.LENGTH_SHORT).show();
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

    private void loadCurrentApiKey() {
        FirebaseDatabase.getInstance().getReference().child("Current-API-KEY")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        MAPS_API_KEY = snapshot.getValue(String.class);
                        keyIsLoaded = true;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        FirebaseDatabase.getInstance().getReference().child("GOONG_API_KEY")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        GOONG_API_KEY = snapshot.getValue(String.class);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void waitDataFullLoad() {
        new Thread() {
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (keyIsLoaded && driverPosIsLoaded && tripIsLoaded) {
                            try {
                                drawRoute(
                                        DecodeTool.getLatLngFromString(driverCurrentPosition.getPosition()),
                                        DecodeTool.getLatLngFromString(trip.getPickUpLocation()),
                                        DecodeTool.getLatLngFromString(trip.getDropOffLocation())
                            );
                                loadTripInfo(trip);
                                markPickUpAndDropOff(trip);
                                setVehicleIcon(trip);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            waitDataFullLoad();
                        }
                    }
                });
            }
        }.start();
    }

    private void drawRoute(LatLng driverPos, LatLng origin, LatLng destination) throws IOException {
        if (trip.getStatus().equals(Const.waitingPickUp) || trip.getStatus().equals(Const.driverArrivedPickUp)) {
            direction(driverPos, origin, "pick-up");
        } else if (trip.getStatus().equals(Const.onGoing) || trip.getStatus().equals(Const.arrivedDropOff)) {
            direction(origin, destination, "drop-off");
        }
    }

    private void direction(LatLng origin, LatLng destination, String option) throws IOException {
        String strOrigin = origin.latitude + ", " + origin.longitude;
        String strDestination = destination.latitude + ", " + destination.longitude;

        String vehicleType;
        if (trip.getVehicleType().equals(Const.car)) {
            vehicleType = "car";
        } else {
            vehicleType = "bike";
        }

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String url = Uri.parse("https://rsapi.goong.io/Direction")
                .buildUpon()
                .appendQueryParameter("origin", strOrigin)
                .appendQueryParameter("destination", strDestination)
                .appendQueryParameter("vehicle", vehicleType)
                .appendQueryParameter("api_key", GOONG_API_KEY)
                .toString();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new com.android.volley.Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                progressDialog.dismiss();
                try {
                        JSONArray routes = response.getJSONArray("routes");

                        ArrayList<LatLng> points;
                        PolylineOptions polylineOptions = null;

                        for (int i=0;i<routes.length();i++) {
                            points = new ArrayList<>();
                            polylineOptions = new PolylineOptions();
                            JSONArray legs = routes.getJSONObject(i).getJSONArray("legs");

                            for (int j = 0; j < legs.length(); j++) {
                                JSONArray steps = legs.getJSONObject(j).getJSONArray("steps");

                                for (int k = 0; k < steps.length(); k++) {
                                    String polyline = steps.getJSONObject(k).getJSONObject("polyline").getString("points");
                                    List<LatLng> list = decodePoly(polyline);

                                    for (int l = 0; l < list.size(); l++) {
                                        LatLng position = new LatLng((list.get(l)).latitude, (list.get(l)).longitude);
                                        points.add(position);
                                    }
                                }
                            }
                            polylineOptions.addAll(points);
                            polylineOptions.width(polyWidth);
                            polylineOptions.geodesic(true);
                            polylineOptions.color(ContextCompat.getColor(getApplicationContext(), R.color.blue));
                        }
                        assert polylineOptions != null;
                        Polyline tempPoly = map.addPolyline(polylineOptions);
                        LatLngBounds bounds = new LatLngBounds.Builder()
                                .include(new LatLng(destination.latitude, destination.longitude))
                                .include(new LatLng(origin.latitude, origin.longitude)).build();
                        Point point = new Point();
                        getWindowManager().getDefaultDisplay().getSize(point);

                        if (option.equals("pick-up")) {
                            pickupPolyline = tempPoly;
                            pickUpIsDrawn = true;
                        } else {
                            dropOffPolyLine = tempPoly;
                            dropOffIsDrawn = true;
                        }
                        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, point.x, 800, 250));
                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        });
        RetryPolicy retryPolicy = new DefaultRetryPolicy(30000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsonObjectRequest.setRetryPolicy(retryPolicy);
        requestQueue.add(jsonObjectRequest);
    }

    private List<LatLng> decodePoly(String encoded){

        return PolyUtil.decode(encoded);
    }

    @Override
    protected void onStart() {
        super.onStart();
        isRunning = true;
        MyLocationService.isFindingTrip = false;

        if (!(isMyServiceRunning(MyLocationService.class, this))) {
            startServiceFunc();
        }

        loadCurrentApiKey();
        loadDriverInfo();
        getTripInfo();
        loadDriverPosition();
    }

    private void loadDriverPosition() {
        FirebaseDatabase.getInstance().getReference().child("CurrentPosition").child("Driver")
                .child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        driverCurrentPosition = snapshot.getValue(CurrentPosition.class);
                        driverPosIsLoaded = true;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        isRunning = false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        isRunning = false;
        MainActivity.pickUpActivityIsStart = false;
    }
}