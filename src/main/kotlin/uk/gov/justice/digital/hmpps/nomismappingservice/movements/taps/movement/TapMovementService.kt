package uk.gov.justice.digital.hmpps.nomismappingservice.movements.taps.movement

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException
import java.util.UUID

@Service
class TapMovementService(
  private val movementRepository: TapMovementRepository,
) {

  @Transactional
  suspend fun createExternalMovementMapping(mappingDto: TapMovementMappingDto) {
    movementRepository.save(mappingDto.toMapping())
  }

  @Transactional
  suspend fun updateExternalMovementMapping(mappingDto: TapMovementMappingDto) {
    movementRepository.findById(mappingDto.dpsMovementId)
      ?.let {
        it.nomisAddressId = mappingDto.nomisAddressId
        it.nomisAddressOwnerClass = mappingDto.nomisAddressOwnerClass
        it.dpsAddressText = mappingDto.dpsAddressText
        it.dpsDescription = mappingDto.dpsDescription
        it.dpsPostcode = mappingDto.dpsPostcode
        movementRepository.save(it)
      }
      ?: throw NotFoundException("Mapping for DPS movement id ${mappingDto.dpsMovementId} not found")
  }

  suspend fun getExternalMovementMappingByNomisId(bookingId: Long, movementSeq: Int) = movementRepository.findByNomisBookingIdAndNomisMovementSeq(bookingId, movementSeq)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS booking id $bookingId and movement sequence $movementSeq not found")

  suspend fun getExternalMovementMappingByDpsId(dpsExternalMovementId: UUID) = movementRepository.findById(dpsExternalMovementId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for DPS external movement id $dpsExternalMovementId not found")

  @Transactional
  suspend fun deleteExternalMovementMappingByNomisId(bookingId: Long, movementSeq: Int) = movementRepository.deleteByNomisBookingIdAndNomisMovementSeq(bookingId, movementSeq)
}

fun TapMovementMapping.toMappingDto(): TapMovementMappingDto = TapMovementMappingDto(
  offenderNo,
  nomisBookingId,
  nomisMovementSeq,
  dpsMovementId,
  mappingType = mappingType,
  nomisAddressId,
  nomisAddressOwnerClass,
  dpsAddressText,
  dpsDescription,
  dpsPostcode,
  dpsUprn,
)

fun TapMovementMappingDto.toMapping(): TapMovementMapping = TapMovementMapping(
  dpsMovementId,
  bookingId,
  nomisMovementSeq,
  prisonerNumber,
  nomisAddressId,
  nomisAddressOwnerClass,
  dpsAddressText,
  dpsUprn = dpsUprn,
  dpsDescription = dpsDescription,
  dpsPostcode = dpsPostcode,
  mappingType = mappingType,
)
