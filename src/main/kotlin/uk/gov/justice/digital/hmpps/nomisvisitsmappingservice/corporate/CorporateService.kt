package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.corporate

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.service.NotFoundException
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class CorporateService(
  private val corporateMappingRepository: CorporateMappingRepository,
  private val corporateAddressMappingRepository: CorporateAddressMappingRepository,
  private val corporateAddressPhoneMappingRepository: CorporateAddressPhoneMappingRepository,
  private val corporatePhoneMappingRepository: CorporatePhoneMappingRepository,
  private val corporateEmailMappingRepository: CorporateEmailMappingRepository,
  private val corporateWebMappingRepository: CorporateWebMappingRepository,
) {
  suspend fun getCorporateMappingByNomisId(nomisId: Long) =
    corporateMappingRepository.findOneByNomisId(nomisId = nomisId)
      ?.toDto()
      ?: throw NotFoundException("No corporate mapping found for nomisId=$nomisId")

  suspend fun getCorporateMappingByDpsIdOrNull(dpsId: String) =
    corporateMappingRepository.findOneByDpsId(dpsId = dpsId)
      ?.toDto()

  @Transactional
  suspend fun createMappings(mappings: CorporateMappingsDto) {
    with(mappings) {
      corporateMappingRepository.save(toCorporateMapping())
      corporateAddressMapping.forEach {
        corporateAddressMappingRepository.save(toMapping(it))
      }
      corporateAddressPhoneMapping.forEach {
        corporateAddressPhoneMappingRepository.save(toMapping(it))
      }
      corporatePhoneMapping.forEach {
        corporatePhoneMappingRepository.save(toMapping(it))
      }
      corporateEmailMapping.forEach {
        corporateEmailMappingRepository.save(toMapping(it))
      }
      corporateWebMapping.forEach {
        corporateWebMappingRepository.save(toMapping(it))
      }
    }
  }

  suspend fun getCorporateMappingsByMigrationId(pageRequest: Pageable, migrationId: String): Page<CorporateMappingDto> =
    coroutineScope {
      val mappings = async {
        corporateMappingRepository.findAllByLabelAndMappingTypeOrderByLabelDesc(
          label = migrationId,
          mappingType = CorporateMappingType.MIGRATED,
          pageRequest = pageRequest,
        )
      }

      val count = async {
        corporateMappingRepository.countAllByLabelAndMappingType(
          migrationId = migrationId,
          mappingType = CorporateMappingType.MIGRATED,
        )
      }

      PageImpl(
        mappings.await().toList().map { it.toDto() },
        pageRequest,
        count.await(),
      )
    }
  suspend fun getAllCorporateMappings(pageRequest: Pageable): Page<CorporateMappingDto> = coroutineScope {
    val mappings = async {
      corporateMappingRepository.findAllBy(
        pageRequest = pageRequest,
      )
    }

    val count = async {
      corporateMappingRepository.count()
    }

    PageImpl(
      mappings.await().toList().map { it.toDto() },
      pageRequest,
      count.await(),
    )
  }

  @Transactional
  suspend fun deleteAllMappings() {
    corporateWebMappingRepository.deleteAll()
    corporateEmailMappingRepository.deleteAll()
    corporatePhoneMappingRepository.deleteAll()
    corporateAddressPhoneMappingRepository.deleteAll()
    corporateAddressMappingRepository.deleteAll()
    corporateMappingRepository.deleteAll()
  }
}
private fun CorporateMapping.toDto() = CorporateMappingDto(
  nomisId = nomisId,
  dpsId = dpsId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private fun CorporateMappingsDto.toCorporateMapping() = CorporateMapping(
  dpsId = corporateMapping.dpsId,
  nomisId = corporateMapping.nomisId,
  label = label,
  mappingType = mappingType,
  whenCreated = whenCreated,
)

private inline fun <reified T : AbstractCorporateMapping> CorporateMappingsDto.toMapping(mapping: CorporateMappingIdDto): T =
  T::class.java.getDeclaredConstructor(
    String::class.java,
    Long::class.java,
    String::class.java,
    CorporateMappingType::class.java,
    LocalDateTime::class.java,
  ).newInstance(
    mapping.dpsId,
    mapping.nomisId,
    this.label,
    this.mappingType,
    this.whenCreated,
  )
