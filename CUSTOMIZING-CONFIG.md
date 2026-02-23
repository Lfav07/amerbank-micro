# Customizing Configuration

This guide explains how to fork and customize the configuration repository for your own deployment of Amerbank.

## Overview

The Amerbank microservices rely on a centralized configuration repository stored on GitHub. To customize configurations
for your own deployment, you'll need to fork the repository and update the config-server to point to your fork.

## Step 1: Fork the Configuration Repository

1. Go to the configuration repository: [Lfav07/amerbank-config](https://github.com/Lfav07/amerbank-config)
2. Click the **Fork** button in the top-right corner
3. Select your GitHub account as the destination
4. Wait for the fork to complete

## Step 2: Clone Your Fork

```bash
git clone git@github.com:YOUR_USERNAME/amerbank-config.git
cd amerbank-config
```

## Step 3: Understand the Configuration Structure

The repository has the following structure:

```
amerbank-config/
└── configurations/
    ├── gateway.yaml           # API Gateway configuration
    ├── auth-server.yaml      # Authentication service configuration
    ├── customer.yaml         # Customer service configuration
    ├── account.yaml          # Account service configuration
    ├── transaction.yaml      # Transaction service configuration
    ├── discovery.yaml        # Service discovery configuration
    └── config-server.yaml    # Config server configuration
```

### Configuration Files

| File                 | Service                    | Description                       |
|----------------------|----------------------------|-----------------------------------|
| `gateway.yaml`       | Gateway (8080)             | Routing, filters, port settings   |
| `auth-server.yaml`   | Auth Server (8081)         | JWT settings, token expiration    |
| `customer.yaml`      | Customer Service (8082)    | Database, Redis settings          |
| `account.yaml`       | Account Service (8083)     | Database, Redis, account settings |
| `transaction.yaml`   | Transaction Service (8084) | Database, account service URL     |
| `discovery.yaml`     | Discovery Service (8761)   | Eureka settings                   |
| `config-server.yaml` | Config Server (8888)       | Git repository settings           |

## Step 4: Customize Configuration

Open any configuration file and modify the values as needed.

### Common Customizations

#### Database Credentials

```yaml
# Example: account.yaml
spring:
  datasource:
    username: your_db_username
    password: your_db_password
```

#### JWT Secret

```yaml
# Example: auth-server.yaml
jwt:
  secret: your_256_bit_minimum_secret_key
```

#### Service URLs

```yaml
# Example: transaction.yaml
account-service:
  base-url: http://account-service:8083
```

## Step 5: Commit and Push Changes

```bash
git add .
git commit -m "Customize configuration for my deployment"
git push origin main
```

## Step 6: Update Config Server to Use Your Fork

### Option A: Update Docker Compose

Edit `docker-compose.yml` in the config-server service:

```yaml
services:
  config-server:
    environment:
      - SPRING_CLOUD_CONFIG_SERVER_GIT_URI=git@github.com:YOUR_USERNAME/amerbank-config.git
```

### Option B: Update Application Configuration

Edit `services/config-server/src/main/resources/application.yaml`:

```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: git@github.com:YOUR_USERNAME/amerbank-config.git
```

## Step 7: Rebuild and Restart

```bash
# Rebuild the config-server with your changes
docker-compose build config-server
docker-compose up -d config-server
```

## Using Profiles

You can create environment-specific configurations using Spring profiles.

### Creating a Development Profile

1. Create a new file: `configurations/account-dev.yaml`
2. Add your development-specific settings:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/amerbank_dev
    username: dev_user
    password: dev_password
```
Run the service with the dev profile:

```bash
docker-compose run -e SPRING_PROFILES_ACTIVE=dev account-service
```

### Configuration Precedence

Spring Cloud Config loads configurations in the following order (highest to lowest):

1. Environment-specific file (`application-{profile}.yaml`)
2. Default file (`application.yaml`)
3. Command-line arguments
4. Environment variables

## Security: Encrypting Sensitive Values

The config server supports encrypted configuration values.

### 1. Set Encryption Key

In `config-server.yaml`:

```yaml
encrypt:
  key: your-encryption-key
```

### 2. Encrypt a Value

Use the config server's encrypt endpoint:

```bash
curl -X POST http://localhost:8888/encrypt -d "my-secret-password"
```

Copy the encrypted output (starts with `{cipher}`).

### 3. Use Encrypted Value

```yaml
spring:
  datasource:
    password: '{cipher}ABCD1234...'
```

## Troubleshooting

### Config Server Not Connecting to GitHub

- Verify your SSH key is added to GitHub
- Check the repository URL is correct
- Ensure the private key has read access to the repository

### Changes Not Reflecting

- Restart the microservices to fetch new configuration
- Use `/actuator/refresh` endpoint to reload without restart:

```bash
curl -X POST http://localhost:8080/actuator/refresh
```
