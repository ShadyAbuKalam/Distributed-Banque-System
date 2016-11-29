CREATE TABLE Banks (
  name VARCHAR(10) PRIMARY KEY ,
  auth_token VARCHAR(100) NOT NULL
);
create table ExternalTransactions (
  internal_user VARCHAR(30) not null  ,
  timestamp TIMESTAMP not null   ,
  external_user VARCHAR(30) not null ,
  external_bank VARCHAR(30) not null ,
  PRIMARY KEY (internal_user,timestamp),
  FOREIGN KEY  (internal_user,timestamp) REFERENCES Transactions (user_name,timestamp) ON UPDATE CASCADE ON DELETE RESTRICT,
  FOREIGN KEY (external_bank) REFERENCES Banks (name) ON UPDATE CASCADE ON DELETE RESTRICT
);