/*
 * Copyright (C) 2023 ThinkingData
 */
package ivy.data.analytics.aop.push;

import org.json.JSONObject;

import ivy.data.analytics.utils.TDConstants;

/**
 * <  >.
 *
 * @author liulongbing
 * @create 2023/10/16
 * @since
 */
public class PushEventItem {
    public TDConstants.DataType type;
    public JSONObject properties;
}
