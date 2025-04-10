# NewsNow Image Resizer API - PoC

## Descripción General

Este proyecto es un Proof of Concept (PoC) para un servicio REST API que permite subir imágenes, redimensionarlas a dimensiones específicas y consultar el estado de la tarea. La imagen procesada se almacena en AWS S3 y se sirve a través de Amazon CloudFront, mientras que los metadatos de la tarea se persisten en AWS DynamoDB.

La aplicación está construida con Java 21, Spring Boot, y sigue principios de Arquitectura Hexagonal. La infraestructura en AWS (Lambda, API Gateway REST API, S3, DynamoDB, Cognito, CloudFront, WAF) está completamente definida y gestionada usando Terraform (Infraestructura como Código - IaC). La API está protegida mediante autenticación JWT utilizando AWS Cognito y seguridad en el borde con AWS WAF.

## Arquitectura

*   **API Gateway (REST API):** Expone los endpoints `/v1/task` (POST) y `/v1/task/{taskId}` (GET). Protegida por AWS WAF y Cognito Authorizer.
*   **AWS Lambda (Java 21):** Ejecuta la lógica de negocio de Spring Boot (empaquetada con Maven Shade).
    *   Recibe solicitudes de API Gateway.
    *   Valida la entrada.
    *   Usa Thumbnailator para redimensionar imágenes (recibidas como Base64 en el cuerpo JSON del POST).
    *   Calcula MD5 del original (a partir de bytes).
    *   Interactúa con DynamoDB para persistir/consultar metadatos de tareas.
    *   Sube la imagen procesada a S3.
*   **AWS S3:** Almacena las imágenes procesadas en un bucket privado (`/processed/` folder).
*   **Amazon CloudFront:** Sirve las imágenes desde el bucket S3 privado de forma segura y eficiente (CDN).
*   **AWS DynamoDB:** Tabla NoSQL para almacenar los metadatos de cada tarea de redimensionamiento (ID, timestamp, MD5, resolución, estado, URL de CloudFront).
*   **AWS Cognito:** Gestiona la autenticación de usuarios. Se requiere un ID Token JWT válido para llamar a la API.
*   **AWS WAF:** Protege la API Gateway con reglas Core de AWS y Rate Limiting basado en IP.
*   **AWS IAM:** Roles y políticas con permisos mínimos para Lambda.
*   **Terraform:** Gestiona toda la creación y actualización de la infraestructura AWS.

## Diagrama de arquitectura


## Prerrequisitos

Antes de empezar, asegúrate de tener instalado:

1.  **Java JDK 21:** Verifica con `java -version`.
2.  **Maven:** Verifica con `mvn -version`.
3.  **AWS CLI:** Verifica con `aws --version`.
4.  **Terraform:** Verifica con `terraform -version`.
5.  **Credenciales AWS:** Configura tus credenciales de AWS localmente (con permisos suficientes para crear los recursos definidos) ejecutando `aws configure`.
6.  **Postman (o similar):** Para enviar solicitudes HTTP a la API.
7.  **Configurar Credenciales AWS:** Ejecuta `aws configure` y proporciona un Access Key ID y Secret Access Key de un usuario IAM con permisos para gestionar los recursos necesarios (S3, DynamoDB, IAM, Cognito, Lambda, API Gateway, CloudFront, WAF). Asegúrate de configurar la región por defecto correcta (ej. `us-east-1`).

## Configuración y Despliegue

Sigue estos pasos para desplegar la aplicación y la infraestructura en tu cuenta de AWS:

1.  **Clonar el Repositorio:**
    ```bash
    git clone <URL_DEL_REPOSITORIO>
    cd <NOMBRE_DEL_DIRECTORIO>
    ```

2.  **Revisar Variables de Terraform:**
    *   Abre el archivo `terraform/variables.tf`.
    *   Revisa los valores por defecto para `aws_region`, `s3_bucket_name`, `dynamodb_table_name`, `project_name`, y `environment`. Ajústalos si es necesario (especialmente la región). El nombre del bucket S3 debe ser globalmente único.


3.  **Empaquetar la Aplicación Java (incluye test):**
    *   Desde el directorio **raíz** del proyecto (el que contiene `pom.xml`), ejecuta:
        ```bash
        mvn clean package
        ```
    *   Esto compilará el código y creará el archivo JAR necesario (ej. `target/image-resizer-api-0.0.1-SNAPSHOT.jar`) usando `maven-shade-plugin`. Verifica que el build sea exitoso (`BUILD SUCCESS`).

4.  **Desplegar Infraestructura con Terraform:**
    *   Navega al directorio `terraform`:
        ```bash
        cd terraform
        ```
    *   Inicializa Terraform (descarga proveedores):
        ```bash
        terraform init -upgrade
        ```
    *   Valida la configuración (opcional):
        ```bash
        terraform validate
        ```
    *   Planifica los cambios (revisa qué se va a crear/modificar):
        ```bash
        terraform plan
        ```
    *   Aplica los cambios para crear los recursos en AWS:
        ```bash
        terraform apply
        ```
        *   Confirma escribiendo `yes` cuando se te solicite.
        *   Espera a que termine (la creación de **CloudFront puede tardar varios minutos**).
    *   **IMPORTANTE:** Al finalizar, Terraform mostrará los **Outputs**. Copia y guarda los siguientes valores, los necesitarás para probar:
        *   `rest_api_invoke_url`: La URL base de tu API desplegada.
        *   `cognito_user_pool_id`: El ID de tu pool de usuarios.
        *   `cognito_app_client_id`: El ID de tu cliente de aplicación Cognito.
        *   `cloudfront_domain_name`: El dominio de tu CDN.

