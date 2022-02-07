package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.VisitId
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.VisitIdRepository

@Repository
@Transactional
class Repository(
  val visitIdRepository: VisitIdRepository,
) {
  fun save(visitId: VisitId): VisitId = visitIdRepository.save(visitId)

  fun lookupVisitId(nomisId: Long): VisitId = visitIdRepository.findByIdOrNull(nomisId)!!

  fun delete(nomisId: Long) = visitIdRepository.deleteById(nomisId)
  fun deleteAll() = visitIdRepository.deleteAll()
}
