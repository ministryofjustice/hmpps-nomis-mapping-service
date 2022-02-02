package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class NomisVisitsMappingService

fun main(args: Array<String>) {
  runApplication<NomisVisitsMappingService>(*args)
}
