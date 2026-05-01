package com.lingring.global.auth.jwt;

public interface IdTokenVerifier {

    String verify(String idToken);
}
