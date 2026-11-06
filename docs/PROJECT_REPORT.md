# AdaptIQ: Adaptive Learning & Intelligent Examination Platform

## Abstract

AdaptIQ is a JavaFX desktop application intended to improve examination feedback through secure role-based workflows, objective evaluation, topic-level analysis, and transparent adaptive rules. It uses MySQL for persistent data, JDBC for controlled persistence, and FXML/CSS for the desktop interface.

## Introduction and problem statement

One examination difficulty cannot appropriately challenge every learner. In addition, a final percentage alone does not identify concepts requiring revision. AdaptIQ addresses these gaps by retaining answer history, deriving topic performance, and providing rule-based difficulty decisions and practice recommendations.

## Objectives

- Provide secure student, faculty, and administrator roles.
- Support validated questions, subjects, topics, and normal examinations.
- Evaluate submitted answers automatically and consistently.
- Track performance by topic and difficulty.
- Generate explainable adaptive decisions from recent answers.
- Present live analytics and actionable recommendations.

## Proposed architecture

The application uses layered MVC: FXML views invoke thin JavaFX controllers; controllers delegate to services; services enforce domain rules and authorization; DAOs execute prepared JDBC statements against MySQL. Long-running UI operations use JavaFX `Task` instances.

## Functional requirements

Implemented source includes BCrypt authentication, student registration, dashboards, subject/topic management, MCQ CRUD, normal examination creation/delivery, automatic evaluation, topic performance updates, analytics, recommendations, adaptive-engine logic, and PDF report services.

## Non-functional requirements

Security is based on BCrypt hashes, parameterized queries, role checks in services, and local database credentials excluded from Git. InnoDB foreign keys and transactions preserve answer/result consistency. The UI uses a responsive card/sidebar layout and avoids database operations on the JavaFX application thread.

## Database design

The schema defines users, subjects, topics, questions, question options, exams, exam-question mappings, attempts, answers, topic performance, recommendations, adaptive history, and activity logs. Unique email addresses, indexed lookup fields, foreign keys, and deletion restrictions are defined in `database/adaptiq_schema.sql`.

## Adaptive algorithm

An adaptive attempt starts at MEDIUM. From the latest five valid answers, accuracy of 80% or greater advances one difficulty level; 50% to 79.99% retains the current level; below 50% reduces one level. EASY and HARD are boundaries. The engine excludes prior questions and selects the nearest unused difficulty when the exact pool is unavailable. The resulting decision history is intended to make each adjustment auditable.

## Implementation notes

Normal examination submission is transactionally evaluated. Correct/incorrect/unanswered counts, marks, percentage, accuracy, and topic-performance aggregates are persisted. Dashboard and analytics values are queried from stored records rather than sample constants. Recommendations classify topic performance as revision/easy, reinforcement/medium, or advanced/hard practice.

## Testing and results

JUnit test sources have been written for important business rules, including authentication, authorization, question validation, and adaptive thresholds. FXML files passed XML validation. Maven was unavailable in the development environment, therefore no Maven build, JUnit execution, MySQL integration test, screenshot, or end-to-end result is claimed in this report.

## Limitations and future scope

Required next verification is a clean Maven/MySQL build and complete end-to-end role demonstration. The adaptive engine and history adapter require final UI/lifecycle integration for a complete adaptive-test screen. Future work may include configurable negative marking, stronger database migration tooling, richer faculty reports, and accessibility testing.

## Conclusion

AdaptIQ establishes a maintainable Java desktop foundation for adaptive assessment. Its layered design makes the security, evaluation, and adaptive rules visible and independently testable, while MySQL persistence supports evidence-based feedback rather than static demonstration data.
