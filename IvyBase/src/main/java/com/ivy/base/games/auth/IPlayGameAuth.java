package com.ivy.base.games.auth;

import com.ivy.base.Callback2;
import com.ivy.base.games.IPlayGameEvents;

import org.json.JSONObject;

public interface IPlayGameAuth extends IBaseAuth {

    void initialize(IAuthResponse authCallback, boolean checkLoginState);

    boolean isServiceAvailable();

    String requireServerSideAuthCode();

    String getPlayGamesUserInfo();

    void unlockAchievement(String achievementId);

    void increaseAchievement(String achievementId, int step);

    void setAchievementStep(String achievementId, int step);

    void showAchievement();

    void showLeaderboards();

    void showLeaderboard(String leaderboardId);

    void updateLeaderboard(String leaderboardId, long score);

    void loadLeaderboardScore(String leaderboardId);

    void setListener(IPlayGameEvents listener);

    void save(String archiveName, String actionId, String payload);

    void read(String archiveName);

    void submitEvent(String eventId, int increment);

    void loadEvent(String eventId);

    void loadEvents();

    void refreshAuthCode(Callback2<Boolean> callback);

    void submitGameEvent(String eventId, JSONObject property);

}
