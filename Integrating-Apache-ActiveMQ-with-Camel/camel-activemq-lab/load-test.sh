#!/bin/bash

echo "Starting Load Test for Camel ActiveMQ Integration"
echo "================================================"

send_batch_messages() {
    local batch_size=$1
    local queue_name=$2

    echo "Sending $batch_size messages to $queue_name..."

    for i in $(seq 1 $batch_size); do
        message="LOAD_TEST_ORDER_$i:Customer_$i|Product_$((i % 10))|Quantity_$((i % 5 + 1))"

        curl -s -u admin:admin \
             -d "body=$message" \
             -H "Content-Type: application/x-www-form-urlencoded" \
             "http://localhost:8161/api/message/$queue_name?type=queue" > /dev/null

        if [ $((i % 10)) -eq 0 ]; then
            echo "Sent $i messages..."
        fi
    done

    echo "Batch of $batch_size messages sent successfully!"
}

send_batch_messages 50 "orders.incoming"
sleep 5
send_batch_messages 25 "orders.test.error"

echo "Load test completed. Monitor ActiveMQ console for message processing."
