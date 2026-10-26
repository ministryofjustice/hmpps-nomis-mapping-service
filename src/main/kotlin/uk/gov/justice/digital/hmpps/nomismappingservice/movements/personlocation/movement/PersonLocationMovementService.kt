package uk.gov.justice.digital.hmpps.nomismappingservice.movements.personlocation.movement

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.service.NotFoundException
import java.util.UUID

@Service
class PersonLocationMovementService(
  private val movementRepository: PersonLocationMovementRepository,
) {

  @Transactional
  suspend fun createMovementMapping(mappingDto: PersonLocationMovementIdMappingDto) {
    movementRepository.save(mappingDto.toMapping())
  }

  suspend fun getMovementMappingByNomisId(nomisBookingId: Long, nomisMovementSeq: Int) = movementRepository.findByNomisBookingIdAndNomisMovementSeq(nomisBookingId, nomisMovementSeq)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for NOMIS booking id / movement seq $nomisBookingId/$nomisMovementSeq not found")

  suspend fun getMovementMappingByDpsId(dpsExternalMovementId: UUID) = movementRepository.findById(dpsExternalMovementId)
    ?.toMappingDto()
    ?: throw NotFoundException("Mapping for DPS external movement id $dpsExternalMovementId not found")

  suspend fun getExistingMovementMappingSimilarTo(mapping: PersonLocationMovementIdMappingDto) = runCatching {
    getMovementMappingByNomisId(mapping.nomisBookingId, mapping.nomisMovementSeq)
  }.getOrElse {
    getMovementMappingByDpsId(mapping.dpsExternalMovementId)
  }
}

fun PersonLocationMovementIdMappingDto.toMapping() = PersonLocationMovementMapping(
  dpsExternalMovementId = dpsExternalMovementId,
  nomisBookingId = nomisBookingId,
  nomisMovementSeq = nomisMovementSeq,
  mappingType = mappingType,
)

fun PersonLocationMovementMapping.toMappingDto() = PersonLocationMovementIdMappingDto(
  nomisBookingId = nomisBookingId,
  nomisMovementSeq = nomisMovementSeq,
  dpsExternalMovementId = dpsExternalMovementId,
  mappingType = mappingType,
)
