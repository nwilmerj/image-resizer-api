# Crear la WebACL de WAF (Regional para API Gateway)
resource "aws_wafv2_web_acl" "api_waf_acl" {
  name        = "${var.project_name}-web-acl-${var.environment}"
  description = "WAF WebACL for ${var.project_name} REST API"
  scope       = "REGIONAL"

  lifecycle {
    create_before_destroy = true
  }

  default_action {
    allow {}
  }

  # REGLA 1: Rate Limiting Basado en IP
  rule {
    name     = "IPRateLimitRule"
    priority = 1

    action {
      block {}
    }

    statement {
      rate_based_statement {
        limit              = 1000
        aggregate_key_type = "IP"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${var.project_name}WafRateLimitMetric${var.environment}"
      sampled_requests_enabled   = true
    }
  }

  # REGLA 2: AWS Managed Core Rule Set
  rule {
    name     = "AWSManagedCoreRuleSet"
    priority = 10

    override_action {
    none {} # Respetan las acciones por defecto de AWS Managed Rules
  }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesCommonRuleSet"
        vendor_name = "AWS"
        rule_action_override {
          name = "SizeRestrictions_BODY"
          action_to_use {
            count {}
          }
        }
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${var.project_name}WafCoreMetric${var.environment}"
      sampled_requests_enabled   = true
    }
  }

  visibility_config {
    cloudwatch_metrics_enabled = true
    metric_name                = "${var.project_name}WafMetric${var.environment}"
    sampled_requests_enabled   = true
  }

  tags = {
    Name        = "${var.project_name}-web-acl-${var.environment}"
    Environment = var.environment
    Project     = var.project_name
    ManagedBy   = "Terraform"
  }
}


# Asociar la WebACL creada con el Stage de nuestra REST API
resource "aws_wafv2_web_acl_association" "api_waf_association" {
  resource_arn = aws_api_gateway_stage.api_stage.arn
  web_acl_arn  = aws_wafv2_web_acl.api_waf_acl.arn
}

