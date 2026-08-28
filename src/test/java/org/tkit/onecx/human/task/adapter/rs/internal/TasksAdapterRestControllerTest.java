package org.tkit.onecx.human.task.adapter.rs.internal;

import static io.restassured.RestAssured.given;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;
import org.mockserver.model.MediaType;
import org.tkit.onecx.human.task.adapter.client.model.N8nTaskDecisionRequest;
import org.tkit.onecx.human.task.adapter.rs.internal.controller.TasksAdapterRestController;
import org.tkit.onecx.human.task.adapter.rs.internal.mappers.ExceptionMapper;
import org.tkit.quarkus.security.test.GenerateKeycloakClient;

import gen.org.tkit.onecx.human.task.adapter.rs.internal.model.ProblemDetailResponseAdapterDTO;
import gen.org.tkit.onecx.human.task.adapter.rs.internal.model.ProcessTaskRequestAdapterDTO;
import gen.org.tkit.onecx.human.task.adapter.rs.internal.model.ProviderTypeAdapterDTO;
import io.quarkiverse.mockserver.test.InjectMockServerClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestHTTPEndpoint(TasksAdapterRestController.class)
@GenerateKeycloakClient(clientName = "testClient", scopes = { "ocx-ht:all", "ocx-ht:write" })
class TasksAdapterRestControllerTest extends AbstractTest {

    private static final String ACCEPT_ENDPOINT = "/accept";
    private static final String DECLINE_ENDPOINT = "/decline";
    private static final String N8N_WEBHOOK_PATH = "/webhook-waiting/1";
    private static final String N8N_WEBHOOK_QUERY_PARAM = "signature";
    private static final String N8N_WEBHOOK_QUERY_PARAM_VAL = "099a2";
    private static final String N8N_WEBHOOK_URL = ConfigProvider.getConfig()
            .getValue("quarkus.mockserver.endpoint", String.class) + N8N_WEBHOOK_PATH + "?" + N8N_WEBHOOK_QUERY_PARAM + "="
            + N8N_WEBHOOK_QUERY_PARAM_VAL;
    private static final String TENANT = "org1";

    @InjectMockServerClient
    MockServerClient mockServerClient;

    @AfterEach
    void resetMocks() {
        mockServerClient.clear(HttpRequest.request().withPath(N8N_WEBHOOK_PATH));
    }

    @Test
    void acceptTask() {
        mockN8n(Response.Status.OK.getStatusCode());
        String keycloakToken = getKeycloakClientToken("testClient");

        given().when().auth().oauth2(keycloakToken).header(APM_HEADER_PARAM, createToken(TENANT))
                .contentType(APPLICATION_JSON).body(createProcessTaskRequestDto()).post(ACCEPT_ENDPOINT).then()
                .statusCode(NO_CONTENT.getStatusCode());

        mockServerClient.verify(HttpRequest.request().withMethod(HttpMethod.POST).withPath(N8N_WEBHOOK_PATH)
                .withQueryStringParameter(N8N_WEBHOOK_QUERY_PARAM, N8N_WEBHOOK_QUERY_PARAM_VAL)
                .withHeader("Authorization", "Bearer " + keycloakToken)
                .withBody(JsonBody.json(decision(true, Map.of("ocx-key-1", "ocx-val-1")))));
    }

    @Test
    void acceptTask_shouldReturn404_whenTaskNotFoundInProvider() {
        mockN8n(Response.Status.NOT_FOUND.getStatusCode());

        given().when().auth().oauth2(getKeycloakClientToken("testClient")).header(APM_HEADER_PARAM, createToken(TENANT))
                .contentType(APPLICATION_JSON).body(createProcessTaskRequestDto()).post(ACCEPT_ENDPOINT).then()
                .statusCode(NOT_FOUND.getStatusCode());
    }

