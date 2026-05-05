
$apiUrl = "http://localhost:8080/api/v1/signals/ingest/batch"
$jsonPath = Join-Path $PSScriptRoot "mock_cascading_failure.json"

if (-Not (Test-Path $jsonPath)) {
    Write-Host "Error: Cannot find $jsonPath" -ForegroundColor Red
    exit 1
}

Write-Host "Simulating cascading failure (RDBMS -> MCP -> Workers -> API)..." -ForegroundColor Cyan
Write-Host "Sending batch signals to $apiUrl" -ForegroundColor Gray

$jsonData = Get-Content -Raw -Path $jsonPath

try {
    # Send HTTP POST request
    $response = Invoke-RestMethod -Uri $apiUrl -Method Post -Body $jsonData -ContentType "application/json"
    
    Write-Host "`nSuccess! Incident created." -ForegroundColor Green
    Write-Host "Response from backend:" -ForegroundColor Yellow
    $response | ConvertTo-Json -Depth 3 | Write-Host
} catch {
    Write-Host "`nFailed to send signals. Is the backend running at localhost:8080?" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}
