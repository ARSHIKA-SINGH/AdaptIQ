-- Optional baseline catalogue. It deliberately contains no accounts or passwords.
USE adaptiq_db;
INSERT IGNORE INTO subjects (subject_name, description) VALUES
  ('Java Programming', 'Core Java concepts and object-oriented programming');

INSERT IGNORE INTO topics (subject_id, topic_name, description)
SELECT subject_id, 'Classes and Objects', 'Classes, constructors, and object state'
FROM subjects WHERE subject_name = 'Java Programming';
INSERT IGNORE INTO topics (subject_id, topic_name, description)
SELECT subject_id, 'Inheritance', 'Inheritance, polymorphism, and overriding'
FROM subjects WHERE subject_name = 'Java Programming';

-- Create a faculty user through the application/admin workflow before adding questions:
-- questions.created_by is intentionally protected by a foreign key.
