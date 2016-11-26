create table Transactions (
  user_name VARCHAR(30) not null PRIMARY KEY ,
  timestmap TIMESTAMP not null DEFAULT current_timestamp PRIMARY KEY ,
  amount int NOT NULL,
  type VARCHAR(10) NOT NULL ,
  FOREIGN KEY  (user_name) REFERENCES Accounts (user_name) ON UPDATE CASCADE ON DELETE RESTRICT
);