package uk.gov.justice.digital.hmpps.nomismappingservice.helper.builders

import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomismappingservice.jpa.repository.VisitIdRepository

@Repository
@Transactional
class Repository(private val visitIdRepository: VisitIdRepository) : VisitIdRepository by visitIdRepository
