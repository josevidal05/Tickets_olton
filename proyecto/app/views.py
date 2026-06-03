import secrets
import bcrypt

from django.http import JsonResponse, HttpResponseRedirect
from django.views.decorators.csrf import csrf_exempt
from django.shortcuts import render
import json
from .models import Ticket, Usuario, Empresa

def index(request):
    # Verificar si el usuario está autenticado
    authenticated_user = __get_request_user(request)
    if authenticated_user is None:
        return HttpResponseRedirect('/login/')
    return render(request, 'index.html', {'user': authenticated_user})

def comprobar_tickets(request):
    # Verificar si el usuario está autenticado
    authenticated_user = __get_request_user(request)
    if authenticated_user is None:
        return HttpResponseRedirect('/login/')
    return render(request, 'comprobar_tickets.html')

def tickets_usuario(request):
    # Verificar si el usuario está autenticado
    authenticated_user = __get_request_user(request)
    if authenticated_user is None:
        return HttpResponseRedirect('/login/')

    tickets = Ticket.objects.filter(idUsuario=authenticated_user)
    return render(request, 'tickets_usuario.html', {
        'user': authenticated_user,
        'tickets': tickets
    })

def login(request):
    # Si el usuario ya está logueado, redirigir a perfil
    authenticated_user = __get_request_user(request)
    if authenticated_user is not None:
        return HttpResponseRedirect('/perfil/')
    return render(request, 'login.html')


def perfil(request):
    # Verificar si el usuario está autenticado
    authenticated_user = __get_request_user(request)
    if authenticated_user is None:
        return HttpResponseRedirect('/login/')
    
    # Obtener los tickets del usuario usando la FK idUsuario
    tickets = Ticket.objects.filter(idUsuario=authenticated_user)
    
    return render(request, 'perfil.html', {
        'user': authenticated_user,
        'tickets': tickets
    })

def registro(request):
    return render(request, 'registro.html')

def __get_request_user(request):
    # Primero intentar obtener el token del header (para APIs/Android)
    header_token = request.headers.get('Session', None)
    if header_token is not None:
        try:
            return Usuario.objects.get(token_sesion=header_token)
        except Usuario.DoesNotExist:
            pass
    
    # Luego intentar obtener el token de la sesión de Django (para web)
    session_token = request.session.get('session_token', None)
    if session_token is not None:
        try:
            return Usuario.objects.get(token_sesion=session_token)
        except Usuario.DoesNotExist:
            pass
    
    return None


# Crear ticket para web
@csrf_exempt
def ticket_w(request):

    if request.method == "POST":

        authenticated_user = __get_request_user(request)

        if authenticated_user is None:
            return JsonResponse({"error": "El token de sesión no se ha enviado o no es válido"}, status=401)

        try:
            ticket = Ticket.objects.create(
                idUsuario=authenticated_user,
                tipo_dispositivo=request.POST.get("tipo_dispositivo"),
                id_dispositivo=request.POST.get("id_dispositivo"),
                observaciones=request.POST.get("observaciones"),
                portes=request.POST.get("portes"),
                empresa_transporte=request.POST.get("transporte"),
                archivo=request.FILES.get("archivo")
            )

            return JsonResponse({
                "estado": "Ticket creado correctamente",
                "id": ticket.id
            }, status=201)
 
        except Exception as e:
            return JsonResponse({
                "success": False,
                "error": str(e)
            }, status=400)

    else:
        return JsonResponse({"message": "Método no permitido"}, status=405)



# Registrar usuario (web)
def registar_usuario_w(request):
    if request.method == "POST":
        username = request.POST.get("username")
        password = request.POST.get("password")
        confirm_password = request.POST.get("confirm_password")
        nombre = request.POST.get("nombre")
        empresa_nombre = request.POST.get("empresa")
        correo = request.POST.get("correo")

        if password != confirm_password:
            return render(request, 'registro.html', {
                'error': 'Las contraseñas no coinciden'
            })

        try:
            # Verificar si el usuario ya existe
            if Usuario.objects.filter(username=username).exists():
                return render(request, 'registro.html', {
                    'error': 'El nombre de usuario ya existe'
                })

            if Usuario.objects.filter(correo=correo).exists():
                return render(request, 'registro.html', {
                    'error': 'El correo electrónico ya está en uso'
                })

            # Obtener o crear la empresa
            empresa_obj, _ = Empresa.objects.get_or_create(
                nombre=empresa_nombre,
                defaults={'encargado': username}
            )

            hashed_password = bcrypt.hashpw(password.encode('utf8'), bcrypt.gensalt()).decode('utf8')
            random_token = secrets.token_hex(16)

            user = Usuario.objects.create(
                username=username,
                password=hashed_password,
                nombre=nombre,
                empresa=empresa_obj,
                correo=correo,
                token_sesion=random_token,
            )

            user.save()

            return HttpResponseRedirect('/login/')
        except Exception as e:
            return render(request, 'registro.html', {
                'error': str(e)
            })

    else:
        return render(request, 'registro.html')



