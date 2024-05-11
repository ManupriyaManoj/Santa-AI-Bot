package com.example.santaaibot;

import static com.example.santaaibot.R.drawable.receive_round_box;
import static com.example.santaaibot.R.drawable.send_round_box;
import static com.example.santaaibot.R.id.toolbar;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class ChatbotActivity extends AppCompatActivity {

    private EditText userQuestionEditText;
    private LinearLayout chatContainer; // LinearLayout to hold chat messages

    private Handler backgroundHandler;
    private JSONArray qaData; // Declare qaData as a class variable


    private FirebaseHelper firebaseHelper; //firebase realtime database

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        firebaseHelper = new FirebaseHelper();



        // Initialize UI components
        userQuestionEditText = findViewById(R.id.user_question_edit_text);
        userQuestionEditText.requestFocus();
        userQuestionEditText = findViewById(R.id.user_question_edit_text);
        userQuestionEditText.requestFocus();
        chatContainer = findViewById(R.id.chat_container);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);

        // Initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.menu);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        ImageView menuIcon = findViewById(R.id.menu_icon);  // Replace with your image view ID
        menuIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open the Navigation Drawer here
                DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
                drawerLayout.openDrawer(GravityCompat.START);  // Opens the drawer from the left side (START)
            }
        });
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
                Toast.makeText(ChatbotActivity.this, "Opening Chats", Toast.LENGTH_SHORT).show();
                // startActivity(new Intent(ChatbotActivity.this, ChatActivity.class));
            } else if (itemId == NAV_HISTORY_ID) {
                // Open history activity
                Toast.makeText(ChatbotActivity.this, "Opening History ", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(ChatbotActivity.this, History.class));
            } else if (itemId == NAV_LOGOUT_ID) {
                // Log out user
                Toast.makeText(ChatbotActivity.this, "Opening Profile", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(ChatbotActivity.this, UserProfile.class));
            }

            // Close the navigation drawer
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });



        // Initialize background handler thread (optional, remove if not needed)
        HandlerThread handlerThread = new HandlerThread("ChatbotThread");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());

        // Load data in a background thread (optional, handle loading state)
        backgroundHandler.post(this::loadDataFromJson);

        // Send button click listener
        Button btnSend = findViewById(R.id.send_button);
        btnSend.setOnClickListener(view -> {
            String question = userQuestionEditText.getText().toString();
            if (!TextUtils.isEmpty(question)) {
                userQuestionEditText.setText("");
                addUserChatMessage();  // Add user question to chat view
                searchForAnswer(question); // Call searchForAnswer
            } else {
                Toast.makeText(ChatbotActivity.this, "Please enter text!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cleanup any resources if necessary
        if (backgroundHandler != null) {
            backgroundHandler.getLooper().quitSafely();
        }
    }

    private void loadDataFromJson() {
        try {
            // Load JSON data from assets folder
            AssetManager assetManager = getAssets();
            InputStream inputStream = assetManager.open("data.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            // Log the loaded JSON data (for debugging)
            Log.d("ChatbotActivity", "Loaded JSON data: " + json);

            // Parse JSON data
            JSONObject jsonData = new JSONObject(json);
            JSONArray intentsArray = jsonData.getJSONArray("intents");

            // Store intents data
            qaData = intentsArray;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Log.e("ChatbotActivity", "Error loading data from JSON file!", e);
            Toast.makeText(this, "Error loading data from JSON file!", Toast.LENGTH_SHORT).show();
        }
    }



    private void searchForAnswer(String question) {
        if (qaData != null) {
            try {
                boolean foundMatch = false; // Flag to track if a match is found

                // Iterate through each intent
                for (int i = 0; i < qaData.length(); i++) {
                    JSONObject intent = qaData.getJSONObject(i);
                    JSONArray patterns = intent.getJSONArray("patterns");

                    // Check if the user's question matches any pattern in the current intent
                    for (int j = 0; j < patterns.length(); j++) {
                        String pattern = patterns.getString(j);
                        if (question.toLowerCase().contains(pattern.toLowerCase())) {
                            // If a match is found, retrieve a random response from the intent and display it
                            JSONArray responses = intent.getJSONArray("responses");
                            String randomResponse = responses.getString((int) (Math.random() * responses.length()));

                            // Construct conversation string
                            String userMessage = "You: \n\t\t " + question ;
                            String chatbotAnswer = "Chatbot:\n\t\t " + randomResponse ;

                            // Call functions to add TextViews
                            addUserChatMessage();
                            displayConversation(userMessage, chatbotAnswer);

                            foundMatch = true; // Set flag to true since a match is found
                            break; // Exit the loop once a match is found
                        }
                    }
                    if (foundMatch) {
                        break; // Exit the outer loop if a match is found
                    }
                }

                // If no match is found, display a default "no answer found" message
                if (!foundMatch) {
                    String noAnswerMessage = "Chatbot:\n\t Sorry, no answer found.";
                    displayConversation("You: " + question + "\n", noAnswerMessage);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("ChatbotActivity", "Error parsing JSON data during search!", e);
                Toast.makeText(this, "Error searching data. Please try again later.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Search data not loaded yet. Please try again later.", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayConversation(String userMessage, String chatbotAnswer) {

        // Create a new layout to hold the user's message
        LinearLayout userMessageLayout = new LinearLayout(this);
        LinearLayout.LayoutParams userLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        // Align user message to the right side of the screen
        userLayoutParams.gravity = Gravity.END;
        userMessageLayout.setLayoutParams(userLayoutParams);

        // Create a TextView for the user's message
        TextView userTextView = new TextView(this);
        userTextView.setTextColor(Color.BLACK); // Set text color
        userTextView.setText(userMessage); // Set user message
        userTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16); // Set text size
        userTextView.setBackgroundResource(send_round_box); // Set background resource for user messages
        LinearLayout.LayoutParams userTextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        userTextView.setLayoutParams(userTextParams);

        // Add padding to the TextView to create margin between text and background
        int userpadding = getResources().getDimensionPixelSize(R.dimen.chatbot_text_padding);
        userTextView.setPadding(userpadding, userpadding, userpadding, userpadding);

        // Define right margins
        int usermarginRight = getResources().getDimensionPixelSize(R.dimen.right_margin);

        // Add margin to the TextView to create additional space between text and other views
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) userTextView.getLayoutParams();
        layoutParams.setMargins(0, 0, usermarginRight, 0);
        userTextView.setLayoutParams(layoutParams);

        // Add the user's TextView to the user's message layout
        userMessageLayout.addView(userTextView);

        // Add the user's message layout to the chat container
        chatContainer.addView(userMessageLayout);

        ////////////////////////////////////////////////////////////////////////////////////

        // Create a TextView for the chatbot's answer
        TextView chatbotTextView = new TextView(this);
        chatbotTextView.setTextColor(Color.BLACK); // Set text color
        chatbotTextView.setText(chatbotAnswer); // Set chatbot's answer
        chatbotTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16); // Set text size
        chatbotTextView.setBackgroundResource(receive_round_box); // Set background resource for chatbot messages
        LinearLayout.LayoutParams chatbotTextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        chatbotTextView.setLayoutParams(chatbotTextParams);

        // Add padding to the TextView to create margin between text and background
        int chatbotPadding = getResources().getDimensionPixelSize(R.dimen.chatbot_text_padding);
        chatbotTextView.setPadding(chatbotPadding, chatbotPadding, chatbotPadding, chatbotPadding);

        // Define left margins
        int chatbotMarginLeft = getResources().getDimensionPixelSize(R.dimen.left_margin);
        int chatbotMarginRight = getResources().getDimensionPixelSize(R.dimen.right_margin_chatbot);

        // Add margin to the TextView to create additional space between text and other views
        LinearLayout.LayoutParams chatbotLayoutParamsWithMargins = (LinearLayout.LayoutParams) chatbotTextView.getLayoutParams();
        chatbotLayoutParamsWithMargins.setMargins(chatbotMarginLeft, 20, chatbotMarginRight, 0);
        chatbotTextView.setLayoutParams(chatbotLayoutParamsWithMargins);

        // Add the chatbot's TextView to the chat container
        chatContainer.addView(chatbotTextView);

        // Scroll to the bottom of the chat container
        scrollToBottom();

        // Add the user's message t
        firebaseHelper.addMessage(userMessage, "");
        firebaseHelper.addMessage( chatbotAnswer, "");
    }

    private void addUserChatMessage() {
        // Create a new TextView for the user's message
        TextView textView = new TextView(this);
        // Add some padding to the TextView
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 8, 0, 8);
        textView.setLayoutParams(layoutParams);

        // Add the TextView to the chat container
        chatContainer.addView(textView);
        scrollToBottom();

    }
    private void scrollToBottom() {
        NestedScrollView scrollView = findViewById(R.id.chat_scroll_view);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

}