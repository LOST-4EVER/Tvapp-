@rem
@rem Gradle wrapper script for Windows
@rem
@if "%DEBUG%"=="" @echo off
@if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.

where gradle >nul 2>nul
if %ERRORLEVEL% equ 0 (
    gradle %*
    exit /b %ERRORLEVEL%
)

if exist "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" (
    java -classpath "%DIRNAME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
    exit /b %ERRORLEVEL%
)

echo Error: Neither Gradle CLI nor gradle-wrapper.jar was found.
exit /b 1
