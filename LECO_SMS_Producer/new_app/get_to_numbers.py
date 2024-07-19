from .utils.logger_config import logger
from .db_connection import get_db_connection

schema_name = "ESMS"

def get_allowed_to_numbers():
    try:
        connection = get_db_connection()
        cursor = connection.cursor()
        sql = f"SELECT ID, TO_NUMBER FROM {schema_name}.ALLOWED_TO_NUMBERS"
        cursor.execute(sql)
        results = cursor.fetchall()
        cursor.close()
        connection.close()
        allowed_to_numbers = []
        for result in results:
            allowed_to_numbers.append({
                'id': result[0],
                'to_number': result[1]
            })
        return allowed_to_numbers
    except Exception as e:
        logger.error('Failed to fetch allowed to numbers', exc_info=True)
        return []
