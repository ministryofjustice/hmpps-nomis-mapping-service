package uk.gov.justice.digital.hmpps.nomismappingservice.movements.court.movement

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException
import java.util.UUID

@Service
class CourtMovementService(
  private val movementRepository: CourtMovementRepository,
) {

  @Transactional
  suspend fun createMovementMapping(mappingDto: CourtMovementMappingDto) {
    movementRepository.save(mappingDto.toMapping())
  }

  suspend fun getMovementMappingByNomisId(nomisBookingId: Long, nomisMovementSeq: Int) = movementRepository.findByNomisBookingIdAndNomisMovementSeq(nomisBookingId, nomisMovementSeq)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS booking id / movement seq $nomisBookingId/$nomisMovementSeq not found")

  suspend fun getMovementMappingByDpsId(dpsCourtMovementId: UUID) = movementRepository.findById(dpsCourtMovementId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for DPS court movement id $dpsCourtMovementId not found")
}

fun CourtMovementMappingDto.toMapping(): CourtMovementMapping = CourtMovementMapping(
  dpsCourtMovementId,
  nomisBookingId,
  nomisMovementSeq,
  prisonerNumber,
  mappingType = mappingType,
)

fun CourtMovementMapping.toMappingDto(): CourtMovementMappingDto = CourtMovementMappingDto(
  offenderNo,
  nomisBookingId,
  nomisMovementSeq,
  dpsCourtMovementId,
  mappingType = mappingType,
)
