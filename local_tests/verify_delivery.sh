#!/usr/bin/env bash
set -e

echo "=== Running Local Delivery Service Tests ==="
cd "$(dirname "$0")/../services/delivery-service"

# Run only the specific test class
../../gradlew test --tests "com.finditnow.deliveryservice.service.DeliveryServiceTest"

echo "=== Tests Completed Successfully ==="
