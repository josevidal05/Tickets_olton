from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.shortcuts import render
import json
from .models import Ticket

def index(request):
    return render(request, 'index.html')

def comprobar_tickets(request):
    return render(request, 'comprobar_tickets.html')

def login(request):
    return render(request, 'login.html')


def __get_request_user(request):
    header_token = request.headers.get('Session', None)
    if header_token is None:
        return None
    try:
        return Usuario.objects.get(token_sesion=header_token)
    except Usuario.DoesNotExist:
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
                empresa=request.POST.get("empresa"),
                contacto=request.POST.get("contacto"),
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
        return JsonResponse({"message": "Método no permitido"}, status= 405)



# Crear ticket para android 
@csrf_exempt
def ticket_ad(request):
    if request.method == "POST":
        try:
            # Los textos ahora vienen en request.POST (no en un JSON)
            # El archivo viene en request.FILES
            ticket = Ticket.objects.create(
                empresa=request.POST.get("empresa"),
                contacto=request.POST.get("contacto"),
                tipo_dispositivo=request.POST.get("tipo_dispositivo"),
                id_dispositivo=request.POST.get("id_dispositivo"),
                observaciones=request.POST.get("observaciones"),
                portes=request.POST.get("portes"),
                empresa_transporte=request.POST.get("transporte"),
                archivo=request.FILES.get("archivo") # Guarda el archivo real
            )

            return JsonResponse({"success": True}, status=201)
        
        except Exception as e:
            return JsonResponse({"success": False, "error": str(e)}, status=400)
    

    # Coge todos los tickets
    elif request.method == 'GET':



        tickets = Ticket.objects.all().values()  # Obtiene todos los tickets como diccionarios
        return JsonResponse(list(tickets), safe=False, status=200)


    else:
        return JsonResponse({"message": "Método no permitido"}, status= 405)


# Hacer login
@csrf_exempt
def crear_usuario(request):
    if request.method == "POST":
        try:
            user = User.objects.create(
                username=request.POST.get("username"),
                password=request.POST.get("password"),
                nombre=request.POST.get("nombre"),
                empresa=request.POST.get("empresa"),
                token_sesion=request.POST.get("token_sesion")
            )
            return JsonResponse({"success": True}, status=201)
        except Exception as e:
            return JsonResponse({"success": False, "error": str(e)}, status=400)

    else:
        return JsonResponse({"message": "Método no permitido"}, status= 405)


# Iniciar sesión
def login(request):
    if request.method == "POST":
        username = request.POST.get("username")
        password = request.POST.get("password")

        try:
            user = User.objects.get(username=username, password=password)
            return JsonResponse({"success": True, "token_sesion": user.token_sesion}, status=200)
        except User.DoesNotExist:
            return JsonResponse({"success": False, "error": "Credenciales inválidas"}, status=401)

    else:
        return JsonResponse({"message": "Método no permitido"}, status= 405)

        
def tickets_usuario(request):
    if request.method == 'GET':

        authenticated_user = __get_request_user(request)

        if authenticated_user is None:
            return JsonResponse({"error": "El token de sesión no se ha enviado o no es válido"}, status=401)

        tickets = Ticket.objects.filter(contacto=authenticated_user.username, empresa=authenticated_user.empresa).values()  # Filtra por contacto y empresa del usuario autenticado
        return JsonResponse(list(tickets), safe=False, status=200)

    else:
        return JsonResponse({"message": "Método no permitido"}, status= 405)


def ticket_id(request, ticket_id):
    if request.method == 'GET':
        try:
            ticket = Ticket.objects.get(id=ticket_id)
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
            }, status =200)
        except Ticket.DoesNotExist:
            return JsonResponse ({"error": "El ticket no existe"}, status=404)

    elif request.method == 'DELETE':
        try:
            ticket = Ticket.objects.get(id=ticket_id)
            ticket.delete()
            return JsonResponse({"success": True}, status=200)
        except Ticket.DoesNotExist:
            return JsonResponse({"error": "El ticket no existe"}, status=404)

    elif request.method == 'PUT':
        try:
            ticket = Ticket.objects.get(id=ticket_id)
            data = json.loads(request.body)

            ticket.empresa = data.get("empresa", ticket.empresa)
            ticket.contacto = data.get("contacto", ticket.contacto)
            ticket.tipo_dispositivo = data.get("tipo_dispositivo", ticket.tipo_dispositivo)
            ticket.id_dispositivo = data.get("id_dispositivo", ticket.id_dispositivo)
            ticket.observaciones = data.get("observaciones", ticket.observaciones)
            ticket.portes = data.get("portes", ticket.portes)
            ticket.empresa_transporte = data.get("empresa_transporte", ticket.empresa_transporte)
            ticket.save()
            return JsonResponse({"success": True}, status=200)
        except Ticket.DoesNotExist:
            return JsonResponse({"error": "El ticket no existe"}, status=404)

    else:
        return JsonResponse({"message": "Método no permitido"}, status = 405)
        