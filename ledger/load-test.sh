# create the 123 account
curl -X POST -H "Content-Type: application/json" -d '{"id": "123"}' http://localhost:8080/accounts

# 1000 request to the deposit in the 123 account
ab -p api-requests/deposit.json -T application/json -c 10 -n 1000 http://localhost:8080/accounts/123/deposit

# Make sure the 123 account balance is 1000 * 50 = 50000 using the get balance endpoint
test $(curl --silent  http://localhost:8080/accounts/123/balance | jq '.balance') -eq 50000 && echo "API load test passed" || echo "API load test failed"