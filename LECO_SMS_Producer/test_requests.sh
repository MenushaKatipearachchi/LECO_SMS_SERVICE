#!/bin/bash

for i in {1..1000}
do
   if (( RANDOM % 2 )); then
      body="UTD 0301398603"
   else
      body="CTY"
   fi

   curl -X POST -H "Content-Type: application/json" -d '{
    "from": "94711572503",
    "to": "1910",
    "body": "'"$body"'"
}' http://10.10.150.43:8000/smsapi/
done
