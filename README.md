# HMPPS NOMIS Prisoner Mapping Service

[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-nomis-mapping-service/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-nomis-mapping-service)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://nomis-sync-prisoner-mapping-dev.hmpps.service.justice.gov.uk/webjars/swagger-ui/index.html?configUrl=/v3/api-docs)

Self-contained fat-jar micro-service to interact with prisoners in the NOMIS database

## Building

```./gradlew build```

## Running

Various methods to run the application locally are detailed below.

Once up the application should be available on port 8080 - see the health page at http://localhost:8080/health.

Also try http://localhost:8080/swagger-ui/index.html to see the API specification.

## Running tests

By default a Postgres database is automatically created using testcontainers, unless a database is already running in which case that is used.
To run up a database independently for testing, with the right database name, user etc., use the database entry in docker-compose.yml.
