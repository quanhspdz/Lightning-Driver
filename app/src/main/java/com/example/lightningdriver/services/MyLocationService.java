package com.example.lightningdriver.services;

import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static com.example.lightningdriver.activities.WorkingActivity.currentLocationMarker;
import static com.example.lightningdriver.activities.WorkingActivity.driver;
import static com.example.lightningdriver.activities.WorkingActivity.driverMarkerSize;
import static com.example.lightningdriver.activities.WorkingActivity.map;
import static com.example.lightningdriver.activities.WorkingActivity.markerIconName;
import static com.example.lightningdriver.activities.WorkingActivity.vehicle;
import static com.example.lightningdriver.activities.WorkingActivity.zoomToDriver;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.provider.ContactsContract;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.graphics.drawable.IconCompat;


import com.example.lightningdriver.R;
import com.example.lightningdriver.activities.NewTripFoundActivity;
import com.example.lightningdriver.activities.PickUpActivity;
import com.example.lightningdriver.activities.WorkingActivity;
import com.example.lightningdriver.models.CurrentPosition;
import com.example.lightningdriver.models.Driver;
import com.example.lightningdriver.models.Trip;
import com.example.lightningdriver.models.Vehicle;
import com.example.lightningdriver.tools.Const;
import com.example.lightningdriver.tools.Converter;
import com.example.lightningdriver.tools.DecodeTool;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;


public class MyLocationService extends Service {

    public static ArrayList<LatLng> locationArrayList = new ArrayList<LatLng>();

    FusedLocationProviderClient fusedLocationClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;
    LatLng lastLocation;

    String notificationChannelId = "1";
    String notificationChannelName = "Background Service";

    int notificationId = 1;

    public static HashMap<String, Trip> rejectedTrips;
    public static boolean isFindingTrip = true;

    Driver driver;
    Vehicle vehicle;



    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        new Notification();

        loadDriverInfo();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        rejectedTrips = new HashMap<>();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) createNotificationChanel() ;
        else startForeground(
                1,
                new Notification()
        );

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(500);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location =  locationResult.getLastLocation();
                if (location != null) {
                    if (driver != null && vehicle != null)
                        updateLocationOnFirebase(location);

                    if (WorkingActivity.isRunning) {
                        updateLocationMarkerOnWorkingAct(location);
                    }
                    if(PickUpActivity.isRunning) {
                        updateLocationMarkerOnPickUpAct(location);
                        PickUpActivity.driverCurrentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    }

                    if (isFindingTrip && !PickUpActivity.isRunning)
                        getListTrips(new LatLng(location.getLatitude(), location.getLongitude()));
                }
            }
        };

        startLocationUpdates();
    }

    private void updateLocationMarkerOnPickUpAct(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        lastLocation = latLng;
        float bearing = location.getBearing();

        if (PickUpActivity.map != null) {
            if (PickUpActivity.currentLocationMarker != null)
                PickUpActivity.currentLocationMarker.remove();

            PickUpActivity.currentLocationMarker = PickUpActivity.map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("You are here!")
                    .rotation(bearing)
                    .anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(markerIconName, driverMarkerSize, driverMarkerSize))));

            if (PickUpActivity.focusOnMe)
                PickUpActivity.map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomToDriver));
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChanel() {
        NotificationChannel chan = new NotificationChannel(
                notificationChannelId,
                notificationChannelName,
                NotificationManager.IMPORTANCE_NONE
        );
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = getSystemService(NotificationManager.class);

        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, notificationChannelId);

        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("Lightning driver is running")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setSmallIcon(R.drawable.lightning_circle)
                .build();
        startForeground(notificationId, notification);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void pushNewNotification(String title, String message) {
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, notificationChannelId);

        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setSmallIcon(R.drawable.lightning_circle)
                .build();
        startForeground(notificationId, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void updateLocationMarkerOnWorkingAct(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        lastLocation = latLng;
        float bearing = location.getBearing();

        if (map != null) {
            if (currentLocationMarker != null)
                currentLocationMarker.remove();

            WorkingActivity.currentLocationMarker = map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("You are here!")
                    .rotation(bearing)
                    .anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.fromBitmap(resizeMapIcons(markerIconName, driverMarkerSize, driverMarkerSize))));

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomToDriver));
        }
    }

    public Bitmap resizeMapIcons(String iconName, int width, int height) {
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(iconName, "drawable", getPackageName()));
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(imageBitmap, width, height, false);
        return resizedBitmap;
    }

    public Bitmap getBitmapFromResource(String name) {
        Bitmap imageBitmap = BitmapFactory.decodeResource(getResources(), getResources().getIdentifier(name, "drawable", getPackageName()));
        return imageBitmap;
    }

    public void getListTrips(LatLng lastLocation) {
        FirebaseDatabase.getInstance().getReference().child("Trips")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ArrayList<Trip> listTrips = new ArrayList<>();

                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            Trip trip = dataSnapshot.getValue(Trip.class);
                            if (trip != null && isFindingTrip) {
                                if (trip.getStatus().equals(Const.searching) && trip.getVehicleType().equals(vehicle.getType())
                                    && !rejectedTrips.containsKey(trip.getId())) {
                                    listTrips.add(trip);
                                }
                            }
                        }

                        if (listTrips.size() > 0 && isFindingTrip && !PickUpActivity.isRunning) {
                            getSuitableTrip(listTrips, lastLocation);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void getSuitableTrip(ArrayList<Trip> listTrips, LatLng lastLocation) {
        double minDistance = Double.MAX_VALUE;
        Trip bestTrip = listTrips.get(0);

        for (int i = 0; i < listTrips.size(); i++) {
            Trip trip = listTrips.get(i);
            double distance = distance(DecodeTool.getLatLngFromString(trip.getPickUpLocation()), lastLocation);
            if (minDistance < distance) {
                minDistance = distance;
                bestTrip = trip;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pushNewNotification("Trip found: ", bestTrip.getPickUpName() + " ("+
                    minDistance+")");
            isFindingTrip = false;

            Intent intent = new Intent(this, NewTripFoundActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("tripId", bestTrip.getId());
            startActivity(intent);
        }
    }

    private static double distance(LatLng start, LatLng end) {
        double lat1 = start.latitude;
        double lon1 = start.longitude;
        double lat2 = end.latitude;
        double lon2 = end.longitude;

        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        }
        else {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            dist = dist * 1.609344;

            return (dist);
        }
    }

    public void updateLocationOnFirebase(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        CurrentPosition currentPosition = new CurrentPosition(
                Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid(),
                latLng.toString(),
                String.valueOf(location.getBearing()),
                String.valueOf(Converter.fromMPerSToKmPerHour(location.getSpeed())),
                vehicle.getType(),
                Calendar.getInstance().getTime().toString()
        );

        FirebaseDatabase.getInstance().getReference().child("CurrentPosition")
                .child("Driver")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(currentPosition);
    }

    private void loadDriverInfo() {
        FirebaseDatabase.getInstance().getReference().child("Drivers")
                .child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        driver = snapshot.getValue(Driver.class);
                        if (driver != null) {
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
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

}