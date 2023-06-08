package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing

@Configuration
@EnableR2dbcAuditing
class AuditConfig
