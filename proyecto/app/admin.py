from django.contrib import admin
from .models import Ticket, User, Empresa
# Register your models here.

admin.site.register(Ticket)
admin.site.register(User)
admin.site.register(Empresa)
