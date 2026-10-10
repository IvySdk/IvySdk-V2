package com.ivy.base.games.auth;

public interface IFirebaseAuthReload {
    void onReload(boolean status, String reason); // Kotlin默认"" -> Java直接允许传null
}