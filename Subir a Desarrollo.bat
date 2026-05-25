@echo off
setlocal enabledelayedexpansion

set BRANCH=Desarrollo
set /p mensaje=Escribe el mensaje del commit: 

if "%mensaje%"=="" (
    echo ❌ El mensaje no puede estar vacío.
    pause
    exit /b 1
)

cd /d "%~dp0"

echo 🔄 Preparando rama '%BRANCH%'...
REM Intenta cambiar. Si falla (rama inexistente), la crea.
git checkout %BRANCH% 2>nul
if errorlevel 1 (
    echo 🌿 La rama '%BRANCH%' no existe. Creándola...
    git checkout -b %BRANCH%
)

echo 📥 Sincronizando con remoto...
REM Fetch + Pull (oculta avisos esperados en primera ejecución)
git fetch origin %BRANCH% >nul 2>&1
git pull origin %BRANCH% >nul 2>&1

echo 📦 Agregando cambios...
git add .

echo 💾 Creando commit...
git commit -m "%mensaje%"

echo 🚀 Subiendo a remoto...
REM -u vincula esta rama local con la remota para futuros pushes
git push -u origin %BRANCH%

echo.
echo ✅ Listo. Cambios enviados a %BRANCH%.
pause