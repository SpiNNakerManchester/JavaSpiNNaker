# Spalloc Service Implementation

This is the SpiNNaker Allocation Service, spalloc.

This is a [Spring Boot](https://spring.io/projects/spring-boot) web application.

## Usage
This folder contains configuration which can be used to set up a docker compose instance of the service.  To use this, do the following:

 1. Copy files ``application.yml.example``, ``Dockerfile``, ``docker-compose.yml``. ``spalloc.env.example`` and the folder and contents of ``spalloc-mysql/`` to your deployment folder.
 2. Rename ``application.yml.example`` to ``application.yml``.  This should not need to be edited as it uses environment variables from ``spalloc.env`` but it can be changed if needed.
 3. Rename ``spalloc.env.example`` to ``spalloc.env``
 4. Update ``spalloc.env`` with valid values for your environment
 5. Run ``docker compose build`` to build the docker environment
 6. Run ``docker compose start -d`` to start the services

### Viewing logs
You can view the logs of the spalloc server with:
```
docker logs -f spalloc-spring-container
```

### Connecting to docker compose SQL server
You can connect to the spalloc database with:
```
docker exec -it spalloc-mysql-container /usr/bin/mysql -D spalloc -u <SPRING_DATASOURCE_USERNAME> -p
```
replacing the value of ``<SPRING_DATASOURCE_USERNAME>`` with the username in the env file, and followed by entering the value of ``SPRING_DATASOURCE_PASSWORD`` when asked for a password.
