# Define qué servicio puede asumir (usar) este rol
data "aws_iam_policy_document" "lambda_assume_role_policy" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

# Crea el Rol IAM
resource "aws_iam_role" "lambda_exec_role" {
  # Construye un nombre único usando las variables del proyecto y entorno
  name = "${var.project_name}-lambda-exec-role-${var.environment}"

  # Asigna la política de confianza definida arriba (referencia al 'data source')
  assume_role_policy = data.aws_iam_policy_document.lambda_assume_role_policy.json

  # Añade etiquetas para organización (buenas prácticas)
  tags = {
    Name        = "${var.project_name}-lambda-exec-role-${var.environment}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# Define los permisos específicos que tendrá el rol
data "aws_iam_policy_document" "lambda_permissions_policy_doc" {
  # Permiso para CloudWatch Logs
  statement {
    sid    = "AllowCloudWatchLogs"
    effect = "Allow"
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents"
    ]
    # Permite escribir en cualquier log group/stream (simplificado)
    resources = ["arn:aws:logs:*:*:*"]
  }

  # Permiso para escribir en S3 (carpeta processed/)
  statement {
    sid    = "AllowS3PutObjectProcessed"
    effect = "Allow"
    actions = [
      "s3:PutObject"
    ]
    # RECURSO ESPECÍFICO: Solo permite escribir en la carpeta 'processed/'
    resources = ["${aws_s3_bucket.image_bucket.arn}/processed/*"]
  }

  # Permiso para leer/escribir/actualizar en DynamoDB
  statement {
    sid    = "AllowDynamoDBReadWrite"
    effect = "Allow"
    actions = [
      "dynamodb:PutItem",
      "dynamodb:GetItem",
      "dynamodb:UpdateItem"
    ]
    # RECURSO ESPECÍFICO: Solo permite acciones sobre la tabla creada
    resources = [aws_dynamodb_table.tasks_table.arn]
  }
}

# Crea la Política IAM gestionada con los permisos definidos arriba
resource "aws_iam_policy" "lambda_permissions_policy" {
  name        = "${var.project_name}-lambda-permissions-${var.environment}"
  description = "Política de permisos para la Lambda ${var.project_name}"
  # Asigna el documento JSON generado por el 'data source'
  policy      = data.aws_iam_policy_document.lambda_permissions_policy_doc.json
}

# Adjunta la política de permisos al rol de ejecución de Lambda
resource "aws_iam_role_policy_attachment" "lambda_permissions_attach" {
  role       = aws_iam_role.lambda_exec_role.name
  policy_arn = aws_iam_policy.lambda_permissions_policy.arn
}

# Exporta el ARN del rol para que otros recursos (Lambda) puedan usarlo
output "lambda_exec_role_arn" {
  description = "ARN del Rol IAM para la ejecución de Lambda."
  value       = aws_iam_role.lambda_exec_role.arn
}