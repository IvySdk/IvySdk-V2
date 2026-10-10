package com.ivy.base.games;

public interface IPlayGameEvents {


    void onReadArchive(String archiveName, String result);

    void onWriteArchive(String archiveName, String actionId, Boolean result);

    void onLoadEvents(String data);

    void onLeaderBoardScore(String leaderboardId, String score, String rank);

}
