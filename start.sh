#!/bin/bash
while true; do
    java -jar your-app.jar
    echo "Application crashed with exit code $?. Restarting..." >&2
    sleep 1
done
