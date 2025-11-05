@echo off
echo ================================================
echo    DEADLOCK DETECTION ^& RESOLUTION DEMO
echo ================================================
echo.
echo This script will:
echo  1. Start the backend server
echo  2. Open the dashboard
echo  3. Run the deadlock test
echo.
echo Press any key to start...
pause >nul

powershell.exe -ExecutionPolicy Bypass -File "RUN-COMPLETE-DEMO.ps1"
