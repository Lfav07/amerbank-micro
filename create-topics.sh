#!/bin/bash
set -e KAFKA_BROKER="${KAFKA_BROKER:-kafka:29092}"

echo "Waiting for Kafka broker at $KAFKA_BROKER..."
until kafka-topics --bootstrap-server "$KAFKA_BROKER" --list > /dev/null 2>&1; do
  echo "Kafka not ready yet, retrying in 2s..."
  sleep 2
done

echo "Kafka is ready! Creating topics..."

create_topic() {
  local topic=$1
  local partitions=${2:-1}
  local replication=${3:-1}
  local retention=${4:-"604800000"}

  kafka-topics --bootstrap-server "$KAFKA_BROKER" \
    --create --if-not-exists \
    --topic "$topic" \
    --partitions "$partitions" \
    --replication-factor "$replication" \
    --config retention.ms="$retention"

  echo "✔ Created $topic (partitions=$partitions, retention=${4:-7d})"
}

# ── Define your topics here ──────────────────────────────
create_topic "audit.transactions"         3 1 "86400000"
create_topic "audit.accounts"       3 1 "604800000"
create_topic "audit.auth"  2 1 "604800000"
create_topic "audit.customers"    1 1 "604800000"

create_topic "audit.transactions.dlq" 3 1 "604800000"
create_topic "audit.accounts.dlq"     3 1 "604800000"
create_topic "audit.auth.dlq"         2 1 "604800000"
create_topic "audit.customers.dlq"    1 1 "604800000"
# ────────────────────────────────────────────────────────

echo ""
echo "Topics available:"
kafka-topics --bootstrap-server "$KAFKA_BROKER" --list