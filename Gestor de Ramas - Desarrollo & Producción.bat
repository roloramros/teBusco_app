@echo off
chcp 65001 >nul 2>&1
setlocal
title Gestor de Ramas - Desarrollo & Producción

:MENU
cls
echo ========================================
echo    🌿 GESTOR DE RAMAS (GIT)
echo ========================================
echo  1. 📤 Subir cambios a Desarrollo
echo  2. 🚀 Publicar en Producción (Merge desde Desarrollo)
echo  3. ❌ Salir
echo ========================================
set /p "opcion=Elige una opción [1-3]: "

if "%opcion%"=="1" goto SUBIR_DESARROLLO
if "%opcion%"=="2" goto PUBLICAR_PRODUCCION
if "%opcion%"=="3" goto FIN
echo ⚠️ Opción no válida.
pause >nul
goto MENU

:SUBIR_DESARROLLO
cls
set /p "mensaje=Escribe el mensaje del commit: "
if "%mensaje%"=="" (
    echo ❌ El mensaje no puede estar vacío.
    pause
    goto MENU
)

cd /d "%~dp0"
set "BRANCH=Desarrollo"

echo 🔄 Preparando rama '%BRANCH%'...
git checkout %BRANCH% 2>nul
if errorlevel 1 (
    echo 🌿 La rama '%BRANCH%' no existe. Creándola...
    git checkout -b %BRANCH%
)

echo 📥 Sincronizando con remoto...
git fetch origin >nul 2>&1
git pull origin %BRANCH% >nul 2>&1

echo 📦 Agregando cambios...
git add .
echo 💾 Creando commit...
git commit -m "%mensaje%"
if errorlevel 1 (
    echo ⚠️ No hay cambios pendientes o hubo un error. Revisa la consola.
    pause
    goto MENU
)

echo 🚀 Subiendo a remoto...
git push -u origin %BRANCH%
echo.
echo ✅ Cambios enviados correctamente a '%BRANCH%'.
pause
goto MENU

:PUBLICAR_PRODUCCION
cls
cd /d "%~dp0"
set "BRANCH=Produccion"

echo 🔄 Preparando rama '%BRANCH%'...
git checkout %BRANCH% 2>nul
if errorlevel 1 (
    echo 🌿 Rama '%BRANCH%' no encontrada. Creándola...
    git checkout -b %BRANCH%
)

echo 📥 Sincronizando con remoto...
git fetch origin >nul 2>&1
git pull origin %BRANCH% >nul 2>&1

echo 🔀 Fusionando Desarrollo → Produccion...
git branch --list Desarrollo >nul 2>&1
if errorlevel 1 (
    echo ❌ No existe la rama local 'Desarrollo'. Trabaja en ella primero.
    pause
    goto MENU
)

git merge Desarrollo --no-ff -m "📦 Actualización estable desde Desarrollo"
if errorlevel 1 (
    echo ⚠️ Conflicto al fusionar. Resuélvelo manualmente.
    echo    Después ejecuta: git add . ^& git commit ^& git push
    pause
    goto MENU
)

echo 🚀 Publicando en Produccion...
git push -u origin %BRANCH%
echo.
echo ✅ Producción actualizada con la última versión estable.
pause
goto MENU

:FIN
exit /b