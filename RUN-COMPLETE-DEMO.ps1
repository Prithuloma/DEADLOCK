# ========================================
# Complete Deadlock Detection Demo Flow
# ========================================

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "   DEADLOCK DETECTION & RESOLUTION DEMO" -ForegroundColor Yellow
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# Step 1: Start Backend Server
Write-Host "[STEP 1/4] Starting Backend Server..." -ForegroundColor Green
Write-Host "          Opening new window for backend..." -ForegroundColor Gray
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'c:\Users\Chaitanya\Downloads\deadlock (2)\deadlock_dead\backend' ; Write-Host 'Starting Spring Boot Application...' -ForegroundColor Yellow ; java -jar target\deadlock-detector-1.0.0.jar"

Write-Host "          Waiting 15 seconds for backend to start..." -ForegroundColor Gray
Start-Sleep -Seconds 15

# Step 2: Open Dashboard
Write-Host "[STEP 2/4] Opening Dashboard..." -ForegroundColor Green  
Write-Host "          URL: http://localhost:8080" -ForegroundColor Gray
Start-Process "http://localhost:8080"
Start-Sleep -Seconds 3

# Step 3: Compile Test
Write-Host "[STEP 3/4] Compiling Deadlock Test..." -ForegroundColor Green
Set-Location "c:\Users\Chaitanya\Downloads\deadlock (2)\deadlock_dead\samples"
javac QuickDeadlockTest.java
if ($LASTEXITCODE -eq 0) {
    Write-Host "          ✅ Compilation successful" -ForegroundColor Green
} else {
    Write-Host "          ❌ Compilation failed" -ForegroundColor Red
    exit 1
}

# Step 4: Run Test with JMX
Write-Host "[STEP 4/4] Running Deadlock Test with JMX..." -ForegroundColor Green
Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "   DEMO INSTRUCTIONS" -ForegroundColor Yellow  
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "1. In the dashboard, click 'Select Process'" -ForegroundColor White
Write-Host "2. Choose the Java process on port 9010" -ForegroundColor White
Write-Host "3. Turn ON 'Auto-Resolution' toggle" -ForegroundColor White
Write-Host "4. Watch the following sequence:" -ForegroundColor White
Write-Host "   ⏱️  Deadlock appears (2 threads)" -ForegroundColor Yellow
Write-Host "   📊 Dashboard shows deadlock for 45 seconds" -ForegroundColor Yellow
Write-Host "   🔧 Auto-resolution activates" -ForegroundColor Yellow
Write-Host "   ✅ Deadlock resolved" -ForegroundColor Green
Write-Host "   📈 Check Analytics tab for details" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Starting test now..." -ForegroundColor Green
Write-Host ""

# Run with JMX enabled
java -Dcom.sun.management.jmxremote `
     -Dcom.sun.management.jmxremote.port=9010 `
     -Dcom.sun.management.jmxremote.authenticate=false `
     -Dcom.sun.management.jmxremote.ssl=false `
     -Dcom.sun.management.jmxremote.local.only=true `
     QuickDeadlockTest

Write-Host ""
Write-Host "================================================" -ForegroundColor Cyan
Write-Host "   DEMO COMPLETE!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Backend server is still running in the other window."
Write-Host "You can run this script again to see another demo."
Write-Host ""
