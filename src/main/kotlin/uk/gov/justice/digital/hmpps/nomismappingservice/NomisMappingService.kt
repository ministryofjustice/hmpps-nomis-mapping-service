package uk.gov.justice.digital.hmpps.nomismappingservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication()
class NomisMappingService

fun main(args: Array<String>) {
  runApplication<NomisMappingService>(*args)
}
