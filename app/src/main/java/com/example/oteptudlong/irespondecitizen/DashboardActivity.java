package com.example.oteptudlong.irespondecitizen;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.dd.processbutton.FlatButton;
import com.google.android.gms.maps.model.Dash;
import com.google.firebase.auth.FirebaseAuth;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class DashboardActivity extends AppCompatActivity {

    public Typeface titleFont;
    public TextView sos, crime_mapping, report, profile, my_reports;
    private CardView card_sos, card_map, card_report, card_profile, card_history;
    private final static String admin_phoneNumber = "+639107009997";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("Rui Abreu - AzoSans-Regular.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());
        setContentView(R.layout.activity_dashboard);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(DashboardActivity.this, MainActivity.class));
            finish();
        }

        init();

        card_sos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(DashboardActivity.this);

                View view = getLayoutInflater().inflate(R.layout.layout_sms, null);
                FlatButton btn_call = view.findViewById(R.id.btn_call);
                FlatButton btn_sms = view.findViewById(R.id.btn_sms);

                btn_sms.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String message = "Emergency text message";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            // At least KitKat
                            Intent smsMsgAppVar = new Intent(Intent.ACTION_VIEW);
                            smsMsgAppVar.setData(Uri.parse("sms:" + admin_phoneNumber));
                            smsMsgAppVar.putExtra("sms_body", message);
                            startActivity(smsMsgAppVar);

                        } else {
                            // For early versions, do what worked for you before.
                            Intent smsIntent = new Intent(android.content.Intent.ACTION_VIEW);
                            smsIntent.setType("vnd.android-dir/mms-sms");
                            smsIntent.putExtra("address", admin_phoneNumber);
                            smsIntent.putExtra("sms_body", message);
                            startActivity(smsIntent);
                        }
                    }
                });

                btn_call.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent callIntent = new Intent(Intent.ACTION_CALL);
                        callIntent.setData(Uri.parse(admin_phoneNumber));
                        if (ActivityCompat.checkSelfPermission(DashboardActivity.this, android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        startActivity(callIntent);
                    }
                });

                dialog.setView(view);

                dialog.show();
            }
        });

        card_map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardActivity.this, MapsActivity.class));
            }
        });

        card_report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardActivity.this, ReportActivity.class));
            }
        });

        card_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutDialog();
            }
        });

        card_history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(DashboardActivity.this, "HISTORY", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void init() {

        DashboardActivity.this.setTitle("Dashboard");

        titleFont = Typeface.createFromAsset(this.getAssets(), "Rui Abreu - AzoSans-Medium.otf");
        sos = findViewById(R.id.title_sos);
        crime_mapping = findViewById(R.id.title_crime_mapping);
        report = findViewById(R.id.title_report);
        profile = findViewById(R.id.title_profile);
        my_reports = findViewById(R.id.title_my_reports);

        sos.setTypeface(titleFont);
        crime_mapping.setTypeface(titleFont);
        report.setTypeface(titleFont);
        profile.setTypeface(titleFont);
        my_reports.setTypeface(titleFont);

        card_sos = findViewById(R.id.card_sos);
        card_map = findViewById(R.id.card_map);
        card_report = findViewById(R.id.card_report);
        card_profile = findViewById(R.id.card_profile);
        card_history = findViewById(R.id.card_history);

    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    private void showLogoutDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);

        View view = getLayoutInflater().inflate(R.layout.layout_logout, null);
        FlatButton btn_cancel = view.findViewById(R.id.btn_cancel);
        FlatButton btn_logout = view.findViewById(R.id.btn_logout);

        builder.setView(view);

        final AlertDialog dialog = builder.create();
        dialog.show();

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        btn_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(DashboardActivity.this, MainActivity.class));
                finish();
            }
        });

    }

}
