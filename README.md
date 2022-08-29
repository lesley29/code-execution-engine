# code execution engine

Simple remote code execution web service.
It's designed with scalability and reliability in mind, and can be extended to support any number of programming languages.
Currently, only C# support is implemented.

![Main scheme](./assets/main-scheme.png)

Prerequisites:
* JVM 17+
* Docker

## Run application

* `./run-infra.sh` - set up all required infrastructure (mongodb, kafka)
* `./gradlew :dispatcher:run` - run dispatcher instance
* `./gradlew :worker:run` - run worker instance
* `./gradlew :api:run` - run api instance

## Description

User code runs in a fully isolated environment inside a container leveraging most of the Docker features: 
mem & cpu limitation, without any capabilities, as a non-root user, in a separate network, etc.

Engine enables near real-time running code log streaming support.

The main functionality is implemented, but there is room for improvement though:
* nuget package existence validation
* code syntax check
* background unused resources pruning
* other languages support
* metrics, monitoring and all that jazz
* ...

#### NB
* current implementation does not impose any disk space limitations; 
nevertheless, in a real-world production scenario it's easily achievable via `--storage-opt`
docker run parameter. The reason why it's not added now is that it requires specific backing 
filesystem (more on that [here](https://docs.docker.com/engine/reference/commandline/run/#set-storage-driver-options-per-container)).
* current state & code quality - proof of concept