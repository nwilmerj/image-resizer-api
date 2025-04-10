resource "aws_dynamodb_table" "tasks_table" {
  name         = var.dynamodb_table_name
  billing_mode = "PAY_PER_REQUEST"

  # Atributo para la clave de partición (PK)
  attribute {
    name = "taskId"
    type = "S"
  }

  # Definición de la clave de partición
  hash_key = "taskId"

  tags = {
    Name        = "${var.project_name}-tasks-table-${var.environment}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

output "dynamodb_table_name" {
  description = "El nombre de la tabla DynamoDB creada."
  value       = aws_dynamodb_table.tasks_table.name
}

output "dynamodb_table_arn" {
  description = "El ARN de la tabla DynamoDB creada."
  value       = aws_dynamodb_table.tasks_table.arn
}