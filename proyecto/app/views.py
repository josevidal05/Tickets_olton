import secrets
from urllib import request
import bcrypt
from datetime import datetime

from django.http import JsonResponse, HttpResponseRedirect
from django.views.decorators.csrf import csrf_exempt
from django.shortcuts import render
import json
from .models import Ticket, Usuario, Empresa

# Función para obtener el usuario autenticado 
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


# Registrar usuario
def registar_usuario(request):
    if request.method == "GET":
        return render(request, 'registro.html')

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
            

            # Verificar si la empresa existe (ignorar mayúsculas/minúsculas)
            empresa_obj = Empresa.objects.filter(nombre__iexact=empresa_nombre).first()
            if not empresa_obj:
                return render(request, 'registro.html', {
                    'error': 'No existen empresas con este nombre'
                })

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

            request.session['session_token'] = random_token

            return HttpResponseRedirect('/tickets_usuario/')
        except Exception as e:
            return render(request, 'registro.html', {
                'error': str(e)
            })

    else:
        return render(request, 'registro.html')


# Iniciar sesión
def iniciar_sesion(request):
    if request.method == "GET":
        return render(request, 'login.html')

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
                
                return HttpResponseRedirect('/tickets_usuario/')
            
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


# Crear ticket para web
@csrf_exempt
def crear_ticket(request):

    if request.method == "GET":
#        print("estoy en get")
        # Verificar si el usuario está autenticado
        authenticated_user = __get_request_user(request)
        if authenticated_user is None:
            return HttpResponseRedirect('/login/')
        return render(request, 'crear_ticket.html', {'user': authenticated_user})

    if request.method == "POST":
#        print("estoy en post")

        authenticated_user = __get_request_user(request)

        if authenticated_user is None:
            return JsonResponse({"error": "El token de sesión no se ha enviado o no es válido"}, status=401)
        
        try:
            # Obtener campos y validar presencia
            tipo_disp = request.POST.get("tipo_dispositivo")
            id_disp_raw = request.POST.get("id_dispositivo")
            observaciones = request.POST.get("observaciones")
            portes = request.POST.get("portes")
            transporte = request.POST.get("transporte")

            print(1)
            if request.POST.get("portes") != "debido" and request.POST.get("portes") != "pagado":
                print(request.POST)
                return JsonResponse({"error": "Portes no válidos"}, status=400)


            if not all([tipo_disp, id_disp_raw, observaciones, portes, transporte]):
                return JsonResponse({"error": "Faltan campos obligatorios"}, status=400)

            # Validar que sea número y convertir a int
            if not id_disp_raw.isdigit():
                return JsonResponse({"error": "El ID del dispositivo debe ser un número"}, status=400)

            id_dispositivo = int(id_disp_raw)
            if id_dispositivo < 1:
                return JsonResponse({"error": "El ID del dispositivo debe ser mayor que 0"}, status=400)

            ticket = Ticket.objects.create(
                idUsuario=authenticated_user,
                tipo_dispositivo=tipo_disp,
                id_dispositivo=id_dispositivo,
                observaciones=observaciones,
                portes=portes,
                empresa_transporte=transporte,
                archivo=request.FILES.get("archivo"),
                estado="no leido"
            )

            return JsonResponse({
                "estado": "Ticket creado correctamente",
                "id": ticket.id
            }, status=201)
 
        except Exception as e:
            return JsonResponse({
                "success": False,
                "error": str(e)
            }, status=402)

    else:
        return JsonResponse({"message": "Método no permitido"}, status=405)


