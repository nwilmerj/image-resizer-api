resource "aws_s3_bucket" "image_bucket" {
  # Nombre del bucket - debe ser globalmente único
  bucket = var.s3_bucket_name

  # Buenas prácticas de seguridad: Bloquear acceso público
  force_destroy = true # Poner a true solo si necesitas borrar buckets con contenido fácilmente (¡cuidado!)

  tags = {
    Name        = "${var.project_name}-images-${var.environment}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}

# Configuración de bloqueo de acceso público (Recomendado)
resource "aws_s3_bucket_public_access_block" "image_bucket_pab" {
  bucket = aws_s3_bucket.image_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Configuración de propiedad de objetos (Recomendado para control total)
resource "aws_s3_bucket_ownership_controls" "image_bucket_oc" {
  bucket = aws_s3_bucket.image_bucket.id
  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

# Habilitar cifrado SSE-S3 por defecto (Recomendado)
resource "aws_s3_bucket_server_side_encryption_configuration" "image_bucket_sse" {
  bucket = aws_s3_bucket.image_bucket.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

output "s3_bucket_id" {
  description = "El ID (nombre) del bucket S3 creado."
  value       = aws_s3_bucket.image_bucket.id
}

output "s3_bucket_arn" {
  description = "El ARN (Amazon Resource Name) del bucket S3 creado."
  value       = aws_s3_bucket.image_bucket.arn
}