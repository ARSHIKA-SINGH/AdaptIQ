package com.adaptiq.model.dashboard;

import java.math.BigDecimal;
import java.time.Instant;

public record AttemptSummary(String examTitle, Instant submittedAt, BigDecimal percentage) { }
