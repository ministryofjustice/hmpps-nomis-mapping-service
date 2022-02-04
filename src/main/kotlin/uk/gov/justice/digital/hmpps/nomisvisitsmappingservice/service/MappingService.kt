package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.MappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.MappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.VisitId
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.VisitIdRepository
import java.util.function.Supplier

@Service
@Transactional(readOnly = true)
class MappingService(
  private val visitIdRepository: VisitIdRepository,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun createVisitMapping(createMappingRequest: MappingDto) {
    with(createMappingRequest) {
      visitIdRepository.save(VisitId(nomisId, vsipId, label, MappingType.valueOf(mappingType)))

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
    }
  }

  fun getVisitMappingGivenNomisId(nomisId: Long): MappingDto =
    (visitIdRepository.findByIdOrNull(nomisId) ?: throw NotFoundException("NOMIS visit id=$nomisId"))
      .run { MappingDto(nomisId, vsipId, label, mappingType.name) }

  fun getVisitMappingGivenVsipId(vsipId: String): MappingDto =
    (visitIdRepository.findOneByVsipId(vsipId) ?: throw NotFoundException("VSIP visit id=$vsipId"))
      .run { MappingDto(nomisId, vsipId, label, mappingType.name) }
}

class NotFoundException(message: String?) : RuntimeException(message), Supplier<NotFoundException> {
  override fun get(): NotFoundException {
    return NotFoundException(message)
  }
}