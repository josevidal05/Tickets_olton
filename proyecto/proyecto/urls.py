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
from app import views
from app import views_ad
from django.conf import settings
from django.conf.urls.static import static

urlpatterns = [
    # urls para web
    path('', views.index),
    path('gestion/', views.comprobar_tickets),
    path('tickets_usuario/', views.tickets_usuario),
    path('datos_usuario/', views.datos_usuario),
    path('login/', views.login),
    path('iniciar_sesion/', views.iniciar_sesion, name='iniciar_sesion'),
    path('logout/', views.logout, name='logout'),
    path('admin/', admin.site.urls),
    path("perfil/", views.perfil),
    path("datos_usuario/cambiar_contraseña/", views.contraseña),
    path("perfil_usuario/editar_contraseña/", views.edit_password, name='edit_password'),


    path("tickets/", views.ticket_w),
    path("ticket/<int:ticket_id>/", views.ticket_id),
    path("tickets/<int:ticket_id>/pdf/", views.ticket_pdf),
    path("perfil_usuario/", views.perfil_usuario),
    path("registro/", views.registro),
    path("registrar_usuario/", views.registar_usuario_w, name='registrar_usuario_w'),
    #path("tickets_usuario/", views.tickets_user), # para ver todos los tickets del usuario, esta en json
    
    # urls para android
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
