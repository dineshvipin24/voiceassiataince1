@echo off
echo 🚀 Starting Voice Assistant...

rem Set classpath with all required JAR files
set CLASSPATH=lib\jsapi.jar;lib\freetts.jar;lib\freetts-jsapi10.jar;lib\cmu_us_kal.jar;lib\en_us.jar;lib\cmudict04.jar;lib\cmulex.jar;lib\cmutimelex.jar;lib\cmu_time_awb.jar;lib\mbrola.jar;.

rem Run the application
java -cp "%CLASSPATH%" Main

pause
