package com.adaptiq.service;

import com.adaptiq.dao.AnalyticsDAO;
import com.adaptiq.exception.AuthorizationException;
import com.adaptiq.model.Role;
import com.adaptiq.model.User;
import com.adaptiq.util.SessionManager;

public final class AnalyticsService {
    private final AnalyticsDAO analytics=new AnalyticsDAO();
    public AnalyticsSnapshot myAnalytics(){User user=requireStudent();return analytics.loadStudent(user.getUserId());}
    private User requireStudent(){User user=SessionManager.currentUser().orElseThrow(()->new AuthorizationException("Please sign in to continue."));if(user.getRole()!=Role.STUDENT)throw new AuthorizationException("Only students can access personal analytics.");return user;}
}
