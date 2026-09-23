@echo off
chcp 65001 >nul
title OCR Studio - FinQuery
cd /d "%~dp0"
echo.
echo   Dang khoi dong OCR Studio...
echo.
start "" http://localhost:5179
node server.mjs
pause
