#!/usr/bin/env bash
# Repeatedly hits a normal endpoint and logs timestamp + status code,
# so we can see when the server starts rejecting/recovering during
# a slowloris.py run.
for i in $(seq 1 "${1:-40}"); do
  ts=$(date +%H:%M:%S)
  code=$(curl -s -o /dev/null -w "%{http_code}" -m 2 http://localhost:18080/hello)
  echo "$ts  status=$code"
  sleep 1
done
