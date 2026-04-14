from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
from django.shortcuts import render
import json
from .models import Ticket

def index(request):
    return render(request, 'index.html')

@csrf_exempt
def crear_ticket(request):
    if request.method == "POST":
        try:
            data = json.loads(request.body)

            ticket = Ticket.objects.create(
                nombre=data.get("nombre"),
                nombre_empresa=data.get("nombre_empresa"),
                tipo_dispositivo=data.get("tipo_dispositivo"),
                id_dispositivo=data.get("id_dispositivo"),
                duda=data.get("duda")
            )

            return JsonResponse({
                "success": True,
                "message": "Ticket creado correctamente",
                "id": ticket.id
            }, status=201)

        except Exception as e:
            return JsonResponse({
                "success": False,
                "error": str(e)
            }, status=400)

    return JsonResponse({
        "message": "Método no permitido"
    }, status=405)