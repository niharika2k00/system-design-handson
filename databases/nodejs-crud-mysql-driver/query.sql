-- 🎯 queries required for setting up the database tables
SHOW DATABASES;

CREATE DATABASE student_course_db;
USE student_course_db;

SHOW TABLES;
SHOW COLUMNS FROM <table_name>;
DESCRIBE <table_name>;

CREATE TABLE instructors (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(100) NOT NULL UNIQUE,
  location VARCHAR(50)
);

CREATE TABLE courses (
  id INT AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  description VARCHAR(500),
  instructor_id INT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (instructor_id) REFERENCES instructors(id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE
);

CREATE TABLE students (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(100) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL
);

-- if students table already exists, just add the column:
ALTER TABLE students ADD COLUMN password VARCHAR(255) NOT NULL;

-- junction table | composite key, no separate id
CREATE TABLE enrollments (
  student_id INT,
  course_id INT,
  enrolled_date DATE DEFAULT (CURRENT_DATE),
  PRIMARY KEY (student_id, course_id),
  FOREIGN KEY (student_id) REFERENCES students(id),
  FOREIGN KEY (course_id) REFERENCES courses(id),
  UNIQUE (student_id, course_id)
);

INSERT INTO students (name, email)
VALUES
  ('Aarav Sharma',   'aarav@gmail.com'),
  ('Priya Patel',    'priya@gmail.com'),
  ('Rohit Verma',    'rohit@gmail.com');

INSERT INTO students
SET name  = 'Aarav', email = 'arav@gmail.com';

