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
            ticket = Ticket.objects.create(
                nombre=request.POST.get("nombre"),
                nombre_empresa=request.POST.get("nombre_empresa"),
                tipo_dispositivo=request.POST.get("tipo_dispositivo"),
                id_dispositivo=request.POST.get("id_dispositivo"),
                duda=request.POST.get("duda"),
                archivo=request.FILES.get("archivo")
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