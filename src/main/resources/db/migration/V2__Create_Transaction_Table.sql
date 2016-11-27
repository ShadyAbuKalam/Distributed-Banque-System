create table Transactions (
  user_name VARCHAR(30) not null   ,
  timestmap TIMESTAMP not null DEFAULT current_timestamp  ,
  amount int NOT NULL,
  type VARCHAR(10) NOT NULL ,
  PRIMARY KEY (user_name,timestmap),
  FOREIGN KEY  (user_name) REFERENCES Accounts (user_name) ON UPDATE CASCADE ON DELETE RESTRICT
);