# Iniciar sesión
def iniciar_sesion(request):
    if request.method == "POST":
        username = request.POST.get("username")
        password = request.POST.get("password")

        try:
            user = Usuario.objects.get(username=username)
            # Verificar contraseña con bcrypt
            if bcrypt.checkpw(password.encode('utf8'), user.password.encode('utf8')):
                # Crear un nuevo token de sesión
                new_token = secrets.token_hex(16)
                user.token_sesion = new_token
                user.save()
                
                # Guardar el token en la sesión de Django
                request.session['session_token'] = new_token
                
                return HttpResponseRedirect('/perfil/')
            
            else:
                return render(request, 'login.html', {
                    'error': 'Usuario o contraseña incorrectos'
                })
            
        except Usuario.DoesNotExist:
            return render(request, 'login.html', {
                'error': 'Usuario o contraseña incorrectos'
            })

    else:
        return render(request, 'login.html', {
            'error': 'Método no permitido'
        }, status=405)


# Cerrar sesión
def logout(request):
    if request.method == 'GET':
        authenticated_user = __get_request_user(request)
        
        if authenticated_user is not None:
            # Eliminar el token de sesión
            authenticated_user.token_sesion = ""
            authenticated_user.save()
        
        # Limpiar la sesión de Django
        request.session.flush()
        
        return HttpResponseRedirect('/login/')
    else:
        return JsonResponse({"message": "Método no permitido"}, status=405)


# Tickets de cada usuario
def tickets_user(request):
    if request.method == 'GET':

        authenticated_user = __get_request_user(request)

        if authenticated_user is None:
            return JsonResponse({"error": "El token de sesión no se ha enviado o no es válido"}, status=401)

        tickets = Ticket.objects.filter(idUsuario=authenticated_user).values()
        return JsonResponse(list(tickets), safe=False, status=200)

    else:
        return JsonResponse({"message": "Método no permitido"}, status=405)


# Tickets por id (para poder modificarlos si es necesario)
def ticket_id(request, ticket_id):
    authenticated_user = __get_request_user(request)
    if authenticated_user is None:
        if request.method == 'GET':
            return HttpResponseRedirect('/login/')
        else:
            return JsonResponse({"error": "El token de sesión no se ha enviado o no es válido"}, status=401)

    try:
        ticket = Ticket.objects.get(id=ticket_id)
        # Verificar que el ticket pertenece al usuario
        if ticket.idUsuario != authenticated_user:
            if request.method == 'GET':
                return render(request, 'ticket.html', {'error': 'No tienes permiso para ver este ticket'})
            else:
                return JsonResponse({"error": "No tienes permiso para acceder a este ticket"}, status=403)
    except Ticket.DoesNotExist:
        if request.method == 'GET':
            return render(request, 'ticket.html', {'error': 'El ticket no existe'})
        else:
            return JsonResponse({"error": "El ticket no existe"}, status=404)

    if request.method == 'GET':
        return render(request, 'ticket.html', {'ticket': ticket})

    elif request.method == 'DELETE':
        ticket.delete()
        return JsonResponse({"success": True}, status=200)

    elif request.method == 'POST':
        ticket.tipo_dispositivo = request.POST.get("tipo_dispositivo", ticket.tipo_dispositivo)
        ticket.id_dispositivo = request.POST.get("id_dispositivo", ticket.id_dispositivo)
        ticket.observaciones = request.POST.get("observaciones", ticket.observaciones)
        ticket.portes = request.POST.get("portes", ticket.portes)
        ticket.empresa_transporte = request.POST.get("empresa_transporte", ticket.empresa_transporte)
        uploaded_file = request.FILES.get("archivo")
        if uploaded_file:
            ticket.archivo = uploaded_file
        ticket.save()
        return JsonResponse({"success": True}, status=200)

    elif request.method == 'PUT':
        data = json.loads(request.body)
        ticket.tipo_dispositivo = data.get("tipo_dispositivo", ticket.tipo_dispositivo)
        ticket.id_dispositivo = data.get("id_dispositivo", ticket.id_dispositivo)
        ticket.observaciones = data.get("observaciones", ticket.observaciones)
        ticket.portes = data.get("portes", ticket.portes)
        ticket.empresa_transporte = data.get("empresa_transporte", ticket.empresa_transporte)
        ticket.save()
        
        return JsonResponse({"success": True}, status=200)

    else:
        return JsonResponse({"message": "Método no permitido"}, status=405)