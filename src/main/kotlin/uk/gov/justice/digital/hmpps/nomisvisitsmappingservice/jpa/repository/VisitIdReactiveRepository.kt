package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.repository.reactive.ReactiveSortingRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.MappingType
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.jpa.VisitId

@Repository
interface VisitIdReactiveRepository : ReactiveSortingRepository<VisitId, Long> {
  fun findAllByLabelAndMappingTypeOrderByLabelDesc(label: String, mappingType: MappingType, pageable: Pageable): Flux<VisitId>
  fun countAllByLabelAndMappingType(label: String, mappingType: MappingType): Mono<Long>
}
