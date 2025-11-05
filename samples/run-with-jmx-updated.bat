@echo off
echo ========================================
echo Running QuickDeadlockTest with JMX Enabled
echo ========================================
echo.
echo JMX Configuration:
echo - Port: 9010
echo - No Authentication
echo - Local Access Only
echo.
echo Backend can monitor this process at localhost:9010
echo.

java -Dcom.sun.management.jmxremote ^
     -Dcom.sun.management.jmxremote.port=9010 ^
     -Dcom.sun.management.jmxremote.authenticate=false ^
     -Dcom.sun.management.jmxremote.ssl=false ^
     -Dcom.sun.management.jmxremote.local.only=true ^
     QuickDeadlockTest

echo.
echo ========================================
echo Test completed!
echo ========================================
echo.
echo The program has automatically stopped.
echo Backend is still running - you can run the test again.
echo.
