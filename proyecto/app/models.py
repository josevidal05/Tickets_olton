from django.db import models

# Create your models here.

class Ticket(models.Model):

    nombre = models.CharField(max_length=100)
    nombre_empresa = models.CharField(max_length=150)

    TIPO_DISPOSITIVO_CHOICES = [
        ('maquina', 'Máquina'),
        ('tracker', 'Tracker'),
        ('otro', 'Otro'),
    ]

    tipo_dispositivo = models.CharField(
        max_length=20,
        choices=TIPO_DISPOSITIVO_CHOICES
    )
    id_dispositivo = models.CharField(max_length=100)
    duda = models.TextField()
    archivo = models.FileField(upload_to='tickets/', blank=True, null=True)
    fecha_creacion = models.DateTimeField(auto_now_add=True)