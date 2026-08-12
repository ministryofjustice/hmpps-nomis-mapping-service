package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.movement

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException
import java.util.UUID

@Service
class TransferMovementService(
  private val movementRepository: TransferMovementRepository,
) {

  @Transactional
  suspend fun createMovementMapping(mappingDto: TransferMovementMappingDto) {
    movementRepository.save(mappingDto.toMapping())
  }

  suspend fun getMovementMappingByNomisId(nomisBookingId: Long, nomisMovementSeq: Int) = movementRepository.findByNomisBookingIdAndNomisMovementSeq(nomisBookingId, nomisMovementSeq)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS booking id / movement seq $nomisBookingId/$nomisMovementSeq not found")

  suspend fun getMovementMappingByDpsId(dpsTransferMovementId: UUID) = movementRepository.findById(dpsTransferMovementId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for DPS transfer movement id $dpsTransferMovementId not found")

  @Transactional
  suspend fun deleteMovementMappingByNomisId(bookingId: Long, movementSeq: Int) = movementRepository.deleteByNomisBookingIdAndNomisMovementSeq(bookingId, movementSeq)
}

fun TransferMovementMappingDto.toMapping(): TransferMovementMapping = TransferMovementMapping(
  dpsTransferMovementId,
  nomisBookingId,
  nomisMovementSeq,
  prisonerNumber,
  mappingType = mappingType,
)

fun TransferMovementMapping.toMappingDto(): TransferMovementMappingDto = TransferMovementMappingDto(
  offenderNo,
  nomisBookingId,
  nomisMovementSeq,
  dpsTransferMovementId,
  mappingType = mappingType,
)