# Tickets de cada usuario
def tickets_usuario(request):
    if request.method == 'GET':
        authenticated_user = __get_request_user(request)
        if authenticated_user is None:
            return HttpResponseRedirect('/login/')

        if authenticated_user.admin:
            tickets = Ticket.objects.all()
        elif authenticated_user.empresa.encargado == authenticated_user.username:
            tickets = Ticket.objects.filter(idUsuario__empresa = authenticated_user.empresa)
        else:
            tickets = Ticket.objects.filter(idUsuario=authenticated_user)

        filtros = {
            'usuario': request.GET.get('usuario', '').strip(),
            'id_dispositivo': request.GET.get('id_dispositivo', '').strip(),
            'tipo_dispositivo': request.GET.get('tipo_dispositivo', '').strip(),
            'estado': request.GET.get('estado', '').strip(),
            'fecha': request.GET.get('fecha', '').strip(),
        }

        if filtros['usuario']:
            tickets = tickets.filter(idUsuario__username__icontains=filtros['usuario'])

        if filtros['id_dispositivo']:
            if filtros['id_dispositivo'].isdigit():
                tickets = tickets.filter(id_dispositivo=int(filtros['id_dispositivo']))
            else:
                tickets = tickets.filter(id_dispositivo__icontains=filtros['id_dispositivo'])

        if filtros['tipo_dispositivo']:
            tickets = tickets.filter(tipo_dispositivo=filtros['tipo_dispositivo'])

        if filtros['estado']:
            tickets = tickets.filter(estado=filtros['estado'])

        if filtros['fecha']:
            try:
                fecha_obj = datetime.fromisoformat(filtros['fecha']).date()
                tickets = tickets.filter(fecha_creacion__date=fecha_obj)
            except ValueError:
                pass

        return render(request, 'tickets_usuario.html', {
            'user': authenticated_user,
            'tickets': tickets,
            'filtros': filtros,
            'tipo_dispositivo_choices': Ticket.TIPO_DISPOSITIVO_CHOICES,
            'estado_choices': Ticket.ESTADO_TICKET_CHOICES,
        })

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
    except Ticket.DoesNotExist:
        if request.method == 'GET':
            return render(request, 'ticket.html', {'error': 'El ticket no existe'})
        return JsonResponse({"error": "El ticket no existe"}, status=404)

    if ticket.idUsuario != authenticated_user and authenticated_user.username != ticket.idUsuario.empresa.encargado:
        if request.method == 'GET':
            return render(request, 'ticket.html', {'error': 'No tienes permiso para ver este ticket'})
        return JsonResponse({"error": "No tienes permiso para acceder a este ticket"}, status=403)

    if request.method == 'GET':
        return render(request, 'ticket.html', {'ticket': ticket})

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
        ticket.save()
        
        return JsonResponse({"success": True}, status=200)

    else:
        return JsonResponse({"message": "Método no permitido"}, status=405)

def ticket_pdf(request, ticket_id):
    from reportlab.lib.pagesizes import A4
    from reportlab.lib import colors
    from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
    from reportlab.platypus import SimpleDocTemplate, Table, TableStyle, Paragraph, Spacer
    from io import BytesIO
    from django.http import HttpResponse
    
    authenticated_user = __get_request_user(request)
    if authenticated_user is None:
        return JsonResponse({"error": "No autenticado"}, status=401)
    
    try:
        ticket = Ticket.objects.get(id=ticket_id)
        if ticket.idUsuario != authenticated_user and not authenticated_user.admin:
            return JsonResponse({"error": "No tienes permiso para descargar este ticket"}, status=403)
    except Ticket.DoesNotExist:
        return JsonResponse({"error": "Ticket no encontrado"}, status=404)
    except Exception as e:
        return JsonResponse({"error": f"Error al acceder al ticket: {str(e)}"}, status=500)
    
    try:
        # Crear buffer para PDF
        buffer = BytesIO()
        doc = SimpleDocTemplate(buffer, pagesize=A4, topMargin=20, bottomMargin=20)
        story = []
        styles = getSampleStyleSheet()
        
        # Estilos personalizados
        title_style = ParagraphStyle(
            'CustomTitle',
            parent=styles['Heading1'],
            fontSize=18,
            textColor=colors.HexColor('#1f3a72'),
            spaceAfter=18,
            alignment=1,
            fontName='Helvetica-Bold'
        )
        
        # Encabezado
        story.append(Paragraph(f'Detalle del Ticket #{ticket.id}', title_style))
        story.append(Spacer(1, 12))
        
        # Preparar datos del ticket con valores seguros
        empresa_nombre = ticket.idUsuario.empresa.nombre if (ticket.idUsuario and ticket.idUsuario.empresa) else 'N/A'
        tipo_dispositivo = ticket.get_tipo_dispositivo_display() if hasattr(ticket, 'get_tipo_dispositivo_display') else ticket.tipo_dispositivo
        portes = ticket.get_portes_display() if hasattr(ticket, 'get_portes_display') else ticket.portes
        
        data = [
            ['Campo', 'Valor'],
            ['ID Ticket', str(ticket.id)],
            ['Empresa', empresa_nombre],
            ['Contacto', ticket.idUsuario.username if ticket.idUsuario else 'N/A'],
            ['Tipo de dispositivo', str(tipo_dispositivo)],
            ['ID dispositivo', str(ticket.id_dispositivo) if ticket.id_dispositivo else 'N/A'],
            ['Observaciones', str(ticket.observaciones) if ticket.observaciones else 'N/A'],
            ['Portes', str(portes)],
            ['Empresa de transporte', str(ticket.empresa_transporte) if ticket.empresa_transporte else 'N/A'],
            ['Estado', str(ticket.estado) if ticket.estado else 'N/A'],
            ['Fecha de creación', str(ticket.fecha_creacion) if ticket.fecha_creacion else 'N/A'],
        ]
        
        table = Table(data, colWidths=[140, 310])
        table.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#1f3a72')),
            ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
            ('ALIGN', (0, 0), (-1, -1), 'LEFT'),
            ('VALIGN', (0, 0), (-1, -1), 'TOP'),
            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
            ('FONTSIZE', (0, 0), (-1, 0), 11),
            ('BOTTOMPADDING', (0, 0), (-1, 0), 12),
            ('BACKGROUND', (0, 1), (-1, -1), colors.beige),
            ('GRID', (0, 0), (-1, -1), 1, colors.HexColor('#d7dce8')),
            ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, colors.HexColor('#f9fbff')]),
            ('FONTSIZE', (0, 1), (-1, -1), 10),
        ]))
        
        story.append(table)
        story.append(Spacer(1, 20))
        
        # Pie de página
        footer_text = f'Generado el {datetime.now().strftime("%d/%m/%Y %H:%M:%S")} | Sistema de Tickets'
        footer_style = ParagraphStyle(
            'Footer',
            parent=styles['Normal'],
            fontSize=8,
            textColor=colors.HexColor('#8899aa'),
            alignment=1
        )
        story.append(Paragraph(footer_text, footer_style))
        
        # Construir PDF
        doc.build(story)
        
        # Preparar respuesta
        buffer.seek(0)
        response = HttpResponse(buffer.getvalue(), content_type='application/pdf')
        response['Content-Disposition'] = f'attachment; filename="ticket_{ticket.id}.pdf"'
        return response
        
    except Exception as e:
        import traceback
        print(f"Error en ticket_pdf: {str(e)}")
        print(traceback.format_exc())
        return JsonResponse({"error": f"Error al generar PDF: {str(e)}"}, status=500)



