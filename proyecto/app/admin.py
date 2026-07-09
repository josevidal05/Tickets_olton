from django.contrib import admin
from .models import Ticket, Usuario, Empresa, Dispositivo
# Register your models here.

admin.site.register(Ticket)
admin.site.register(Usuario)
admin.site.register(Empresa)
admin.site.register(Dispositivo)
