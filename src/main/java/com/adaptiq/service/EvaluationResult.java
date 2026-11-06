package com.adaptiq.service;

import java.math.BigDecimal;

/** Immutable submission outcome returned after the transaction commits. */
public record EvaluationResult(long correct, long incorrect, long unanswered, long attempted,
                               BigDecimal totalMarks, BigDecimal obtainedMarks, BigDecimal percentage, BigDecimal accuracy) { }
