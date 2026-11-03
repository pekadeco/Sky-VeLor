@echo off
setlocal EnableExtensions EnableDelayedExpansion
for %%I in ("%~dp0..") do set "ROOT=%%~fI"
set "CLASSES=%ROOT%\build\classes\main"
set "RESOURCES=%ROOT%\build\resources\main"
set "CP_FILE=%ROOT%\build\test-classpath.txt"
set "OUT=%TEMP%\lunaria-tests-%RANDOM%-%RANDOM%"

if not exist "%CLASSES%" (
    echo Main classes not found. Run gradlew.bat build dumpTestClasspath first.
    exit /b 1
)
if not exist "%RESOURCES%" (
    echo Main resources not found. Run gradlew.bat build dumpTestClasspath first.
    exit /b 1
)
if not exist "%CP_FILE%" (
    echo Test classpath not found. Run gradlew.bat build dumpTestClasspath first.
    exit /b 1
)
set /p "TEST_CP="<"%CP_FILE%"
if not defined TEST_CP (
    echo Test classpath is empty: %CP_FILE%
    exit /b 1
)
mkdir "%OUT%" || exit /b 1

for /r "%ROOT%\tests" %%F in (*.java) do echo "%%F" >> "%OUT%\sources.txt"
javac -nowarn -encoding UTF-8 -cp "%CLASSES%;%RESOURCES%;%TEST_CP%" -d "%OUT%" @"%OUT%\sources.txt"
if errorlevel 1 goto fail

set "CP=%OUT%;%CLASSES%;%RESOURCES%;%TEST_CP%"
set /a COUNT=0
for /r "%ROOT%\tests" %%F in (*.java) do (
    findstr /C:"public static void main" "%%F" >nul
    if not errorlevel 1 (
        set "REL=%%~fF"
        set "REL=!REL:%ROOT%\tests\=!"
        set "CLASS=!REL:.java=!"
        set "CLASS=!CLASS:\=.!"
        echo === !CLASS! ===
        java -cp "%CP%" !CLASS!
        if errorlevel 1 goto fail
        set /a COUNT+=1
    )
)

echo All Lunaria standalone tests passed (!COUNT! suites).
rmdir /s /q "%OUT%"
exit /b 0

:fail
echo Tests failed. Temporary output kept at "%OUT%" for diagnosis.
exit /b 1
