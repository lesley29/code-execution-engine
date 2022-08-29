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

## Usage example

```shell
curl \
    -d '{
            "code":"
                Console.WriteLine(\"passed arguments:\");
                foreach (var arg in args) {
                    Console.WriteLine(arg);
                }
                await Task.Delay(1000);
                
                for (var i = 0; i < 5; i++) {
                    Console.WriteLine($\"counter: {i}\");
                    await Task.Delay(100);
                }",
            "arguments":["foo","bar"],
            "target_framework_monikier":"net6.0",
            "nuget_packages":[{"name":"Newtonsoft.Json","version":"13.0.1"}]
        }' \
    -H "Content-Type: application/json" \
    -X POST \
    http://0.0.0.0:8080/tasks
```
response example
```json
{"id":"7b5ad48a-b04f-4fff-a95d-f921abdaa951","status":"Created"}
```
poll for the answer
```shell
curl \
    -X GET \
    http://0.0.0.0:8080/tasks/{task_id}
```

## Description

User code runs in a fully isolated environment inside a container leveraging most of the Docker features: 
mem & cpu limitation, without any capabilities, as a non-root user, in a separate network, etc.

Engine enables near real-time running code log streaming support.

The main functionality is implemented, but there is room for improvement though:
* cover everything with tests
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