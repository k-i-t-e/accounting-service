# public schema

# --- !Ups

CREATE SEQUENCE s_account START WITH 1;
CREATE SEQUENCE s_operation START WITH 1;

CREATE TABLE IF NOT EXISTS account (
  id BIGINT DEFAULT nextval('s_account') PRIMARY KEY,
  owner VARCHAR(512) NOT NULL,
  balance DOUBLE NOT NULL,
  created_date TIMESTAMP NOT NULL
);

CREATE INDEX account_owner_idx ON account(owner);

-- 'Transaction' is a SQL key word, it's better not to use it
CREATE TABLE IF NOT EXISTS operation (
  id BIGINT DEFAULT nextval('s_operation') PRIMARY KEY,
  from_id BIGINT REFERENCES account(id),
  to_id BIGINT REFERENCES account(id),
  created_date TIMESTAMP NOT NULL,
  amount DOUBLE NOT NULL
);

CREATE INDEX operation_from_idx ON operation(from_id);
CREATE INDEX operation_to_idx ON operation(to_id);

# --- !Downs

DROP TABLE IF EXISTS operation;
DROP TABLE IF EXISTS account;
DROP SEQUENCE s_account;
DROP SEQUENCE s_operation;