def perfil(request):
    authenticated_user = __get_request_user(request)
    
    if authenticated_user is None:
        return JsonResponse({"error": "El token de sesión no es válido o no se ha enviado"}, status=401)

    if request.method == "GET":
        # Verificar si el usuario está autenticado
        authenticated_user = __get_request_user(request)
        if authenticated_user is None:
            return HttpResponseRedirect('/login/')
        
        # Obtener los tickets del usuario usando la FK idUsuario
        tickets = Ticket.objects.filter(idUsuario=authenticated_user)
        
        is_encargado = False
        empresa = None
        company_tickets = None
        company_ticket_count = 0

        if str(authenticated_user.empresa) and str(authenticated_user.empresa.encargado) == str(authenticated_user.username):
            is_encargado = True
            empresa = authenticated_user.empresa
            company_tickets = Ticket.objects.filter(idUsuario__empresa=empresa)
            company_ticket_count = company_tickets.count()


        return render(request, 'perfil/perfil.html', {
            'user': authenticated_user,
            'tickets': tickets,
            'is_encargado': is_encargado,
            'empresa': empresa,
            'company_tickets': company_tickets,
            'company_ticket_count': company_ticket_count,
        })


    if request.method == "PUT":
        try:
            data = json.loads(request.body)
        except ValueError:
            return JsonResponse({"error": "JSON inválido"}, status=400)

        username = data.get("username")
        nombre = data.get("nombre")
        empresa_nombre = data.get("empresa")
        correo = data.get("correo")
        password_actual = data.get("contrasena_actual")
        password = data.get("contrasena_nueva")
        confirm_password = data.get("contrasena_nueva_confirmar")

        if password_actual and not bcrypt.checkpw(password_actual.encode('utf8'), authenticated_user.password.encode('utf8')):
            return JsonResponse({"error": "La contraseña actual es incorrecta"}, status=401)

        if username == "" or correo == "" or nombre == "" or empresa_nombre == "":
            return JsonResponse({"error": "No se han proporcionado campos para actualizar"}, status=400)

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
            if password != confirm_password:
                return JsonResponse({"error": "Las contraseñas no coinciden"}, status=400)
            hashed_password = bcrypt.hashpw(password.encode('utf8'), bcrypt.gensalt()).decode('utf8')
            authenticated_user.password = hashed_password

        try:
            authenticated_user.save()
            return JsonResponse({"success": True}, status=200)
        except Exception as e:
            return JsonResponse({"error": str(e)}, status=400)

    else:
        return JsonResponse({"message": "Método no permitido"}, status=405)


def datos_usuario(request):
    # Verificar si el usuario está autenticado
    authenticated_user = __get_request_user(request)
    if authenticated_user is None:
        return HttpResponseRedirect('/login/')
    
    return render(request, 'perfil/datos_usuario.html', {
        'user': authenticated_user
    })


