package com.example.oteptudlong.irespondecitizen;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.angmarch.views.NiceSpinner;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import dmax.dialog.SpotsDialog;

public class ReportActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, View.OnLongClickListener {

    public NiceSpinner niceSpinner;
    public Button btn_gallery, btn_camera;
    private final static int CAMERA_REQUEST_CODE = 2000, GALLERY_IMAGE_REQUEST_CODE = 3000;

    private static final int GOOGLE_PLAY_SERVICES_REQUEST_CODE = 1996, PERMISSION_REQUEST_CODE = 6000;
    private GoogleApiClient googleApiClient;
    public LocationRequest locationRequest;
    public Location mLastLocation;

    public Button btn_next;
    public List<ImageView> imageViews;
    private String address = null;
    public DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child("Citizen Report");
    public DatabaseReference mDbLocation = FirebaseDatabase.getInstance().getReference().child("Report Location");
    private SpotsDialog loadingDialog;
    public FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
    private List<String> downloadUrls;
    private String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private List<String> dataset;
    private EditText description;

    private static final String randomString = "nXMk8bjWv7";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        setUpLocation();

        loadingDialog = new SpotsDialog(this, R.style.Custom);
        description = findViewById(R.id.edit_description);

        niceSpinner = findViewById(R.id.niceSpinner);
        dataset = new LinkedList<>(Arrays.asList("Select incident", "Fire", "Near Miss", "Road Accident", "Thief", "Property Damage", "Rape", "Other"));
        niceSpinner.attachDataSource(dataset);

        btn_gallery = findViewById(R.id.btn_gallery);
        btn_camera = findViewById(R.id.btn_camera);
        ImageView imageView1 = findViewById(R.id.imageView1);
        ImageView imageView2 = findViewById(R.id.imageView2);
        ImageView imageView3 = findViewById(R.id.imageView3);
        ImageView imageView4 = findViewById(R.id.imageView4);

        imageViews = new ArrayList<>();
        imageViews.add(imageView1);
        imageViews.add(imageView2);
        imageViews.add(imageView3);
        imageViews.add(imageView4);

        for (int i = 0; i < imageViews.size(); i++) {
            imageViews.get(i).setImageDrawable(null);
            imageViews.get(i).setBackgroundResource(R.drawable.no_image);
            imageViews.get(i).setTag(i);
            imageViews.get(i).setOnLongClickListener(this);
        }

