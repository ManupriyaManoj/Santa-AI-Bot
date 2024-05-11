package com.example.santaaibot;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.util.ArrayList;
import java.util.List;

public class History extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private LinearLayout chatHistoryLayout;
    private List<String> chatHistoryList;
    private int chatBubblePadding;
    private int textPadding;
    private int chatBubbleMargin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        chatHistoryLayout = findViewById(R.id.history_container);
        chatHistoryList = new ArrayList<>();

        Resources resources = getResources();
        chatBubblePadding = resources.getDimensionPixelSize(R.dimen.chat_bubble_padding);
        textPadding = resources.getDimensionPixelSize(R.dimen.text_padding);
        chatBubbleMargin = resources.getDimensionPixelSize(R.dimen.chat_bubble_margin);


        // Initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();


        // Navigation drawer setup
        NavigationView navigationView = findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_chat) {
                Toast.makeText(History.this, "Opening Chats", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(History.this, ChatbotActivity.class));
            } else if (itemId == R.id.nav_history) {
                Toast.makeText(History.this, "Opening History", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(History.this, History.class));
            } else if (itemId == R.id.nav_logout) {
                Toast.makeText(History.this, "Opening Profile", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(History.this, UserProfile.class));
            }
            drawerLayout.close();
            return true;
        });

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            // Retrieve chat history for the current user
            retrieveChatHistory(userEmail);
        }
    }

    private void retrieveChatHistory(String userEmail) {
        String encodedEmail = encodeEmail(userEmail);
        DatabaseReference userChatRef = mDatabase.child("users").child(encodedEmail).child("chats");
        userChatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatHistoryList.clear();
                for (DataSnapshot chatSnapshot : snapshot.getChildren()) {
                    // Check if the snapshot's value is a String
                    if (chatSnapshot.getValue() instanceof String) {
                        String chatMessage = chatSnapshot.getValue(String.class);
                        chatHistoryList.add(chatMessage);
                    } else {
                        // If the value is not a String, log an error
                        Log.e("HistoryActivity", "Error: Chat message is not a String");
                    }
                }
                displayChatHistory();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HistoryActivity", "Error retrieving chat history: " + error.getMessage());
            }
        });
    }



    private void displayChatHistory() {
        // Iterate through the chat history list and display each conversation
        for (int i = 0; i < chatHistoryList.size(); i += 2) {
            String userQuestion = chatHistoryList.get(i);
            String chatbotAnswer = (i + 1 < chatHistoryList.size()) ? chatHistoryList.get(i + 1) : ""; // Ensure there's a chatbot answer
            displayConversation(userQuestion, chatbotAnswer);
        }


    }

    private String encodeEmail(String email) {
        // Replace invalid characters with valid ones
        return email.replace(".", ",");
    }

    private void displayConversation(String userQuestion, String chatbotAnswer) {
        // Concatenate user question and chatbot answer
        String conversationText = userQuestion + "\n" + chatbotAnswer + "\n\n";

        // Create a TextView for displaying the conversation
        TextView conversationTextView = new TextView(this);
        conversationTextView.setText(conversationText);
        conversationTextView.setTextColor(Color.BLACK); // Set text color
        conversationTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16); // Set text size

        // Add padding to the TextView to create margin between text and background
        int padding = getResources().getDimensionPixelSize(R.dimen.chat_bubble_padding);
        conversationTextView.setPadding(padding, padding, 20, 0);

        //set bg color
        conversationTextView.setBackgroundResource(R.drawable.chat_history); // Set the drawable resource
        // Create layout parameters with margins to add padding between TextViews
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(30, 0, 30, padding); // Set bottom margin to add space between TextViews
        conversationTextView.setLayoutParams(layoutParams);

        // Add the TextView to the chat container
        chatHistoryLayout.addView(conversationTextView);
    }


}