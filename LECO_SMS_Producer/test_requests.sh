#!/bin/bash

for i in {1..100}
do
   curl -X POST -H "Content-Type: application/json" -d '{
    "from": "0787165415",
    "to": "1910",
    "body": "ACB 0309937309"
}' http://127.0.0.1:8000/smsapi/
done
