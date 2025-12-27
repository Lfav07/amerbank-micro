
CREATE SEQUENCE users_id_seq
    START WITH 1
    INCREMENT BY 50;


CREATE TABLE users (
                       id BIGINT NOT NULL PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       active BOOLEAN NOT NULL DEFAULT TRUE
);


CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            roles VARCHAR(255) NOT NULL,

                            CONSTRAINT fk_user_roles_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
                                    ON DELETE CASCADE
);
