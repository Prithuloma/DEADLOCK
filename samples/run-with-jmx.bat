@echo off
echo ========================================
echo Starting SimpleDeadlock with JMX Enabled
echo ========================================
echo.
echo This fixes the "management-agent.jar" error!
echo.

java -Dcom.sun.management.jmxremote ^
     -Dcom.sun.management.jmxremote.port=0 ^
     -Dcom.sun.management.jmxremote.authenticate=false ^
     -Dcom.sun.management.jmxremote.ssl=false ^
     SimpleDeadlock

pause
