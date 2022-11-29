# Tickle

![build](https://github.com/lllbllllb/tickle/actions/workflows/build.yml/badge.svg)

A simple tool for load testing with a very visual process

![tickle gif](/media/tickle.webp)

## Getting it
You can get the Tickle with [Docker](https://hub.docker.com/repository/docker/mbllllb/tickle)

**docker-compose.yaml** example
```yaml
services:
  tickle-service:
    container_name: tickle
    image: mbllllb/tickle:latest
    ports:
      - "8088:8080"
    deploy:
      resources:
        limits:
          cpus: '4'
          memory: 1G
        reservations:
          cpus: '2'
          memory: 1G
    restart: always
```

## Capabilities
* create a test-plan for multiple services at the same time
* save as json and load your plans
* rich options for customization
* run loading for any number of services at the same time *

&ast; you are only limited by the power of your device

