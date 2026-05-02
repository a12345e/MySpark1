# ==============================
# Elasticsearch 8 SSL Setup (Windows + Docker)
# ==============================

$baseDir = "C:\dockers\elastic\elastic-docker2"
$certDir = "$baseDir\certs"

Write-Host "Creating cert directory..." -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $certDir | Out-Null

# ------------------------------
# Step 1: Generate CA
# ------------------------------
Write-Host "Generating CA certificate..." -ForegroundColor Cyan

docker run --rm -v "${certDir}:/certs" docker.elastic.co/elasticsearch/elasticsearch:8.13.4 `
bash -c "elasticsearch-certutil ca --silent --out /certs/elastic-stack-ca.p12 --pass ''"

# ------------------------------
# Step 2: Verify CA exists
# ------------------------------
if (!(Test-Path "$certDir\elastic-stack-ca.p12")) {
    Write-Host "CA generation failed!" -ForegroundColor Red
    exit 1
}

Write-Host "CA created successfully." -ForegroundColor Green

# ------------------------------
# Step 3: Generate node certificate
# ------------------------------
Write-Host "Generating node certificate..." -ForegroundColor Cyan

docker run --rm -v "${certDir}:/certs" docker.elastic.co/elasticsearch/elasticsearch:8.13.4 `
bash -c "elasticsearch-certutil cert --silent --ca /certs/elastic-stack-ca.p12 --ca-pass '' --out /certs/elastic-certificates.p12 --pass ''"

# ------------------------------
# Step 4: Verify output
# ------------------------------
Write-Host "Verifying output files..." -ForegroundColor Cyan

Get-ChildItem $certDir

if (!(Test-Path "$certDir\elastic-certificates.p12")) {
    Write-Host "Certificate generation failed!" -ForegroundColor Red
    exit 1
}

# ------------------------------
# Step 5: Fix Windows permissions (important for Docker)
# ------------------------------
Write-Host "Fixing permissions..." -ForegroundColor Cyan

icacls $certDir /grant Everyone:F /T | Out-Null

# ------------------------------
# Done
# ------------------------------
Write-Host "`nSUCCESS: Certificates ready for Docker Elasticsearch 8" -ForegroundColor Green
Write-Host "Location: $certDir" -ForegroundColor Yellow