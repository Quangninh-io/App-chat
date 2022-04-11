package com.example.chatapp.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


import com.example.chatapp.adapter.ChatAdapter;
import com.example.chatapp.databinding.ActivityChatBinding;
import com.example.chatapp.model.ChatMessage;
import com.example.chatapp.model.Users;
import com.example.chatapp.network.APIClient;
import com.example.chatapp.network.APIService;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends BaseActivity {
    private ActivityChatBinding binding;

    private Users receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;

    private PreferenceManager preferenceManager;
    private FirebaseFirestore db;
    private String conversionId = null;

    private Boolean isReceiverAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListener();
        loadReceiverDetail();
        init();
        listenerMessages();
        Log.e("screen","chat");
    }
    private void setListener(){
        binding.imageBack.setOnClickListener(v->onBackPressed());
        binding.layoutSend.setOnClickListener(v->sendMessage());
    }
    private final EventListener<QuerySnapshot> eventListener = ((value, error) -> {
        if(error !=null){
            return;
        }
        if(value!=null){
            int count = chatMessages.size();
            for(DocumentChange documentChange: value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
            // sort message by time
            Collections.sort(chatMessages, new Comparator<ChatMessage>() {
                @Override
                public int compare(ChatMessage obj1, ChatMessage obj2) {
                    return obj1.dateObject.compareTo(obj2.dateObject);
                }
            });
            // abbreviation
            //Collections.sort(chatMessages,(obj1,obj2)->obj1.dateObject.compareTo(obj2.dateObject));
            if(count==0){
                chatAdapter.notifyDataSetChanged();
            }else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(),chatMessages.size());
                binding.chatRecycleView.smoothScrollToPosition(chatMessages.size()-1);
            }
            binding.chatRecycleView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if(conversionId ==null){
            checkConversion();
        }
    });

    private void sendNotification(String messageBody){
        APIClient.getClient().create(APIService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call,@NonNull Response<String> response) {
                if(response.isSuccessful()){
                    if(response.body()!=null){
                        try {
                            JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results = responseJson.getJSONArray("results");
                            if(responseJson.getInt("failure")==1){
                                JSONObject error = (JSONObject) results.get(0);
                                showToast(error.getString("error"));
                                return;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call,@NonNull Throwable t) {

            }
        });
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void listenerAvailabilityOfReceiver(){
        db.collection(Constants.KEY_COLLECTION_USER).document(
                receiverUser.id
        ).addSnapshotListener(ChatActivity.this, new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if(error!=null){
                    return;
                }
                if(value!=null){
                    if(value.getLong(Constants.KEY_AVAILABILITY)!=null){
                        int availability=  value.getLong(Constants.KEY_AVAILABILITY).intValue();
                        isReceiverAvailable = availability == 1;
                    }
                    receiverUser.token = value.getString(Constants.KEY_FCM_TOKEN);
                    if(receiverUser.image==null){
                        receiverUser.image = value.getString(Constants.KEY_IMAGE);
                        chatAdapter.setReceiverProfileImage(getBitmapFromEncodedString(receiverUser.image));
                        chatAdapter.notifyItemRangeInserted(0,chatMessages.size());
                    }

                }
                if(isReceiverAvailable){
                    binding.textAvailability.setVisibility(View.VISIBLE);
                }else{
                    binding.textAvailability.setVisibility(View.GONE);
                }
            }
        });
    }


    private void sendMessage(){
        HashMap<String,Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));

        message.put(Constants.KEY_RECEIVER_ID,receiverUser.id);

        message.put(Constants.KEY_MESSAGE,binding.inputTextMessage.getText().toString());
        message.put(Constants.KEY_TIMESTAMP,new Date());
        db.collection(Constants.KEY_COLLECTION_CHAT).add(message);
        if(conversionId!=null){
            updateConversion(binding.inputTextMessage.getText().toString());
        }else{
            HashMap<String,Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME,preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE,preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID,receiverUser.id);
            conversion.put(Constants.KEY_RECEIVER_NAME,receiverUser.name);
            conversion.put(Constants.KEY_RECEIVER_IMAGE,receiverUser.image);
            conversion.put(Constants.KEY_LAST_MESSAGE,binding.inputTextMessage.getText().toString());
            conversion.put(Constants.KEY_TIMESTAMP,new Date());
            addConversion(conversion);
        }
        if(!isReceiverAvailable){
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.token);

                JSONObject data = new JSONObject();

                data.put(Constants.KEY_USER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME,preferenceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN,preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                data.put(Constants.KEY_MESSAGE,binding.inputTextMessage.getText().toString());

                JSONObject body = new JSONObject();
                body.put(Constants.REMOTE_MSG_DATA,data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS,tokens);

                sendNotification(body.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        binding.inputTextMessage.setText(null);
    }

    private void listenerMessages(){
        db.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverUser.id)
                .addSnapshotListener(eventListener);

        db.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }


    private void addConversion(HashMap<String,Object> conversion){
        db.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        conversionId = documentReference.getId();
                    }
                });

    }
    private void updateConversion(String message){
        DocumentReference documentReference = db.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE,message,
                Constants.KEY_TIMESTAMP,new Date()
        );
    }

    private void checkConversion(){
        if(chatMessages.size()!=0){
            checkForConversionRemotely(
                    preferenceManager.getString(Constants.KEY_USER_ID),
                    receiverUser.id
            );
            checkForConversionRemotely(
                    receiverUser.id,
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );
        }
    }

    private void checkForConversionRemotely(String senderId, String receiverId){
        db.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful() && task.getResult() !=null &&task.getResult().getDocuments().size()>0){
                            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                            conversionId = documentSnapshot.getId();
                        }
                    }
                });
    }


    private Bitmap getBitmapFromEncodedString(String encodeImage){
        if(encodeImage!=null){
            byte[] bytes =Base64.decode(encodeImage,Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        }else {
            return null;
        }

    }
    private void  loadReceiverDetail(){
        receiverUser = (Users) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiverUser.name);
    }
    private String getReadDateTime(Date date){
        return  new SimpleDateFormat("MMMM/dd/yyyy -hh:mm", Locale.getDefault()).format(date);
    }

    private void init(){
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                getBitmapFromEncodedString(receiverUser.image),
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecycleView.setAdapter(chatAdapter);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();
        listenerAvailabilityOfReceiver();
    }
}