package com.example.chatapp.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.View;


import com.example.chatapp.adapter.UserAdapter;
import com.example.chatapp.databinding.ActivityUserBinding;
import com.example.chatapp.listener.UserListener;
import com.example.chatapp.model.Users;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;


import java.util.ArrayList;
import java.util.List;

public class UserActivity extends BaseActivity implements UserListener {

    private ActivityUserBinding binding;
    private PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListener();
        getUsers();
        Log.e("screen","user");
        Log.e("key_user",preferenceManager.getString(Constants.KEY_USER_ID));

    }
    private void setListener(){
        binding.imageBack.setOnClickListener(v->onBackPressed());
    }

    private void getUsers(){
        loading(true);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(Constants.KEY_COLLECTION_USER)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if(task.isSuccessful() &&task.getResult()!=null){
                        List<Users> users = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if(currentUserId.equals(queryDocumentSnapshot.getId())){
                                continue;
                            }
                            Users user = new Users();
                            user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                            user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                            user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                            user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            user.id = queryDocumentSnapshot.getId();
                            users.add(user);
                        }
                        if(users.size()>0){
                            UserAdapter userAdapter = new UserAdapter(users,this);
                            binding.userRecycleView.setAdapter(userAdapter);
                            binding.userRecycleView.setVisibility(View.VISIBLE);
                        }else{
                            showErrorMessage();
                        }
                    }
                });
    }
    private void showErrorMessage(){
        binding.textErrorMessage.setText(String.format("%s","No user available"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }

    private void loading(Boolean isLoading){
        if(isLoading){
            binding.progressBar.setVisibility(View.VISIBLE);
        }else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserClicked(Users users) {
        Intent intent = new Intent(getApplicationContext(),ChatActivity.class);
        intent.putExtra(Constants.KEY_USER,users);
        startActivity(intent);
        finish();
    }
}