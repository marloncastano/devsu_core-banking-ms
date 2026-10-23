-- Client-supplied key (Idempotency-Key header) so a retried request is never applied twice.
-- Nullable because the header is optional; PostgreSQL allows many NULLs in a unique column.
ALTER TABLE movement ADD COLUMN idempotency_key VARCHAR(100);
ALTER TABLE movement ADD CONSTRAINT uk_movement_idempotency_key UNIQUE (idempotency_key);
