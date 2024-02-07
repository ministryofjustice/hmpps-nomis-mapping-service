package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.builders

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository.LocationMappingRepository

@Repository
@Transactional
class LocationRepository(@Qualifier("locationMappingRepository") private val locationRepository: LocationMappingRepository) :
  LocationMappingRepository by locationRepository
