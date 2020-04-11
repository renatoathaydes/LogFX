@echo off

set DIR=%~dp0

NEW_VERSION_ZIP=$HOME/.logfx/logfx-update.zip

if exist "%NEW_VERSION_ZIP%" (
  rem "Updating LogFX..."
  "%DIR%java" -m com.athaydes.logfx/com.athaydes.logfx.update.LogFXReplacer > tmp.txt
  set /p TMP_LOCATION=<tmp.txt
  rem "TMP_LOCATION=%TMP_LOCATION%"
  del /q tmp.txt
  for %%d in (%DIR%) do set LOGFX_DIR=%%~dpd
  rem "LOGFX_DIR=%LOGFX_DIR%"
  del /q %LOGFX_DIR%\*
  for /d %%x in (%LOGFX_DIR%\*) do @rd /s /q "%%x"
  robocopy /move /e %TMP_LOCATION% %LOGFX_DIR%
  del /q %NEW_VERSION_ZIP%
  rem "LogFX updated successfully!"
)

set VM_OPTIONS=-splash:%DIR%logfx-logo.png -Xms32m -XX:+UseSerialGC
"%DIR%java" %VM_OPTIONS% -m com.athaydes.logfx/com.athaydes.logfx.LogFX %*
