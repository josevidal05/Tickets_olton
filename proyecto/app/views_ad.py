import secrets
import bcrypt

from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
import json
from .models import Ticket, Usuario


def __get_request_user_ad(request):
    print(request)
    token = request.headers.get('Authorization') or request.META.get('HTTP_SESSION')

    if not token:
        return None
    try:
#        print(token)
#        usuario = Usuario.objects.all()

#        for u in usuario:
#            print(u.token_sesion)

        return Usuario.objects.get(token_sesion=token)
    
    except Usuario.DoesNotExist:
        return None


@csrf_exempt
def ticket_ad(request):
    if request.method == "POST":


        authenticated_user = __get_request_user_ad(request)

        if authenticated_user is None:
          return JsonResponse({"error": "Token inválido"}, status=401)

        try:
            data = json.loads(request.body)

            # Validar campos requeridos antes de crear el ticket
            required = ["tipo_dispositivo", "id_dispositivo", "observaciones", "portes", "transporte"]
            for field in required:
                val = data.get(field)
                if val is None or (isinstance(val, str) and val.strip() == ""):
                    return JsonResponse({"error": "Ningún campo puede estar vacío: %s" % field}, status=400)

            ticket = Ticket.objects.create(
                tipo_dispositivo=data.get("tipo_dispositivo"),
                id_dispositivo=data.get("id_dispositivo"),
                observaciones=data.get("observaciones"),
                portes=data.get("portes"),
                empresa_transporte=data.get("transporte"),
                archivo=data.get("archivo"),
                idUsuario=authenticated_user
            )

            return JsonResponse({"success": True, "id": ticket.id}, status=201)
        except Exception as e:
            return JsonResponse({"success": False, "error": str(e)}, status=400)

    elif request.method == 'GET':
        tickets = Ticket.objects.all().values()
        return JsonResponse(list(tickets), safe=False, status=200)

    else:
        return JsonResponse({"message": "Método no permitido"}, status=405)


@csrf_exempt
def registrar_usuario_ad(request):
    if request.method != 'POST':
        return JsonResponse({'error': 'HTTP method unsupported'}, status=405)

    try:
        body_json = json.loads(request.body)
    except ValueError:
        return JsonResponse({"error": "JSON inválido"}, status=400)

    username = body_json.get('username') or body_json.get('new_username')
    password = body_json.get('password')
    confirm_password = body_json.get('confirm_password')
    nombre = body_json.get('nombre') or body_json.get('name') or ''
    empresa = body_json.get('empresa') or body_json.get('company') or ''
    correo = body_json.get('correo') or body_json.get('email') or ''

    if username is None or password is None or correo is None:
        return JsonResponse({"error": "Faltan parámetros"}, status=400)

    if len(username) < 3:
        return JsonResponse({"error": "El nombre de usuario es demasiado corto"}, status=400)

    if len(password) < 8:
        return JsonResponse({"error": "La contraseña es demasiado corta"}, status=400)
    
    if password != confirm_password:
        return JsonResponse({"error": "Las contraseñas no coinciden"}, status=400)

    if Usuario.objects.filter(username=username).exists():
        return JsonResponse({"error": "El nombre de usuario ya existe"}, status=409)

    hashed_password = bcrypt.hashpw(password.encode('utf8'), bcrypt.gensalt()).decode('utf8')
    random_token = secrets.token_hex(16)

    user_object = Usuario.objects.create(
        username=username,
        password=hashed_password,
        nombre=nombre,
        empresa=empresa,
        correo=correo,
        token_sesion=random_token,
    )
    user_object.save()

    return JsonResponse({"success": True, "token": random_token}, status=201)


@csrf_exempt
def iniciar_sesion_ad(request):
    if request.method != 'POST':
        return JsonResponse({'error': 'HTTP method unsupported'}, status=405)

    try:
        body_json = json.loads(request.body)
    except ValueError:
        return JsonResponse({"error": "JSON inválido"}, status=400)

    username = body_json.get('username') or body_json.get('new_username')
    password = body_json.get('password')

    if not username or not password:
        return JsonResponse({"error": "Faltan parámetros"}, status=400)

    try:
        user = Usuario.objects.get(username=username)
        if bcrypt.checkpw(password.encode('utf8'), user.password.encode('utf8')):
            token = secrets.token_hex(16)
            user.token_sesion = token
            user.save()
            return JsonResponse({"success": True, "token": token}, status=200)
    except Usuario.DoesNotExist:
        pass

    return JsonResponse({"error": "Usuario o contraseña incorrectos"}, status=401)


