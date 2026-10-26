package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.movement

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException

@Service
class PersonLocationMovementService(
  private val movementRepository: PersonLocationMovementRepository,
) {

  suspend fun getMovementMappingByNomisId(nomisBookingId: Long, nomisMovementSeq: Int) = movementRepository.findByNomisBookingIdAndNomisMovementSeq(nomisBookingId, nomisMovementSeq)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS booking id / movement seq $nomisBookingId/$nomisMovementSeq not found")
}

fun PersonLocationMovementMapping.toMappingDto() = PersonLocationMovementIdMappingDto(
  nomisBookingId = nomisBookingId,
  nomisMovementSeq = nomisMovementSeq,
  dpsExternalMovementId = dpsExternalMovementId,
  mappingType = mappingType,
)
