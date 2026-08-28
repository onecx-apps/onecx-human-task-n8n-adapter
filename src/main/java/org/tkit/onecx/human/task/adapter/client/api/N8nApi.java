package org.tkit.onecx.human.task.adapter.client.api;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.net.URI;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.tkit.onecx.human.task.adapter.client.model.N8nTaskDecisionRequest;

import io.quarkus.rest.client.reactive.Url;

@RegisterRestClient(configKey = "n8n")
public interface N8nApi {

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    Response callWebhook(@Url URI url, @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization,
            N8nTaskDecisionRequest decisionRequest);
}
