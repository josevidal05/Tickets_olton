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

@csrf_exempt
def crear_ticket(request):
    if request.method == "POST":
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

    return JsonResponse({
        "message": "Método no permitido"
    }, status=405)


@csrf_exempt
def subir_ticket(request):
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
