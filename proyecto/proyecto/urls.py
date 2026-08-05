"""
URL configuration for proyecto project.

The `urlpatterns` list routes URLs to views. For more information please see:
    https://docs.djangoproject.com/en/6.0/topics/http/urls/
Examples:
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
"""
from django.contrib import admin
from django.urls import path
from app import views, views_ad
from django.conf import settings
from django.conf.urls.static import static

urlpatterns = [
    # URLS PARA WEB

    # urls de sesiones
    path('login/', views.iniciar_sesion, name='iniciar_sesion'),
    path("registro/", views.registar_usuario, name='registrar_usuario'),
    path('logout/', views.logout, name='logout'),

    # urls de perfil
    path("perfil/", views.perfil),
    path('datos_usuario/', views.datos_usuario),
    path("cambiar_contraseña/", views.contraseña, name='edit_password'),

    # urls de tickets
    path('crear_ticket/', views.crear_ticket, name='crear_ticket'),
    path('mis_tickets/', views.tickets_usuario),
    path("ticket/<int:ticket_id>/", views.ticket_id),
    path("ticket/<int:ticket_id>/pdf/", views.ticket_pdf),
    path('gestion_tickets/', views.gestion_tickets),

    # urls de empresa
    path("datos_empresa/", views.datos_empresa),
    path("tickets_empresa/", views.tickets_empresa),
    path('usuarios_empresa/', views.usuarios_empresa),
    path('gestion_empresas/', views.gestion_empresas),
    path('datos_empresa/<int:empresa_id>/', views.empresa_id),
    path('crear_empresa/', views.crear_empresa, name='crear_empresa'),

    # urls de usuarios
    path('gestion_usuarios/', views.gestion_usuarios),
    path('crear_usuario/', views.crear_usuario_admin, name='registrar_usuario_admin'),
    path('usuario/<int:usuario_id>/', views.usuario_id),
    path('cambiar_contraseña/<int:usuario_id>/', views.contraseña_admin),

    # urls de dispositivos
    path('gestion_dispositivos/', views.gestion_dispositivos),
    path('crear_dispositivo/', views.crear_dispositivo, name='crear_dispositivo'),
    path('datos_dispositivo/<int:dispositivo_id>/', views.dispositivo_id),

    # urls para encargado
    path('menu_encargado/', views.encargado),

    # urls para taller
    path('menu_taller/', views.taller),
    path('tickets_asignados/', views.tickets_asignados),
    
    # urls para admin
    path('admin/', admin.site.urls),
    path('menu_administrador/', views.administrador), # menu administrador

    ############################
    # metodos para android
    path("android/registrar_usuario/", views_ad.registrar_usuario_ad),
    path("android/login/", views_ad.iniciar_sesion_ad),
    path("android/logout/", views_ad.logout_ad),
    path("android/tickets/", views_ad.ticket_ad),
    path("android/tickets/<int:ticket_id>/", views_ad.ticket_id_ad),
    path("android/tickets_usuario/", views_ad.tickets_usuario_ad),
    path("android/perfil/", views_ad.perfil_ad),
]


if settings.DEBUG:
    urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
