from .utils.logger_config import logger
from .db_connection import get_db_connection

schema_name = "SMS"

def get_all_msg_codes():
    try:
        connection = get_db_connection()
        cursor = connection.cursor()
        sql = f"SELECT * FROM {schema_name}.SMS_MSG_TYPE WHERE IS_ENABLED = 1"
        cursor.execute(sql)
        results = cursor.fetchall()
        cursor.close()
        connection.close()
        msg_codes = {}
        for result in results:
            msg_code = result[1]
            msg_codes[msg_code] = {
                'msg_code': msg_code,
                'call_type': result[2],
                'api': result[3],
                'priority': result[4],
                'message_template': result[6]
            }
        return msg_codes
    except Exception as e:
        logger.error('Failed to fetch all message codes', exc_info=True)
        return {}


