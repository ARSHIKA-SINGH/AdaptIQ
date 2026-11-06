package com.adaptiq.dao;

import com.adaptiq.config.DatabaseConnection;
import com.adaptiq.exception.DatabaseException;
import com.adaptiq.model.Difficulty;
import com.adaptiq.service.AdaptiveEngine;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Persists the audit trail required to explain adaptive decisions. */
public final class AdaptiveHistoryDAO {
    public Set<Long> shownQuestionIds(long attemptId) {
        String sql="SELECT question_id FROM adaptive_history WHERE attempt_id=?";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,attemptId);try(ResultSet r=s.executeQuery()){List<Long> ids=new ArrayList<>();while(r.next())ids.add(r.getLong(1));return ids.stream().collect(Collectors.toSet());}}
        catch(SQLException e){throw new DatabaseException("Could not retrieve adaptive history.",e);}
    }
    public List<Boolean> recentCorrectness(long attemptId) {
        String sql="SELECT sa.is_correct FROM adaptive_history h JOIN student_answers sa ON sa.attempt_id=h.attempt_id AND sa.question_id=h.question_id WHERE h.attempt_id=? AND sa.is_correct IS NOT NULL ORDER BY h.created_at";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,attemptId);try(ResultSet r=s.executeQuery()){List<Boolean> values=new ArrayList<>();while(r.next())values.add(r.getBoolean(1));return values;}}
        catch(SQLException e){throw new DatabaseException("Could not retrieve adaptive performance.",e);}
    }
    public Difficulty currentDifficulty(long attemptId) {
        String sql="SELECT selected_difficulty FROM adaptive_history WHERE attempt_id=? ORDER BY history_id DESC LIMIT 1";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,attemptId);try(ResultSet r=s.executeQuery()){return r.next()?Difficulty.valueOf(r.getString(1)):Difficulty.MEDIUM;}}
        catch(SQLException e){throw new DatabaseException("Could not retrieve adaptive difficulty.",e);}
    }
    public void record(long attemptId,long questionId,Difficulty previous,Difficulty selected,BigDecimal accuracy) {
        String sql="INSERT INTO adaptive_history (attempt_id,question_id,previous_difficulty,selected_difficulty,accuracy_at_decision) VALUES (?, ?, ?, ?, ?)";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,attemptId);s.setLong(2,questionId);s.setString(3,previous.name());s.setString(4,selected.name());s.setBigDecimal(5,accuracy);s.executeUpdate();}
        catch(SQLException e){throw new DatabaseException("Could not record adaptive decision.",e);}
    }
}
