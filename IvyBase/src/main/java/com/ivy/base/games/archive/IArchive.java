package com.ivy.base.games.archive;

public interface IArchive {

    void set(String collection, String documentId, String jsonData, IArchiveResult callback);

    void read(String collection, String documentId, IArchiveResult callback);

    void merge(String collection, String documentId, String jsonData, IArchiveResult callback);

    void query(String collection, String documentId, IArchiveResult callback);

    void delete(String collection, String documentId, IArchiveResult callback);

    void update(String collection, String documentId, String jsonData, IArchiveResult callback);

    void snapshot(String collection, String documentId, IArchiveResult callback);
}
