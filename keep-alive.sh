# keep-alive.sh
while true; do
  curl -m 5 http://localhost:8080/actuator/health
  sleep 60
done
