# Rse Idam Simulator using spring-boot-template

[![Build Status](https://travis-ci.org/hmcts/spring-boot-template.svg?branch=master)](https://travis-ci.org/hmcts/spring-boot-template)

## Notes

Since Spring Boot 2.1 bean overriding is disabled. If you want to enable it you will need to set `spring.main.allow-bean-definition-overriding` to `true`.

JUnit 5 is now enabled by default in the project. Please refrain from using JUnit4 and use the next generation

## Building and deploying the application

### Building the application

The project uses [Gradle](https://gradle.org) as a build tool. It already contains
`./gradlew` wrapper script, so there's no need to install gradle.

To build the project execute the following command:

```bash
  ./gradlew build
```

### Running the application

Create the image of the application by executing the following command:

```bash
  ./gradlew assemble
```

Create docker image:

```bash
  docker-compose build
```

Run the distribution (created in `build/install/rse-idam-simulator` directory)
by executing the following command:

```bash
  docker-compose up
```

This will start the API container exposing the application's port
(set to `5556` in this template app).

In order to test if the application is up, you can call its health endpoint:

```bash
  curl http://localhost:5556/health
```

You should get a response similar to this:

```
  {"status":"UP","diskSpace":{"status":"UP","total":249644974080,"free":137188298752,"threshold":10485760}}
```

### Alternative script to run application

To skip all the setting up and building, just execute the following command:

```bash
./bin/run-in-docker.sh
```

For more information:

```bash
./bin/run-in-docker.sh -h
```

Script includes bare minimum environment variables necessary to start api instance. Whenever any variable is changed or any other script regarding docker image/container build, the suggested way to ensure all is cleaned up properly is by this command:

```bash
docker-compose rm
```

It clears stopped containers correctly. Might consider removing clutter of images too, especially the ones fiddled with:

```bash
docker images

docker image rm <image-id>
```

There is no need to remove postgres and java or similar core images.


## How to use the simulator
Check IdamSimulatorController to see how works the endpoints. These endpoints are all the enpoints required to have the idam java client working correctly,
and one endpoint to add a user in the local memory map of the simulator. Keep in mind that username and email are the same in Idam system.

Here a quick start to request a token.

Add an user by doing this request:
```
POST  http://localhost:5556/simulator/user
{

"email": "myemail-test@hmcts.net",
"given_name": "John",
"family_name": "Smith",
"roles": ["role1", "role2"]

}
```

Have an openId Token using this call
```
POST  http://localhost:5556/o/token
Content-type: application/x-www-form-urlencoded
client_id: sometestservice
client_secret: sometestservice
grant_type: password
redirect_uri: https://davidtestservice.com
username: myemail-test@hmcts.net
password: somePassword
scope: openid profile roles
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

