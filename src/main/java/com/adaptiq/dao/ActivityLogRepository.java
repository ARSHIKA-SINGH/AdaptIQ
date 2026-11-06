package com.adaptiq.dao;

public interface ActivityLogRepository {
    void record(Long userId, String activity);
}
