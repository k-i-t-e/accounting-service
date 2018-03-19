# public schema

# --- !Ups
ALTER TABLE account ADD CONSTRAINT balance_positive CHECK (balance >= 0);
-- It's impossible to correctly update these fields in a concurrent environment without creating cumbersome locking
-- logic, so we'd better remove them. (At least, it is not possible in H2, but might be possible in PostgreSQL)
ALTER TABLE operation DROP COLUMN from_balance;
ALTER TABLE operation DROP COLUMN to_balance;
-- Instead, we'll add a column, containing account's initial balance, so that in future it would be possible to restore
-- the whole monetary transaction history if needed
ALTER TABLE account ADD COLUMN initial_balance DOUBLE NOT NULL;

# --- !Downs
ALTER TABLE account DROP CONSTRAINT balance_positive;
ALTER TABLE operation ADD COLUMN from_balance DOUBLE;
ALTER TABLE operation ADD COLUMN to_balance DOUBLE;
ALTER TABLE account DROP COLUMN initial_balance;