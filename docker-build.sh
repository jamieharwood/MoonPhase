#!/bin/bash

# MoonPhase Docker Build and Deploy Script
# Usage: ./docker-build.sh [command] [options]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
IMAGE_NAME="moonphase"
IMAGE_TAG="1.0-SNAPSHOT"
CONTAINER_NAME="moonphase"
REGISTRY="${REGISTRY:-}"

# Functions
print_usage() {
    echo "MoonPhase Docker Management Script"
    echo ""
    echo "Usage: ./docker-build.sh [command] [options]"
    echo ""
    echo "Commands:"
    echo "  build              Build the Docker image"
    echo "  build-prod         Build production-optimized image"
    echo "  run                Run the container"
    echo "  run-dev            Run development container (updates every minute)"
    echo "  stop               Stop the running container"
    echo "  remove             Remove the container"
    echo "  logs               View container logs"
    echo "  shell              Open shell in running container"
    echo "  compose-up         Start with docker-compose (production)"
    echo "  compose-down       Stop docker-compose"
    echo "  compose-dev        Start with docker-compose (development)"
    echo "  push               Push image to registry"
    echo "  clean              Remove image and container"
    echo "  help               Show this help message"
    echo ""
    echo "Options:"
    echo "  --registry <url>   Docker registry URL (for push)"
    echo "  --tag <tag>        Docker image tag (default: 1.0-SNAPSHOT)"
    echo "  --no-cache         Build without using cache"
    echo ""
    echo "Examples:"
    echo "  ./docker-build.sh build"
    echo "  ./docker-build.sh run"
    echo "  ./docker-build.sh logs"
    echo "  ./docker-build.sh push --registry myregistry.azurecr.io"
}

build_image() {
    local dockerfile="${1:-Dockerfile}"
    echo -e "${YELLOW}Building Docker image from ${dockerfile}...${NC}"
    docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -f ${dockerfile} .
    echo -e "${GREEN}Image built successfully: ${IMAGE_NAME}:${IMAGE_TAG}${NC}"
}

build_prod_image() {
    echo -e "${YELLOW}Building production Docker image...${NC}"
    docker build -t ${IMAGE_NAME}:${IMAGE_TAG}-prod -f Dockerfile.production .
    echo -e "${GREEN}Production image built successfully: ${IMAGE_NAME}:${IMAGE_TAG}-prod${NC}"
}

run_container() {
    local interval="${1:-3600000}"
    local log_level="${2:-INFO}"

    echo -e "${YELLOW}Starting container...${NC}"
    docker run -d \
        --name ${CONTAINER_NAME} \
        --network host \
        -e MOON_PHASE_UPDATE_INTERVAL=${interval} \
        -e LOGGING_LEVEL_ROOT=${log_level} \
        -e TZ=UTC \
        ${IMAGE_NAME}:${IMAGE_TAG}

    echo -e "${GREEN}Container started: ${CONTAINER_NAME}${NC}"
    echo "Use './docker-build.sh logs' to view output"
}

    run_dev_container() {
    echo -e "${YELLOW}Starting development container (1-minute updates)...${NC}"
    docker run -d \
        --name ${CONTAINER_NAME}-dev \
        --network host \
        -e MOON_PHASE_UPDATE_INTERVAL=60000 \
        -e LOGGING_LEVEL_ROOT=DEBUG \
        -e TZ=UTC \
        ${IMAGE_NAME}:${IMAGE_TAG}

    echo -e "${GREEN}Development container started: ${CONTAINER_NAME}-dev${NC}"
}

stop_container() {
    echo -e "${YELLOW}Stopping container...${NC}"
    docker stop ${CONTAINER_NAME} || true
    echo -e "${GREEN}Container stopped${NC}"
}

remove_container() {
    echo -e "${YELLOW}Removing container...${NC}"
    docker rm -f ${CONTAINER_NAME} || true
    echo -e "${GREEN}Container removed${NC}"
}

view_logs() {
    echo -e "${YELLOW}Displaying logs (Ctrl+C to exit)...${NC}"
    docker logs -f ${CONTAINER_NAME}
}

open_shell() {
    echo -e "${YELLOW}Opening shell in container...${NC}"
    docker exec -it ${CONTAINER_NAME} /bin/bash
}

compose_up() {
    echo -e "${YELLOW}Starting with docker-compose (production)...${NC}"
    docker compose up -d
    echo -e "${GREEN}Container started${NC}"
}

compose_down() {
    echo -e "${YELLOW}Stopping docker-compose...${NC}"
    docker compose down
    echo -e "${GREEN}Container stopped${NC}"
}

compose_dev() {
    echo -e "${YELLOW}Starting with docker-compose (development)...${NC}"
    docker compose -f docker-compose.dev.yml up -d
    echo -e "${GREEN}Development container started${NC}"
}

push_image() {
    if [ -z "${REGISTRY}" ]; then
        echo -e "${RED}Error: Registry URL not specified${NC}"
        echo "Use: ./docker-build.sh push --registry <url>"
        exit 1
    fi

    local full_tag="${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
    echo -e "${YELLOW}Tagging image as ${full_tag}...${NC}"
    docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${full_tag}

    echo -e "${YELLOW}Pushing image to registry...${NC}"
    docker push ${full_tag}
    echo -e "${GREEN}Image pushed successfully${NC}"
}

clean_up() {
    echo -e "${YELLOW}Cleaning up...${NC}"
    docker stop ${CONTAINER_NAME} || true
    docker rm -f ${CONTAINER_NAME} || true
    docker rmi ${IMAGE_NAME}:${IMAGE_TAG} || true
    echo -e "${GREEN}Cleanup complete${NC}"
}

# Parse arguments
COMMAND="${1:-help}"

case "${COMMAND}" in
    build)
        build_image "Dockerfile"
        ;;
    build-prod)
        build_prod_image
        ;;
    run)
        remove_container
        run_container 3600000 INFO
        ;;
    run-dev)
        remove_container
        run_dev_container
        ;;
    stop)
        stop_container
        ;;
    remove)
        remove_container
        ;;
    logs)
        view_logs
        ;;
    shell)
        open_shell
        ;;
    compose-up)
        compose_up
        ;;
    compose-down)
        compose_down
        ;;
    compose-dev)
        compose_dev
        ;;
    push)
        # Parse registry argument
        if [ "$2" == "--registry" ] && [ -n "$3" ]; then
            REGISTRY="$3"
        fi
        push_image
        ;;
    clean)
        clean_up
        ;;
    help|--help|-h)
        print_usage
        ;;
    *)
        echo -e "${RED}Unknown command: ${COMMAND}${NC}"
        print_usage
        exit 1
        ;;
esac

