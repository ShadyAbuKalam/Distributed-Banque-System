CREATE TABLE Banks (
  name VARCHAR(10) PRIMARY KEY ,
  auth_token VARCHAR(100) NOT NULL
);
create table ExternalTransaction (
  internal_user VARCHAR(30) not null PRIMARY KEY ,
  timestmap TIMESTAMP not null  PRIMARY KEY ,
  external_user VARCHAR(30) not null ,
  external_bank VARCHAR(30) not null ,
  FOREIGN KEY  (internal_user,timestmap) REFERENCES Transactions (user_name,timestmap) ON UPDATE CASCADE ON DELETE RESTRICT,
  FOREIGN KEY (external_bank) REFERENCES Banks (name) ON UPDATE CASCADE ON DELETE RESTRICT
);