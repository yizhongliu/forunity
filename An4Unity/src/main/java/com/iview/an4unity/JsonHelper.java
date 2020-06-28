package com.iview.an4unity;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonHelper {

    public static JSONObject generateNotifyMsg(String type, String action, JSONObject arg) {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("type", type);
            jsonObject.put("action", action);
            jsonObject.put("arg", arg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
