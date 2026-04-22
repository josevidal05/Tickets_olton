from django.db import models
from django.core.exceptions import ValidationError
from django.core.validators import FileExtensionValidator
import mimetypes

# Create your models here.

def validate_image_file(value):
    if not value:
        return
    mime_type, _ = mimetypes.guess_type(value.name)
    if not mime_type or not mime_type.startswith('image/'):
        raise ValidationError('Solo se permiten archivos de imagen.')

#class User(models.Model):

class Ticket(models.Model):

    empresa = models.CharField(max_length=150)
    contacto = models.CharField(max_length=100)

    TIPO_DISPOSITIVO_CHOICES = [
        ('maquina', 'Máquina'),
        ('tracker', 'Tracker'),
        ('otro', 'Otro'),
    ]

    tipo_dispositivo = models.CharField(
        max_length = 20,
        choices = TIPO_DISPOSITIVO_CHOICES
    )
    id_dispositivo = models.IntegerField()
    observaciones = models.TextField()
    archivo = models.FileField(upload_to='tickets/', blank=True, null=True, validators=[FileExtensionValidator(allowed_extensions=['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp']), validate_image_file])

    TIPO_PORTES_CHOICES = [
        ('pagado', 'Pagado'),
        ('debido', 'Debido'),
    ]

    portes = models.CharField(
        max_length = 20,
        choices = TIPO_PORTES_CHOICES
    )
    empresa_transporte = models.CharField(max_length=100)

    ESTADO_TICKET_CHOICES = [
        ('leido', 'Leido'),
        ('no leido', 'No leido'),
        ('abierto', 'Abierto'),
        ('cerrado', 'Cerrado'),
    ]

           
    fecha_creacion = models.DateTimeField(auto_now_add=True)