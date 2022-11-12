package com.example.lightningdriver.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lightningdriver.R;
import com.example.lightningdriver.models.Driver;
import com.example.lightningdriver.models.Vehicle;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class VehicleRegistrationActivity extends AppCompatActivity {

    CircleImageView imageVehicle;
    TextView textUploadImage;
    EditText edtName, edtPlateNumber;
    RadioButton radioButtonCar, radioButtonMotorbike;
    RadioGroup radioGroup;
    AppCompatButton buttonConfirm;

    Vehicle vehicle;
    Driver driver;
    Uri imageUri;
    String vehicleType;
    ProgressDialog progressDialog;

    final int PICK_IMAGE_REQUEST = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_registration);

        init();
        listener();
    }

    private void listener() {
        imageVehicle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImage();
            }
        });
        textUploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickImage();
            }
        });

        buttonConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkInputData()) {
                    uploadImageAndRegister(getApplicationContext(), imageUri, vehicle);
                    buttonConfirm.setEnabled(false);
                }
            }
        });


        radioButtonMotorbike.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    vehicleType = "motorbike";
                }
            }
        });

        radioButtonCar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    vehicleType = "car";
                }
            }
        });
    }

    private void pickImage() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, PICK_IMAGE_REQUEST);
    }

    private boolean checkInputData() {
        String name = edtName.getText().toString().trim();
        String plateNumber = edtPlateNumber.getText().toString().trim();

        if (imageUri == null) {
            Toast.makeText(this, "Please select your vehicle Image!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (name.isEmpty()) {
            Toast.makeText(this, "Your vehicle's name can not be empty!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (plateNumber.isEmpty()) {
            Toast.makeText(this, "Your vehicle's plate number can not be empty!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (vehicleType == null) {
            Toast.makeText(this, "Please select your vehicle type!", Toast.LENGTH_SHORT).show();   
            return false;
        }

        vehicle.setName(name);
        vehicle.setPlateNumber(plateNumber);
        vehicle.setType(vehicleType);
        vehicle.setDriverId(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());

        return true;
    }

    private void init() {
        imageVehicle = findViewById(R.id.img_vehicle);
        textUploadImage = findViewById(R.id.text_uploadImage);
        edtName = findViewById(R.id.edt_name);
        edtPlateNumber = findViewById(R.id.edt_plate);
        radioButtonCar = findViewById(R.id.radio_car);
        radioButtonMotorbike = findViewById(R.id.radio_motorbike);
        buttonConfirm = findViewById(R.id.btnConfirm);
        radioGroup = findViewById(R.id.group_selectType);

        vehicle = new Vehicle();
        progressDialog = new ProgressDialog(VehicleRegistrationActivity.this);
    }

    private void uploadImageAndRegister(Context context, Uri imageUri, Vehicle vehicle) {
        progressDialog.setMessage("Registering your vehicle...");
        progressDialog.show();

        //get image name & extension
        StorageReference filePath = FirebaseStorage.getInstance().getReference("VehicleImages")
                .child(System.currentTimeMillis() + ".jpg");

        //get image url
        StorageTask uploadTask = filePath.putFile(imageUri);
        uploadTask.continueWithTask(new Continuation() {
            @Override
            public Object then(@NonNull Task task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return filePath.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                //upload post
                Uri downloadUri = (Uri) task.getResult();
                String imgUrl = downloadUri.toString();

                vehicle.setVehicleImageUrl(imgUrl);
                registerVehicle(context, vehicle);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
                buttonConfirm.setEnabled(true);
            }
        });
    }

    private void registerVehicle(Context context, Vehicle vehicle) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Vehicles");

        vehicle.setId(databaseReference.push().getKey());

        //upload vehicle info to FirebaseDatabase
        databaseReference.child(vehicle.getId()).setValue(vehicle)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(context, "Register successful!", Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                            Intent intent = new Intent(VehicleRegistrationActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                        buttonConfirm.setEnabled(true);
                    }
                });

        //update vehicle id in driver info
        getAndUpdateCurrentDriverInfo(vehicle.getId());
    }

    private void getAndUpdateCurrentDriverInfo (String vehicleId) {
        FirebaseDatabase.getInstance().getReference().child("Drivers")
                .child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        driver = snapshot.getValue(Driver.class);
                        assert driver != null;
                        driver.setVehicleId(vehicleId);

                        if (driver != null) {
                            updateCurrentDriverInfo(driver);
                        } else {
                            Toast.makeText(VehicleRegistrationActivity.this, "Driver is null!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    private void updateCurrentDriverInfo(Driver driver) {
        FirebaseDatabase.getInstance().getReference().child("Drivers")
                .child(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                .setValue(driver)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(VehicleRegistrationActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            imageVehicle.setImageURI(imageUri);
        } else {
            Toast.makeText(this, "Error, please try again!", Toast.LENGTH_SHORT).show();
        }
    }
}