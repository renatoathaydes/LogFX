@echo off

set DIR=%~dp0

set VM_OPTIONS=%VM_OPTIONS% -Djavafx.preloader=com.athaydes.logfx.SplashPreloader -Xms32m
start "%DIR%java" %VM_OPTIONS% -m com.athaydes.logfx/com.athaydes.logfx.LogFX %*
exit
