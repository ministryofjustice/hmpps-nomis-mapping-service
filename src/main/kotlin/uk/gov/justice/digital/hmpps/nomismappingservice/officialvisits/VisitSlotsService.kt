package uk.gov.justice.digital.hmpps.nomismappingservice.officialvisits

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException

@Service
@Transactional
class VisitSlotsService(
  private val visitTimeSlotMappingRepository: VisitTimeSlotMappingRepository,
  private val visitSlotMappingRepository: VisitSlotMappingRepository,
) {
  suspend fun getVisitTimeSlotMappingByNomisId(nomisPrisonId: String, nomisDayOfWeek: String, nomisSlotSequence: Int) = visitTimeSlotMappingRepository.findOneByNomisPrisonIdAndNomisDayOfWeekAndNomisSlotSequence(
    nomisPrisonId = nomisPrisonId,
    nomisDayOfWeek = nomisDayOfWeek,
    nomisSlotSequence = nomisSlotSequence,
  )
    ?.toDto()
    ?: throw NotFoundException("No visit time slot mapping found for nomisPrisonId=$nomisPrisonId,nomisDayOfWeek=$nomisDayOfWeek,nomisSlotSequence=$nomisSlotSequence")

  suspend fun getVisitSlotMappingByNomisId(nomisId: Long) = visitSlotMappingRepository.findOneByNomisId(
    nomisId = nomisId,
  )
    ?.toDto()
    ?: throw NotFoundException("No visit slot mapping found for nomisVisitSlotId=$nomisId")

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

  suspend fun getVisitTimeSlotMappingsByMigrationId(
    pageRequest: Pageable,
    migrationId: String,
  ): Page<VisitTimeSlotMappingDto> = coroutineScope {
    val mappings = async {
      visitTimeSlotMappingRepository.findAllByLabelOrderByLabelDesc(
        label = migrationId,
        pageRequest = pageRequest,
      )
    }

    val count = async {
      visitTimeSlotMappingRepository.countAllByLabel(
        migrationId = migrationId,
      )
    }

    PageImpl(
      mappings.await().toList().map { it.toDto() },
      pageRequest,
      count.await(),
    )
  }

  suspend fun deleteAllMappings() {
    visitTimeSlotMappingRepository.deleteAll()
    visitSlotMappingRepository.deleteAll()
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
private fun VisitSlotMapping.toDto() = VisitSlotMappingDto(
  dpsId = dpsId,
  nomisId = nomisId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)
