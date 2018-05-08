package com.example.oteptudlong.irespondecitizen;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dd.processbutton.FlatButton;
import com.github.aakira.compoundicontextview.CompoundIconTextView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.raycoarana.codeinputview.CodeInputView;
import com.raycoarana.codeinputview.OnCodeCompleteListener;

import java.util.concurrent.TimeUnit;

import dmax.dialog.SpotsDialog;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class RegisterActivity extends AppCompatActivity {

    private EditText edit_firstname, edit_lastname, edit_email, edit_phone, edit_password, edit_confirm;
    private FlatButton btn_register;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks verificationStateChangedCallbacks;
    private PhoneAuthProvider.ForceResendingToken resendingToken;
    private String phoneVerificationId;
    private SpotsDialog loadingDialog;
    private Typeface btnFont;
    public CodeInputView codeInputView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
            .setDefaultFontPath("Rui Abreu - AzoSans-Regular.otf")
            .setFontAttrId(R.attr.fontPath)
            .build());
        setContentView(R.layout.activity_register);

        appBar();
        init();

        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String firstname = edit_firstname.getText().toString().trim();
                final String lastname = edit_lastname.getText().toString().trim();
                String email = edit_email.getText().toString().trim();
                final String phone = edit_phone.getText().toString().trim();
                String password = edit_password.getText().toString().trim();
                String confirm = edit_confirm.getText().toString().trim();

                if (TextUtils.isEmpty(firstname)) {
                    return;
                }
                if (TextUtils.isEmpty(lastname)) {
                    return;
                }
                if (TextUtils.isEmpty(email)) {
                    return;
                }
                if (TextUtils.isEmpty(phone)) {
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    return;
                }
                if (TextUtils.isEmpty(confirm)) {
                    return;
                }
                if (!password.equals(confirm)) {
                    return;
                }
                loadingDialog.show();
                // Check if the email is exist or not
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {

                                // delete the user
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                user.delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                String phoneNumber = "+639" + phone;
                                                sendCode(phoneNumber);
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                loadingDialog.dismiss();
                                                Log.e("USER DEL ERR", e.getMessage());
                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                loadingDialog.dismiss();
                                Toast.makeText(RegisterActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    private void showAlertDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.layout_codeinput, null);

        codeInputView = view.findViewById(R.id.edit_code);
        TextView title = view.findViewById(R.id.title);
        CompoundIconTextView resendCode = view.findViewById(R.id.resend_code);

        resendCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // resend code
                resendCode();
            }
        });
        title.setTypeface(btnFont);
        codeInputView.addOnCompleteListener(new OnCodeCompleteListener() {
            @Override
            public void onCompleted(String code) {
                // verify code
                verifyCode(code);
            }
        });

        dialog.setView(view);
        dialog.show();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    private void appBar() {
        RegisterActivity.this.setTitle("Sign Up");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void init() {
        edit_firstname = findViewById(R.id.edit_firstname);
        edit_lastname = findViewById(R.id.edit_lastname);
        edit_email = findViewById(R.id.edit_email);
        edit_phone = findViewById(R.id.edit_phone);
        edit_password = findViewById(R.id.edit_password);
        edit_confirm = findViewById(R.id.edit_confirm);
        btn_register = findViewById(R.id.btn_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        loadingDialog = new SpotsDialog(this, R.style.Custom);
        btnFont = Typeface.createFromAsset(this.getAssets(), "Rui Abreu - AzoSans-Bold.otf");
        btn_register.setTypeface(btnFont);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void sendCode(String number) {
        if (!TextUtils.isEmpty(number)) {
            setUpVerificationCallBacks();
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    number,
                    60,
                    TimeUnit.SECONDS,
                    this,
                    verificationStateChangedCallbacks
            );
        }
    }

    private void setUpVerificationCallBacks() {
        verificationStateChangedCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    Toast.makeText(RegisterActivity.this, "Invalid phone number.", Toast.LENGTH_SHORT).show();
                }else if (e instanceof FirebaseTooManyRequestsException) {
                    Toast.makeText(RegisterActivity.this, "SMS Quota exceeded", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(RegisterActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ERROR", e.getMessage());
                }
                loadingDialog.dismiss();
            }

            @Override
            public void onCodeSent(String s, PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                phoneVerificationId = s;
                resendingToken = forceResendingToken;
                loadingDialog.dismiss();
                showAlertDialog();
            }
        };
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential phoneAuthCredential) {
        FirebaseAuth.getInstance().signInWithCredential(phoneAuthCredential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        // delete user
                        // sign in with email and password
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        user.delete()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        final String firstname = edit_firstname.getText().toString().trim();
                                        final String lastname = edit_lastname.getText().toString().trim();
                                        String email = edit_email.getText().toString().trim();
                                        final String phone = edit_phone.getText().toString().trim();
                                        String password = edit_password.getText().toString().trim();

                                        mAuth.createUserWithEmailAndPassword(email, password)
                                                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                                                    @Override
                                                    public void onSuccess(AuthResult authResult) {

                                                        FirebaseUser user = mAuth.getCurrentUser();
                                                        final String uid = user.getUid();

                                                        UserProfileChangeRequest profileChangeRequest = new UserProfileChangeRequest.Builder()
                                                                .setDisplayName(firstname + " " + lastname)
                                                                .build();

                                                        user.updateProfile(profileChangeRequest)
                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                    @Override
                                                                    public void onSuccess(Void aVoid) {
                                                                        Citizen citizen = new Citizen();
                                                                        citizen.setPhoneNumber("+639" + phone);
                                                                        citizen.setUid(uid);
                                                                        mDatabase.child("citizens").child(uid).setValue(citizen)
                                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                    @Override
                                                                                    public void onSuccess(Void aVoid) {
                                                                                        Toast.makeText(RegisterActivity.this, "SUCCESSFULLY SIGNED IN", Toast.LENGTH_SHORT).show();
                                                                                        // Go to home activity
                                                                                        startActivity(new Intent(RegisterActivity.this, DashboardActivity.class));
                                                                                        finish();
                                                                                    }
                                                                                })
                                                                                .addOnFailureListener(new OnFailureListener() {
                                                                                    @Override
                                                                                    public void onFailure(@NonNull Exception e) {
                                                                                        loadingDialog.dismiss();
                                                                                        Toast.makeText(RegisterActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                                    }
                                                                                });
                                                                    }
                                                                })
                                                                .addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        loadingDialog.dismiss();
                                                                        Toast.makeText(RegisterActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });

                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {

                                                    }
                                                });

                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        loadingDialog.dismiss();
                                        Toast.makeText(RegisterActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        loadingDialog.dismiss();
                        // show the error in code input
                        codeInputView.setError(e.getMessage());
                    }
                });
    }

    private void verifyCode(String code) {
        loadingDialog.show();
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(phoneVerificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void resendCode() {
        String phone = "+639" + edit_phone.getText().toString().trim();
        if (!TextUtils.isEmpty(phone)) {
            loadingDialog.show();
            setUpVerificationCallBacks();
            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                    phone,
                    60,
                    TimeUnit.SECONDS,
                    this,
                    verificationStateChangedCallbacks,
                    resendingToken
            );
        }
    }

}
