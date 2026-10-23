-- Timestamp of the last customer event applied, used to ignore events that arrive out of order.
-- Nullable because rows may have been loaded manually before events existed.
ALTER TABLE customer_ref ADD COLUMN last_event_at TIMESTAMPTZ;
