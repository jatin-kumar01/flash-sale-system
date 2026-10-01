# ============================================
# Flash Sale System - Start All Services
# No Docker
# Java 21 + Maven Wrapper
# ============================================

$ROOT = $PSScriptRoot

$envFile = "$ROOT\set-local-env.ps1"
if (Test-Path $envFile) {
    . $envFile
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "   FLASH SALE SYSTEM - STARTING" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# --------------------------------------------
# 1. Install/build common module
# --------------------------------------------

Write-Host "`n[1/4] Building common module..." -ForegroundColor Yellow

Set-Location "$ROOT\common"

& "$ROOT\mvnw.cmd" clean install -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Common module build failed." -ForegroundColor Red
    exit 1
}

Write-Host "Common module built successfully." -ForegroundColor Green


# --------------------------------------------
# 2. Start Eureka Server
# --------------------------------------------

Write-Host "`n[2/4] Starting Eureka Server..." -ForegroundColor Yellow

Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "if (Test-Path '$envFile') { . '$envFile' }; Set-Location '$ROOT\eureka-server'; & '$ROOT\mvnw.cmd' spring-boot:run"
)
 
Write-Host "Waiting for Eureka Server..." -ForegroundColor Yellow
 
$eurekaReady = $false
 
for ($i = 1; $i -le 30; $i++) {
 
    Start-Sleep -Seconds 2
 
    try {
        $connection = Test-NetConnection `
            -ComputerName "localhost" `
            -Port 8761 `
            -WarningAction SilentlyContinue
 
        if ($connection.TcpTestSucceeded) {
            $eurekaReady = $true
            break
        }
    }
    catch {
        # Keep waiting
    }
 
    Write-Host "Waiting... $($i * 2) seconds"
}
 
if ($eurekaReady) {
    Write-Host "Eureka Server is ready." -ForegroundColor Green
}
else {
    Write-Host "WARNING: Eureka did not respond on port 8761." -ForegroundColor Red
    Write-Host "Continuing anyway..." -ForegroundColor Yellow
}
 
 
# --------------------------------------------
# 3. Start Microservices
# --------------------------------------------
 
Write-Host "`n[3/4] Starting microservices..." -ForegroundColor Yellow
 
$services = @(
    "auth-service",
    "product-service",
    "inventory-service",
    "order-service",
    "payment-service",
    "notification-service"
)
 
foreach ($service in $services) {
 
    Write-Host "Starting $service..." -ForegroundColor Cyan
 
    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-Command",
        "if (Test-Path '$envFile') { . '$envFile' }; Set-Location '$ROOT\$service'; & '$ROOT\mvnw.cmd' spring-boot:run"
    )
 
    Start-Sleep -Seconds 3
}
 
 
# --------------------------------------------
# 4. Start API Gateway
# --------------------------------------------
 
Write-Host "`n[4/4] Starting API Gateway..." -ForegroundColor Yellow
 
Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "if (Test-Path '$envFile') { . '$envFile' }; Set-Location '$ROOT\api-gateway'; & '$ROOT\mvnw.cmd' spring-boot:run"
)

Start-Sleep -Seconds 3


# --------------------------------------------
# 5. Start Frontend
# --------------------------------------------

if (Test-Path "$ROOT\frontend\package.json") {

    Write-Host "`nStarting React Frontend..." -ForegroundColor Yellow

    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-Command",
        "Set-Location '$ROOT\frontend'; npm run dev"
    )

    Write-Host "Frontend starting..." -ForegroundColor Green
}
else {
    Write-Host "Frontend package.json not found." -ForegroundColor Red
}


# --------------------------------------------
# Done
# --------------------------------------------

Write-Host "`n==========================================" -ForegroundColor Green
Write-Host "   ALL SERVICES HAVE BEEN STARTED" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green

Write-Host "`nEureka:"
Write-Host "http://localhost:8761"

Write-Host "`nCheck the individual terminal windows for:"
Write-Host "- Eureka Server"
Write-Host "- Auth Service"
Write-Host "- Product Service"
Write-Host "- Inventory Service"
Write-Host "- Order Service"
Write-Host "- Payment Service"
Write-Host "- Notification Service"
Write-Host "- API Gateway"
Write-Host "- React Frontend"

Write-Host "`nPress Ctrl+C here to close this launcher window."