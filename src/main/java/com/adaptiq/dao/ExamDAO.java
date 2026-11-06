package com.adaptiq.dao;

import com.adaptiq.config.ConnectionProvider;
import com.adaptiq.config.DatabaseConnection;
import com.adaptiq.exception.DatabaseException;
import com.adaptiq.model.*;
import java.sql.*;
import java.util.*;

/** Persisted fixed-question examination definitions. */
public final class ExamDAO implements ExamRepository {
    private final ConnectionProvider connections;
    public ExamDAO() { this(DatabaseConnection::getConnection); }
    public ExamDAO(ConnectionProvider connections) { this.connections=connections; }
    @Override public long create(Exam exam, List<Long> ids) {
        try (Connection c=connections.getConnection()) { c.setAutoCommit(false); try {
            exam.setTotalMarks(validateQuestionScope(c, exam.getSubjectId(), ids)); long examId;
            try (PreparedStatement s=c.prepareStatement("INSERT INTO exams (title, description, subject_id, faculty_id, duration_minutes, total_marks, exam_mode, status) VALUES (?, ?, ?, ?, ?, ?, 'NORMAL', 'PUBLISHED')", Statement.RETURN_GENERATED_KEYS)) {
                s.setString(1,exam.getTitle()); s.setString(2,exam.getDescription()); s.setLong(3,exam.getSubjectId()); s.setLong(4,exam.getFacultyId()); s.setInt(5,exam.getDurationMinutes()); s.setBigDecimal(6,exam.getTotalMarks()); s.executeUpdate(); try(ResultSet k=s.getGeneratedKeys()){if(!k.next())throw new DatabaseException("Exam creation did not return an identifier.");examId=k.getLong(1);} }
            try(PreparedStatement s=c.prepareStatement("INSERT INTO exam_questions (exam_id, question_id, question_order) VALUES (?, ?, ?)")){int order=1;for(Long id:ids){s.setLong(1,examId);s.setLong(2,id);s.setInt(3,order++);s.addBatch();}s.executeBatch();} c.commit(); return examId;
        } catch(SQLException|RuntimeException e){rollback(c);throw wrap("Could not create the examination.",e);} finally {autoCommit(c);} } catch(SQLException e){throw new DatabaseException("Could not create the examination.",e);}
    }
    @Override public List<Exam> findPublishedNormal() { return find("SELECT exam_id,title,description,subject_id,faculty_id,duration_minutes,total_marks,exam_mode,status,created_at FROM exams WHERE exam_mode='NORMAL' AND status='PUBLISHED' ORDER BY created_at DESC", null); }
    @Override public Optional<Exam> findById(long id) { List<Exam> values=find("SELECT exam_id,title,description,subject_id,faculty_id,duration_minutes,total_marks,exam_mode,status,created_at FROM exams WHERE exam_id=?",id); return values.stream().findFirst(); }
    @Override public List<ExamQuestionView> studentQuestions(long examId) {
        String sql="SELECT q.question_id,q.question_text,q.marks,o.option_id,o.option_label,o.option_text FROM exam_questions eq JOIN questions q ON q.question_id=eq.question_id JOIN question_options o ON o.question_id=q.question_id WHERE eq.exam_id=? ORDER BY eq.question_order,o.option_label";
        try(Connection c=connections.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,examId);try(ResultSet r=s.executeQuery()){Map<Long,MutableQuestion> grouped=new LinkedHashMap<>();while(r.next()){MutableQuestion q=grouped.computeIfAbsent(r.getLong("question_id"),id->new MutableQuestion(id,r));q.options.add(new ExamQuestionView.OptionView(r.getLong("option_id"),r.getString("option_label"),r.getString("option_text")));}return grouped.values().stream().map(MutableQuestion::view).toList();}}
        catch(SQLException e){throw new DatabaseException("Could not load examination questions.",e);}
    }
    private List<Exam> find(String sql,Long id){try(Connection c=connections.getConnection();PreparedStatement s=c.prepareStatement(sql)){if(id!=null)s.setLong(1,id);try(ResultSet r=s.executeQuery()){List<Exam> exams=new ArrayList<>();while(r.next())exams.add(map(r));return exams;}}catch(SQLException e){throw new DatabaseException("Could not retrieve examinations.",e);}}
    private Exam map(ResultSet r)throws SQLException{Exam e=new Exam();e.setExamId(r.getLong("exam_id"));e.setTitle(r.getString("title"));e.setDescription(r.getString("description"));e.setSubjectId(r.getLong("subject_id"));e.setFacultyId(r.getLong("faculty_id"));e.setDurationMinutes(r.getInt("duration_minutes"));e.setTotalMarks(r.getBigDecimal("total_marks"));e.setExamMode(ExamMode.valueOf(r.getString("exam_mode")));e.setStatus(ExamStatus.valueOf(r.getString("status")));e.setCreatedAt(r.getTimestamp("created_at").toInstant());return e;}
    private java.math.BigDecimal validateQuestionScope(Connection c,long subjectId,List<Long> ids)throws SQLException{if(ids.isEmpty()||new HashSet<>(ids).size()!=ids.size())throw new DatabaseException("An examination needs distinct questions.");String marks="?".repeat(ids.size()).replace("?", "?,");marks=marks.substring(0,marks.length()-1);String sql="SELECT COUNT(*), COALESCE(SUM(q.marks),0) FROM questions q JOIN topics t ON t.topic_id=q.topic_id WHERE t.subject_id=? AND q.question_id IN ("+marks+")";try(PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,subjectId);for(int i=0;i<ids.size();i++)s.setLong(i+2,ids.get(i));try(ResultSet r=s.executeQuery()){r.next();if(r.getInt(1)!=ids.size())throw new DatabaseException("Every selected question must belong to the examination subject.");return r.getBigDecimal(2);}}}
    private void rollback(Connection c){try{c.rollback();}catch(SQLException ignored){}}
    private void autoCommit(Connection c){try{c.setAutoCommit(true);}catch(SQLException ignored){}}
    private DatabaseException wrap(String m,Exception e){return e instanceof DatabaseException d?d:new DatabaseException(m,e);}
    private static final class MutableQuestion { final long id; final String text; final java.math.BigDecimal marks; final List<ExamQuestionView.OptionView> options=new ArrayList<>(); MutableQuestion(long id,ResultSet r){this.id=id;try{text=r.getString("question_text");marks=r.getBigDecimal("marks");}catch(SQLException e){throw new IllegalStateException(e);}} ExamQuestionView view(){return new ExamQuestionView(id,text,List.copyOf(options),marks);} }
}
