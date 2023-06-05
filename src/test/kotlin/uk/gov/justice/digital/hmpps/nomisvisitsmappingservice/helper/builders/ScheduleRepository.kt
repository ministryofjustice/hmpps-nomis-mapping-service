package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.builders

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.ActivityScheduleMappingRepository

@Repository
@Transactional
class ScheduleRepository(@Qualifier("activityScheduleMappingRepository") private val activityScheduleRepository: ActivityScheduleMappingRepository) : ActivityScheduleMappingRepository by activityScheduleRepository
