# Crear un Pool de Usuarios de Cognito
resource "aws_cognito_user_pool" "user_pool" {
  name = "${var.project_name}-user-pool-${var.environment}"

  # Políticas de contraseña (ajustar según necesidad)
  password_policy {
    minimum_length    = 8
    require_lowercase = true
    require_numbers   = true
    require_symbols   = false
    require_uppercase = true
  }

  # Permitir que los administradores creen usuarios sin enviar invitación inicial
  admin_create_user_config {
    allow_admin_create_user_only = true
  }

  # Cómo se verifican los usuarios (email o teléfono) - Usaremos email
  auto_verified_attributes = ["email"]
  username_attributes      = ["email"]

  # Configuración MFA (opcional)
  # mfa_configuration = "OFF"

  tags = {
    Name        = "${var.project_name}-user-pool-${var.environment}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# Crear un Cliente de Aplicación para el User Pool
resource "aws_cognito_user_pool_client" "app_client" {
  name = "${var.project_name}-app-client-${var.environment}"
  user_pool_id = aws_cognito_user_pool.user_pool.id

  # IMPORTANTE: Sin secreto de cliente para aplicaciones web/SPA/móviles
  # o si API Gateway valida tokens directamente.
  generate_secret = false

  # Flujos de autenticación permitidos
  explicit_auth_flows = ["ALLOW_USER_PASSWORD_AUTH", "ALLOW_REFRESH_TOKEN_AUTH", "ALLOW_ADMIN_USER_PASSWORD_AUTH"]

  # Habilitar Token Revocation (buena práctica)
  enable_token_revocation = true
}

# Outputs para IDs importantes
output "cognito_user_pool_id" {
  description = "ID del Cognito User Pool."
  value       = aws_cognito_user_pool.user_pool.id
}
output "cognito_app_client_id" {
  description = "ID del Cognito App Client."
  value       = aws_cognito_user_pool_client.app_client.id
}
output "cognito_user_pool_arn" {
  description = "ARN del Cognito User Pool (usado por API Gateway Authorizer)."
  value       = aws_cognito_user_pool.user_pool.arn
}