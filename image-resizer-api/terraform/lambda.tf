
# Crea la función AWS Lambda
resource "aws_lambda_function" "image_resizer_function" {
  function_name = "${var.project_name}-function-${var.environment}"
  role = aws_iam_role.lambda_exec_role.arn
  filename = "../target/image-resizer-api-0.0.1-SNAPSHOT.jar"
  handler = "org.springframework.cloud.function.adapter.aws.FunctionInvoker::handleRequest"
  runtime = "java21"

  # Asignación de recursos a la función Lambda
  memory_size = 1024
  timeout     = 60

  # Variables de entorno que estarán disponibles para el código Java dentro de Lambda
  environment {
    variables = {
      AWS_S3_BUCKET_NAME      = var.s3_bucket_name
      AWS_DYNAMODB_TABLE_NAME = var.dynamodb_table_name
      CLOUDFRONT_DOMAIN     = aws_cloudfront_distribution.s3_distribution.domain_name
      MAIN_CLASS              = "com.newsnow.imageapi.ImageResizerApiApplication"
    }
  }

  # Configuración de Logging
  tags = {
    Name        = "${var.project_name}-function-${var.environment}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }

  # Dependencia explícita: Asegura que el rol IAM y su política
  depends_on = [aws_iam_role_policy_attachment.lambda_permissions_attach]
}

#Definir Outputs
output "lambda_function_name" {
  description = "Nombre de la función Lambda."
  value       = aws_lambda_function.image_resizer_function.function_name
}
output "lambda_function_arn" {
  description = "ARN de la función Lambda."
  value       = aws_lambda_function.image_resizer_function.arn
}
output "lambda_invoke_arn" {
  description = "ARN para invocar la función Lambda (usado por API Gateway)."
  value       = aws_lambda_function.image_resizer_function.invoke_arn
}