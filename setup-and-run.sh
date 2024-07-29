#!/bin/bash

# Step 1: Run Keep-Alive Script
nohup ./keep-alive.sh &

# Step 2: Build and Run the Application
./mvnw spring-boot:run &

# Step 3: Monitor Logs
LOG_FILE="app.log"
if [ -f "$LOG_FILE" ]; then
  tail -f "$LOG_FILE"
else
  echo "$LOG_FILE not found. Monitoring default output..."
  tail -f nohup.out
fi
