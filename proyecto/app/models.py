from django.db import models
from django.core.exceptions import ValidationError
from django.core.validators import FileExtensionValidator
from django.utils import timezone
import mimetypes

# Create your models here.
def validate_image_file(value):
    if not value:
        return
    mime_type, _ = mimetypes.guess_type(value.name)
    if not mime_type or not mime_type.startswith('image/'):
        import os
        ext = os.path.splitext(value.name)[1].lower()
        if ext in ['.jpg', '.jpeg', '.png', '.gif', '.bmp', '.webp']:
            return
        raise ValidationError('Solo se permiten archivos de imagen.')


class Empresa(models.Model):
    nombre = models.CharField(max_length=150, unique=True)
    encargado = models.ForeignKey('Usuario', on_delete=models.SET_NULL, related_name='empresas', null=True, blank=True)
    numero_tickets = models.IntegerField(default=0)
    correo = models.CharField(max_length=254, blank=True, null=True, unique=True)

    def __str__(self):
        return self.nombre


class Usuario(models.Model):
    username = models.CharField(max_length=50, unique=True, null=False)
    password = models.CharField(max_length=155)
    nombre = models.CharField(max_length=100)
    empresa = models.ForeignKey('Empresa', on_delete=models.CASCADE, related_name='usuarios')
    correo = models.EmailField(max_length=254, unique=True)
    token_sesion = models.CharField(max_length=150, null=True)
    token_sesion_expiracion = models.DateTimeField(null=True, blank=True)

    TIPO_USUARIO_CHOICES = [
        ('cliente', 'Cliente'),
        ('taller', 'Taller'),
        ('admin', 'Admin'),
    ]
    
    tipo_usuario = models.CharField(
        max_length=20,
        choices=TIPO_USUARIO_CHOICES,
        default = 'cliente'
    )

    def __str__(self):
        return self.username

    def is_session_token_valid(self):
        if not self.token_sesion or not self.token_sesion_expiracion:
            return False
        return timezone.now() <= self.token_sesion_expiracion

    def clear_session_token(self):
        self.token_sesion = ""
        self.token_sesion_expiracion = None
        self.save(update_fields=["token_sesion", "token_sesion_expiracion"])

class Dispositivo (models.Model):
    nombre = models.CharField(max_length=100)

    def __str__(self):
        return self.nombre
    
class Ticket(models.Model): 

    tipo_dispositivo = models.ForeignKey('Dispositivo', on_delete=models.CASCADE, related_name='dispositivo_tickets')
    id_dispositivo = models.IntegerField()
    observaciones = models.TextField()
    archivo = models.FileField(
        upload_to='tickets/',
        blank=True,
        null=True,
        validators=[
            FileExtensionValidator(allowed_extensions=['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp']),
            validate_image_file
        ]
    )

    TIPO_PORTES_CHOICES = [ 
        ('pagado', 'Pagado'),
        ('debido', 'Debido'),
    ]

    portes = models.CharField(
        max_length=20,
        choices=TIPO_PORTES_CHOICES
    )
    empresa_transporte = models.CharField(max_length=100)

    ESTADO_TICKET_CHOICES = [
        ('leido', 'Leido'),
        ('no leido', 'No leido'),
        ('abierto', 'Abierto'),
        ('cerrado', 'Cerrado'),
    ]

    estado = models.CharField(
        max_length=20,
        choices=ESTADO_TICKET_CHOICES,
        default='no leido'
    )

    idUsuario = models.ForeignKey('Usuario', on_delete=models.CASCADE, related_name='usuario_tickets')
    fecha_creacion = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return f"Ticket #{self.id} - {self.idUsuario.username}"
    

