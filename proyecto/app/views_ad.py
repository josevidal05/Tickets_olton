import secrets
import bcrypt

from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
import json
from .models import Ticket, Usuario, Empresa


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

        # Determinar si la petición es JSON
        content_type = request.content_type or request.META.get('CONTENT_TYPE', '')
        print("--- DEBUG ticket_ad POST ---")
        print("Content-Type:", content_type)
        print("POST keys:", list(request.POST.keys()))
        print("FILES keys:", list(request.FILES.keys()))
        print("----------------------------")

        if 'application/json' in content_type:
            try:
                data = json.loads(request.body)
            except ValueError:
                return JsonResponse({"success": False, "error": "JSON inválido"}, status=400)
        else:
            # Si no es JSON (es multipart/form-data o urlencoded), leemos de POST
            data = request.POST

        # Obtener los campos
        tipo_dispositivo = data.get("tipo_dispositivo")
        id_dispositivo = data.get("id_dispositivo")
        observaciones = data.get("observaciones")
        portes = data.get("portes")
        # El JSON de Android envía 'transporte', en el modelo es 'empresa_transporte'
        transporte = data.get("transporte") or data.get("empresa_transporte")
        archivo = request.FILES.get("archivo")

        # Normalizar tipo_dispositivo para que coincida con las opciones del modelo Django
        # TIPO_DISPOSITIVO_CHOICES: ('maquina', 'Máquina'), ('tracker', 'Tracker'), ('otro', 'Otro')
        if tipo_dispositivo:
            tipo_lower = tipo_dispositivo.lower().strip()
            if 'máquina' in tipo_lower or 'maquina' in tipo_lower:
                tipo_dispositivo = 'maquina'
            elif 'tracker' in tipo_lower:
                tipo_dispositivo = 'tracker'
            else:
                tipo_dispositivo = 'otro'
        else:
            tipo_dispositivo = 'otro'

        # Normalizar portes para que coincida con las opciones del modelo Django
        # TIPO_PORTES_CHOICES: ('pagado', 'Pagado'), ('debido', 'Debido')
        if portes:
            portes_lower = portes.lower().strip()
            if 'pagado' in portes_lower:
                portes = 'pagado'
            else:
                portes = 'debido'
        else:
            portes = 'debido'

        if id_dispositivo is isinstance(id_dispositivo, str):
            return JsonResponse({"success": False, "error": "El ID del dispositivo debe ser un número entero"}, status=400)
        
        if id_dispositivo < 1:
            return JsonResponse ({"success": False, "error": "El ID del dispositivo tiene que ser mayor que 0"}, status=400)
        
        try:
            ticket = Ticket.objects.create(
                idUsuario=authenticated_user,
                tipo_dispositivo=tipo_dispositivo,
                id_dispositivo=id_dispositivo if id_dispositivo else 0,
                observaciones=observaciones,
                portes=portes,
                empresa_transporte=transporte,
                archivo=archivo
            )
            return JsonResponse({"success": True, "id": ticket.id}, status=201)
        except Exception as e:
            return JsonResponse({"success": False, "error": str(e)}, status=400)

    elif request.method == 'GET':
        ticket_list = []
        for ticket in Ticket.objects.all().select_related('idUsuario__empresa'):
            ticket_list.append({
                "id": ticket.id,
                "contacto": ticket.idUsuario.username,
                "empresa": ticket.idUsuario.empresa.nombre,
                "tipo_dispositivo": ticket.tipo_dispositivo,
                "id_dispositivo": ticket.id_dispositivo,
                "observaciones": ticket.observaciones,
                "archivo": ticket.archivo.name if ticket.archivo else "",
                "portes": ticket.portes,
                "empresa_transporte": ticket.empresa_transporte,
                "fecha_creacion": ticket.fecha_creacion,
            })
        return JsonResponse(ticket_list, safe=False, status=200)

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
    confirm_password = body_json.get('confirm_password') or body_json.get('confirm_contrasena')
    nombre = body_json.get('nombre') or body_json.get('name') or ''
    empresa_nombre = body_json.get('empresa') or body_json.get('company') or ''
    correo = body_json.get('correo') or body_json.get('email') or ''

    if username is None or password is None:
        return JsonResponse({"error": "Faltan parámetros"}, status=400)

    if password != confirm_password:
        return JsonResponse({"error": "Las contraseñas no coinciden"}, status=400)

    if len(username) < 3:
        return JsonResponse({"error": "Username too short"}, status=400)

    if len(password) < 8:
        return JsonResponse({"error": "Password too short"}, status=400)

    if Usuario.objects.filter(username=username).exists():
        return JsonResponse({"error": "Username already exists"}, status=409)

    if correo and Usuario.objects.filter(correo=correo).exists():
        return JsonResponse({"error": "El correo electrónico ya está en uso"}, status=409)

    # Obtener o crear la empresa
    empresa_obj, _ = Empresa.objects.get_or_create(
        nombre=empresa_nombre if empresa_nombre else 'Sin empresa',
        defaults={'encargado': username}
    )

    hashed_password = bcrypt.hashpw(password.encode('utf8'), bcrypt.gensalt()).decode('utf8')
    random_token = secrets.token_hex(16)

    user_object = Usuario.objects.create(
        username=username,
        password=hashed_password,
        nombre=nombre,
        empresa=empresa_obj,
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

    tickets = Ticket.objects.filter(idUsuario=authenticated_user)
    ticket_list = []
    for ticket in tickets:
        ticket_list.append({
            "id": ticket.id,
            "contacto": ticket.idUsuario.username,
            "empresa": ticket.idUsuario.empresa.nombre,
            "tipo_dispositivo": ticket.tipo_dispositivo,
            "id_dispositivo": ticket.id_dispositivo,
            "observaciones": ticket.observaciones,
            "archivo": ticket.archivo.name if ticket.archivo else "",
            "portes": ticket.portes,
            "empresa_transporte": ticket.empresa_transporte,
            "fecha_creacion": ticket.fecha_creacion,
        })
    return JsonResponse(ticket_list, safe=False, status=200)


@csrf_exempt
def ticket_id_ad(request, ticket_id):
    authenticated_user = __get_request_user_ad(request)
    if authenticated_user is None:
        return JsonResponse({"error": "Token inválido"}, status=401)

    try:
        ticket = Ticket.objects.get(id=ticket_id)
    except Ticket.DoesNotExist:
        return JsonResponse({"error": "El ticket no existe"}, status=404)

    if ticket.idUsuario != authenticated_user:
        return JsonResponse({"error": "No autorizado"}, status=403)

    if request.method == 'GET':
        return JsonResponse({
            "id": ticket.id,
            "idUsuario": ticket.idUsuario.id,
            "empresa": ticket.idUsuario.empresa.nombre,
            "contacto": ticket.idUsuario.username,
            "tipo_dispositivo": ticket.tipo_dispositivo,
            "id_dispositivo": ticket.id_dispositivo,
            "observaciones": ticket.observaciones,
            "portes": ticket.portes,
            "empresa_transporte": ticket.empresa_transporte,
            "fecha_creacion": ticket.fecha_creacion,
            "archivo": ticket.archivo.name if ticket.archivo else "",
            "archivo_url": ticket.archivo.url if ticket.archivo else "",
        }, status=200)

    elif request.method == 'DELETE':
        ticket.delete()
        return JsonResponse({"success": True}, status=200)

    elif request.method == 'POST':
        # multipart/form-data update (from Android or Web)
        ticket.tipo_dispositivo = request.POST.get("tipo_dispositivo", ticket.tipo_dispositivo)
        ticket.id_dispositivo = request.POST.get("id_dispositivo", ticket.id_dispositivo)
        ticket.observaciones = request.POST.get("observaciones", ticket.observaciones)
        ticket.portes = request.POST.get("portes", ticket.portes)
        transporte = request.POST.get("transporte") or request.POST.get("empresa_transporte")
        if transporte is not None:
            ticket.empresa_transporte = transporte

        # Normalizar tipo_dispositivo
        if ticket.tipo_dispositivo:
            tipo_lower = ticket.tipo_dispositivo.lower().strip()
            if 'máquina' in tipo_lower or 'maquina' in tipo_lower:
                ticket.tipo_dispositivo = 'maquina'
            elif 'tracker' in tipo_lower:
                ticket.tipo_dispositivo = 'tracker'
            else:
                ticket.tipo_dispositivo = 'otro'

        # Normalizar portes
        if ticket.portes:
            portes_lower = ticket.portes.lower().strip()
            if 'pagado' in portes_lower:
                ticket.portes = 'pagado'
            else:
                ticket.portes = 'debido'

        uploaded_file = request.FILES.get("archivo")
        if uploaded_file:
            ticket.archivo = uploaded_file

        try:
            ticket.save()
            return JsonResponse({"success": True}, status=200)
        except Exception as e:
            return JsonResponse({"success": False, "error": str(e)}, status=400)

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
        ticket.save()
        return JsonResponse({"success": True}, status=200)

    else:
        return JsonResponse({"message": "Método no permitido"}, status=405)


@csrf_exempt
def perfil_ad(request):
    authenticated_user = __get_request_user_ad(request)
    if authenticated_user is None:
        return JsonResponse({"error": "El token de sesión no es válido o no se ha enviado"}, status=401)

    if request.method == "GET":
        # Devuelve el perfil en una lista/array para que JsonArrayRequestWithCustomAuth lo parsee correctamente
        user_data = {
            "id": authenticated_user.id,
            "username": authenticated_user.username,
            "nombre": authenticated_user.nombre,
            "empresa": authenticated_user.empresa.nombre,
            "correo": authenticated_user.correo,
        }
        return JsonResponse([user_data], safe=False, status=200)

    elif request.method == "PUT":
        try:
            data = json.loads(request.body)
        except ValueError:
            return JsonResponse({"error": "JSON inválido"}, status=400)

        username = data.get("username")
        nombre = data.get("nombre")
        empresa_nombre = data.get("empresa")
        correo = data.get("correo")
        password = data.get("password")

        # Validación de campos únicos excluyendo al usuario actual
        if username:
            if Usuario.objects.filter(username=username).exclude(id=authenticated_user.id).exists():
                return JsonResponse({"error": "El nombre de usuario ya existe"}, status=409)
            authenticated_user.username = username

        if correo:
            if Usuario.objects.filter(correo=correo).exclude(id=authenticated_user.id).exists():
                return JsonResponse({"error": "El correo electrónico ya está en uso"}, status=409)
            authenticated_user.correo = correo

        if nombre is not None:
            authenticated_user.nombre = nombre

        if empresa_nombre is not None:
            # Obtener o crear la empresa por nombre
            empresa_obj, _ = Empresa.objects.get_or_create(
                nombre=empresa_nombre,
                defaults={'encargado': authenticated_user.username}
            )
            authenticated_user.empresa = empresa_obj

        if password and password.strip() != "":
            hashed_password = bcrypt.hashpw(password.encode('utf8'), bcrypt.gensalt()).decode('utf8')
            authenticated_user.password = hashed_password

        try:
            authenticated_user.save()
            return JsonResponse({"success": True}, status=200)
        except Exception as e:
            return JsonResponse({"error": str(e)}, status=400)

    else:
        return JsonResponse({"message": "Método no permitido"}, status=405)
