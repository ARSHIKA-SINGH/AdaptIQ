package com.adaptiq.service;

import com.adaptiq.dao.SubjectDAO;
import com.adaptiq.dao.TopicDAO;
import com.adaptiq.exception.AuthorizationException;
import com.adaptiq.exception.ValidationException;
import com.adaptiq.model.Role;
import com.adaptiq.model.Subject;
import com.adaptiq.model.Topic;
import com.adaptiq.model.User;
import com.adaptiq.util.SessionManager;
import java.util.List;
import java.util.Objects;

/** Administration of academic subjects/topics; read access supports faculty question entry. */
public final class CatalogService {
    private final SubjectDAO subjects;
    private final TopicDAO topics;
    public CatalogService() { this(new SubjectDAO(), new TopicDAO()); }
    public CatalogService(SubjectDAO subjects, TopicDAO topics) { this.subjects = Objects.requireNonNull(subjects); this.topics = Objects.requireNonNull(topics); }
    public List<Subject> subjects() { requireAuthenticated(); return subjects.findAll(); }
    public List<Topic> topics(long subjectId) { requireAuthenticated(); return topics.findBySubject(subjectId); }
    public long saveSubject(Subject subject) { requireAdmin(); validate(subject == null ? null : subject.getSubjectName(), "Subject name"); if (subject.getSubjectId() == null) { long id=subjects.create(subject); subject.setSubjectId(id); return id; } subjects.update(subject); return subject.getSubjectId(); }
    public long saveTopic(Topic topic) { requireAdmin(); if (topic == null || topic.getSubjectId() == null) throw new ValidationException("Select a subject for the topic."); validate(topic.getTopicName(), "Topic name"); if (topic.getTopicId() == null) { long id=topics.create(topic); topic.setTopicId(id); return id; } topics.update(topic); return topic.getTopicId(); }
    public void deleteSubject(long id) { requireAdmin(); subjects.delete(id); }
    public void deleteTopic(long id) { requireAdmin(); topics.delete(id); }
    private void validate(String value, String label) { if (value == null || value.trim().isEmpty() || value.trim().length() > 120) throw new ValidationException(label + " must contain between 1 and 120 characters."); }
    private User requireAuthenticated() { return SessionManager.currentUser().orElseThrow(() -> new AuthorizationException("Please sign in to continue.")); }
    private void requireAdmin() { if (requireAuthenticated().getRole() != Role.ADMIN) throw new AuthorizationException("Only administrators can manage subjects and topics."); }
}