@csrf_exempt
def logout_ad(request):
    if request.method != 'POST':
        return JsonResponse({'error': 'HTTP method unsupported'}, status=405)

    authenticated_user = __get_request_user_ad(request)
    if authenticated_user is None:
        return JsonResponse({"error": "Token inválido"}, status=401)

    authenticated_user.token_sesion = ""
    authenticated_user.save()
    return JsonResponse({"success": True}, status=200)


@csrf_exempt
def tickets_usuario_ad(request):
    if request.method != 'GET':
        return JsonResponse({'error': 'HTTP method unsupported'}, status=405)

    authenticated_user = __get_request_user_ad(request)
    if authenticated_user is None:
        return JsonResponse({"error": "Token inválido"}, status=401)

    tickets = Ticket.objects.filter(idUsuario=authenticated_user).values()
    return JsonResponse(list(tickets), safe=False, status=200)


@csrf_exempt
def ticket_id_ad(request, ticket_id):
    authenticated_user = __get_request_user_ad(request)
    if authenticated_user is None:
        return JsonResponse({"error": "Token inválido"}, status=401)

    try:
        ticket = Ticket.objects.get(id=ticket_id)

    except Ticket.DoesNotExist:
        return JsonResponse({"error": "El ticket no existe"}, status=404)


    if ticket.idUsuario_id != authenticated_user.id:
        print(ticket.idUsuario.id)
        return JsonResponse({"error": "No autorizado"}, status=403)


    if request.method == 'GET':
        return JsonResponse({
            "id": ticket.id,
            "tipo_dispositivo": ticket.tipo_dispositivo,
            "id_dispositivo": ticket.id_dispositivo,
            "observaciones": ticket.observaciones,
            "portes": ticket.portes,
            "empresa_transporte": ticket.empresa_transporte,
            "idUsuario": ticket.idUsuario_id,
            "fecha_creacion": ticket.fecha_creacion,
        }, status=200)

    elif request.method == 'DELETE':
        ticket.delete()
        return JsonResponse({"success": True}, status=200)

    elif request.method == 'PUT':
        try:
            data = json.loads(request.body)
        except ValueError:
            return JsonResponse({"error": "JSON inválido"}, status=400)

        ticket.tipo_dispositivo = data.get("tipo_dispositivo", ticket.tipo_dispositivo)
        ticket.id_dispositivo = data.get("id_dispositivo", ticket.id_dispositivo)
        ticket.observaciones = data.get("observaciones", ticket.observaciones)
        ticket.portes = data.get("portes", ticket.portes)
        ticket.empresa_transporte = data.get("empresa_transporte", ticket.empresa_transporte)

        if data.get("tipo_dispositivo") == "" or data.get("id_dispositivo") is None or data.get("observaciones") == "" or data.get("portes") == "" or data.get("empresa_transporte") == "":
            return JsonResponse({"error": "Ningún campo puede estar vacío"}, status=400)

        ticket.save()
        return JsonResponse({"success": True}, status=200)

    else:
        return JsonResponse({"message": "Método no permitido"}, status=405)
#$2b$12$siU8cNQt9vRKUJ9cpH0O0em39QAKnpFMUWH1AfhwIa2bqKoyujHZS
#$2b$12$siU8cNQt9vRKUJ9cpH0O0em39QAKnpFMUWH1AfhwIa2bqKoyujHZS

@csrf_exempt
def perfil_ad(request):
    authenticated_user = __get_request_user_ad(request)
    if authenticated_user is None:
        return JsonResponse({"error": "Token inválido"}, status=401)

    if request.method == 'GET':
        return JsonResponse({
            "username": authenticated_user.username,
            "password": authenticated_user.password,
            "nombre": authenticated_user.nombre,
            "empresa": authenticated_user.empresa,
            "correo": authenticated_user.correo,
        }, status=200)

    elif request.method == 'PUT':
        try:
            data = json.loads(request.body)
        except ValueError:
            return JsonResponse({"error": "JSON inválido"}, status=400)

        print("contraseña", data.get("password"),"hola")
        print("contraseña antigua", authenticated_user.password)
        # Actualizar contraseña solo si se envía y no está vacía
        new_password = data.get("password")
        hashed_password = None
        if new_password:
            hashed_password = bcrypt.hashpw(new_password.encode('utf8'), bcrypt.gensalt()).decode('utf8')
            authenticated_user.password = hashed_password

        authenticated_user.username = data.get("username", authenticated_user.username)
        authenticated_user.nombre = data.get("nombre", authenticated_user.nombre)
        authenticated_user.empresa = data.get("empresa", authenticated_user.empresa)
        authenticated_user.correo = data.get("correo", authenticated_user.correo)
        
        if hashed_password:
            print("contraseña nueva", hashed_password)
        

        authenticated_user.save()
        return JsonResponse({"success": True}, status=200)

    else:
        return JsonResponse({"message": "Método no permitido"}, status=405)