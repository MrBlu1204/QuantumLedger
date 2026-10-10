CREATE TABLE users (
    user_id         BIGINT          NOT NULL AUTO_INCREMENT,
    title           VARCHAR(5),
    first_name      VARCHAR(50)     NOT NULL ,
    last_name       VARCHAR(50),
    full_name       VARCHAR(100)    NOT NULL ,
    email_id        VARCHAR(255)    NOT NULL ,
    country_code    VARCHAR(20)     NOT NULL DEFAULT 'India(+91)' ,
    phone           VARCHAR(15)     NOT NULL ,
    password_hash   VARCHAR(100)    NOT NULL ,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' ,
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0 ,
    create_time     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    update_time     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                    ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (user_id),
    CONSTRAINT unique_users_email UNIQUE (email_id),
    CONSTRAINT unique_users_phone UNIQUE (country_code,phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;