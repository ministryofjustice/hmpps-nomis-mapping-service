package uk.gov.justice.digital.hmpps.nomismappingservice.officialvisits

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException
import java.time.DayOfWeek

@Service
@Transactional(readOnly = true)
class VisitSlotsService(private val visitTimeSlotMappingRepository: VisitTimeSlotMappingRepository) {
  suspend fun getVisitTimeSlotMappingByNomisId(nomisPrisonId: String, nomisDayOfWeek: DayOfWeek, nomisSlotSequence: Int) = visitTimeSlotMappingRepository.findOneByNomisPrisonIdAndNomisDayOfWeekAndNomisSlotSequence(
    nomisPrisonId = nomisPrisonId,
    nomisDayOfWeek = nomisDayOfWeek,
    nomisSlotSequence = nomisSlotSequence,
  )
    ?.toDto()
    ?: throw NotFoundException("No visit slot mapping found for nomisPrisonId=$nomisPrisonId,nomisDayOfWeek=$nomisDayOfWeek,nomisSlotSequence=$nomisSlotSequence")
}

private fun VisitTimeSlotMapping.toDto() = VisitTimeSlotMappingDto(
  dpsId = dpsId,
  nomisPrisonId = nomisPrisonId,
  nomisDayOfWeek = nomisDayOfWeek,
  nomisSlotSequence = nomisSlotSequence,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
