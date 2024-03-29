package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.builders

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.IncentiveMappingRepository

@Repository
@Transactional
class IncentiveRepository(@Qualifier("incentiveMappingRepository") private val incentiveRepository: IncentiveMappingRepository) : IncentiveMappingRepository by incentiveRepository
