# public schema

# --- !Ups
-- Adding a couple of fields to record the state of accounts after transaction, for the usage in history
ALTER TABLE operation ADD COLUMN from_balance DOUBLE;
ALTER TABLE operation ADD COLUMN to_balance DOUBLE;

# --- !Downs

ALTER TABLE operation DROP COLUMN from_balance;
ALTER TABLE operation DROP COLUMN to_balance;