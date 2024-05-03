import logging
from logging.handlers import TimedRotatingFileHandler

# Create a custom logger
logger = logging.getLogger(__name__)

# handlers
c_handler = logging.StreamHandler()
f_handler = TimedRotatingFileHandler('logs/sms.log', when="midnight", interval=1, backupCount=90)
c_handler.setLevel(logging.WARNING)
f_handler.setLevel(logging.WARNING)

# formatters in handlers
c_format = logging.Formatter('%(name)s - %(levelname)s - %(message)s')
f_format = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
c_handler.setFormatter(c_format)
f_handler.setFormatter(f_format)

# handlers in the logger
logger.addHandler(c_handler)
logger.addHandler(f_handler)
