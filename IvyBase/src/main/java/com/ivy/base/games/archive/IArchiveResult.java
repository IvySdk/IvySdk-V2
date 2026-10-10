package com.ivy.base.games.archive;

public interface IArchiveResult {

    /**
     * 成功回调
     * @param collection 集合名
     * @param documentId 文档ID，不可为 null
     * @param data 返回的数据，可为 null
     */
    void onSuccess(String collection, String documentId, String data);

    /**
     * 失败回调
     * @param collection 集合名
     * @param documentId 文档ID，不可为 null
     * @param reason 失败原因，可为 null
     */
    void onFailure(String collection, String documentId, String reason);
}
