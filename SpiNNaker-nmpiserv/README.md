# Spalloc Service Implementation

This is the SpiNNaker NMPI service.

This is a [Spring Boot](https://spring.io/projects/spring-boot) web application.

## Usage
This folder contains configuration which can be used to set up a docker compose instance of the service.  To use this, do the following:

 1. Copy files ``nmpi.properties.example``, ``Dockerfile``, ``docker-compose.yml``, ``nginx.conf``, ``startup.sh``, ``env.example`` and ``execute_pynn.bash`` to your deployment folder.
 2. Rename ``nmpi.properties.example`` to ``nmpi.properties``
 3. Rename ``env.example`` to ``.env``
 4. Update ``.env`` with valid values for your environment
 5. Run ``docker compose build`` to build the docker environment
 6. Run ``docker compose up -d`` to start the services

### Viewing logs
You can view the logs of the spalloc server with:
```
docker compose logs -f nmpi
```
