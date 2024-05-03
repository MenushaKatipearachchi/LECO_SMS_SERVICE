import oracledb
import os

oracledb.init_oracle_client(lib_dir=r"C:\instantclient-basic\instantclient_21_12")

# Database connection information
dsn = os.getenv("DB_DSN")
username = os.getenv("DB_USERNAME")
password = os.getenv("DB_PASSWORD")

# Function to establish database connection
def get_db_connection():
    connection = oracledb.connect(
        user=username,
        password=password,
        dsn=dsn)
    return connection
