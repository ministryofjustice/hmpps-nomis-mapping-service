package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.MappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.MappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.VisitId
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.VisitIdRepository
import javax.validation.ValidationException

@Service
@Transactional(readOnly = true)
class MappingService(
  private val visitIdRepository: VisitIdRepository,
  private val telemetryClient: TelemetryClient,
) {
  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun createVisitMapping(createMappingRequest: MappingDto): Mono<Void> =
    with(createMappingRequest) {
      visitIdRepository.findById(nomisId)
        .map { throw ValidationException("Nomis visit id = $nomisId already exists") }
        .thenMany(visitIdRepository.findOneByVsipId(vsipId))
        .map { throw ValidationException("VSIP visit id=$vsipId already exists") }
        .thenMany(visitIdRepository.save(VisitId(nomisId, vsipId, label, MappingType.valueOf(mappingType))))
        .map {
          telemetryClient.trackEvent(
            "visit-created",
            mapOf(
              "nomisVisitId" to nomisId.toString(),
              "vsipVisitId" to vsipId,
              "batchId" to label,
            ),
            null
          )
          log.debug("Mapping created with VSIP visit id = $vsipId, Nomis visit id = $nomisId")
        }.then()
    }

  fun getVisitMappingGivenNomisId(nomisId: Long): Mono<MappingDto> =
    visitIdRepository.findById(nomisId).map { MappingDto(nomisId, it.vsipId, it.label, it.mappingType.name) }
      .switchIfEmpty(Mono.error(NotFoundException("NOMIS visit id=$nomisId")))

  fun getVisitMappingGivenVsipId(vsipId: String): Mono<MappingDto> =
    visitIdRepository.findOneByVsipId(vsipId)
      .map { MappingDto(it.nomisId, vsipId, it.label, it.mappingType.name) }
      .switchIfEmpty(Mono.error(NotFoundException(("VSIP visit id=$vsipId"))))
}

class NotFoundException(message: String) : RuntimeException(message)
