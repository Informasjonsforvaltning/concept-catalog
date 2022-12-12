# Concept-catalogue

Backend for the concept-registration.

## Requirements

- maven

- java 17

- docker

- docker-compose

## Run tests

```shell
mvn verify
```

## Run locally

### docker-compose

```shell
docker-compose up -d --build
```

### If running on mac

If you are running this on a mac you might have issues with `mvn verify`

Change the port in these files in order to get wiremock to work properly e.g. to `6000`:
`..src/main/resources/application.yml#L46`

`..src/test/kotlin/no/fdk/concept_catalog/utils/ApiTestContext.kt#L47`

`..src/test/kotlin/no/fdk/concept_catalog/utils/TestData.kt#L7`
