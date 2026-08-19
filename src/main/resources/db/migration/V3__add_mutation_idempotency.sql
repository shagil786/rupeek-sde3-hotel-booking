alter table payments add column idempotency_key varchar(200);
alter table bookings add column cancellation_idempotency_key varchar(200);
alter table payments add constraint uq_payment_idempotency unique (booking_id, idempotency_key);
