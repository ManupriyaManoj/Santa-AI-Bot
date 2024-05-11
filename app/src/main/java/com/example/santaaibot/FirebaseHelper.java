
package com.example.santaaibot;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class FirebaseHelper {
    private final FirebaseAuth mAuth;
    private final DatabaseReference mDatabase;
    private DatabaseReference
            database;

    // Constructor
    public FirebaseHelper() {
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference(); // Initialize rootRef
        this.database = FirebaseDatabase.getInstance().getReference();

    }
    // Method to add a message to Firebase database

    public void addMessage(String message, String messageType) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String userEmail = currentUser.getEmail().replace(".", ",");

            // Remove the "|user" or "|chatbot" tags from the message
            String cleanedMessage = message.replaceAll("\\|user|\\|chatbot", "");
            // Store the cleaned message along with its type (user or chatbot) under the user's email branch
            mDatabase.child("users").child(userEmail).child("chats").push().setValue(cleanedMessage);

        }
    }

    // Add
    // Method to retrieve all messages for a specific user from Firebase Realtime Database
    public void getAllMessages(String userId, String branch, OnMessagesLoadedListener listener) {
        DatabaseReference userMessagesRef = database.child(branch).child(userId);
        userMessagesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<MessageWithTimestamp> messages = new ArrayList<>();
                for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                    // Assuming MessageWithTimestamp class has a constructor that takes DataSnapshot
                    MessageWithTimestamp message = messageSnapshot.getValue(MessageWithTimestamp.class);
                    messages.add(message);
                }
                listener.onMessagesLoaded(messages);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onMessagesLoadError(databaseError.getMessage());
            }
        });
    }

    public DatabaseReference getUserChatsReference() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail().replace(".", ",");
            return mDatabase.child("").child(userEmail).child("");
        } else {
            // Handle user not logged in
            return null;
        }
    }

    // Class to represent a message along with its timestamp
    public static class MessageWithTimestamp {
        private String timestamp;
        private String message;

        public MessageWithTimestamp(String timestamp, String message) {
            this.timestamp = timestamp;
            this.message = message;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getMessage() {
            return message;
        }
    }

    // Interface to handle callbacks when messages are loaded
    public interface OnMessagesLoadedListener {
        void onMessagesLoaded(List<MessageWithTimestamp> messages);
        void onMessagesLoadError(String error);
    }
    public void getChatHistory(String userEmail, ValueEventListener listener) {
        DatabaseReference userChatRef = mDatabase.child("").child(userEmail);
        userChatRef.addListenerForSingleValueEvent(listener);
    }
    public void getLastChatTime(String userEmail, final ValueEventListener listener) {
        DatabaseReference userChatRef = mDatabase.child("users").child(userEmail.replace(".", ",")).child("chats");

        // Apply the query operations directly to the DatabaseReference
        userChatRef.orderByChild("timestamp").limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // ... (handle data retrieval)
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // ... (handle errors)
            }
        });
    }

}