import django.db.models.deletion
from django.db import migrations, models


def crear_empresas_desde_usuarios(apps, schema_editor):
    """
    Lee empresa_str (ex-empresa CharField) de cada Usuario,
    crea la Empresa si no existe, y actualiza empresa_id en la tabla con SQL.
    """
    Empresa = apps.get_model('app', 'Empresa')
    Usuario = apps.get_model('app', 'Usuario')

    for usuario in Usuario.objects.all():
        nombre = usuario.empresa_str or 'Sin empresa'
        empresa_obj, _ = Empresa.objects.get_or_create(
            nombre=nombre,
            defaults={'encargado': usuario.username}
        )
        # Actualizar el campo empresa_id directamente con SQL nativo de SQLite
        with schema_editor.connection.cursor() as cursor:
            cursor.execute(
                "UPDATE app_usuario SET empresa_id = %s WHERE id = %s",
                [empresa_obj.id, usuario.id]
            )


class Migration(migrations.Migration):

    dependencies = [
        ('app', '0010_alter_usuario_token_sesion'),
    ]

    operations = [
        # ── 1. Renombrar empresa (CharField) → empresa_str para conservar datos ──
        migrations.RenameField(
            model_name='usuario',
            old_name='empresa',
            new_name='empresa_str',
        ),

        # ── 2. Añadir FK empresa (nullable temporalmente) ──
        migrations.AddField(
            model_name='usuario',
            name='empresa',
            field=models.ForeignKey(
                null=True,
                on_delete=django.db.models.deletion.CASCADE,
                related_name='usuarios',
                to='app.empresa',
            ),
        ),

        # ── 3. Data migration: crear Empresas y rellenar empresa_id ──
        migrations.RunPython(
            crear_empresas_desde_usuarios,
            migrations.RunPython.noop
        ),

        # ── 4. Eliminar el campo empresa_str ya no necesario ──
        migrations.RemoveField(
            model_name='usuario',
            name='empresa_str',
        ),

        # ── 5. Hacer la FK empresa NOT NULL ──
        migrations.AlterField(
            model_name='usuario',
            name='empresa',
            field=models.ForeignKey(
                on_delete=django.db.models.deletion.CASCADE,
                related_name='usuarios',
                to='app.empresa',
            ),
        ),

        # ── 6. Quitar campos contacto y empresa de Ticket ──
        migrations.RemoveField(
            model_name='ticket',
            name='contacto',
        ),
        migrations.RemoveField(
            model_name='ticket',
            name='empresa',
        ),

        # ── 7. Añadir FK idUsuario en Ticket ──
        migrations.AddField(
            model_name='ticket',
            name='idUsuario',
            field=models.ForeignKey(
                default=1,
                on_delete=django.db.models.deletion.CASCADE,
                related_name='tickets',
                to='app.usuario',
            ),
            preserve_default=False,
        ),

        # ── 8. Ajustar AutoField IDs ──
        migrations.AlterField(
            model_name='empresa',
            name='id',
            field=models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID'),
        ),
        migrations.AlterField(
            model_name='ticket',
            name='id',
            field=models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID'),
        ),
        migrations.AlterField(
            model_name='usuario',
            name='id',
            field=models.AutoField(auto_created=True, primary_key=True, serialize=False, verbose_name='ID'),
        ),
    ]
