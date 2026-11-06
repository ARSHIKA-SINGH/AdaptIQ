package com.adaptiq.model.dashboard;

import java.math.BigDecimal;

public record FacultyDashboardData(long totalExams, long publishedExams, long totalAttempts,
                                   BigDecimal averagePercentage) { }
