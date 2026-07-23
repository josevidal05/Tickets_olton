from .models import Usuario


def user_permissions(request):
    excluded_paths = ['/login/', '/registro/']
    if request.path in excluded_paths:
        return {}

    user = None

    header_token = request.headers.get('Session')
    if header_token:
        try:
            user = Usuario.objects.get(token_sesion=header_token)
            if not user.is_session_token_valid():
                user.clear_session_token()
                user = None
        except Usuario.DoesNotExist:
            user = None

    if user is None:
        session_token = request.session.get('session_token')
        if session_token:
            try:
                user = Usuario.objects.get(token_sesion=session_token)
                if not user.is_session_token_valid():
                    user.clear_session_token()
                    user = None
                    request.session.pop('session_token', None)
            except Usuario.DoesNotExist:
                user = None

    if user is None:
        return {
            'is_admin': False,
            'is_taller': False,
            'is_admin_or_taller': False,
            'is_encargado': False,
        }

    is_admin = user.tipo_usuario == 'admin'
    is_taller = user.tipo_usuario == 'taller'
    is_admin_or_taller = is_admin or is_taller

    is_encargado = False
    if getattr(user, 'empresa', None) is not None:
        is_encargado = str(user.empresa.encargado) == str(user.username)

    return {
        'is_admin': is_admin,
        'is_taller': is_taller,
        'is_admin_or_taller': is_admin_or_taller,
        'is_encargado': is_encargado,
    }
