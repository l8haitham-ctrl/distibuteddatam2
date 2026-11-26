CREATE DATABASE IF NOT EXISTS analytics;
CREATE TABLE IF NOT EXISTS analytics.customer (
  customer_id UInt64, first_name String, last_name String,
  email Nullable(String), city LowCardinality(String), created_at DateTime
) ENGINE = MergeTree ORDER BY customer_id;
CREATE TABLE IF NOT EXISTS analytics.invoice (
  invoice_id UInt64, customer_id UInt64, invoice_date Date,
  amount Decimal(12,2), status LowCardinality(String)
) ENGINE = MergeTree PARTITION BY toYYYYMM(invoice_date)
ORDER BY (invoice_date, customer_id, invoice_id);
CREATE TABLE IF NOT EXISTS analytics.payment (
  payment_id UInt64, invoice_id UInt64, payment_date Date,
  amount Decimal(12,2), payment_method LowCardinality(String)
) ENGINE = MergeTree PARTITION BY toYYYYMM(payment_date)
ORDER BY (payment_date, invoice_id, payment_id);

-- Independent synthetic fixture. Existing tables are never truncated or reseeded.
INSERT INTO analytics.customer
SELECT number+1, concat('Client',toString(number+1)), 'Demo',
  if(number%10=0,NULL,concat('client',toString(number+1),'@example.test')),
  ['Casablanca','Rabat','Fes','Tanger'][number%4+1],
  toDateTime('2025-01-01 00:00:00')+toIntervalDay(number%365)
FROM numbers(1000) WHERE (SELECT count() FROM analytics.customer)=0;
INSERT INTO analytics.invoice
SELECT number+1, number%1000+1, toDate('2025-01-01')+toIntervalDay(number%365),
  toDecimal64((number%10000+100)/100,2), ['paid','pending','cancelled'][number%3+1]
FROM numbers(5000) WHERE (SELECT count() FROM analytics.invoice)=0;
INSERT INTO analytics.payment
SELECT number+1, number%5000+1, toDate('2025-01-02')+toIntervalDay(number%365),
  toDecimal64((number%5000+50)/100,2), ['card','transfer','cash'][number%3+1]
FROM numbers(8000) WHERE (SELECT count() FROM analytics.payment)=0;
