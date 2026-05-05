
param(
    [Parameter(Mandatory=$true)]
    [string]$PgPassword
)

$env:PATH += ";C:\Program Files\PostgreSQL\17\bin"
$env:PGPASSWORD = $PgPassword

Write-Host "Testing connection to PostgreSQL..." -ForegroundColor Yellow
psql -U postgres -c "SELECT version();" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Could not connect to PostgreSQL with provided password." -ForegroundColor Red
    exit 1
}

Write-Host "Creating ims_user..." -ForegroundColor Yellow
psql -U postgres -c "DO `$`$ BEGIN IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'ims_user') THEN CREATE ROLE ims_user LOGIN PASSWORD 'ims_pass'; END IF; END `$`$;" 2>&1

Write-Host "Creating ims_db database..." -ForegroundColor Yellow
psql -U postgres -c "SELECT 1 FROM pg_database WHERE datname='ims_db'" | findstr "1 row" 2>&1
psql -U postgres -c "CREATE DATABASE ims_db OWNER ims_user;" 2>&1

Write-Host "Granting privileges..." -ForegroundColor Yellow
psql -U postgres -d ims_db -c "GRANT ALL PRIVILEGES ON DATABASE ims_db TO ims_user;" 2>&1
psql -U postgres -d ims_db -c "GRANT ALL ON SCHEMA public TO ims_user;" 2>&1

Write-Host ""
Write-Host " Database setup complete!" -ForegroundColor Green
Write-Host "   DB:   ims_db"
Write-Host "   User: ims_user"
Write-Host "   Pass: ims_pass"
Write-Host ""
Write-Host "Now run the backend: cd backend && mvn spring-boot:run" -ForegroundColor Cyan
