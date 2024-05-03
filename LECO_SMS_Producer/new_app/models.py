from django.db import models

# Create your models here.
class EsmsInbox1910AutoPush(models.Model):
    from_no = models.CharField(max_length=20, db_column='FROM_NO')
    to_no = models.CharField(max_length=20, db_column='TO_NO')
    message = models.TextField(db_column='MESSAGE')
    date_time = models.DateTimeField(db_column='DATE_TIME')

    def __str__(self):
        return f"From: {self.from_no}, To: {self.to_no}"