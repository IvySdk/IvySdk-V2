package com.ivy.firebase;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.fastjson.parser.Feature;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.ivy.base.ILog;
import com.ivy.base.IUnProguard;
import com.ivy.base.games.archive.IArchiveResult;
import com.ivy.base.games.archive.IIArchive;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FirestoreImpl implements IIArchive, IUnProguard {

    public static final String TAG = "Firestore";

    private FirebaseFirestore db = null;

    @Override
    public void setup(JSONObject config, boolean debug) {
        FirebaseInit.initApp();
        AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
            try {
                int retryCount = 0;
                int maxRetries = 50; //最长等待11s

                while (!FirebaseInit.isReady()) {
                    retryCount++;
                    if (retryCount > maxRetries) {
                        ILog.e(TAG, "Firestore 异步初始化超时，Firebase 核心未能按时就绪。");
                        return;
                    }

                    if (retryCount < 10) {
                        Thread.sleep(100);
                    } else {
                        Thread.sleep(250);
                    }
                }

                FirebaseFirestore.setLoggingEnabled(debug);
                db = FirebaseFirestore.getInstance();
                ILog.i(TAG, "Firestore 异步实例成功就绪（重试消耗次数: " + retryCount + "）");
            } catch (Throwable e) {
                ILog.e(TAG, "Firestore 异步初始化发生异常: " + e.getMessage());
            }
        });
    }

    private Map<String, Object> jsonToMap(String jsonData) {
        try {
            int disableBigDecimal = JSON.DEFAULT_PARSER_FEATURE & ~Feature.UseBigDecimal.mask;
            return JSON.parseObject(jsonData, new TypeReference<Map<String, Object>>() {
            }.getType(), disableBigDecimal);
        } catch (Exception e) {
            ILog.e(TAG, "format data err:" + e.getMessage());
            ILog.e(TAG, "err data :" + jsonData);
        }
        return null;
    }

    private Map<String, Object> jsonObjectToMap(JSONObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        try {
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = jsonObject.get(key);
                if (value instanceof JSONObject) {
                    map.put(key, jsonObjectToMap((JSONObject) value));
                } else {
                    map.put(key, value);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public void set(String collection, String documentId, String jsonData, IArchiveResult callback) {
        if (documentId == null || documentId.isEmpty()) {
            ILog.e(TAG, "invalid document id");
            callback.onFailure(collection, documentId, "invalid document id");
            return;
        }

        if (db == null) {
            ILog.w(TAG, "invalid firestore instance");
            callback.onFailure(collection, documentId, "invalid firestore instance");
            return;
        }

        try {
            Map<String, Object> data = jsonToMap(jsonData);
            if (data == null) {
                ILog.e(TAG, "set data failed");
                ILog.e(TAG, "data:" + jsonData);
                callback.onFailure(collection, documentId, "invalid data format");
                return;
            }

            db.collection(collection).document(documentId).set(data).addOnSuccessListener(aVoid -> callback.onSuccess(collection, documentId, "")).addOnFailureListener(e -> {
                ILog.e(TAG, "set data failed:" + e.getMessage());
                ILog.e(TAG, "data:" + jsonData);
                callback.onFailure(collection, documentId, e.getMessage());
            });

        } catch (Exception e) {
            ILog.e(TAG, "set data failed:" + e.getMessage());
            ILog.e(TAG, "data:" + jsonData);
            callback.onFailure(collection, documentId, e.getMessage());
        }
    }

    @Override
    public void read(String collection, String documentId, IArchiveResult callback) {
        if (documentId == null || documentId.isEmpty()) {
            ILog.e(TAG, "invalid document id");
            callback.onFailure(collection, documentId, "invalid document id");
            return;
        }

        if (db == null) {
            ILog.w(TAG, "invalid firestore instance");
            callback.onFailure(collection, documentId, "invalid firestore instance");
            return;
        }

        try {
            db.collection(collection).document(documentId).get().addOnSuccessListener(document -> {
                if (document == null || !document.exists()) {
                    ILog.e(TAG, collection + ";" + documentId + " data does not exists");
                    callback.onSuccess(collection, documentId, "{}");
                    return;
                }
                Map<String, Object> data = document.getData();
                if (data != null) {
                    callback.onSuccess(collection, documentId, new JSONObject(data).toString());
                } else {
                    ILog.e(TAG, collection + ";" + documentId + " data is null");
                    callback.onSuccess(collection, documentId, "{}");
                }
            }).addOnFailureListener(e -> {
                ILog.e(TAG, "read data failed:" + e.getMessage());
                callback.onFailure(collection, documentId, e.getMessage());
            });

        } catch (Exception e) {
            ILog.e(TAG, "read data failed:" + e.getMessage());
            callback.onFailure(collection, documentId, e.getMessage());
        }
    }

    @Override
    public void merge(String collection, String documentId, String jsonData, IArchiveResult callback) {
        if (documentId == null || documentId.isEmpty()) {
            ILog.e(TAG, "invalid document id");
            callback.onFailure(collection, documentId, "invalid document id");
            return;
        }

        if (db == null) {
            ILog.w(TAG, "invalid firestore instance");
            callback.onFailure(collection, documentId, "invalid firestore instance");
            return;
        }

        try {
            Map<String, Object> data = jsonToMap(jsonData);
            if (data == null) {
                ILog.e(TAG, "merge data failed");
                ILog.e(TAG, "data:" + jsonData);
                callback.onFailure(collection, documentId, "invalid data format");
                return;
            }

            db.collection(collection).document(documentId).set(data, SetOptions.merge()).addOnSuccessListener(aVoid -> callback.onSuccess(collection, documentId, null)).addOnFailureListener(e -> {
                ILog.e(TAG, "merge data failed:" + e.getMessage());
                ILog.e(TAG, "data:" + jsonData);
                callback.onFailure(collection, documentId, e.getMessage());
            });

        } catch (Exception e) {
            ILog.e(TAG, "merge data failed:" + e.getMessage());
            ILog.e(TAG, "data:" + jsonData);
            callback.onFailure(collection, documentId, e.getMessage());
        }
    }

    @Override
    public void delete(String collection, String documentId, IArchiveResult callback) {
        if (documentId == null || documentId.isEmpty()) {
            ILog.e(TAG, "invalid document id");
            callback.onFailure(collection, documentId, "invalid document id");
            return;
        }

        if (db == null) {
            ILog.w(TAG, "invalid firestore instance");
            callback.onFailure(collection, documentId, "invalid firestore instance");
            return;
        }

        try {
            db.collection(collection).document(documentId).delete().addOnSuccessListener(aVoid -> callback.onSuccess(collection, documentId, null)).addOnFailureListener(e -> {
                ILog.e(TAG, "delete data failed:" + e.getMessage());
                callback.onFailure(collection, documentId, e.getMessage());
            });
        } catch (Exception e) {
            ILog.e(TAG, "delete data failed:" + e.getMessage());
            callback.onFailure(collection, documentId, e.getMessage());
        }
    }

    @Override
    public void update(String collection, String documentId, String jsonData, IArchiveResult callback) {
        if (documentId == null || documentId.isEmpty()) {
            ILog.e(TAG, "invalid document id");
            callback.onFailure(collection, documentId, "invalid document id");
            return;
        }

        if (db == null) {
            ILog.w(TAG, "invalid firestore instance");
            callback.onFailure(collection, documentId, "invalid firestore instance");
            return;
        }

        try {
            Map<String, Object> data = jsonToMap(jsonData);
            if (data == null) {
                ILog.e(TAG, "update data failed");
                ILog.e(TAG, "data:" + jsonData);
                callback.onFailure(collection, documentId, "invalid data format");
                return;
            }

            db.collection(collection).document(documentId).update(data).addOnSuccessListener(aVoid -> callback.onSuccess(collection, documentId, null)).addOnFailureListener(e -> {
                ILog.e(TAG, "update data failed:" + e.getMessage());
                ILog.e(TAG, "data:" + jsonData);
                callback.onFailure(collection, documentId, e.getMessage());
            });

        } catch (Exception e) {
            ILog.e(TAG, "update data failed:" + e.getMessage());
            ILog.e(TAG, "data:" + jsonData);
            callback.onFailure(collection, documentId, e.getMessage());
        }
    }

    @Override
    public void query(final String collection, final String documentId, final IArchiveResult callback) {
        if (documentId == null || documentId.isEmpty()) {
            ILog.e(TAG, "invalid document id");
            callback.onFailure(collection, documentId, "invalid document id");
            return;
        }

        if (db != null) {
            try {
                db.collection(collection).document(documentId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot document) {
                        JSONArray json = new JSONArray();
                        if (document != null && document.exists() && document.getData() != null) {
                            json.put(new JSONObject(document.getData()));
                        }
                        // 如果你希望把结果回调，也可以加：
                        callback.onSuccess(collection, documentId, json.toString());
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        ILog.e(TAG, "query data failed:" + e.getMessage());
                        callback.onFailure(collection, documentId, e.getMessage());
                    }
                });
            } catch (Exception e) {
                ILog.e(TAG, "query data failed:" + e.getMessage());
                callback.onFailure(collection, documentId, e.getMessage());
            }
        } else {
            ILog.w(TAG, "invalid firestore instance");
            callback.onFailure(collection, documentId, "invalid firestore instance");
        }
    }


    @Override
    public void snapshot(String collection, String documentId, IArchiveResult callback) {
        if (documentId == null || documentId.isEmpty()) {
            ILog.e(TAG, "invalid document id");
            callback.onFailure(collection, documentId, "invalid document id");
            return;
        }

        if (db == null) {
            ILog.w(TAG, "invalid firestore instance");
            callback.onFailure(collection, documentId, "invalid firestore instance");
            return;
        }

        try {
            db.collection(collection).document(documentId).addSnapshotListener((value, error) -> {
                if (error != null) {
                    ILog.e(TAG, "snapshot err:" + error.getCode() + ";" + error.getMessage());
                    callback.onFailure(collection, documentId, error.getMessage());
                    return;
                }

                if (value != null && value.exists()) {
                    Map<String, Object> data = value.getData();
                    if (data != null) {
                        callback.onSuccess(collection, documentId, new JSONObject(data).toString());
                    } else {
                        ILog.e(TAG, "snapshot data invalid");
                        callback.onFailure(collection, documentId, "invalid data");
                    }
                } else {
                    ILog.e(TAG, "snapshot invalid");
                    callback.onFailure(collection, documentId, "invalid snapshot");
                }
            });
        } catch (Exception e) {
            ILog.e(TAG, "snapshot data failed:" + e.getMessage());
            callback.onFailure(collection, documentId, e.getMessage());
        }
    }
}
