@echo off
title Certificate Service Demo
echo =================================================================
echo Starting Certificate Service Demo...
echo =================================================================
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0demo.ps1"
echo.
pause