def contraseña(request):
    authenticated_user = __get_request_user(request)
    if authenticated_user is None:
        return HttpResponseRedirect('/login/')

    if request.method == "GET":    
        # Verificar si el usuario está autenticado
        return render(request, 'perfil/cambiar_contraseña.html', {
            'user': authenticated_user
        })
    
    if request.method == "PUT":
        try:
            data = json.loads(request.body)
        except ValueError:
            return JsonResponse({"error": "JSON inválido"}, status=400)

        password_actual = data.get("contrasena_actual")
        new_password = data.get("contrasena_nueva")
        confirm_new_password = data.get("contrasena_nueva_confirmar")

        if password_actual and not bcrypt.checkpw(password_actual.encode('utf8'), authenticated_user.password.encode('utf8')):
            return JsonResponse({"error": "La contraseña actual es incorrecta"}, status=401)
        
        if new_password == "" or new_password is None:
            return JsonResponse({"error": "La nueva contraseña no puede estar vacía"}, status=401)
    
        if new_password != confirm_new_password:
            return JsonResponse({"error": "Las contraseñas no coinciden"}, status=401)

        hashed_password = bcrypt.hashpw(new_password.encode('utf8'), bcrypt.gensalt()).decode('utf8')
        authenticated_user.password=hashed_password

        try:
            authenticated_user.save()
            return JsonResponse({"success": True}, status=200)
        except Exception as e:
            return JsonResponse({"error": str(e)}, status=400)

    else: 
        JsonResponse({"error": "Método no válido"}, status=405 )

        


# MÉTODOS PARA LOS ENCARGADOS DE LAS EMPRESAS
def empresa(request):
    authenticated_user = __get_request_user(request)
    if authenticated_user is None:
        return HttpResponseRedirect('/login/')

    # Que el usuario registrado sea encargado de la empresa
    if not str(authenticated_user.empresa) or str(authenticated_user.empresa.encargado) != str(authenticated_user.username):
        return HttpResponseRedirect('/perfil/')

    empresa_obj = authenticated_user.empresa
    empleados = Usuario.objects.filter(empresa=empresa_obj)
    ticket_count = Ticket.objects.filter(idUsuario__empresa=empresa_obj).count()

    return render(request, 'empresa.html', {
        'user': authenticated_user,
        'empresa': empresa_obj,
        'empleados': empleados,
        'ticket_count': ticket_count,
    })

def tickets_empresa(request):
    authenticated_user = __get_request_user(request)
    if authenticated_user is None:
        return HttpResponseRedirect('/login/')

    # Que el usuario registrado sea encargado de la empresa
    if not str(authenticated_user.empresa) or str(authenticated_user.empresa.encargado) != str(authenticated_user.username):
        return HttpResponseRedirect('/perfil/')

    empresa_obj = authenticated_user.empresa
    tickets = Ticket.objects.filter(idUsuario__empresa=empresa_obj)

    return render(request, 'tickets_empresa.html', {
        'user': authenticated_user,
        'tickets': tickets,
        'empresa': empresa_obj,
    })

#MÉTODOS PARA ADMINISTRADORES

def usuarios(request):
    if request.method == "GET":

        authenticated_user = __get_request_user(request)

        if authenticated_user is None or authenticated_user.admin == False:
            return JsonResponse({"error": "El token de sesión no se ha enviado o no es válido"}, status=401)
        
    
def usuario_id (request):
    if request.method == "GET":

        authenticated_user = __get_request_user(request)

    if authenticated_user is None or authenticated_user.admin == False:
        return JsonResponse({"error": "El token de sesión no se ha enviado o no es válido"}, status=401)
        

def empresas(request):
    if request.method == "GET":

        authenticated_user = __get_request_user(request)

        if authenticated_user is None or authenticated_user.admin == False:
            return JsonResponse({"error": "El token de sesión no se ha enviado o no es válido"}, status=401)

# admin
def comprobar_tickets(request):
    # Verificar si el usuario está autenticado
    authenticated_user = __get_request_user(request)
    if authenticated_user is None:
        return HttpResponseRedirect('/login/')
    if authenticated_user.admin == False:
        return render (request, "error.html")
    return render(request, 'admin/comprobar_tickets.html')

def gestion_empresas(request):
    # Verificar si el usuario está autenticado
    authenticated_user = __get_request_user(request)
    if authenticated_user is None:
        return HttpResponseRedirect('/login/')
    if authenticated_user.admin == False:
        return render (request, "error.html")
    
    empresas = Empresa.objects.all().order_by('nombre')
    return render(request, 'admin/empresas.html', {
        'empresas': empresas
    })

def gestion_usuarios(request):
    # Verificar si el usuario está autenticado
    authenticated_user = __get_request_user(request)
    if authenticated_user is None:
        return HttpResponseRedirect('/login/')
    if authenticated_user.admin == False:
        return render (request, "error.html")
    
    return render(request, 'admin/usuarios.html')

def gestion_usuario_id(request):
    # Verificar si el usuario está autenticado
    authenticated_user = __get_request_user(request)
    if authenticated_user is None:
        return HttpResponseRedirect('/login/')
    if authenticated_user.admin == False:
        return render (request, "error.html")
    
    return render(request, 'admin/usuario_id.html')