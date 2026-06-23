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
from app import views, views_admin, views_ad
from django.conf import settings
from django.conf.urls.static import static

urlpatterns = [
    # urls para web
    path('login/', views.iniciar_sesion, name='iniciar_sesion'),
    path("registro/", views.registar_usuario, name='registrar_usuario'),
    path('logout/', views.logout, name='logout'),

    path('', views.crear_ticket),
    path('tickets_usuario/', views.tickets_usuario),
    path("ticket/<int:ticket_id>/", views.ticket_id),
    path("ticket/<int:ticket_id>/pdf/", views.ticket_pdf),
    
    path("perfil/", views.perfil),
    path('datos_usuario/', views.datos_usuario),
    path("cambiar_contraseña/", views.contraseña, name='edit_password'),
 
    path('todos_usuarios/', views.usuarios),
    path('todas_empresas/', views.empresas),
    path('usuario_id/<int:usuario_id>', views.usuario_id),

    #urls para encargados de empresa
    path("empresa/", views.empresa),
    path("tickets_empresa/", views.tickets_empresa),
    
    # urls para admin
    path('admin/', admin.site.urls),
    path('gestion/', views.comprobar_tickets),
    path('usuarios/', views.gestion_usuarios),
    path('empresas/', views.gestion_empresas),
    path('usuario/<int:usuario_id>', views.gestion_usuario_id),



    
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