## Creación de Usuario de Prueba (Cognito)

La API requiere autenticación. Necesitas crear un usuario y obtener un token:

1.  **Crear Usuario:** Usa la AWS CLI (reemplaza email y contraseña):
    ```bash
    # Crear usuario inicial (sin contraseña temporal)
    aws cognito-idp admin-create-user \
        --user-pool-id TU_COGNITO_USER_POOL_ID \
        --username usuario@ejemplo.com \
        --user-attributes Name=email,Value=usuario@ejemplo.com Name=email_verified,Value=true \
        --message-action SUPPRESS

    # Establecer contraseña permanente
    aws cognito-idp admin-set-user-password \
        --user-pool-id TU_COGNITO_USER_POOL_ID \
        --username usuario@ejemplo.com \
        --password "TuContraseñaSegura123!" \
        --permanent
    ```
    *(Reemplaza `TU_COGNITO_USER_POOL_ID` con el valor del output de Terraform).*
Si la ejecución fue exitosa no obtendrá un mensaje de éxito.
2.  **Obtener ID Token:** Usa la AWS CLI (reemplaza IDs, email y contraseña):
    ```bash
    aws cognito-idp initiate-auth \
        --auth-flow USER_PASSWORD_AUTH \
        --client-id TU_COGNITO_APP_CLIENT_ID \
        --auth-parameters USERNAME=usuario@ejemplo.com,PASSWORD="TuContraseñaSegura123!"
    ```
    *   De la salida JSON, copia el valor completo del **`IdToken`**.

## Pruebas de la API Desplegada

Usa Postman o `curl` para probar los endpoints. Necesitarás la URL base de la API (`rest_api_invoke_url`) y el `IdToken` obtenido.

**1. Probar POST /v1/task (Crear Tarea)**

*   **Método:** `POST`
*   **URL:** `{URL_BASE}/v1/task` (Ej: `https://abc123xyz.execute-api.us-east-1.amazonaws.com/dev/v1/task`)
*   **Headers:**
    *   `Authorization`: `Bearer {TU_ID_TOKEN}`
    *   `Content-Type`: `application/json`
*   **Body:** (Selecciona `raw` y `JSON`)
    *   Prepara una imagen de prueba (ej. `test.jpg`) y conviértela a Base64.
    *   Pega el siguiente JSON, reemplazando los valores:
        ```json
        {
          "imageData": "PASTE_TU_BASE64_STRING_AQUI",
          "filename": "test.jpg",
          "width": 150,
          "height": 100
        }
        ```
*   **Respuesta Esperada:**
    *   Status: `201 Created`
    *   Body: JSON con `taskId`, `timestamp`, `originalMD5`, `resolution`, y `imageUrl` (apuntando a CloudFront).
    ```json
    {
        "taskId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
        "timestamp": "...",
        "originalMD5": "...",
        "resolution": "150x100",
        "imageUrl": "https://{cloudfront_domain}/processed/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx.jpg"
    }
    ```
*   **Verificación Adicional:** Revisa que la imagen aparezca en S3 (vía CloudFront) y el ítem en DynamoDB.

**2. Probar GET /v1/task/{taskId} (Consultar Tarea)**

*   **Método:** `GET`
*   **URL:** `{URL_BASE}/v1/task/{ID_DE_TAREA_OBTENIDO_DEL_POST}`
*   **Headers:**
    *   `Authorization`: `Bearer {TU_ID_TOKEN}`
*   **Body:** Ninguno.
*   **Respuesta Esperada:**
    *   Status: `200 OK`
    *   Body: El mismo JSON que devolvió la solicitud POST para esa tarea.
*   **Prueba de Error:** Intenta con un UUID inexistente para obtener un `404 Not Found`.

## Limpieza (Destruir Infraestructura)

**¡Importante!** Para evitar costos inesperados, destruye la infraestructura cuando termines de probar.

*   Navega al directorio `terraform`:
    ```bash
    cd terraform
    ```
*   Ejecuta el comando destroy:
    ```bash
    terraform destroy
    ```
*   Confirma escribiendo `yes`. Esto eliminará todos los recursos creados por Terraform (Lambda, API GW, S3, DynamoDB, Cognito, CloudFront, WAF, IAM).

## Consideraciones y Próximos Pasos

*   **Arquitectura Asíncrona:** Para producción, se recomienda una arquitectura asíncrona usando SQS para desacoplar la subida del procesamiento de imágenes.
*   **Seguridad:** Se implementó Cognito, WAF básico y CloudFront. Mejoras adicionales podrían incluir validación de tipos MIME más estricta, políticas de seguridad más granulares, etc.
*   **Observabilidad:** Añadir Tracing con X-Ray y Dashboards/Alarmas en CloudWatch.
*   **CI/CD:** Implementar un pipeline para despliegue automatizado.
