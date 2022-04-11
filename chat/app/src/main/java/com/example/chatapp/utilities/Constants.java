package com.example.chatapp.utilities;

import java.util.HashMap;

public class Constants {
    public static final String KEY_COLLECTION_USER = "users";
    public static final String KEY_NAME = "name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_PREFERENCE_NAME = "chatAppPreference";
    public static final String KEY_IS_SIGN_IN = "isSignIn";
    public static final String KEY_USER_ID = "userID";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_FCM_TOKEN = "fcmToken";
    public static final String KEY_USER ="user";
    public static final String KEY_COLLECTION_CHAT ="chat";
    public static final String KEY_SENDER_ID = "sender id";
    public static final String KEY_RECEIVER_ID = "receiver id";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TIMESTAMP ="timestamp";
    public static final String KEY_COLLECTION_CONVERSATIONS = "conversations";
    public static final String KEY_SENDER_NAME ="sender name";
    public static final String KEY_RECEIVER_NAME ="receiver name";
    public static final String KEY_SENDER_IMAGE ="sender image";
    public static final String KEY_RECEIVER_IMAGE ="receiver image";
    public static final String KEY_LAST_MESSAGE ="last message";
    public static final String KEY_AVAILABILITY ="availability";
    public static final String REMOTE_MSG_AUTHORIZATION = "Authorization";
    public static final String REMOTE_MSG_CONTENT_TYPE="Content-Type";
    public static final String REMOTE_MSG_DATA ="data";
    public static final String REMOTE_MSG_REGISTRATION_IDS="registration_ids";

    public static HashMap<String,String> remoteMsgHeaders = null;
    public static HashMap<String,String> getRemoteMsgHeaders(){
        if(remoteMsgHeaders==null){
            remoteMsgHeaders = new HashMap<>();
            remoteMsgHeaders.put(
                    REMOTE_MSG_AUTHORIZATION,
                    "key=AAAAzNdtLEk:APA91bECBsXfSDapSunyFi3vFi171aH7vbxcE-3ZsM6vhxaijV_gVEK6pBjL7yQm5Mf70ngNRMLpBcOtoRQlAFXcYtzzESLlLZb9tZlIOWXxrUaHQw38wna6KFa2mpxRv2zozjaUXYp_"
            );
            remoteMsgHeaders.put(
                    REMOTE_MSG_CONTENT_TYPE,
                    "application/json"
            );
        }
        return remoteMsgHeaders;
    }


}
