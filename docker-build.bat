@echo off
REM MoonPhase Docker Build and Deploy Script (Windows)
REM Usage: docker-build.bat [command] [options]

setlocal enabledelayedexpansion

REM Configuration
set IMAGE_NAME=moonphase
set IMAGE_TAG=1.0-SNAPSHOT
set CONTAINER_NAME=moonphase
set REGISTRY=

REM Parse arguments
set COMMAND=%1
if "!COMMAND!"=="" set COMMAND=help

REM Colors (limited support on Windows)
set YELLOW=[1;33m
set GREEN=[0;32m
set RED=[0;31m
set NC=[0m

goto !COMMAND!

:help
echo MoonPhase Docker Management Script (Windows)
echo.
echo Usage: docker-build.bat [command] [options]
echo.
echo Commands:
echo   build              Build the Docker image
echo   build-prod         Build production-optimized image
echo   run                Run the container
echo   run-dev            Run development container (updates every minute)
echo   stop               Stop the running container
echo   remove             Remove the container
echo   logs               View container logs
echo   shell              Open shell in running container
echo   compose-up         Start with docker-compose (production)
echo   compose-down       Stop docker-compose
echo   compose-dev        Start with docker-compose (development)
echo   push               Push image to registry
echo   clean              Remove image and container
echo   help               Show this help message
echo.
echo Examples:
echo   docker-build.bat build
echo   docker-build.bat run
echo   docker-build.bat logs
echo   docker-build.bat push myregistry.azurecr.io
goto :eof

:build
echo Building Docker image...
docker build -t %IMAGE_NAME%:%IMAGE_TAG% -f Dockerfile .
echo Image built successfully: %IMAGE_NAME%:%IMAGE_TAG%
goto :eof

:build-prod
echo Building production Docker image...
docker build -t %IMAGE_NAME%:%IMAGE_TAG%-prod -f Dockerfile.production .
echo Production image built successfully: %IMAGE_NAME%:%IMAGE_TAG%-prod
goto :eof

:run
echo Stopping existing container...
docker stop %CONTAINER_NAME% >nul 2>&1
docker rm %CONTAINER_NAME% >nul 2>&1

echo Starting container...
docker run -d ^
    --name %CONTAINER_NAME% ^
    -e MOON_PHASE_UPDATE_INTERVAL=3600000 ^
    -e LOGGING_LEVEL_ROOT=INFO ^
    -e TZ=UTC ^
    %IMAGE_NAME%:%IMAGE_TAG%

echo Container started: %CONTAINER_NAME%
echo Use 'docker-build.bat logs' to view output
goto :eof

:run-dev
echo Stopping existing container...
docker stop %CONTAINER_NAME%-dev >nul 2>&1
docker rm %CONTAINER_NAME%-dev >nul 2>&1

echo Starting development container (1-minute updates)...
docker run -d ^
    --name %CONTAINER_NAME%-dev ^
    -e MOON_PHASE_UPDATE_INTERVAL=60000 ^
    -e LOGGING_LEVEL_ROOT=DEBUG ^
    -e TZ=UTC ^
    %IMAGE_NAME%:%IMAGE_TAG%

echo Development container started: %CONTAINER_NAME%-dev
goto :eof

:stop
echo Stopping container...
docker stop %CONTAINER_NAME% >nul 2>&1
echo Container stopped
goto :eof

:remove
echo Removing container...
docker rm -f %CONTAINER_NAME% >nul 2>&1
echo Container removed
goto :eof

:logs
echo Displaying logs (Ctrl+C to exit)...
docker logs -f %CONTAINER_NAME%
goto :eof

:shell
echo Opening shell in container...
docker exec -it %CONTAINER_NAME% cmd
goto :eof

:compose-up
echo Starting with docker-compose (production)...
docker compose up -d
echo Container started
goto :eof

:compose-down
echo Stopping docker-compose...
docker compose down
echo Container stopped
goto :eof

:compose-dev
echo Starting with docker-compose (development)...
docker compose -f docker-compose.dev.yml up -d
echo Development container started
goto :eof

:push
echo Pushing image to registry...
set REGISTRY=%2
if "!REGISTRY!"=="" (
    echo Error: Registry URL not specified
    echo Usage: docker-build.bat push [registry-url]
    goto :eof
)

set FULL_TAG=!REGISTRY!/%IMAGE_NAME%:%IMAGE_TAG%
echo Tagging image as !FULL_TAG!...
docker tag %IMAGE_NAME%:%IMAGE_TAG% !FULL_TAG!

echo Pushing to registry...
docker push !FULL_TAG!
echo Image pushed successfully
goto :eof

:clean
echo Cleaning up...
docker stop %CONTAINER_NAME% >nul 2>&1
docker rm -f %CONTAINER_NAME% >nul 2>&1
docker rmi %IMAGE_NAME%:%IMAGE_TAG% >nul 2>&1
echo Cleanup complete
goto :eof

:default
echo Unknown command: %COMMAND%
call :help

