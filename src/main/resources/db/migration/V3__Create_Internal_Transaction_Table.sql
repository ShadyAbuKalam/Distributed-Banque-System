create table InternalTransactions (
  send_user VARCHAR(30) not null  ,
  timestamp TIMESTAMP not null   ,
  target_user VARCHAR(30) not null ,
  PRIMARY KEY (send_user,timestamp),
  FOREIGN KEY  (send_user,timestamp) REFERENCES Transactions (user_name,timestamp) ON UPDATE CASCADE ON DELETE RESTRICT,
  FOREIGN KEY (target_user) REFERENCES Accounts (user_name) ON UPDATE CASCADE ON DELETE RESTRICT
);