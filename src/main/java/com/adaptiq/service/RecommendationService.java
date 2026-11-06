package com.adaptiq.service;

import com.adaptiq.config.DatabaseConnection;
import com.adaptiq.dao.RecommendationDAO;
import com.adaptiq.exception.AuthorizationException;
import com.adaptiq.exception.DatabaseException;
import com.adaptiq.model.Difficulty;
import com.adaptiq.model.Recommendation;
import com.adaptiq.model.Role;
import com.adaptiq.model.User;
import com.adaptiq.util.SessionManager;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Turns real topic-performance records into transparent, refreshable learning actions. */
public final class RecommendationService {
    private final RecommendationDAO recommendations=new RecommendationDAO();
    public List<Recommendation> refreshMine(){User student=requireStudent();List<Recommendation> generated=new ArrayList<>();String sql="SELECT p.topic_id,t.topic_name,p.accuracy FROM topic_performance p JOIN topics t ON t.topic_id=p.topic_id WHERE p.student_id=? AND p.total_attempted>0";try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,student.getUserId());try(ResultSet r=s.executeQuery()){while(r.next()){BigDecimal accuracy=r.getBigDecimal(3);Recommendation x=new Recommendation();x.setTopicId(r.getLong(1));x.setTopicName(r.getString(2));if(accuracy.compareTo(BigDecimal.valueOf(50))<0){x.setRecommendedDifficulty(Difficulty.EASY);x.setRecommendationText("Revise "+x.getTopicName()+" concepts and attempt 10 easy-level practice questions.");}else if(accuracy.compareTo(BigDecimal.valueOf(80))<0){x.setRecommendedDifficulty(Difficulty.MEDIUM);x.setRecommendationText("Strengthen "+x.getTopicName()+" with 10 medium-level practice questions.");}else{x.setRecommendedDifficulty(Difficulty.HARD);x.setRecommendationText("Extend mastery of "+x.getTopicName()+" with 10 advanced questions.");}generated.add(x);}}}catch(SQLException e){throw new DatabaseException("Could not generate recommendations.",e);}recommendations.replaceActive(student.getUserId(),generated);return recommendations.findActive(student.getUserId());}
    public List<Recommendation> mine(){return recommendations.findActive(requireStudent().getUserId());}
    public void complete(long id){recommendations.markCompleted(id,requireStudent().getUserId());}
    private User requireStudent(){User u=SessionManager.currentUser().orElseThrow(()->new AuthorizationException("Please sign in to continue."));if(u.getRole()!=Role.STUDENT)throw new AuthorizationException("Only students can manage recommendations.");return u;}
}
