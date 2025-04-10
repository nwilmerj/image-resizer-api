#!/bin/bash

# deploy.sh
# Script para empaquetar la aplicación Java y desplegar/actualizar
# la infraestructura con Terraform.
# Ejecutar desde la raíz del proyecto.

# Abort script on first error
set -e

# -- PASO 1: Probar y Empaquetar --
echo "🧪 Ejecutando pruebas y empaquetando aplicación Java..."
mvn clean package
# Si llegamos aquí, las pruebas pasaron y el empaquetado fue exitoso.
echo "✅ Pruebas y empaquetado completados."

# -- PASO 2: Desplegar/Actualizar con Terraform --
echo "🚀 Desplegando/Actualizando infraestructura con Terraform..."
# Cambiar al directorio de Terraform
cd terraform || { echo "Error: No se pudo entrar al directorio 'terraform'"; exit 1; }

echo "   🔹 Inicializando Terraform (con upgrade)..."
terraform init -upgrade

# Opcional: revisar plan antes si es necesario para revisión manual
# echo "   🔹 Planificando cambios Terraform..."
# terraform plan -out=tfplan
# echo "   -> Revisa el plan 'tfplan'. Presiona Enter para continuar o Ctrl+C para abortar."
# read

echo "   🔹 Aplicando cambios Terraform ..."
terraform apply -auto-approve

# -- PASO 3: Mostrar Outputs --
# Opcional: Mostrar outputs después de aplicar
echo "Outputs de Terraform:"
terraform output

# Volver al directorio raíz (opcional, buena práctica)
cd ..

echo "✅ Despliegue completado."
echo "🟢 API Gateway URL:"
(terraform -chdir=./terraform output -raw rest_api_invoke_url) || echo "-> Ejecuta 'terraform output' en la carpeta 'terraform' para ver la URL."