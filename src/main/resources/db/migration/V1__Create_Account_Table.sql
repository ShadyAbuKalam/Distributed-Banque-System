create table Accounts (
    user_name VARCHAR(30) not null PRIMARY KEY ,
    password varchar(100) not null ,
    balance int NOT NULL DEFAULT 0
);