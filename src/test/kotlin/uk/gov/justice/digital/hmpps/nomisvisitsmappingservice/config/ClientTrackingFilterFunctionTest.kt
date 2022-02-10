package uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.config

import com.microsoft.applicationinsights.web.internal.RequestTelemetryContext
import com.microsoft.applicationinsights.web.internal.ThreadContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.MapEntry.entry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.nomisvisitsmappingservice.helper.JwtAuthHelper
import java.time.Duration

class ClientTrackingFilterFunctionTest {
  private val jwtAuthHelper = JwtAuthHelper()
  private val request: ClientRequest = mock()
  private val response: ClientResponse = mock()
  private val httpHeaders: HttpHeaders = mock()
  private val next: ExchangeFunction = mock()
  private val filter = ClientTrackingFilterFunction()

  @BeforeEach
  fun setup() {
    ThreadContext.setRequestTelemetryContext(RequestTelemetryContext(1L))
    whenever(request.headers()).thenReturn(httpHeaders)
    whenever(next.exchange(any())).thenReturn(Mono.just(response))
  }

  @AfterEach
  fun tearDown() {
    ThreadContext.remove()
  }

  @Test
  fun shouldAddClientIdAndUserNameToInsightTelemetry() {
    val token = jwtAuthHelper.createJwt("AUTH_ADM")
    whenever(httpHeaders.getFirst(anyString())).thenReturn("Bearer $token")
    filter.filter(request, next)
    val insightTelemetry = ThreadContext.getRequestTelemetryContext().httpRequestTelemetry.properties
    assertThat(insightTelemetry).contains(entry("username", "AUTH_ADM"), entry("clientId", "nomis-visits-mapping"))
  }

  @Test
  fun shouldAddClientIdAndUserNameToInsightTelemetryEvenIfTokenExpired() {
    val token = jwtAuthHelper.createJwt("Fred", expiryTime = Duration.ofHours(-1L))
    whenever(httpHeaders.getFirst(anyString())).thenReturn("Bearer $token")
    filter.filter(request, next)
    val insightTelemetry = ThreadContext.getRequestTelemetryContext().httpRequestTelemetry.properties
    assertThat(insightTelemetry).contains(entry("username", "Fred"), entry("clientId", "nomis-visits-mapping"))
  }
}