        btn_next = findViewById(R.id.btn_next);

        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLastLocation != null) {
                    String time = DateFormat.getTimeInstance().format(new Date());
                    String date = DateFormat.getDateInstance().format(new Date());
                    final String futureKey = mDatabase.child("iyot").push().getKey();
                    String incident = dataset.get(niceSpinner.getSelectedIndex());
                    String strDescription = description.getText().toString().trim();
                    Log.e("HELLO", String.valueOf(address));
                    if (address != null) {
                        if (niceSpinner.getSelectedIndex() != 0) {
                            loadingDialog.show();

                            Report report = new Report();
                            report.setStrDate(date);
                            report.setStrTime(time);
                            report.setUid(uid);
                            report.setLocation_name(address);
                            report.setIncident(incident);
                            report.setKey(futureKey);
                            report.setLatitude(mLastLocation.getLatitude());
                            report.setLongtitude(mLastLocation.getLongitude());
                            if (!strDescription.equals("")) {
                                report.setDescription(strDescription);
                            }

                            final List<Bitmap> bitmap_images = new ArrayList<>();
                            for (int i = 0; i < imageViews.size(); i++) {
                                if (imageViews.get(i).getDrawable() != null) {
                                    bitmap_images.add(((BitmapDrawable)imageViews.get(i).getDrawable()).getBitmap());
                                }
                            }

                            mDatabase.child(futureKey).setValue(report)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // Geoofire
                                            GeoFire geoFire = new GeoFire(mDbLocation.child(futureKey));
                                            geoFire.setLocation("firebase-hq", new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
                                                @Override
                                                public void onComplete(String key, DatabaseError error) {
                                                    if (error != null) {
                                                        loadingDialog.dismiss();
                                                        Toast.makeText(ReportActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                                                        Log.e("ERROR", error.getMessage());
                                                    }else {
                                                        processImage(bitmap_images, futureKey);
                                                    }
                                                }
                                            });
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(ReportActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                            loadingDialog.dismiss();
                                        }
                                    });
                        }else {
                            Toast.makeText(ReportActivity.this, "Select incident", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

        btn_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), GALLERY_IMAGE_REQUEST_CODE);
            }
        });

        btn_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, CAMERA_REQUEST_CODE);
            }
        });

    }

    private void processImage(List<Bitmap> bitmap_images, String futureKey) {
        if (bitmap_images.size() == 0) {
            Toast.makeText(ReportActivity.this, "Successfully inserted", Toast.LENGTH_SHORT).show();
            loadingDialog.dismiss();
            finish();
        }else {
            for (int i = 0; i < bitmap_images.size(); i++) {
                downloadUrls = new ArrayList<>();
                uploadImage(bitmap_images.get(i), i, futureKey);
            }
        }
    }

    private void resetImage(ImageView imageView) {
        imageView.setImageDrawable(null);
        imageView.setBackgroundResource(R.drawable.no_image);
    }

    private void uploadImage(Bitmap bitmap, final int count, final String futureKey) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        StorageReference imageUpload = firebaseStorage.getReference().child("images").child(randomName() + ".jpg");
        imageUpload.putBytes(data)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        downloadUrls.add(taskSnapshot.getDownloadUrl().toString());
                        if (count == (downloadUrls.size() - 1)) {
                            Log.e("URL SIZE", String.valueOf(downloadUrls.size()));
                            uploadImageUrlsToFirebase(futureKey);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        loadingDialog.dismiss();
                        Log.e("ERROR UPLOADING", e.getMessage());
                    }
                });
    }

    private void uploadImageUrlsToFirebase(String futurekey) {
        final String newDownloadUrlStr = concatUrls(downloadUrls);
        HashMap<String, Object> childImages = new HashMap<>();
        childImages.put("images", newDownloadUrlStr);
        mDatabase.child(futurekey).updateChildren(childImages)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.e("URLS", newDownloadUrlStr);
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        loadingDialog.dismiss();
                        Log.e("UPLOADING DATABASE", e.getMessage());
                    }
                });
    }

    private String concatUrls(List<String> url) {
        StringBuilder output = new StringBuilder();

        for (int i = 0; i < url.size(); i++) {
            if (i != url.size() - 1) {
                output.append(url.get(i) + randomString);
            }else {
                output.append(url.get(i));
            }
        }

        return output.toString();
    }

    private String randomName() {
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = generator.nextInt(10);
        char tempChar;
        for (int i = 0; i < randomLength; i++){
            tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(ReportActivity.this, new String[]{
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        } else {
            if (googlePlayServicesAvailable()) {
                buildGoogleApiClient();
                setUpLocationRequest();
            }
        }
    }

    private void setUpLocationRequest() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(500);
    }

    private void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(ReportActivity.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    private boolean googlePlayServicesAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int status = googleApiAvailability.isGooglePlayServicesAvailable(ReportActivity.this);
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(ReportActivity.this, status, GOOGLE_PLAY_SERVICES_REQUEST_CODE).show();
            } else {
                Toast.makeText(this, "Device is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_IMAGE_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri filePath = data.getData();
            try {
                Bitmap gallery_image = MediaStore.Images.Media.getBitmap(ReportActivity.this.getContentResolver(), filePath);
                for (int i = 0; i < imageViews.size(); i++) {
                    if (imageViews.get(i).getDrawable() == null) {
                        addImage(imageViews.get(i), gallery_image);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            Bitmap camera_image = (Bitmap) data.getExtras().get("data");
            for (int i = 0; i < imageViews.size(); i++) {
                if (imageViews.get(i).getDrawable() == null) {
                    addImage(imageViews.get(i), camera_image);
                    break;
                }
            }
        }
    }

    private void addImage(ImageView imageView, Bitmap image) {
        imageView.setImageDrawable(null);
        imageView.setImageBitmap(image);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        setLocation();
        setUpLocationRequest();
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
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    private void setLocation() {
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
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (mLastLocation != null) {
            LocationAddress locationAddress = new LocationAddress();
            locationAddress.getAddressFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(),
                    getApplicationContext(), new GeocoderHandler());
        }
    }

    private class GeocoderHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            String locationAddress;
            switch (message.what) {
                case 1:
                    Bundle bundle = message.getData();
                    locationAddress = bundle.getString("address");
                    break;
                default:
                    locationAddress = "null";
            }
            address = locationAddress;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (googlePlayServicesAvailable()) {
                    buildGoogleApiClient();
                    setLocation();
                    setUpLocationRequest();
                }
            }else {
                Log.d("HELLO", "FAILED");
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        int position = (int) v.getTag();
        resetImage(imageViews.get(position));
        return false;
    }
}
