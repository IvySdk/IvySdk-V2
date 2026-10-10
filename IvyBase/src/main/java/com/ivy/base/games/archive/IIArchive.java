package com.ivy.base.games.archive;

import org.json.JSONObject;

public interface IIArchive extends IArchive {
    void setup(JSONObject config, boolean debug);
}