$pgBin = "C:\Program Files\PostgreSQL\17\bin\psql.exe"

Write-Host "=== Setting up IMS PostgreSQL Database ===" -ForegroundColor Cyan

# Create user if not exists
Write-Host "Creating user ims_user..." -ForegroundColor Yellow
& $pgBin -U postgres -c "DO `$`$ BEGIN IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'ims_user') THEN CREATE ROLE ims_user WITH LOGIN PASSWORD 'ims_pass'; END IF; END `$`$;"

# Create database if not exists
Write-Host "Creating database ims_db..." -ForegroundColor Yellow
& $pgBin -U postgres -c "SELECT 'CREATE DATABASE ims_db OWNER ims_user' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ims_db')" | Out-Null
& $pgBin -U postgres -c "CREATE DATABASE ims_db OWNER ims_user" 2>&1 | ForEach-Object { if ($_ -notmatch "already exists") { Write-Host $_ } }

# Grant privileges
Write-Host "Granting privileges..." -ForegroundColor Yellow
& $pgBin -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE ims_db TO ims_user;"

Write-Host "=== Database setup complete! ===" -ForegroundColor Green
