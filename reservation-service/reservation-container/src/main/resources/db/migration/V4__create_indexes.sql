CREATE INDEX IF NOT EXISTS idx_reservations_customer_id ON reservations (customer_id);
CREATE INDEX IF NOT EXISTS idx_reservations_status ON reservations (status);
