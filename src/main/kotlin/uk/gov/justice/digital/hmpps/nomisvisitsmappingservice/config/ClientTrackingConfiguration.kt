package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config

import com.microsoft.applicationinsights.web.internal.ThreadContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import java.text.ParseException
import java.util.Optional

@Configuration
@ConditionalOnExpression("T(org.apache.commons.lang3.StringUtils).isNotBlank('\${applicationinsights.connection.string:}')")
class ClientTrackingFilterFunction : ExchangeFilterFunction {
  private val bearer = "Bearer "

  override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
    val token = request.headers().getFirst(HttpHeaders.AUTHORIZATION)
    if (token?.startsWith(bearer, ignoreCase = true) == true) {
      try {
        val jwtBody = getClaimsFromJWT(token)
        val properties = ThreadContext.getRequestTelemetryContext().httpRequestTelemetry.properties
        val user = Optional.ofNullable(jwtBody.getClaim("user_name"))
        user.map { it.toString() }.ifPresent { properties["username"] = it }
        properties["clientId"] = jwtBody.getClaim("client_id").toString()
      } catch (e: ParseException) {
        log.warn("problem decoding jwt public key for application insights", e)
      }
    }
    return next.exchange(request)
  }

  private fun getClaimsFromJWT(token: String): JWTClaimsSet =
    SignedJWT.parse(token.replace(bearer, "")).jwtClaimsSet

  private companion object {
    private val log = LoggerFactory.getLogger(ClientTrackingFilterFunction::class.java)
  }
}
