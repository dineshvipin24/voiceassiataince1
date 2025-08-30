@echo off
echo 🔨 Compiling Voice Assistant...

rem Set classpath with all required JAR files
set CLASSPATH=lib\jsapi.jar;lib\freetts.jar;lib\freetts-jsapi10.jar;lib\cmu_us_kal.jar;lib\en_us.jar;lib\cmudict04.jar;lib\cmulex.jar;lib\cmutimelex.jar;lib\cmu_time_awb.jar;lib\mbrola.jar;.

rem Compile Java files
javac -cp "%CLASSPATH%" *.java

if %ERRORLEVEL% EQU 0 (
    echo ✅ Compilation successful!
    echo 🚀 Run with: java -cp "%CLASSPATH%" Main
) else (
    echo ❌ Compilation failed!
    pause
)
