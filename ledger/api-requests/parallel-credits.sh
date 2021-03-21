#!/bin/bash

for i in $(seq 10) # perform the inner command 100 times.
do
  echo "user ${i}"
  for i in $(seq 100) # perform the inner command 100 times.
  do
    curl --silent -d "@credit.json" -H "Content-Type: application/json"  -X POST http://localhost:8080/accounts/123/credit > /dev/null &
  done
done

wait