    @Test
    void acceptTask_shouldReturn400_whenProviderReturnsError() {
        mockN8n(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        var exception = given().when().auth().oauth2(getKeycloakClientToken("testClient"))
                .header(APM_HEADER_PARAM, createToken(TENANT)).contentType(APPLICATION_JSON).body(createProcessTaskRequestDto())
                .post(ACCEPT_ENDPOINT).then().statusCode(BAD_REQUEST.getStatusCode()).contentType(APPLICATION_JSON)
                .extract().as(ProblemDetailResponseAdapterDTO.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(ExceptionMapper.ErrorKeys.PROVIDER_ERROR.name());
    }

    @Test
    void acceptTask_shouldReturn400_whenProviderTypeIsNotN8N() {
        mockN8n(Response.Status.OK.getStatusCode());

        var request = new ProcessTaskRequestAdapterDTO(ProviderTypeAdapterDTO.CAMUNDA, "task-1", N8N_WEBHOOK_URL);

        var exception = given().when().auth().oauth2(getKeycloakClientToken("testClient"))
                .header(APM_HEADER_PARAM, createToken(TENANT)).contentType(APPLICATION_JSON).body(request).post(ACCEPT_ENDPOINT)
                .then().statusCode(BAD_REQUEST.getStatusCode()).contentType(APPLICATION_JSON).extract()
                .as(ProblemDetailResponseAdapterDTO.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(ExceptionMapper.ErrorKeys.INVALID_PROVIDER_TYPE.name());

        mockServerClient.verifyZeroInteractions();
    }

    @Test
    void acceptTask_shouldReturn400_whenProviderURLIsNotAllowed() {
        var request = new ProcessTaskRequestAdapterDTO(ProviderTypeAdapterDTO.N8_N, "task-1",
                "http://other-n8n.example.com/webhook/human-task");

        var exception = given().when().auth().oauth2(getKeycloakClientToken("testClient"))
                .header(APM_HEADER_PARAM, createToken(TENANT)).contentType(APPLICATION_JSON).body(request).post(ACCEPT_ENDPOINT)
                .then().statusCode(BAD_REQUEST.getStatusCode()).contentType(APPLICATION_JSON).extract()
                .as(ProblemDetailResponseAdapterDTO.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(ExceptionMapper.ErrorKeys.INVALID_PROVIDER_URL.name());

        mockServerClient.verifyZeroInteractions();
    }

    @Test
    void acceptTask_shouldReturn400_whenBodyDoesNotExist() {
        var exception = given().when().auth().oauth2(getKeycloakClientToken("testClient"))
                .header(APM_HEADER_PARAM, createToken(TENANT)).contentType(APPLICATION_JSON).post(ACCEPT_ENDPOINT).then()
                .statusCode(BAD_REQUEST.getStatusCode()).contentType(APPLICATION_JSON).extract()
                .as(ProblemDetailResponseAdapterDTO.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(ExceptionMapper.ErrorKeys.CONSTRAINT_VIOLATIONS.name());
        assertThat(exception.getInvalidParams()).isNotEmpty();
    }

    @Test
    void declineTask() {
        mockN8n(Response.Status.OK.getStatusCode());
        String keycloakToken = getKeycloakClientToken("testClient");

        given().when().auth().oauth2(keycloakToken).header(APM_HEADER_PARAM, createToken(TENANT))
                .contentType(APPLICATION_JSON).body(createProcessTaskRequestDto()).post(DECLINE_ENDPOINT).then()
                .statusCode(NO_CONTENT.getStatusCode());

        mockServerClient.verify(HttpRequest.request().withMethod(HttpMethod.POST).withPath(N8N_WEBHOOK_PATH)
                .withQueryStringParameter(N8N_WEBHOOK_QUERY_PARAM, N8N_WEBHOOK_QUERY_PARAM_VAL)
                .withHeader("Authorization", "Bearer " + keycloakToken)
                .withBody(JsonBody.json(decision(false, Map.of("ocx-key-1", "ocx-val-1")))));
    }

    @Test
    void declineTask_shouldReturn404_whenTaskNotFoundInProvider() {
        mockN8n(Response.Status.NOT_FOUND.getStatusCode());

        given().when().auth().oauth2(getKeycloakClientToken("testClient")).header(APM_HEADER_PARAM, createToken(TENANT))
                .contentType(APPLICATION_JSON).body(createProcessTaskRequestDto()).post(DECLINE_ENDPOINT).then()
                .statusCode(NOT_FOUND.getStatusCode());
    }

    private ProcessTaskRequestAdapterDTO createProcessTaskRequestDto() {
        var request = new ProcessTaskRequestAdapterDTO(ProviderTypeAdapterDTO.N8_N, "task-1", N8N_WEBHOOK_URL);
        request.setCustomInput(Map.of("ocx-key-1", "ocx-val-1"));
        return request;
    }

    private N8nTaskDecisionRequest decision(boolean accepted, Map<String, String> customInput) {
        var decision = new N8nTaskDecisionRequest();
        decision.setAccepted(accepted);
        decision.setCustomInput(customInput);
        return decision;
    }

    private void mockN8n(int statusCode) {
        mockServerClient
                .when(HttpRequest.request().withPath(N8N_WEBHOOK_PATH)
                        .withQueryStringParameter(N8N_WEBHOOK_QUERY_PARAM, N8N_WEBHOOK_QUERY_PARAM_VAL)
                        .withMethod(HttpMethod.POST))
                .withPriority(100).withId(MOCK_ID)
                .respond(_ -> HttpResponse.response().withStatusCode(statusCode)
                        .withContentType(MediaType.APPLICATION_JSON));
    }
}
