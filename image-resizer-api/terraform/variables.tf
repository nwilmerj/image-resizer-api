variable "aws_region" {
  description = "La región AWS donde se desplegarán los recursos."
  type        = string
  default     = "us-east-1"
}

variable "s3_bucket_name" {
  description = "El nombre único global para el bucket S3."
  type        = string
  default     = "newsnow-image-bucket"
}

variable "project_name" {
  description = "Nombre base para etiquetar recursos."
  type        = string
  default     = "newsnow-image-resizer"
}

variable "environment" {
  description = "Entorno de despliegue (e.g., dev, staging, prod)."
  type        = string
  default     = "dev"
}

variable "dynamodb_table_name" {
  description = "Nombre de la tabla DynamoDB para las tareas."
  type        = string
  default     = "ImageTasks"
}