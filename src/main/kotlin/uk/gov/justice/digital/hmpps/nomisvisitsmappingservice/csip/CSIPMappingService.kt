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
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees.CSIPAttendeeMappingService
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees.CSIPAttendeeMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.attendees.toDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors.CSIPFactorMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors.CSIPFactorMappingService
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors.CSIPFactorMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.factors.toDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews.CSIPInterviewMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews.CSIPInterviewMappingService
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews.CSIPInterviewMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.interviews.toDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans.CSIPPlanMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans.CSIPPlanMappingService
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans.CSIPPlanMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.plans.toDto
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews.CSIPReviewMappingRepository
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews.CSIPReviewMappingService
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews.CSIPReviewMappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.csip.reviews.toDto
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
  private val csipAttendeeMappingService: CSIPAttendeeMappingService,
  private val csipFactorMappingService: CSIPFactorMappingService,
  private val csipInterviewMappingService: CSIPInterviewMappingService,
  private val csipPlanMappingService: CSIPPlanMappingService,
  private val csipReviewMappingService: CSIPReviewMappingService,
) {

  @Transactional
  suspend fun createCSIPMapping(mapping: CSIPReportMappingDto) =
    csipMappingRepository.save(mapping.fromDto())

  @Transactional
  suspend fun createCSIPMappingWithChildren(fullMappingDto: CSIPFullMappingDto) {
    with(fullMappingDto) {
      csipMappingRepository.save(reportMapping.fromDto())
      csipAttendeeMappingService.createMappings(attendeeMappings)
      csipFactorMappingService.createMappings(factorMappings)
      csipInterviewMappingService.createMappings(interviewMappings)
      csipPlanMappingService.createMappings(planMappings)
      csipReviewMappingService.createMappings(reviewMappings)
    }
  }

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
          reportMapping = csipReportMapping.toDto(),
          attendeeMappings = csipAttendeeMappingService.findAllByDpsCSIPReportId(dpsCSIPReportId),
          factorMappings = csipFactorMappingService.findAllByDpsCSIPReportId(dpsCSIPReportId),
          interviewMappings = csipInterviewMappingService.findAllByDpsCSIPReportId(dpsCSIPReportId),
          planMappings = csipPlanMappingService.findAllByDpsCSIPReportId(dpsCSIPReportId),
          reviewMappings = csipReviewMappingService.findAllByDpsCSIPReportId(dpsCSIPReportId),
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
