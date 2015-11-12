CREATE DATABASE pid_db
  WITH OWNER = {{ easy_pid_generator_db_username }}
       ENCODING = 'UTF8'
       CONNECTION LIMIT = -1;
