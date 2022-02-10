package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
@EnableR2dbcRepositories
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
class ResourceServerConfiguration {

  @Bean
  fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
    return http
      .authorizeExchange {
        it.pathMatchers(
          "/webjars/**", "/favicon.ico", "/csrf",
          "/health/**", "/info",
          "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
        ).permitAll()
          .anyExchange().authenticated()
      }
      .oauth2ResourceServer { it.jwt().jwtAuthenticationConverter(AuthAwareTokenConverter()) }
      .build()
  }
}
