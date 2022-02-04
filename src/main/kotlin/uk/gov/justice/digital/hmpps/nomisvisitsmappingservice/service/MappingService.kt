package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.data.MappingDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.VisitId
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.VisitIdSource
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.VisitIdRepository
import java.util.function.Supplier

@Service
@Transactional(readOnly = true)
class VisitService(
  private val visitIdRepository: VisitIdRepository,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    const val VSIP_PREFIX = "VSIP_"
  }

  @Transactional
  fun createVisitMapping(createMappingRequest: MappingDto) {
    with(createMappingRequest) {
      visitIdRepository.save(VisitId(nomisId, vsipId, label, VisitIdSource.valueOf(visitType)))

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

  fun getVisitMappingGivenNomisId(nomisId: Long): MappingDto {
    return visitIdRepository.findById(nomisId).orElseThrow(NotFoundException("NOMIS visit id=$nomisId"))
      .run { MappingDto(nomisId, vsipId, label, source.name) }
  }

  fun getVisitMappingGivenVsipId(vsipId: String): MappingDto {
    return visitIdRepository.findOneByVsipId(vsipId).orElseThrow(NotFoundException("VSIP visit id=$vsipId"))
      .run { MappingDto(nomisId, vsipId, label, source.name) }
  }
}

class NotFoundException(message: String?) : RuntimeException(message), Supplier<NotFoundException> {
  override fun get(): NotFoundException {
    return NotFoundException(message)
  }
}
