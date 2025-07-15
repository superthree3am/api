package com.bni.api.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.springframework.stereotype.Service;

@Service
public class FirebaseAuthService {

    // Verify the ID token sent by frontend
    public FirebaseToken verifyIdToken(String idToken) throws Exception {
        return FirebaseAuth.getInstance().verifyIdToken(idToken);
    }

    // Fetch the full user record (to get phone number etc.)
    public UserRecord getUserByUid(String uid) throws Exception {
        return FirebaseAuth.getInstance().getUser(uid);
    }
}
