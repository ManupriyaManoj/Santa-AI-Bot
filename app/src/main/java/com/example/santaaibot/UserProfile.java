package com.example.santaaibot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class UserProfile extends AppCompatActivity {

    private DatabaseReference mUserRef;
    private FirebaseUser mCurrentUser;

    private TextView userEmailTextView;
    private TextView accountCreatedDateTextView;
    private TextView activeTimeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        mCurrentUser = mAuth.getCurrentUser();
        mUserRef = mDatabase.getReference("users").child(mCurrentUser.getUid());

        userEmailTextView = findViewById(R.id.user_email_text_view);
        accountCreatedDateTextView = findViewById(R.id.account_created_date_text_view);
        activeTimeTextView = findViewById(R.id.last_active_time_text_view);
        // Retrieve and display user data including account creation date
        displayUserData();

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        // nav--bar
        NavigationView navigationView = findViewById(R.id.nav_view);

        final int NAV_CHAT_ID = R.id.nav_chat;
        final int NAV_HISTORY_ID = R.id.nav_history;
        final int NAV_LOGOUT_ID = R.id.nav_logout;

        navigationView.setNavigationItemSelectedListener(item -> {
            // Handle navigation item clicks here
            int itemId = item.getItemId();
            if (itemId == NAV_CHAT_ID) {
                // Open chat activity
                Toast.makeText(UserProfile.this, "Opening Chats", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(UserProfile.this, ChatbotActivity.class));
            } else if (itemId == NAV_HISTORY_ID) {
                // Open history activity
                Toast.makeText(UserProfile.this, "Opening History", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(UserProfile.this, History.class));
            } else if (itemId == NAV_LOGOUT_ID) {
                // Log out user
                Toast.makeText(UserProfile.this, "Opening Profile", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(UserProfile.this, UserProfile.class));
            }

            // Close the navigation drawer
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

    }

    private void displayUserData() {
        if (mCurrentUser != null) {
            // User's email
            userEmailTextView.setText(mCurrentUser.getEmail());

            // Retrieve additional user data from Firebase Realtime Database
            mUserRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Check if the user data exists
                    if (dataSnapshot.exists()) {
                        // Account created date
                        Long accountCreatedTimestamp = dataSnapshot.child("created_at").getValue(Long.class);
                        if (accountCreatedTimestamp != null) {
                            String accountCreatedDateTime = getFormattedDateTime(accountCreatedTimestamp);
                            accountCreatedDateTextView.setText("Account created on: " + accountCreatedDateTime);
                        } else {
                            accountCreatedDateTextView.setText("Account creation date not available");
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle database error
                    Toast.makeText(UserProfile.this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show();
                }
            });

            // Get the last message's timestamp from the user's chat history
            FirebaseHelper firebaseHelper = new FirebaseHelper();
            firebaseHelper.getChatHistory(Objects.requireNonNull(mCurrentUser.getEmail()).replace(".", ","), new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        long lastMessageTimestamp = 0;
                        for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                            FirebaseHelper.MessageWithTimestamp chatMessage = messageSnapshot.getValue(FirebaseHelper.MessageWithTimestamp.class);
                            if (chatMessage != null) {
                                // Convert timestamp to long
                                long messageTimestamp = Long.parseLong(chatMessage.getTimestamp());
                                // Compare timestamps to find the latest one
                                if (messageTimestamp > lastMessageTimestamp) {
                                    lastMessageTimestamp = messageTimestamp;
                                }
                            }
                        }
                        // Convert the last message timestamp to a human-readable format
                        String lastMessageTime = getFormattedDateTime(lastMessageTimestamp);
                        activeTimeTextView.setText("Last message: " + lastMessageTime);
                    } else {
                        activeTimeTextView.setText("No messages yet");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle database error
                    Toast.makeText(UserProfile.this, "Failed to retrieve chat history.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    // Method to format timestamp to a human-readable date and time
    private String getFormattedDateTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

}

