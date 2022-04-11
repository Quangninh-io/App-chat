package com.example.chatapp.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.chatapp.R;

import com.example.chatapp.databinding.ActivitySingInBinding;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class SingInActivity extends AppCompatActivity {

    private ActivitySingInBinding binding;
    private PreferenceManager preferenceManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySingInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        if(preferenceManager.getBoolean(Constants.KEY_IS_SIGN_IN)){
            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            finish();
        }
        setListeners();
    }

    private void setListeners() {
        binding.textCreateNewAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SingInActivity.this, SignUpActivity.class);
                startActivity(intent);
            }
        });
        binding.buttonSignIn.setOnClickListener(v->{
            if(isValidSignIn()){
                signIn();
            }
        });

    }
    private void signIn(){
        loading(true);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(Constants.KEY_COLLECTION_USER)
                .whereEqualTo(Constants.KEY_EMAIL,binding.inputEmailAddress.getText().toString().trim())
                .whereEqualTo(Constants.KEY_PASSWORD,binding.inputPassword.getText().toString().trim())
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && task.getResult()!=null && task.getResult().getDocuments().size()>0) {
                            loading(false);
                            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                            preferenceManager.putBoolean(Constants.KEY_IS_SIGN_IN,true);
                            preferenceManager.putString(Constants.KEY_USER_ID,documentSnapshot.getId());
                            preferenceManager.putString(Constants.KEY_NAME,documentSnapshot.getString(Constants.KEY_NAME));
                            preferenceManager.putString(Constants.KEY_IMAGE,documentSnapshot.getString(Constants.KEY_IMAGE));

                            Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } else {
                            loading(false);
                            showToast("unstable to sign in");
                        }
                    }
                });
    }

    private void loading(Boolean isLoading){
        if(isLoading){
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        }else{
            binding.buttonSignIn.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    private Boolean isValidSignIn(){
        if(binding.inputEmailAddress.getText().toString().trim().isEmpty()){
            showToast("Enter gmail");
            return false;
        }else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmailAddress.getText().toString().trim()).matches()){
            showToast("Enter valid gmail");
            return false;
        }
        else if(binding.inputPassword.getText().toString().trim().isEmpty()){
            showToast("Enter password");
            return false;
        }else{
            return true;
        }
    }

}