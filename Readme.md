### Producer

To set up the project and run it locally, follow these steps:

```bash
# Set up a Python virtual environment
py -3 -m venv .venv

# Activate the virtual environment
.venv\scripts\activate

# Upgrade pip to the latest version
python -m pip install --upgrade pip

# Install Django
python -m pip install django

# Install pika (RabbitMQ client library)
python -m pip install pika

# Install python-dotenv (for managing environment variables)
python -m pip install python-dotenv

# Install Oracle Database driver (replace `oracledb` with the appropriate package name if using a different driver)
python -m pip install oracledb

# Run the Django development server
python manage.py runserver
```

Once the server is running, you can access the producer endpoint at [https:////127.0.0.1:8000/smsapi/](https:////127.0.0.1:8000/smsapi/). Use a POST request with the following JSON payload to produce a message:

```json
{
    "from": "94711306818",
    "to": "1910",
    "body": "UTD 0301398603"
}
```

Replace `"body"`, `"from"` with the desired message content.

The `"body"` can be,
* `UTD 0301398603` (replace the account number), 
* `LLL 0301398603 E` (replace the account number and E/S/T -> E-English, S-Sinhala, T-Tamil)
* `CTY`
