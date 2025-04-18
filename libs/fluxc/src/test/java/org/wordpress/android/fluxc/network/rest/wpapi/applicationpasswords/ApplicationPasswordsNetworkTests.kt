package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.HttpMethod
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertIs

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class ApplicationPasswordsNetworkTests {
    private val testSite = SiteModel().apply {
        url = "http://test-site.com"
    }
    private val testCredentials = ApplicationPasswordCredentials(
        userName = "username",
        password = "password",
        uuid = "uuid"
    )

    private val requestQueue: RequestQueue = mock()
    private val userAgent: UserAgent = mock()
    private val listener: ApplicationPasswordsListener = mock()
    private val mApplicationPasswordsManager: ApplicationPasswordsManager = mock()
    private lateinit var network: ApplicationPasswordsNetwork

    @Before
    fun setup() {
        network = ApplicationPasswordsNetwork(
            requestQueue = requestQueue,
            userAgent = userAgent,
            listener = Optional.of(listener)
        ).apply {
            mApplicationPasswordsManager = this@ApplicationPasswordsNetworkTests.mApplicationPasswordsManager
        }
    }

    @Test
    fun `when sending a new request, then fetch the application password`() = runTest {
        givenSuccessResponse()
        whenever(mApplicationPasswordsManager.getApplicationCredentials(testSite))
            .thenReturn(ApplicationPasswordCreationResult.Created(testCredentials))

        network.executeGsonRequest(testSite, HttpMethod.GET, "path", TestResponse::class.java)

        verify(mApplicationPasswordsManager).getApplicationCredentials(testSite)
    }

    @Test
    fun `given a locally existing password, when password is revoked, then regenerate a new one`() = runTest {
        whenever(mApplicationPasswordsManager.getApplicationCredentials(testSite))
            .thenReturn(ApplicationPasswordCreationResult.Existing(testCredentials))
            .thenReturn(ApplicationPasswordCreationResult.Created(testCredentials))
        val networkError = VolleyError(NetworkResponse(401, byteArrayOf(), true, 0, emptyList()))
        givenErrorResponse(networkError)

        network.executeGsonRequest(testSite, HttpMethod.GET, "path", TestResponse::class.java)

        verify(mApplicationPasswordsManager).deleteLocalApplicationPassword(testSite)
        verify(mApplicationPasswordsManager, times(2)).getApplicationCredentials(testSite)
    }

    @Test
    fun `given request succeeds, when request is executed, then return the response`() = runTest {
        val expectedResponse = TestResponse("value")
        givenSuccessResponse(expectedResponse)
        whenever(mApplicationPasswordsManager.getApplicationCredentials(testSite))
            .thenReturn(ApplicationPasswordCreationResult.Existing(testCredentials))

        val response = network.executeGsonRequest(testSite, HttpMethod.GET, "path", TestResponse::class.java)

        assertIs<WPAPIResponse.Success<TestResponse>>(response)
        assertEquals(expectedResponse, response.data)
    }

    @Test
    fun `given request fails, when request is executed, then return the error`() = runTest {
        val networkError = VolleyError(NetworkResponse(500, byteArrayOf(), true, 0, emptyList()))
        givenErrorResponse(networkError)
        whenever(mApplicationPasswordsManager.getApplicationCredentials(testSite))
            .thenReturn(ApplicationPasswordCreationResult.Existing(testCredentials))

        val response = network.executeGsonRequest(testSite, HttpMethod.GET, "path", TestResponse::class.java)

        assertIs<WPAPIResponse.Error<TestResponse>>(response)
        assertEquals(networkError, response.error.volleyError)
    }

    @Test
    fun `given site doesn't support application passwords, when a new request, then notify listener`() =
        runTest {
            val networkError = BaseNetworkError(VolleyError(NetworkResponse(501, byteArrayOf(), true, 0, emptyList())))
            whenever(mApplicationPasswordsManager.getApplicationCredentials(testSite))
                .thenReturn(ApplicationPasswordCreationResult.NotSupported(BaseNetworkError(networkError)))

            network.executeGsonRequest(testSite, HttpMethod.GET, "path", TestResponse::class.java)

            verify(listener).onFeatureUnavailable(eq(testSite), any())
        }

    @Test
    fun `when a new password is created, then notify listener`() =
        runTest {
            givenSuccessResponse()
            whenever(mApplicationPasswordsManager.getApplicationCredentials(testSite))
                .thenReturn(ApplicationPasswordCreationResult.Created(testCredentials))

            network.executeGsonRequest(testSite, HttpMethod.GET, "path", TestResponse::class.java)

            verify(listener).onNewPasswordCreated(isPasswordRegenerated = false)
        }

    @Test
    fun `given a revoked local password, when a new password is created, then notify listener`() =
        runTest {
            whenever(mApplicationPasswordsManager.getApplicationCredentials(testSite))
                .thenReturn(ApplicationPasswordCreationResult.Existing(testCredentials))
                .thenReturn(ApplicationPasswordCreationResult.Created(testCredentials))
            val networkError = VolleyError(NetworkResponse(401, byteArrayOf(), true, 0, emptyList()))
            givenErrorResponse(networkError)

            network.executeGsonRequest(testSite, HttpMethod.GET, "path", TestResponse::class.java)

            verify(listener).onNewPasswordCreated(isPasswordRegenerated = true)
        }

    @Suppress("UNCHECKED_CAST")
    private fun givenSuccessResponse(response: TestResponse = TestResponse("")) {
        whenever(requestQueue.add(any<WPAPIGsonRequest<TestResponse>>())).thenAnswer { invocation ->
            val request = (invocation.arguments.first() as WPAPIGsonRequest<TestResponse>)

            val deliverMethod = Request::class.java.getDeclaredMethod("deliverResponse", Any::class.java)
            deliverMethod.isAccessible = true
            deliverMethod.invoke(request, response)

            return@thenAnswer request
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun givenErrorResponse(error: VolleyError) {
        whenever(requestQueue.add(any<WPAPIGsonRequest<TestResponse>>())).thenAnswer { invocation ->
            val request = (invocation.arguments.first() as WPAPIGsonRequest<TestResponse>)
            request.deliverError(error)

            return@thenAnswer request
        }
    }

    data class TestResponse(val value: String)
}
