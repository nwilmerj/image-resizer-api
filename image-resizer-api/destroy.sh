#!/bin/bash

# destroy.sh
# Script para destruir TODA la infraestructura gestionada por Terraform.
# Ejecutar desde la raíz del proyecto.
# ¡¡USAR CON PRECAUCIÓN!!

# Abort script on first error
set -e

echo "🧨 Destruyendo infraestructura con Terraform..."
# Cambiar al directorio de Terraform
cd terraform

echo "   🔹 Inicializando Terraform (necesario antes de destroy)..."
terraform init -upgrade

echo "   🔹 Destruyendo recursos (¡¡ESTO BORRARÁ TODO!!)..."
# Quita -auto-approve si quieres confirmar manualmente (RECOMENDADO para destroy)
terraform destroy -auto-approve

cd .. # Volver al directorio raíz

echo "✅ Destrucción completada."