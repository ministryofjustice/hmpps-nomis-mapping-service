# HMPPS NOMIS Prisoner Mapping Service

[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-nomis-visits-mapping-service/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-nomis-visits-mapping-service)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://nomis-visits-mapping-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html?configUrl=/v3/api-docs)

Self-contained fat-jar micro-service to interact with prisoners in the NOMIS database

## Building

```./gradlew build```

## Running

Various methods to run the application locally are detailed below.

Once up the application should be available on port 8101 - see the health page at http://localhost:8101/health.

Also try http://localhost:8101/swagger-ui/configUrl=/v3/api-docs to see the API specification.

###Available spring profiles
- dev: runs locally with H2
- postgres: runs locally with the dev auth server and a postgres database configured by setting:
  DATABASE_ENDPOINT, DATABASE_NAME, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD
