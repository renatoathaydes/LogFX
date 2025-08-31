@echo off

set DIR=%~dp0

set VM_OPTIONS=%VM_OPTIONS% -Xms32m -XX:+UseSerialGC
start "%DIR%java" -splash:"%DIR%logfx-logo.png" %VM_OPTIONS% -m com.athaydes.logfx/com.athaydes.logfx.LogFX %*
exit
