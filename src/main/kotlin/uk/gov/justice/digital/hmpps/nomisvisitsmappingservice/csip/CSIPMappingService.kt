package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees.CSIPAttendeeMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees.CSIPAttendeeMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees.toCSIPAttendeeDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors.CSIPFactorMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors.CSIPFactorMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors.toCSIPFactorDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews.CSIPInterviewMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews.CSIPInterviewMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews.toCSIPInterviewDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans.CSIPPlanMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans.CSIPPlanMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans.toCSIPPlanDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews.CSIPReviewMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews.CSIPReviewMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews.toCSIPReviewDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException

@Service
@Transactional(readOnly = true)
class CSIPMappingService(
  private val csipMappingRepository: CSIPMappingRepository,
  private val csipAttendeeMappingRepository: CSIPAttendeeMappingRepository,
  private val csipFactorMappingRepository: CSIPFactorMappingRepository,
  private val csipPlanMappingRepository: CSIPPlanMappingRepository,
  private val csipInterviewMappingRepository: CSIPInterviewMappingRepository,
  private val csipReviewMappingRepository: CSIPReviewMappingRepository,
) {

  @Transactional
  suspend fun createCSIPMapping(mapping: CSIPReportMappingDto) =
    csipMappingRepository.save(mapping.fromDto())

  suspend fun getMappingByNomisCSIPId(nomisCSIPReportId: Long): CSIPReportMappingDto =
    csipMappingRepository.findOneByNomisCSIPId(
      nomisCSIPId = nomisCSIPReportId,
    )
      ?.toDto()
      ?: throw NotFoundException("No CSIP Report mapping found for nomisCSIPReportId=$nomisCSIPReportId")

  suspend fun getMappingByDPSCSIPId(dpsCSIPReportId: String): CSIPReportMappingDto =
    csipMappingRepository.findById(dpsCSIPReportId)
      ?.toDto()
      ?: throw NotFoundException("No CSIP Report mapping found for dpsCSIPReportId=$dpsCSIPReportId")

  suspend fun getAllMappingsByDPSCSIPId(dpsCSIPReportId: String): CSIPFullMappingDto =
    csipMappingRepository.findById(dpsCSIPReportId)
      ?.let { csipReportMapping ->
        CSIPFullMappingDto(
          nomisCSIPReportId = csipReportMapping.nomisCSIPId,
          dpsCSIPReportId = csipReportMapping.dpsCSIPId,
          attendeeMappings = csipAttendeeMappingRepository.findAllByDpsCSIPReportId(dpsCSIPReportId).map { it.toCSIPAttendeeDto() },
          factorMappings = csipFactorMappingRepository.findAllByDpsCSIPReportId(dpsCSIPReportId).map { it.toCSIPFactorDto() },
          interviewMappings = csipInterviewMappingRepository.findAllByDpsCSIPReportId(dpsCSIPReportId).map { it.toCSIPInterviewDto() },
          planMappings = csipPlanMappingRepository.findAllByDpsCSIPReportId(dpsCSIPReportId).map { it.toCSIPPlanDto() },
          reviewMappings = csipReviewMappingRepository.findAllByDpsCSIPReportId(dpsCSIPReportId).map { it.toCSIPReviewDto() },
        )
      }
      ?: throw NotFoundException("No CSIP Report mapping found for dpsCSIPReportId=$dpsCSIPReportId")

  @Transactional
  suspend fun deleteMappingByDPSId(dpsCSIPId: String) {
    deleteChildMappingsByDPSId(dpsCSIPId)
    csipMappingRepository.deleteById(dpsCSIPId)
  }

  @Transactional
  suspend fun deleteMigratedChildren() {
    csipPlanMappingRepository.deleteByMappingTypeEquals(CSIPPlanMappingType.MIGRATED)
    csipInterviewMappingRepository.deleteByMappingTypeEquals(CSIPInterviewMappingType.MIGRATED)
    csipReviewMappingRepository.deleteByMappingTypeEquals(CSIPReviewMappingType.MIGRATED)
    csipAttendeeMappingRepository.deleteByMappingTypeEquals(CSIPAttendeeMappingType.MIGRATED)
    csipFactorMappingRepository.deleteByMappingTypeEquals(CSIPFactorMappingType.MIGRATED)
  }

  @Transactional
  suspend fun deleteAllChildren() {
    csipPlanMappingRepository.deleteAll()
    csipInterviewMappingRepository.deleteAll()
    csipReviewMappingRepository.deleteAll()
    csipAttendeeMappingRepository.deleteAll()
    csipFactorMappingRepository.deleteAll()
  }

  @Transactional
  suspend fun deleteChildMappingsByDPSId(dpsCSIPId: String) {
    csipPlanMappingRepository.deleteByDpsCSIPReportId(dpsCSIPId)
    csipInterviewMappingRepository.deleteByDpsCSIPReportId(dpsCSIPId)
    csipReviewMappingRepository.deleteByDpsCSIPReportId(dpsCSIPId)
    csipAttendeeMappingRepository.deleteByDpsCSIPReportId(dpsCSIPId)
    csipFactorMappingRepository.deleteByDpsCSIPReportId(dpsCSIPId)
  }

  @Transactional
  suspend fun deleteMappings(onlyMigrated: Boolean) =
    onlyMigrated.takeIf { it }?.apply {
      // The status of the child mapping will match the top level report status
      deleteMigratedChildren()
      csipMappingRepository.deleteByMappingTypeEquals(CSIPMappingType.MIGRATED)
    } ?: run {
      deleteAllChildren()
      csipMappingRepository.deleteAll()
    }

  suspend fun getByMigrationId(pageRequest: Pageable, migrationId: String): Page<CSIPReportMappingDto> =
    coroutineScope {
      val csipMapping = async {
        csipMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
          label = migrationId,
          CSIPMappingType.MIGRATED,
          pageRequest,
        )
      }

      val count = async {
        csipMappingRepository.countAllByLabelAndMappingType(migrationId, mappingType = CSIPMappingType.MIGRATED)
      }

      PageImpl(
        csipMapping.await().toList().map { it.toDto() },
        pageRequest,
        count.await(),
      )
    }

  suspend fun getMappingForLatestMigrated(): CSIPReportMappingDto =
    csipMappingRepository.findFirstByMappingTypeOrderByWhenCreatedDesc(CSIPMappingType.MIGRATED)
      ?.toDto()
      ?: throw NotFoundException("No migrated mapping found")
}

fun CSIPMapping.toDto() = CSIPReportMappingDto(
  dpsCSIPReportId = dpsCSIPId,
  nomisCSIPReportId = nomisCSIPId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

fun CSIPReportMappingDto.fromDto() = CSIPMapping(
  dpsCSIPId = dpsCSIPReportId,
  nomisCSIPId = nomisCSIPReportId,
  label = label,
  mappingType = mappingType,
)
