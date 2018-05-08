package com.example.oteptudlong.irespondecitizen;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dd.processbutton.FlatButton;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import dmax.dialog.SpotsDialog;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {

    private TextView text_forgot;
    private EditText edit_email, edit_password;
    private Button btn_login, btn_register;
    private FirebaseAuth mAuth;
    public Typeface btnFont;
    private SpotsDialog loadingDialog;
    private DatabaseReference mCitizens = FirebaseDatabase.getInstance().getReference().child("Citizen");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("Rui Abreu - AzoSans-Regular.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());
        setContentView(R.layout.activity_main);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(MainActivity.this, DashboardActivity.class));
            finish();
        }

        init();

        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RegisterActivity.class));
            }
        });

        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithEmail();
            }
        });

        text_forgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                View view = getLayoutInflater().inflate(R.layout.layout_forgotpassword, null);
                final EditText edit_forgot = view.findViewById(R.id.edit_forgot);
                FlatButton btn_submit = view.findViewById(R.id.btn_sign_submit);

                btn_submit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String email = edit_forgot.getText().toString().trim();
                        if (!TextUtils.isEmpty(email)) {
                            loadingDialog.show();
                            mAuth.sendPasswordResetEmail(email)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            loadingDialog.dismiss();
                                            Toast.makeText(MainActivity.this, "An email with link has been sent to your email address.", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            loadingDialog.dismiss();
                                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }
                });

                dialog.setView(view);
                dialog.show();
            }
        });

    }

    private void signInWithEmail() {
        String email = edit_email.getText().toString().trim();
        String password = edit_password.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            return;
        }
        if (TextUtils.isEmpty(password)) {
            return;
        }
        loadingDialog.show();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        final String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        mCitizens.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.hasChild(uid)) {
                                    loadingDialog.dismiss();
                                    startActivity(new Intent(MainActivity.this, DashboardActivity.class));
                                    finish();
                                }else {
                                    AuthUI.getInstance().signOut(MainActivity.this)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    loadingDialog.dismiss();
                                                    Toast.makeText(MainActivity.this, "Invalid username and password.", Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    loadingDialog.dismiss();
                                                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                loadingDialog.dismiss();
                                Toast.makeText(MainActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        loadingDialog.dismiss();
                        Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void init() {
        MainActivity.this.setTitle("Sign In");

        edit_email = findViewById(R.id.edit_email);
        edit_password = findViewById(R.id.edit_password);
        btn_login = findViewById(R.id.btn_sign_in);
        btn_register = findViewById(R.id.btn_sign_up);
        mAuth = FirebaseAuth.getInstance();
        text_forgot = findViewById(R.id.text_forgot);

        btnFont = Typeface.createFromAsset(this.getAssets(), "Rui Abreu - AzoSans-Bold.otf");

        btn_login.setTypeface(btnFont);
        btn_register.setTypeface(btnFont);

        loadingDialog = new SpotsDialog(this, R.style.Custom);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

}
