@echo off
echo ================================================
echo    TESTING FIXED DEADLOCK TRACKING
echo ================================================
echo.
echo This will test the complete deadlock lifecycle:
echo  1. Deadlock detected
echo  2. Backend monitors external process
echo  3. Test self-resolves after 45 seconds
echo  4. Backend detects resolution
echo  5. Timeline shows success
echo.
echo Make sure backend is running!
echo Press any key to start the test...
pause >nul

echo.
echo Starting test with JMX enabled...
echo.

java -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false QuickDeadlockTest

echo.
echo ================================================
echo    TEST COMPLETE!
echo ================================================
echo.
echo Check the dashboard timeline - you should see:
echo  ✅ "Deadlock Detected"
echo  ✅ "Monitoring external process"  
echo  ✅ "Deadlock Resolved" (after 45 seconds)
echo.
pause
