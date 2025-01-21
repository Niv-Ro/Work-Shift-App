package com.example.workshiftapp.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.Navigation;

import com.example.workshiftapp.R;
import com.example.workshiftapp.models.Worker;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private GoogleAccountCredential googleAccountCredential;
    private GoogleSignInClient googleSignInClient;

    private String emailUser;
    private String fullName;

    private String calendarID;

    private String userPhoto;
    private boolean userExists;
    private double wage;


    public interface UserNameCallback {
        void onUserNameRetrieved(String fullName);
    }
    public interface wageCallback{
        void onWageRetrieved(double wage);
    }
    public interface calendarIDCallback{
        void onCalendarIDRetrieved(String calendarID);
    }


    // ActivityResultLauncher to handle sign-in result
    private final ActivityResultLauncher<Intent> activityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    new ActivityResultCallback<ActivityResult>() {
                        @Override
                        public void onActivityResult(ActivityResult result) {
                            if (result.getResultCode() == RESULT_OK) {
                                Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                                View btn = findViewById(R.id.signIn);
                                try {
                                    GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                                    if (signInAccount != null) {
                                        fullName = signInAccount.getDisplayName();
                                        emailUser = signInAccount.getEmail();
                                        userPhoto = (signInAccount.getPhotoUrl() != null)
                                                ? signInAccount.getPhotoUrl().toString()
                                                : "No photo available";
                                    }

                                    // Firebase sign-in with the Google ID token
                                    AuthCredential authCredential = GoogleAuthProvider.getCredential(
                                            signInAccount.getIdToken(), null);
                                    mAuth.signInWithCredential(authCredential)
                                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                                @Override
                                                public void onComplete(@NonNull Task<AuthResult> task) {
                                                    if (task.isSuccessful()) {
                                                        mAuth = FirebaseAuth.getInstance();
                                                        Toast.makeText(MainActivity.this,
                                                                "Signed in successfully!",
                                                                Toast.LENGTH_SHORT).show();

                                                        // Now that user is signed in, set up the Calendar credential
                                                        setupGoogleAccountCredential();
                                                        initWage(new wageCallback() {
                                                            @Override
                                                            public void onWageRetrieved(double wageuser) {
                                                                wage = wageuser;
                                                            }
                                                        });
                                                        initCalendarID(new calendarIDCallback(){
                                                            @Override
                                                            public void onCalendarIDRetrieved(String calendarIDUser) {
                                                                calendarID = calendarIDUser;
                                                                Navigation.findNavController(btn)
                                                                        .navigate(R.id.action_loginScreen_to_generalAppScreen);
                                                            }
                                                        });
                                                        if (calendarID == null)
                                                            showPopupDialog();


                                                        // Navigate to next screen

                                                    } else {
                                                        Toast.makeText(MainActivity.this,
                                                                "Failed to sign in: " + task.getException(),
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                } catch (ApiException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
    }

    // Method to start Google Sign-In (called from Fragment, presumably)
    public void startGoogleSignIn() {
        // Configure GoogleSignInOptions to request Calendar scope
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .requestScopes(new com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/calendar"))
                .build();

        googleSignInClient = GoogleSignIn.getClient(MainActivity.this, options);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        activityResultLauncher.launch(signInIntent);
    }

    public void login(View v) {
        String email = ((EditText) findViewById(R.id.login_TextEmail)).getText().toString();
        String password = ((EditText) findViewById(R.id.login_TextPassword)).getText().toString();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        emailUser = email;
                        if (task.isSuccessful()) {
                            initWage(new wageCallback() {
                                @Override
                                public void onWageRetrieved(double wageuser) {
                                    // Handle the retrieved wage here
                                    wage = wageuser;

                                }
                            });
                            initCalendarID(new calendarIDCallback(){
                                @Override
                                public void onCalendarIDRetrieved(String calendarIDUser) {
                                    calendarID = calendarIDUser;
                                    // Proceed with setting fullName if not set yet
                                    if (fullName == null) {
                                        getUserName(emailUser, name -> {
                                            if (name != null) {
                                                fullName = name;
                                            }
                                            // After fullName and wage are set, navigate
                                            Navigation.findNavController(v).navigate(R.id.action_loginScreen_to_generalAppScreen);
                                        });
                                    } else {
                                        // If fullName is already set, navigate directly
                                        Navigation.findNavController(v).navigate(R.id.action_loginScreen_to_generalAppScreen);
                                    }

                                }
                            });
                        } else {
                            Toast.makeText(MainActivity.this, "login fail", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    public void register() {
        String email = ((EditText) findViewById(R.id.reg_TextEmail)).getText().toString();
        String password = ((EditText) findViewById(R.id.reg_TextPassword)).getText().toString();
        String fullName = ((EditText) findViewById(R.id.reg_FullName)).getText().toString();
        String calendarID = ((EditText) findViewById(R.id.reg_CalendarID)).getText().toString();
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "register completed", Toast.LENGTH_LONG).show();
                            addData(email, fullName,calendarID);
                            userExists =false;
                        } else {
                            Toast.makeText(MainActivity.this, "Email already exists", Toast.LENGTH_LONG).show();
                            userExists=true;
                        }
                    }
                });
    }

    public void addData(String email, String fullName,String calendarID) {
        String sanitizedEmail = email.replace(".", "_");
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("Root").child("Users").child(sanitizedEmail);
        Worker worker = new Worker(email, fullName,calendarID);
        myRef.setValue(worker);
    }

    public GoogleAccountCredential getGoogleAccountCredential() {
        return googleAccountCredential;
    }
    public GoogleSignInClient getGetGoogleSignInClient() {
        return googleSignInClient;
    }

    public void setGetGoogleSignInClient(GoogleSignInClient getGoogleSignInClient) {
        this.googleSignInClient = getGoogleSignInClient;
    }
    public void initWage(wageCallback callback) {
        String sanitizedEmail = emailUser.replace(".", "_");
        DatabaseReference myRef = FirebaseDatabase.getInstance()
                .getReference("Root")
                .child("Users")
                .child(sanitizedEmail)
                .child("wage");

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue() != null) {
                    // Retrieve wage value
                    Double currWage = snapshot.getValue(Double.class);
                    if (currWage != null) {
                        callback.onWageRetrieved(currWage); // Pass retrieved wage to callback
                    } else {
                        Log.e("Firebase", "Wage value is null!");
                    }
                } else {
                    callback.onWageRetrieved(0.0); // Pass retrieved wage to callback

                    Log.e("Firebase", "Snapshot does not exist or is empty!");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to read value: " + error.getMessage());
            }
        });
    }
    public void initCalendarID(calendarIDCallback callback) {
        String sanitizedEmail = emailUser.replace(".", "_");
        DatabaseReference myRef = FirebaseDatabase.getInstance()
                .getReference("Root")
                .child("Users")
                .child(sanitizedEmail);

        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue() != null) {
                    // Retrieve wage value
                    String calendarIDSnap = snapshot.child("calendarID").getValue(String.class);
                    if (calendarIDSnap != null) {
                        callback.onCalendarIDRetrieved(calendarIDSnap);
                    } else {
                        Log.e("Firebase", "Wage value is null!");
                    }
                } else {
                    /////Need to implement for google new users
                    showPopupDialog();
                    Log.e("Firebase", "Snapshot does not exist or is empty!");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to read value: " + error.getMessage());
            }
        });
    }
    public double getWage(){
        return this.wage;
    }
    public void setWage(double wage) {
        this.wage = wage;
    }
    public void setGoogleAccountCredential(GoogleAccountCredential googleAccountCredential) {
        this.googleAccountCredential = googleAccountCredential;
    }
    public String getEmailUser() {
        return emailUser;
    }
    public boolean getuserExists(){
        return userExists;
    }
    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public String getUserPhoto() {
        return userPhoto;
    }
    public void setUserPhoto(String userPhoto) {
        this.userPhoto = userPhoto;
    }

    public void setEmailUser(String emailUser) {
        this.emailUser = emailUser;
    }


    public void setGoogleSignInClient(GoogleSignInClient googleSignInClient) {
        this.googleSignInClient = googleSignInClient;
    }

    public void setmAuth(FirebaseAuth mAuth) {
        this.mAuth = mAuth;
    }
    private void getUserName(String email, UserNameCallback callback) {
        String sanitizedEmail = email.replace(".", "_");
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Root");

        ref.child("Users").child(sanitizedEmail).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DataSnapshot snapshot = task.getResult();
                fullName = snapshot.child("fullName").getValue(String.class);
                //mainActivity.setFullName(fullName);
                callback.onUserNameRetrieved(fullName);
            } else {
                Log.e("Firebase", "Failed to get user name: " + task.getException());
                callback.onUserNameRetrieved(null);
            }
        });
    }
    private void setupGoogleAccountCredential() {

        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (signInAccount != null) {
            googleAccountCredential = GoogleAccountCredential.usingOAuth2(
                    this,
                    Collections.singleton("https://www.googleapis.com/auth/calendar")
            );
            googleAccountCredential.setSelectedAccount(signInAccount.getAccount());


        }
    }
    public String getCalendarID() {
        return calendarID;
    }

    public void setCalendarID(String calendarID) {
        this.calendarID = calendarID;
    }

    private void showPopupDialog() {
        // Inflate the custom layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View popupView = inflater.inflate(R.layout.insert_id_dialog, null);

        // Initialize UI elements in the popup
        EditText inputField = popupView.findViewById(R.id.inputField);
        Button submitButton = popupView.findViewById(R.id.submitButton);

        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(popupView);
        builder.setCancelable(false); // Prevent dismissal without action

        AlertDialog dialog = builder.create();
        dialog.show();

        // Handle the submit button
        submitButton.setOnClickListener(v -> {
            String input = inputField.getText().toString().trim();
            if (input.isEmpty()) {

                Toast.makeText(this, "Please fill in the required data", Toast.LENGTH_SHORT).show();
            } else {
                // Close the dialog only when data is valid
                setCalendarID(input);
                //Toast.makeText(this, "Data submitted: " + input, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
    }


}