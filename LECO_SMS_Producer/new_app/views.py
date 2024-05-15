# channel.basicQos to control the rate of message consumption
# {'x-max-priority': 10} to prioritize the delivery of certain messages within each queue

from django.shortcuts import render
import json
import pika
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.views.decorators.http import require_POST
from django.core.exceptions import ValidationError

from .utils.logger_config import logger
from .get_message_codes import get_all_msg_codes

msg_codes = get_all_msg_codes()

def check_msg_code_exists(msg_code):
    global msg_codes
    try:
        if msg_code in msg_codes:
            return msg_codes[msg_code]
        else:
            # Refresh the msg_codes dictionary
            msg_codes = get_all_msg_codes()
            # Check again if the msg_code exists in the refreshed dictionary
            if msg_code in msg_codes:
                return msg_codes[msg_code]
            else:
                return None
    except Exception as e:
        logger.error('Failed to check message code exists', exc_info=True)
        return None

@csrf_exempt
@require_POST
def publish_to_rabbitmq(request):
    try:
        data = json.loads(request.body)
        from_number = data['from']
        to_number = data['to']
        body = data['body']
        
        if not from_number or not to_number:
            raise ValidationError('Invalid user data')

        connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))
        channel = connection.channel()

        channel.exchange_declare(exchange='sms_exchange', durable=True, exchange_type='topic')
        
        messageCode = body[:3]
        
        msg_code_exists = check_msg_code_exists(messageCode)

        routing_key = ''
        api = ''
        if msg_code_exists:
            priority = msg_code_exists['priority'].lower()
            api = msg_code_exists['api']
            call_type = msg_code_exists['call_type']
            message_template = msg_code_exists['message_template']
            if priority == 'l':
                routing_key = 'low_processing'
            elif priority == 'm':
                routing_key = 'medium_processing'
            elif priority == 'h':
                routing_key = 'high_processing'

            channel.queue_declare(queue=routing_key, durable=True)
            channel.queue_bind(exchange='sms_exchange', queue=routing_key, routing_key=routing_key)
            
            message = {
                'from_number': from_number,
                'to_number': to_number,
                'body': body,
                'api': api,
                'call_type': call_type,
                'message_template': message_template,
            }
            
            channel.basic_publish(
                exchange='sms_exchange',
                routing_key=routing_key,
                body=json.dumps(message),
                properties=pika.BasicProperties(
                    delivery_mode=2,
                ))

            print(f"MessageCode: {messageCode} was sent to the queue: {routing_key}")

            connection.close()
            logger.warning('Message published to RabbitMQ: %s, Queue: %s', message, routing_key)
            return JsonResponse({"status": "Message published to RabbitMQ"})
        else:
            customer_message = {'from_number': from_number, 'to_number': to_number, 'body': body}
            logger.warning(f"Customer's message: {customer_message}")
            logger.warning(f"Message code \"{messageCode}\" does not exist")
            return JsonResponse({"status": "Message code does not exist"}, status=400)

        
    except ValidationError as ve:
        logger.error('User validation failed', exc_info=True)
        return JsonResponse({"status": "User validation failed", "error": str(ve)}, status=400)
    except json.JSONDecodeError:
        logger.error('Invalid JSON format', exc_info=True)
        return JsonResponse({"status": "Invalid JSON format"}, status=400)
    except Exception as e:
        logger.error('Failed to publish message to RabbitMQ', exc_info=True)
        return JsonResponse({"status": "Failed to publish message to RabbitMQ", "error": str(e)}, status=500)

