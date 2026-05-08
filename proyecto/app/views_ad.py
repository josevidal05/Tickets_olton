import secrets
import bcrypt

from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
import json
from .models import Ticket, Usuario


def __get_request_user_ad(request):
    token = request.headers.get('Session') or request.META.get('HTTP_SESSION')
    if not token:
        return None
    try:
        return Usuario.objects.get(token_sesion=token)
    except Usuario.DoesNotExist:
        return None


@csrf_exempt
def ticket_ad(request):
    if request.method == "POST":
        authenticated_user = __get_request_user_ad(request)
        if authenticated_user is None:
            return JsonResponse({"error": "El token de sesión no es válido"}, status=401)

        try:
            ticket = Ticket.objects.create(
                empresa=request.POST.get("empresa"),
                contacto=authenticated_user.username,
                tipo_dispositivo=request.POST.get("tipo_dispositivo"),
                id_dispositivo=request.POST.get("id_dispositivo"),
                observaciones=request.POST.get("observaciones"),
                portes=request.POST.get("portes"),
                empresa_transporte=request.POST.get("transporte"),
                archivo=request.FILES.get("archivo")
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
    nombre = body_json.get('nombre') or body_json.get('name') or ''
    empresa = body_json.get('empresa') or body_json.get('company') or ''

    if username is None or password is None:
        return JsonResponse({"error": "Faltan parámetros"}, status=400)

    if len(username) < 3:
        return JsonResponse({"error": "Username too short"}, status=400)

    if len(password) < 8:
        return JsonResponse({"error": "Password too short"}, status=400)

    if Usuario.objects.filter(username=username).exists():
        return JsonResponse({"error": "Username already exists"}, status=409)

    hashed_password = bcrypt.hashpw(password.encode('utf8'), bcrypt.gensalt()).decode('utf8')
    random_token = secrets.token_hex(16)

    user_object = Usuario.objects.create(
        username=username,
        password=hashed_password,
        nombre=nombre,
        empresa=empresa,
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

    tickets = Ticket.objects.filter(contacto=authenticated_user.username, empresa=authenticated_user.empresa).values()
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

    if ticket.contacto != authenticated_user.username or ticket.empresa != authenticated_user.empresa:
        return JsonResponse({"error": "No autorizado"}, status=403)

    if request.method == 'GET':
        return JsonResponse({
            "id": ticket.id,
            "empresa": ticket.empresa,
            "contacto": ticket.contacto,
            "tipo_dispositivo": ticket.tipo_dispositivo,
            "id_dispositivo": ticket.id_dispositivo,
            "observaciones": ticket.observaciones,
            "portes": ticket.portes,
            "empresa_transporte": ticket.empresa_transporte,
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

        ticket.empresa = data.get("empresa", ticket.empresa)
        ticket.contacto = data.get("contacto", ticket.contacto)
        ticket.tipo_dispositivo = data.get("tipo_dispositivo", ticket.tipo_dispositivo)
        ticket.id_dispositivo = data.get("id_dispositivo", ticket.id_dispositivo)
        ticket.observaciones = data.get("observaciones", ticket.observaciones)
        ticket.portes = data.get("portes", ticket.portes)
        ticket.empresa_transporte = data.get("empresa_transporte", ticket.empresa_transporte)
        ticket.save()
        return JsonResponse({"success": True}, status=200)

    else:
        return JsonResponse({"message": "Método no permitido"}, status=405)



