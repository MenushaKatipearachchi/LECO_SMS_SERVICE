from django.core.management.base import BaseCommand
from tasks import consume_from_rabbitmq

class Command(BaseCommand):
    help = 'Starts the RabbitMQ consumer'

    def handle(self, *args, **options):
        self.stdout.write('Starting RabbitMQ consumer...')
        consume_from_rabbitmq()
