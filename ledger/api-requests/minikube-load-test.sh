red=$(tput setaf 1)
green=$(tput setaf 2)
reset=$(tput sgr0)

KUBE_IP=$(minikube ip)

accountId=$RANDOM

createAccountBody="{\"id\": \"${accountId}\"}"
echo $createAccountBody

# create the 123 account
curl -vvv -X POST -H "Content-Type: application/json" -d "$createAccountBody" http://"$KUBE_IP"/api/accounts > /dev/null
wait

# 100 requests to the credit into a random account number
ab -v -w -l -p credit.json -T application/json -c 4 -n 100 "http://"$KUBE_IP"/api/accounts/${accountId}/credit"

wait

# Make sure the 123 account balance is 100 * 50 = 5000 using the get balance endpoint
balance=$(curl --silent  http://"$KUBE_IP"/api/accounts/${accountId}/balance | jq '.amount')
test "${balance}" -eq 5000 && echo "${green}API load test passed${reset}" || echo "${red}API load test failed - expected balance is 50000 but found was ${balance}${reset}"