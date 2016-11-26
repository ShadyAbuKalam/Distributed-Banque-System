create table InternalTransactions (
  send_user VARCHAR(30) not null PRIMARY KEY ,
  timestmap TIMESTAMP not null  PRIMARY KEY ,
  target_user VARCHAR(30) not null ,
  FOREIGN KEY  (send_user,timestmap) REFERENCES Transactions (user_name,timestmap) ON UPDATE CASCADE ON DELETE RESTRICT,
  FOREIGN KEY (target_user) REFERENCES Accounts (user_name) ON UPDATE CASCADE ON DELETE RESTRICT
);