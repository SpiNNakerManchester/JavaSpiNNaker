# Spalloc Service Implementation

This is the SpiNNaker NMPI service.

This is a [Spring Boot](https://spring.io/projects/spring-boot) web application.

## Usage
This folder contains configuration which can be used to set up a docker compose instance of the service.  To use this, do the following:

 1. Copy files ``application.properties.example``, ``Dockerfile``, ``docker-compose.yml``. ``nginx.conf`` and ``execute_pynn.bash`` to your deployment folder.
 2. Rename ``application.properties.example`` to ``application.properties``
 3. Update ``application.properties`` with valid values for your environment, particularly replacing items in angle brackets.
 4. Update ``docker-compose.yml`` with valid values for your environment,
 particularly replacing items in angle brackets.
 4. Run ``docker compose build`` to build the docker environment
 5. Run ``docker compose start -d`` to start the services

### Viewing logs
You can view the logs of the spalloc server with:
```
docker logs -f nmpi-host
```
