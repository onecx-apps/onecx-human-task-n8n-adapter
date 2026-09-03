package org.tkit.onecx.human.task.adapter.rs.internal.controller;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.onecx.human.task.adapter.client.api.N8nApi;
import org.tkit.onecx.human.task.adapter.client.model.N8nTaskDecisionRequest;
import org.tkit.onecx.human.task.adapter.rs.internal.config.AdapterConfig;
import org.tkit.onecx.human.task.adapter.rs.internal.mappers.ExceptionMapper;
import org.tkit.onecx.human.task.adapter.rs.internal.mappers.TaskAdapterMapper;
import org.tkit.quarkus.log.cdi.LogService;

import gen.org.tkit.onecx.human.task.adapter.rs.internal.TasksAdapterApi;
import gen.org.tkit.onecx.human.task.adapter.rs.internal.model.ProblemDetailResponseAdapterDTO;
import gen.org.tkit.onecx.human.task.adapter.rs.internal.model.ProcessTaskRequestAdapterDTO;
import gen.org.tkit.onecx.human.task.adapter.rs.internal.model.ProviderTypeAdapterDTO;

@ApplicationScoped
@Transactional(Transactional.TxType.NOT_SUPPORTED)
@LogService
public class TasksAdapterRestController implements TasksAdapterApi {

    @Inject
    TaskAdapterMapper mapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Inject
    AdapterConfig config;

    @Inject
    @RestClient
    N8nApi client;

    @Context
    HttpHeaders headers;

    @Override
    public Response acceptTask(ProcessTaskRequestAdapterDTO processTaskRequestAdapterDTO) {
        return response(processTaskRequestAdapterDTO.getProviderType(),
                URI.create(processTaskRequestAdapterDTO.getProviderURL()),
                mapper.toAcceptTaskRequest(processTaskRequestAdapterDTO));
    }

    @Override
    public Response declineTask(ProcessTaskRequestAdapterDTO processTaskRequestAdapterDTO) {
        return response(processTaskRequestAdapterDTO.getProviderType(),
                URI.create(processTaskRequestAdapterDTO.getProviderURL()),
                mapper.toDeclineTaskRequest(processTaskRequestAdapterDTO));
    }

    private Response response(ProviderTypeAdapterDTO providerType, URI providerURL,
            N8nTaskDecisionRequest decision) {
        if (!Objects.equals(providerType, ProviderTypeAdapterDTO.N8_N)) {
            return exceptionMapper.providerException(Response.Status.BAD_REQUEST,
                    ExceptionMapper.ErrorKeys.INVALID_PROVIDER_TYPE.name(),
                    "This adapter only supports provider type N8N.");
        }
        try {
            validateUrl(providerURL);
        } catch (IllegalArgumentException ex) {
            return exceptionMapper.providerException(Response.Status.BAD_REQUEST,
                    ExceptionMapper.ErrorKeys.INVALID_PROVIDER_URL.name(), ex.getMessage());
        }
        try (var _ = client.callWebhook(providerURL, headers.getHeaderString(HttpHeaders.AUTHORIZATION), decision)) {
            return Response.noContent().build();
        }
    }

    private void validateUrl(URI providerURL) {
        URI base = config.baseUrl();
        if (!Objects.equals(providerURL.getHost(), base.getHost())) {
            throw new IllegalArgumentException("providerURL does not match the configured n8n instance " + config.baseUrl());
        }
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseAdapterDTO> constraint(ConstraintViolationException ex) {
        return exceptionMapper.constraint(ex);
    }

    @ServerExceptionMapper
    public Response restException(ClientWebApplicationException ex) {
        return exceptionMapper.clientException(ex);
    }

    @ServerExceptionMapper
    public Response timeoutException(SocketTimeoutException ex) {
        return exceptionMapper.timeoutException(ex);
    }
}
