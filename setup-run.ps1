function Ensure-Database($ContainerId, $DbUser, $DbName)
{
    Write-Host ">>> Checking database '$DbName'"

    $exists = docker exec $ContainerId psql `
    -U $DbUser `
    -d postgres `
    -tAc "SELECT 1 FROM pg_database WHERE datname='$DbName'" 2> $null

    if ($exists -eq "1")
    {
        Write-Host "    Database '$DbName' already exists."
        return
    }

    Write-Host "    Database '$DbName' missing. Creating..."

    docker exec $ContainerId psql -U $DbUser -d postgres -c "CREATE DATABASE $DbName;"


    if ($LASTEXITCODE -eq 0)
    {
        Write-Host "    Database '$DbName' created."
    }
    else
    {
        Write-Host "!! Failed to create database '$DbName'"
        exit 1
    }
}

WRITE-HOST "*****finditnow:setup-run*****"

$ScriptPath = $MyInvocation.MyCommand.Path
$ROOT_DIR = Split-Path -Parent (Resolve-Path $ScriptPath)
$INFRA_DIR = "$ROOT_DIR\infra"
$DATA_DIR = "$INFRA_DIR\data"

# Load .env
$EnvFile = "$ROOT_DIR\.env"

Write-Host "TEST_FILE: $EnvFile"
Write-Host "Test-Path says: $( Test-Path $EnvFile )"

if (Test-Path $EnvFile)
{
    Write-Host "> Loading root .env"
    Get-Content $EnvFile | ForEach-Object {
        if ($_ -match "^\s*#")
        {
            return
        }
        if ($_ -match "^\s*$")
        {
            return
        }
        $parts = $_.Split("=", 2)
        $name = $parts[0].Trim()
        $value = $parts[1].Trim()
        Set-Item -Path env:$name -Value $value
    }
}

Write-Host ">>>Ensuring container persistence volume"
New-Item -ItemType Directory -Force -Path "$DATA_DIR\postgres" | Out-Null
New-Item -ItemType Directory -Force -Path "$DATA_DIR\redis" | Out-Null
New-Item -ItemType Directory -Force -Path "$INFRA_DIR\nginx" | Out-Null

Write-Host ">>>Starting infra containers"
cd $INFRA_DIR

docker compose up -d

Write-Host ">>>Waiting for postgres"
for ($i = 0; $i -lt 20; $i++) {
    $pg = docker compose ps -q finditnow-postgres
    $ready = docker exec $pg pg_isready -U $env:DB_USER 2> $null
    if ($LASTEXITCODE -eq 0)
    {
        break
    }
    Start-Sleep -Seconds 1
}

if ($LASTEXITCODE -ne 0)
{
    Write-Host "!! Postgres timeout"; exit 1
}

# Get container ID
$pg = docker compose ps -q finditnow-postgres

# DB user (fallback to devuser)
$dbUser = if ($env:DB_USER)
{
    $env:DB_USER
}
else
{
    "devuser"
}

# List of DBs to ensure (one per microservice)
$ServiceDatabases = @(
    "auth_service",
    "user_service",
    "shop_service",
    "order_service",
    "delivery_db"
)

foreach ($db in $ServiceDatabases)
{
    Ensure-Database -ContainerId $pg -DbUser $dbUser -DbName $db
}

# Wait for Redis
Write-Host ">>>Waiting for Redis..."
for ($i = 0; $i -lt 20; $i++) {
    $rd = docker compose ps -q finditnow-redis
    $ping = docker exec $rd redis-cli ping 2> $null
    if ($ping -eq "PONG")
    {
        break
    }
    Start-Sleep -Seconds 1
}
if ($ping -ne "PONG")
{
    Write-Host "!! Redis timeout"; exit 1
}

# --- RUN MICROSERVICES (Windows style — new CMD windows) ---
Write-Host ">>>Starting microservices in new CMD windows..."

cd $ROOT_DIR

# Check if Windows Terminal is available
$wtPath = Get-Command wt.exe -ErrorAction SilentlyContinue

if ($wtPath) {
    # Windows Terminal command with multiple tabs
    wt.exe `
        new-tab --title "Auth Service" -d "$ROOT_DIR" cmd.exe /k "gradlew.bat :services:auth:run" `; `
        new-tab --title "User Service" -d "$ROOT_DIR" cmd.exe /k "gradlew.bat :services:user-service:bootRun" `; `
        new-tab --title "Shop Service" -d "$ROOT_DIR" cmd.exe /k "gradlew.bat :services:shop-service:bootRun" `; `
        new-tab --title "Order Service" -d "$ROOT_DIR" cmd.exe /k "gradlew.bat :services:order-service:bootRun" `; `
        new-tab --title "File Gateway" -d "$ROOT_DIR" cmd.exe /k "gradlew.bat :services:file-gateway:run" `; `
        new-tab --title "Delivery Service" -d "$ROOT_DIR" cmd.exe /k "gradlew.bat :services:delivery-service:bootRun"

    Write-Host ">>>Opened Windows Terminal with service tabs."
} else {
    Write-Host "!! Windows Terminal (wt.exe) not found. Falling back to separate CMD windows..."
    Start-Process cmd.exe -ArgumentList "/k gradlew.bat :services:auth:run"
    Start-Process cmd.exe -ArgumentList "/k gradlew.bat :services:user-service:bootRun"
    Start-Process cmd.exe -ArgumentList "/k gradlew.bat :services:shop-service:bootRun"
    Start-Process cmd.exe -ArgumentList "/k gradlew.bat :services:order-service:bootRun"
    Start-Process cmd.exe -ArgumentList "/k gradlew.bat :services:file-gateway:run"
    Write-Host ">>>Opened separate CMD windows for services."
}

Write-Host ""
Write-Host "===[ Everything running ]==="