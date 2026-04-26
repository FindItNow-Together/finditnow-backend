This repository is the part of Full stack development project and encapsulates the backend business logic of the web application


Each service in services folder can have its own .env file which should include the required environment variables as a key value pairs.
.env.example file should mention the required keys for the services to work properly.

After cloning the repository, create the required .env file at each directory location wherever the .env.example file is present, which should include the local urls such as postgres db url, etc


To run any service, use this command: gradlew.bat :services:[service-name]:[run-command]
ex. To run auth-service: gradlew.bat :services:auth:run
ex. To run user-service: gradlew.bat :services:user-service:bootRun


To Do
Containerization of backend as a combined service, with individual service containers


To create new service using gradle task use following command:
windows(cmd): gradlew.bat createService -PserviceName=<service-name>
linux terminal: gradlew createService -PserviceName=<service-name>
This creates a new service inside the services folder with bare minimum folder structure and gradle build, may need borrowing of
gradle build information from existing service gradle build files.

Development:
1. For local development and testing, use localhost for db, redis and grpc hosts inside the respective .envs.
2. For production integration use respective service names (keys as per docker-compose.prod.yml) in the db, redis, and grpc hosts.

---

## 🚀 Deployment & Production

The production environment is hosted on an Oracle Cloud VPS using Docker Compose and managed via GitHub Actions.

### 🛰️ CI/CD Pipeline
- **Automation**: Managed via `.github/workflows/deploy.yml`.
- **Trigger**: Manual trigger (`workflow_dispatch`). Navigate to **GitHub Actions** -> **Build and Deploy** -> **Run workflow**.
- **Images**: Automatically built and pushed to **GitHub Container Registry (GHCR)**.

### 🏗️ Production Setup
1. **Server Provisioning**: Use `infra/setup-server.sh` to install Docker, Nginx, and Certbot on a fresh Ubuntu VPS.
2. **Environment**: Maintain a single `.env` file in the root directory on the server.
3. **Database**: PostgreSQL is initialized automatically using scripts in `infra/postgres/init/`.
4. **Networking**: Nginx acts as a Reverse Proxy (Port 80/443) routing to internal microservices.

### 🔐 SSL & Domain
- **Domain**: Point your domain (e.g., via DuckDNS) to the VPS IP.
- **Certificate**: Use Certbot on the host:
  ```bash
  sudo certbot certonly --standalone -d your-domain.com
  ```
- **Nginx**: Certificates are mounted into the Nginx container from the host `/etc/letsencrypt` folder.

### 🩺 Maintenance
- **View Logs**: `docker compose -f docker-compose.prod.yml logs -f`
- **Check Status**: `docker compose -f docker-compose.prod.yml ps`
- **Update Single Service**:
  ```bash
  docker compose -f docker-compose.prod.yml pull <service_name>
  docker compose -f docker-compose.prod.yml up -d <service_name>
  ```