package uk.gov.justice.digital.hmpps.nomismappingservice.movements.transfer.movement

import org.springframework.stereotype.Service

@Service
class TransferMovementService(
  private val movementRepository: TransferMovementRepository,
) {

  suspend fun createMovementMapping(mappingDto: TransferMovementMappingDto): Unit = TODO("Not yet implemented")
}
