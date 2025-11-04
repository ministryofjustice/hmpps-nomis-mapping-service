package uk.gov.justice.digital.hmpps.nomismappingservice.officialvisits

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException
import java.time.DayOfWeek

@Service
@Transactional
class VisitSlotsService(
  private val visitTimeSlotMappingRepository: VisitTimeSlotMappingRepository,
  private val visitSlotMappingRepository: VisitSlotMappingRepository,
) {
  suspend fun getVisitTimeSlotMappingByNomisId(nomisPrisonId: String, nomisDayOfWeek: DayOfWeek, nomisSlotSequence: Int) = visitTimeSlotMappingRepository.findOneByNomisPrisonIdAndNomisDayOfWeekAndNomisSlotSequence(
    nomisPrisonId = nomisPrisonId,
    nomisDayOfWeek = nomisDayOfWeek,
    nomisSlotSequence = nomisSlotSequence,
  )
    ?.toDto()
    ?: throw NotFoundException("No visit slot mapping found for nomisPrisonId=$nomisPrisonId,nomisDayOfWeek=$nomisDayOfWeek,nomisSlotSequence=$nomisSlotSequence")

  suspend fun getVisitTimeSlotMappingByDpsIdOrNull(dpsId: String) = visitTimeSlotMappingRepository.findOneByDpsId(dpsId)
    ?.toDto()

  suspend fun createMappings(mappings: VisitTimeSlotMigrationMappingDto) {
    with(mappings) {
      visitTimeSlotMappingRepository.save(
        VisitTimeSlotMapping(
          dpsId = dpsId,
          nomisPrisonId = nomisPrisonId,
          nomisDayOfWeek = nomisDayOfWeek,
          nomisSlotSequence = nomisSlotSequence,
          label = label,
          mappingType = mappingType,
          whenCreated = whenCreated,
        ),
      )

      visitSlots.forEach {
        visitSlotMappingRepository.save(
          VisitSlotMapping(
            dpsId = it.dpsId,
            nomisId = it.nomisId,
            label = label,
            mappingType = mappingType,
            whenCreated = whenCreated,
          ),
        )
      }
    }
  }
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
