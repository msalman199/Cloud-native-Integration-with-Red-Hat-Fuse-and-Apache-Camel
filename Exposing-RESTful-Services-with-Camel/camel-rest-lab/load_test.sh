#!/bin/bash

echo "Starting load test..."

# Test GET requests
for i in {1..10}; do
    echo "GET Request $i"
    curl -s -X GET http://localhost:8080/api/users > /dev/null &
done

# Test POST requests
for i in {1..5}; do
    echo "POST Request $i"
    curl -s -X POST http://localhost:8080/api/users \
      -H "Content-Type: application/json" \
      -d "{\"name\":\"User$i\",\"email\":\"user$i@test.com\",\"age\":25}" > /dev/null &
done

wait
echo "Load test completed"
