-- AdaptIQ schema for MySQL 8.0+. Run with an account permitted to create databases.
CREATE DATABASE IF NOT EXISTS adaptiq_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE adaptiq_db;

CREATE TABLE users (
    user_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role ENUM('STUDENT','FACULTY','ADMIN') NOT NULL,
    status ENUM('ACTIVE','INACTIVE','PENDING') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE subjects (
    subject_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    subject_name VARCHAR(120) NOT NULL,
    description TEXT NULL,
    CONSTRAINT uq_subjects_name UNIQUE (subject_name)
) ENGINE=InnoDB;

CREATE TABLE topics (
    topic_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    subject_id BIGINT UNSIGNED NOT NULL,
    topic_name VARCHAR(120) NOT NULL,
    description TEXT NULL,
    CONSTRAINT uq_topics_subject_name UNIQUE (subject_id, topic_name),
    CONSTRAINT fk_topics_subject FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE questions (
    question_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    topic_id BIGINT UNSIGNED NOT NULL,
    question_text TEXT NOT NULL,
    difficulty ENUM('EASY','MEDIUM','HARD') NOT NULL,
    marks DECIMAL(8,2) NOT NULL,
    explanation TEXT NULL,
    created_by BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_questions_marks CHECK (marks > 0),
    CONSTRAINT fk_questions_topic FOREIGN KEY (topic_id) REFERENCES topics(topic_id) ON DELETE RESTRICT,
    CONSTRAINT fk_questions_creator FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE question_options (
    option_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT UNSIGNED NOT NULL,
    option_label CHAR(1) NOT NULL,
    option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_option_label CHECK (option_label IN ('A','B','C','D')),
    CONSTRAINT uq_question_option_label UNIQUE (question_id, option_label),
    CONSTRAINT fk_options_question FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE exams (
    exam_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    subject_id BIGINT UNSIGNED NOT NULL,
    faculty_id BIGINT UNSIGNED NOT NULL,
    duration_minutes SMALLINT UNSIGNED NOT NULL,
    total_marks DECIMAL(10,2) NOT NULL,
    exam_mode ENUM('NORMAL','ADAPTIVE') NOT NULL,
    status ENUM('DRAFT','PUBLISHED','CLOSED') NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_exams_duration CHECK (duration_minutes > 0),
    CONSTRAINT chk_exams_marks CHECK (total_marks >= 0),
    CONSTRAINT fk_exams_subject FOREIGN KEY (subject_id) REFERENCES subjects(subject_id) ON DELETE RESTRICT,
    CONSTRAINT fk_exams_faculty FOREIGN KEY (faculty_id) REFERENCES users(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE exam_questions (
    exam_id BIGINT UNSIGNED NOT NULL,
    question_id BIGINT UNSIGNED NOT NULL,
    question_order SMALLINT UNSIGNED NOT NULL,
    PRIMARY KEY (exam_id, question_id),
    CONSTRAINT uq_exam_question_order UNIQUE (exam_id, question_order),
    CONSTRAINT fk_exam_questions_exam FOREIGN KEY (exam_id) REFERENCES exams(exam_id) ON DELETE CASCADE,
    CONSTRAINT fk_exam_questions_question FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE exam_attempts (
    attempt_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT UNSIGNED NOT NULL,
    exam_id BIGINT UNSIGNED NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP NULL,
    score DECIMAL(10,2) NULL,
    percentage DECIMAL(5,2) NULL,
    accuracy DECIMAL(5,2) NULL,
    status ENUM('IN_PROGRESS','SUBMITTED','EXPIRED') NOT NULL DEFAULT 'IN_PROGRESS',
    CONSTRAINT chk_attempt_percentage CHECK (percentage IS NULL OR percentage BETWEEN 0 AND 100),
    CONSTRAINT chk_attempt_accuracy CHECK (accuracy IS NULL OR accuracy BETWEEN 0 AND 100),
    CONSTRAINT fk_attempts_student FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_attempts_exam FOREIGN KEY (exam_id) REFERENCES exams(exam_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE student_answers (
    answer_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    attempt_id BIGINT UNSIGNED NOT NULL,
    question_id BIGINT UNSIGNED NOT NULL,
    selected_option_id BIGINT UNSIGNED NULL,
    is_correct BOOLEAN NULL,
    marks_obtained DECIMAL(10,2) NOT NULL DEFAULT 0,
    answered_at TIMESTAMP NULL,
    CONSTRAINT uq_attempt_question UNIQUE (attempt_id, question_id),
    CONSTRAINT fk_answers_attempt FOREIGN KEY (attempt_id) REFERENCES exam_attempts(attempt_id) ON DELETE CASCADE,
    CONSTRAINT fk_answers_question FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE RESTRICT,
    CONSTRAINT fk_answers_option FOREIGN KEY (selected_option_id) REFERENCES question_options(option_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE topic_performance (
    performance_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT UNSIGNED NOT NULL,
    topic_id BIGINT UNSIGNED NOT NULL,
    total_attempted INT UNSIGNED NOT NULL DEFAULT 0,
    total_correct INT UNSIGNED NOT NULL DEFAULT 0,
    accuracy DECIMAL(5,2) NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_topic_performance UNIQUE (student_id, topic_id),
    CONSTRAINT chk_performance_counts CHECK (total_correct <= total_attempted),
    CONSTRAINT fk_performance_student FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_performance_topic FOREIGN KEY (topic_id) REFERENCES topics(topic_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE recommendations (
    recommendation_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT UNSIGNED NOT NULL,
    topic_id BIGINT UNSIGNED NOT NULL,
    recommendation_text TEXT NOT NULL,
    recommended_difficulty ENUM('EASY','MEDIUM','HARD') NOT NULL,
    recommended_question_count SMALLINT UNSIGNED NOT NULL DEFAULT 10,
    status ENUM('ACTIVE','COMPLETED','ARCHIVED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_recommendation_count CHECK (recommended_question_count > 0),
    CONSTRAINT fk_recommendations_student FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_recommendations_topic FOREIGN KEY (topic_id) REFERENCES topics(topic_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE adaptive_history (
    history_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    attempt_id BIGINT UNSIGNED NOT NULL,
    question_id BIGINT UNSIGNED NOT NULL,
    previous_difficulty ENUM('EASY','MEDIUM','HARD') NOT NULL,
    selected_difficulty ENUM('EASY','MEDIUM','HARD') NOT NULL,
    accuracy_at_decision DECIMAL(5,2) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_adaptive_attempt_question UNIQUE (attempt_id, question_id),
    CONSTRAINT fk_history_attempt FOREIGN KEY (attempt_id) REFERENCES exam_attempts(attempt_id) ON DELETE CASCADE,
    CONSTRAINT fk_history_question FOREIGN KEY (question_id) REFERENCES questions(question_id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE activity_logs (
    log_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NULL,
    activity VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_logs_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_topics_subject ON topics(subject_id);
CREATE INDEX idx_questions_topic_difficulty ON questions(topic_id, difficulty);
CREATE INDEX idx_questions_creator ON questions(created_by);
CREATE INDEX idx_exams_subject_status ON exams(subject_id, status);
CREATE INDEX idx_attempts_student_submitted ON exam_attempts(student_id, submitted_at);
CREATE INDEX idx_attempts_exam_status ON exam_attempts(exam_id, status);
CREATE INDEX idx_answers_attempt ON student_answers(attempt_id);
CREATE INDEX idx_logs_user_created ON activity_logs(user_id, created_at);
