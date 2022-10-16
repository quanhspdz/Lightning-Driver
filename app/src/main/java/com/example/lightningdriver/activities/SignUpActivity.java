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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lightningdriver.R;
import com.example.lightningdriver.models.Driver;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class SignUpActivity extends AppCompatActivity {

    TextView txtUploadImage, txtSwitchToLogin;
    CircleImageView imgProfile;
    EditText edtName, edtPhone, edtEmail, edtPassword, edtPasswordAgain;
    AppCompatButton btnSignup;

    Driver driver;
    Uri imageUri;
    String gPassword;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        init();

        listener();

    }

    private Boolean checkInputData() {
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String passwordAgain = edtPasswordAgain.getText().toString().trim();

        if (imageUri == null) {
            Toast.makeText(this, "Please upload your image!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (name.isEmpty()) {
            Toast.makeText(this, "Your name must not be empty!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (phone.isEmpty()) {
            Toast.makeText(this, "Your phone number must not be empty!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (email.isEmpty()) {
            Toast.makeText(this, "Your email must not be empty!", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            if (!(android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())) {
                Toast.makeText(this, "Your email is invalid!", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Your password must not be empty!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (passwordAgain.isEmpty()) {
            Toast.makeText(this, "Please retype your password!", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!(password.equals(passwordAgain))) {
            Toast.makeText(this, "Your password and your confirm password are not matched!", Toast.LENGTH_SHORT).show();
            return false;
        }

        driver = new Driver(null, name, phone, email, null, null);
        gPassword = password;
        return true;
    }

    private void listener() {
        txtUploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity().start(SignUpActivity.this);
            }
        });

        imgProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CropImage.activity().start(SignUpActivity.this);
            }
        });

        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkInputData()) {
                    uploadImageAndRegister(getApplicationContext(),
                            imageUri,
                            driver,
                            gPassword);
                }
            }
        });
    }

    private void init() {
        txtUploadImage = findViewById(R.id.text_uploadImage);
        txtSwitchToLogin = findViewById(R.id.text_switchToLogin);

        imgProfile = findViewById(R.id.img_profile);

        edtName = findViewById(R.id.edt_name);
        edtPhone = findViewById(R.id.edt_phoneNumber);
        edtEmail = findViewById(R.id.edt_email);
        edtPassword = findViewById(R.id.edt_password);
        edtPasswordAgain = findViewById(R.id.edt_password_again);

        btnSignup = findViewById(R.id.buttonSignUp);

        driver = new Driver();
        progressDialog = new ProgressDialog(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            assert result != null;
            imageUri = result.getUri();
            imgProfile.setImageURI(imageUri);
        } else {
            Toast.makeText(this, "Error, please try again!", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageAndRegister(Context context, Uri imageUri, Driver driver, String password) {
        progressDialog.setMessage("Creating your new account...");
        progressDialog.show();

        //get image name & extension
        StorageReference filePath = FirebaseStorage.getInstance().getReference("DriverProfileImages")
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

                driver.setDriverImageUrl(imgUrl);
                registerUser(context, driver, password);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerUser(Context context, Driver driver, String password) {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseAuth.createUserWithEmailAndPassword(driver.getEmail(), password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                driver.setId(Objects.requireNonNull(firebaseAuth.getCurrentUser()).getUid());
                FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

                firebaseDatabase.getReference().child("Drivers").child(driver.getId())
                        .setValue(driver)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                progressDialog.dismiss();
                                Toast.makeText(context, "Welcome to Lightning for driver!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(context, VehicleRegistrationActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            }
                        });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        });
    }

}