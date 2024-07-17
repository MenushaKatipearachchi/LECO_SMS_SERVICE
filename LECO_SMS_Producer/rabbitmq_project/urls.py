from django.urls import path
from new_app.views import publish_message

urlpatterns = [
    path('smsapi/', publish_message),
]
