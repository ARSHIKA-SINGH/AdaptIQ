package com.adaptiq.util;

import com.adaptiq.service.NormalExamSession;
import java.util.Optional;

/** Holds the active in-memory UI session; the attempt and answers remain persisted in MySQL. */
public final class ExamSessionManager {
    private static NormalExamSession session;
    private ExamSessionManager() { }
    public static synchronized void set(NormalExamSession value) { session=value; }
    public static synchronized Optional<NormalExamSession> current() { return Optional.ofNullable(session); }
    public static synchronized void clear() { session=null; }
}
