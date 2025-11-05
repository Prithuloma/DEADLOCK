@echo off
cls
echo.
echo ========================================================
echo     DEADLOCK DETECTION - COMPLETE SYSTEM TEST
echo ========================================================
echo.
echo This test will demonstrate the COMPLETE lifecycle:
echo.
echo  [0s]     Deadlock forms
echo  [0.5s]   Backend detects deadlock
echo  [0-45s]  Monitoring period (view visualization)
echo  [45s]    Test self-resolves
echo  [47s]    Backend detects resolution
echo  [48s]    Test completes successfully
echo.
echo ========================================================
echo.
echo PREREQUISITES:
echo  1. Backend server must be running on port 8080
echo  2. Dashboard open at http://localhost:8080
echo  3. Select process and enable auto-resolution
echo.
echo Press any key when ready...
pause >nul

echo.
echo Starting test...
echo.

java -Dcom.sun.management.jmxremote ^
     -Dcom.sun.management.jmxremote.port=9010 ^
     -Dcom.sun.management.jmxremote.authenticate=false ^
     -Dcom.sun.management.jmxremote.ssl=false ^
     -Dcom.sun.management.jmxremote.local.only=true ^
     QuickDeadlockTest

echo.
echo ========================================================
echo     TEST EXECUTION SUMMARY
echo ========================================================
echo.
echo What you should have seen:
echo  * Deadlock detected (2 threads)
echo  * Monitored for 45 seconds
echo  * Auto-resolution activated
echo  * Deadlock resolved
echo  * Test completed successfully
echo.
echo Check the dashboard:
echo  * Activity timeline shows "Deadlock Resolved"
echo  * Metrics show 0 deadlocked threads
echo  * Export report contains resolution details
echo.
echo ========================================================
echo.
echo Program stopped automatically after 48 seconds.
echo Backend is still running - you can run the test again.
echo.
