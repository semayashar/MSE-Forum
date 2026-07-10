# Forum API

Backend service for a simple forum. The application exposes REST endpoints for users, authentication, topics, replies, health checks, and maintenance mode.

## Local Run

The app requires PostgreSQL and a JWT secret.

PowerShell example:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:JWT_SECRET = "change-me-to-a-long-random-secret-at-least-32-chars"
$env:JWT_EXPIRATION_MS = "86400000"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/forum_app"
$env:SPRING_DATASOURCE_USERNAME = "admin"
$env:SPRING_DATASOURCE_PASSWORD = "admin"
$env:SERVER_PORT = "9001"
```

Start from VS Code with the `ForumApplication` launch configuration, or run:

```powershell
.\mvnw.cmd spring-boot:run
```

## Docker Compose

Start PostgreSQL, the backend API, and Adminer:

```powershell
docker compose up --build
```

Useful URLs:

```text
Backend: http://localhost:9001
Health: http://localhost:9001/readyz
Adminer: http://localhost:18080
Frontend placeholder: http://localhost:14200
```

Adminer login values:

```text
System: PostgreSQL
Server: postgres
Username: forum_app
Password: forum_app_password
Database: forum
```

The backend runs on port 9000 inside the container and is published on localhost:9001.
It connects to PostgreSQL through the Docker Compose service name:

```text
jdbc:postgresql://postgres:5432/forum
```

Start the optional frontend placeholder too:

```powershell
docker compose --profile frontend up --build
```

Stop the stack:

```powershell
docker compose down
```

## Main Flow

Register:

```http
POST /users
```

Login:

```http
POST /auth/login
```

Create topic:

```http
POST /posts
```

Open topic with replies:

```http
GET /posts/{id}?page=0&size=10
```

Create reply:

```http
POST /posts/{postId}/replies
```

Edit topic or reply:

```http
PUT /posts/{id}
PUT /replies/{id}
```

## Roles

- `ADMIN` can manage users and edit all forum content.
- `MODERATOR` can edit all topics and replies.
- `USER` can create topics and replies, and edit only their own content.

## Health

```http
GET /livez
GET /readyz
GET /actuator/health
```
