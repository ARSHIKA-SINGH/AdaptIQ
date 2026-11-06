package com.adaptiq.model;

import java.math.BigDecimal;
import java.util.List;

/** Student-safe question projection. Correctness and explanations are intentionally absent. */
public record ExamQuestionView(long questionId, String questionText, List<OptionView> options, BigDecimal marks) {
    public record OptionView(long optionId, String optionLabel, String optionText) { }
}
