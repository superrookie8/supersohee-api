package com.supersohee.api.auth.google;

public interface GoogleIdTokenVerifier {
    GoogleIdentity verify(String idToken);
}
