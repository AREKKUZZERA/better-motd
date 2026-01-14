@echo off
setlocal

set DIR=%~dp0
set WRAPPER_JAR=%DIR%gradle\wrapper\gradle-wrapper.jar

if not exist "%WRAPPER_JAR%" (
  echo Gradle Wrapper jar not found: %WRAPPER_JAR%
  exit /b 1
)

java -Dfile.encoding=UTF-8 -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
