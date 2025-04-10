# Control de Acceso de Origen (OAC) para permitir a CloudFront acceder a S3 privado
resource "aws_cloudfront_origin_access_control" "oac" {
  name                              = "${var.project_name}-s3-oac-${var.environment}"
  description                       = "OAC for ${var.project_name} S3 bucket"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

# Distribución de CloudFront
resource "aws_cloudfront_distribution" "s3_distribution" {
  enabled             = true
  comment             = "CDN for ${var.project_name} processed images"

  # Origen principal (nuestro bucket S3)
  origin {
    domain_name              = aws_s3_bucket.image_bucket.bucket_regional_domain_name
    origin_id                = "S3-${var.s3_bucket_name}"
    origin_access_control_id = aws_cloudfront_origin_access_control.oac.id
  }

  # Comportamiento de Caché por Defecto (cómo maneja las solicitudes)
  default_cache_behavior {
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "S3-${var.s3_bucket_name}"

    # Política de caché (usaremos una gestionada por AWS optimizada para S3)
    cache_policy_id        = "658327ea-f89d-4fab-a63d-7e88639e58f6"
    viewer_protocol_policy = "redirect-to-https"
    compress               = true
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
      locations        = []
    }
  }

  # Configuración del Viewer (navegador/cliente)
  viewer_certificate {
    cloudfront_default_certificate = true
  }
  price_class = "PriceClass_100" # Empezar con el más barato para el PoC

  tags = {
    Name        = "${var.project_name}-cdn-${var.environment}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }

  # Esperar a que el OAC exista
  depends_on = [aws_cloudfront_origin_access_control.oac]
}

# Política de Bucket S3 para permitir acceso DESDE CloudFront vía OAC
data "aws_iam_policy_document" "s3_bucket_policy_for_cloudfront" {
  statement {
    sid       = "AllowCloudFrontServicePrincipalReadOnly"
    effect    = "Allow"
    actions   = ["s3:GetObject"] # Solo permiso de lectura
    resources = ["${aws_s3_bucket.image_bucket.arn}/*"]

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    # Condición para asegurar que la solicitud viene de NUESTRA distribución específica
    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.s3_distribution.arn]
    }
  }
}

# Aplicar la política al bucket S3
resource "aws_s3_bucket_policy" "allow_cloudfront_access" {
  bucket = aws_s3_bucket.image_bucket.id
  policy = data.aws_iam_policy_document.s3_bucket_policy_for_cloudfront.json
}


# Output del nombre de dominio de CloudFront
output "cloudfront_domain_name" {
  description = "El nombre de dominio de la distribución de CloudFront."
  value       = aws_cloudfront_distribution.s3_distribution.domain_name
}