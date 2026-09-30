#!/bin/bash

echo "Testing Error Handling and Message Reliability"
echo "=============================================="

# Function to send test message
send_test_message() {
    local message="$1"
    local queue="$2"

    echo "Sending message '$message' to queue '$queue'"

    curl -s -u admin:admin \
         -d "body=$message" \
         -H "Content-Type: application/x-www-form-urlencoded" \
         "http://localhost:8161/api/message/$queue?type=queue"

    echo ""
}

# Test normal message processing
echo "1. Testing normal message processing..."
send_test_message "TEST_ORDER:Normal processing test" "orders.test.error"
sleep 2

# Test error scenario
echo "2. Testing error scenario (should go to DLQ after retries)..."
send_test_message "ERROR_ORDER:This will cause an error" "orders.test.error"
sleep 10

# Test multiple error messages
echo "3. Testing multiple error messages..."
for i in {1..3}; do
    send_test_message "ERROR_ORDER_$i:Batch error test $i" "orders.test.error"
    sleep 1
done

echo "Test completed. Check ActiveMQ console and application logs for DLQ entries."
