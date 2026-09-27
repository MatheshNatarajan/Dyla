# run.ps1 - Automation script for the Ticket Stampede (Docker Native)

Write-Host "========================================="
Write-Host "1. Building and Starting the Entire Stack"
Write-Host "========================================="
Write-Host "This will pull a Maven image, build the Spring Boot app, and launch Postgres and Redis."
docker-compose up -d --build

Write-Host "`nWaiting 25 seconds for the Spring Boot API and Databases to fully boot..."
Start-Sleep -Seconds 25

Write-Host "`n========================================="
Write-Host "2. Executing the Go Load Client"
Write-Host "========================================="
cd buyer
& "C:\Program Files\Go\bin\go.exe" run main.go

Write-Host "`n========================================="
Write-Host "Test complete!"
Write-Host "To view the Spring Boot logs, run: docker-compose logs -f seller"
Write-Host "To shut down the cluster, run: docker-compose down"
Write-Host "========================================="
