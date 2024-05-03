from django.urls import path
from new_app.views import publish_to_rabbitmq

urlpatterns = [
    path('smsapi/', publish_to_rabbitmq),
]
