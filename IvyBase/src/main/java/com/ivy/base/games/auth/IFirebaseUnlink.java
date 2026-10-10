package com.ivy.base.games.auth;

public interface IFirebaseUnlink {
    void onUnlinked(String unlinkChannel, boolean status, String reason); // Kotlin默认"" -> Java直接允许传null
}