@echo off
echo.
echo 🚀 Java Deadlock Detection Tool - Quick Start
echo ============================================
echo.

:: Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java not found! Please install Java 17+ first.
    echo 📥 Download from: https://adoptium.net/
    pause
    exit /b 1
)

:: Check if Maven is installed
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Maven not found! Please install Maven 3.6+ first.
    echo 📥 Download from: https://maven.apache.org/download.cgi
    pause
    exit /b 1
)

echo ✅ Java and Maven found!
echo.

:: Build the project
echo 🔨 Building the project...
cd backend
mvn clean compile package -q -DskipTests

if %errorlevel% neq 0 (
    echo ❌ Build failed! Check error messages above.
    pause
    exit /b 1
)

echo ✅ Build successful!
echo.

:: Start the application
echo 🚀 Starting Deadlock Detection Tool...
echo.
echo 📊 Dashboard will be available at: http://localhost:8080
echo 🛑 Press Ctrl+C to stop the application
echo.

start "" "http://localhost:8080"
mvn spring-boot:run

pause