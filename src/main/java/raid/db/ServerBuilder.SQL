DROP DATABASE file_system;
CREATE DATABASE file_system;
USE file_system;

CREATE TABLE system_user (
                             user_dni VARCHAR(9),
                             user_name VARCHAR(50),
                             user_password varchar(100)
);

ALTER TABLE system_user ADD CONSTRAINT pk_system_user primary key system_user(user_dni);

CREATE TABLE file_owner (
                            user_owner VARCHAR(9),
                            file_name VARCHAR(100)
);

ALTER TABLE file_owner ADD CONSTRAINT pk_file_owner PRIMARY KEY file_owner(user_owner, file_name);
ALTER TABLE file_owner ADD CONSTRAINT fk_user_owner_user_dni FOREIGN KEY (user_owner) REFERENCES system_user(user_dni);