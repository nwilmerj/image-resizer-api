# Crear la REST API (reemplaza aws_apigatewayv2_api)
resource "aws_api_gateway_rest_api" "rest_api" {
  name        = "${var.project_name}-rest-api-${var.environment}"
  description = "REST API para ${var.project_name}"

  # Endpoint configuration (regional es lo más común)
  endpoint_configuration {
    types = ["REGIONAL"]
  }

  # Política de API (opcional, para control de acceso a nivel de API)
  tags = {
    Name        = "${var.project_name}-rest-api-${var.environment}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# Output para el ID de la REST API
output "rest_api_id" {
  description = "ID de la REST API."
  value       = aws_api_gateway_rest_api.rest_api.id
}
output "rest_api_execution_arn" {
  description = "ARN de ejecución base para la REST API."
  value       = aws_api_gateway_rest_api.rest_api.execution_arn
}

# Crear el recurso '/v1'
resource "aws_api_gateway_resource" "v1_resource" {
  rest_api_id = aws_api_gateway_rest_api.rest_api.id
  parent_id   = aws_api_gateway_rest_api.rest_api.root_resource_id
  path_part   = "v1"
}

# Crear el recurso '/task' bajo '/v1'
resource "aws_api_gateway_resource" "task_resource" {
  rest_api_id = aws_api_gateway_rest_api.rest_api.id
  parent_id   = aws_api_gateway_resource.v1_resource.id
  path_part   = "task"
}

# Crear el recurso '/{taskId}' bajo '/v1/task' para la variable de ruta
resource "aws_api_gateway_resource" "task_id_resource" {
  rest_api_id = aws_api_gateway_rest_api.rest_api.id
  parent_id   = aws_api_gateway_resource.task_resource.id
  path_part   = "{taskId}"
}

# Crear el método POST en '/v1/task'
resource "aws_api_gateway_method" "post_task_method" {
  rest_api_id   = aws_api_gateway_rest_api.rest_api.id
  resource_id   = aws_api_gateway_resource.task_resource.id
  http_method   = "POST"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer_rest.id
  # Opcional: API Key requerida (si usamos API Keys)
  # api_key_required = true
}

# Crear el método GET en '/v1/task/{taskId}'
resource "aws_api_gateway_method" "get_task_method" {
  rest_api_id   = aws_api_gateway_rest_api.rest_api.id
  resource_id   = aws_api_gateway_resource.task_id_resource.id
  http_method   = "GET"
  authorization = "COGNITO_USER_POOLS"
  authorizer_id = aws_api_gateway_authorizer.cognito_authorizer_rest.id
  # api_key_required = true

  # Necesitamos definir cómo se mapean los parámetros de la solicitud
  request_parameters = {
    # Indicar que el path parameter 'taskId' es requerido
    "method.request.path.taskId" = true
  }
}

# Crear Authorizer de tipo COGNITO_USER_POOLS para REST API
resource "aws_api_gateway_authorizer" "cognito_authorizer_rest" {
  name                   = "${var.project_name}-cognito-authorizer-rest-${var.environment}"
  rest_api_id            = aws_api_gateway_rest_api.rest_api.id
  type                   = "COGNITO_USER_POOLS"

  # ARN del User Pool (o lista de ARNs)
  provider_arns          = [aws_cognito_user_pool.user_pool.arn]

  # Dónde buscar el token (igual que antes)
  identity_source        = "method.request.header.Authorization"

  # (Opcional) Tiempo de caché para resultados de autorización
  # authorizer_result_ttl_in_seconds = 300
}

# Crear Integración Lambda para el método POST
resource "aws_api_gateway_integration" "post_task_lambda_integration" {
  rest_api_id             = aws_api_gateway_rest_api.rest_api.id
  resource_id             = aws_api_gateway_resource.task_resource.id # Recurso '/v1/task'
  http_method             = aws_api_gateway_method.post_task_method.http_method # Método POST
  integration_http_method = "POST" # Método usado para INVOCAR Lambda (debe ser POST para proxy)
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.image_resizer_function.invoke_arn
}

# Crear Integración Lambda para el método GET
resource "aws_api_gateway_integration" "get_task_lambda_integration" {
  rest_api_id             = aws_api_gateway_rest_api.rest_api.id
  resource_id             = aws_api_gateway_resource.task_id_resource.id
  http_method             = aws_api_gateway_method.get_task_method.http_method # Método GET
  integration_http_method = "POST" # Método usado para INVOCAR Lambda (debe ser POST para proxy)
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.image_resizer_function.invoke_arn
}

# Crear un Despliegue explícito de la API REST
resource "aws_api_gateway_deployment" "api_deployment" {
  rest_api_id = aws_api_gateway_rest_api.rest_api.id

  # Terraform necesita saber cuándo volver a crear el despliegue.
  triggers = {
    redeployment = sha1(jsonencode([
      aws_api_gateway_resource.v1_resource.id,
      aws_api_gateway_resource.task_resource.id,
      aws_api_gateway_resource.task_id_resource.id,
      aws_api_gateway_method.post_task_method.id,
      aws_api_gateway_method.get_task_method.id,
      aws_api_gateway_integration.post_task_lambda_integration.id,
      aws_api_gateway_integration.get_task_lambda_integration.id,
      aws_api_gateway_authorizer.cognito_authorizer_rest.id
      # Añadir aquí cualquier otro recurso cuya modificación deba disparar un nuevo despliegue
    ]))
  }

  # El ciclo de vida asegura que se cree un nuevo despliegue antes de destruir el viejo
  lifecycle {
    create_before_destroy = true
  }
}

# Crear el Stage (ej. 'dev' o 'v1') para el despliegue
resource "aws_api_gateway_stage" "api_stage" {
  deployment_id = aws_api_gateway_deployment.api_deployment.id
  rest_api_id   = aws_api_gateway_rest_api.rest_api.id
  stage_name    = var.environment

  # (Opcional) Habilitar logs de ejecución/acceso a CloudWatch
  # access_log_settings { ... }
  # xray_tracing_enabled = true # Para X-Ray

  tags = {
    Name        = "${var.project_name}-rest-api-stage-${var.environment}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# Output para la URL de invocación del stage
output "rest_api_invoke_url" {
  description = "La URL base para invocar la REST API en el stage desplegado."
  # La URL incluye el nombre del stage
  value       = aws_api_gateway_stage.api_stage.invoke_url
}

# Permiso para que la REST API invoque Lambda
resource "aws_lambda_permission" "rest_api_invoke_lambda" {
  statement_id  = "AllowRestApiInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.image_resizer_function.function_name
  principal     = "apigateway.amazonaws.com"

  # Source ARN para REST API: arn:aws:execute-api:region:account_id:rest_api_id/*/*/*
  source_arn = "${aws_api_gateway_rest_api.rest_api.execution_arn}/*/*"
}