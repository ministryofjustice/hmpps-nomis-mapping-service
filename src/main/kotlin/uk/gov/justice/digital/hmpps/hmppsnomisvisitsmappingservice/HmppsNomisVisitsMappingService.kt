package uk.gov.justice.digital.hmpps.hmppsnomisvisitsmappingservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class HmppsNomisVisitsMappingService

fun main(args: Array<String>) {
  runApplication<HmppsNomisVisitsMappingService>(*args)
}
