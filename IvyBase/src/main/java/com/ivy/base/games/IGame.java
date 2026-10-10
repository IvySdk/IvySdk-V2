package com.ivy.base.games;

import android.content.Intent;

import com.ivy.base.games.archive.IArchive;
import com.ivy.base.games.auth.IAuthResponse;
import com.ivy.base.games.auth.IFirebaseAuthReload;
import com.ivy.base.games.auth.IFirebaseUnlink;

import org.json.JSONObject;

public interface IGame extends IArchive {

    void onActivityResult(int requestCode, int resultCode, Intent data);

    boolean isPlayGamesLogged();

    void loginPlayGames();

    void logoutPlayGames();

    String getPlayGamesUserInfo();

    String getPlayGamesUserId();

    void unlockAchievement(String achievementId);

    void increaseAchievement(String achievementId, int step);

    void setAchievementStep(String achievementId, int step);

    void showAchievement();

    void showLeaderboards();

    void showLeaderboard(String leaderboardId);

    void updateLeaderboard(String leaderboardId, long score);

    void loadLeaderboardScore(String leaderboardId);

    void saveArchive(String archiveName, String actionId, String payload);

    void readArchive(String archiveName);

    void submitActivityEvent(String eventId, int increment);

    void loadActivityEventById(String eventId);

    void loadActivityEvents();

    void submitPlayGameEvent(String eventId, JSONObject property);

    void loginFacebook();

    void logoutFacebook();

    boolean isFacebookLogged();

    String getFacebookFriends();

    String getFacebookUserInfo();

    String getFacebookUserId();

    void loginGoogle();

    void logoutGoogle();

    boolean isGoogleLogged();

    String getGoogleUserInfo();

    String getGoogleUserId();

    void logoutFirebase();

    String getFirebaseUserInfo(String channel); // channel 可传 null

    String getFirebaseUserId();

    boolean isFirebaseAnonymousLogged();

    boolean isFirebaseLinkedWithChannel(String channel);

    boolean canFirebaseUnlinkWithChannel(String channel);

    void unlinkFirebaseWithChannel(String channel, IFirebaseUnlink callback);

    void reloadFirebaseLastSign(IFirebaseAuthReload authReload);

    void loginAnonymous(IAuthResponse authResult);

    void loginWithGoogle(IAuthResponse authResult);

    void loginWithPlayGames(IAuthResponse authResult);

    void loginWithFacebook(IAuthResponse authResult);

    void loginWithEmailAndPassword(String email, String password, IAuthResponse authResult);
}

