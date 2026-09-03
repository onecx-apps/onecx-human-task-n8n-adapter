package org.tkit.onecx.human.task.adapter.rs.internal.mappers;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.jboss.resteasy.reactive.RestResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import gen.org.tkit.onecx.human.task.adapter.rs.internal.model.ProblemDetailInvalidParamAdapterDTO;
import gen.org.tkit.onecx.human.task.adapter.rs.internal.model.ProblemDetailResponseAdapterDTO;

@Mapper
public interface ExceptionMapper {

    default RestResponse<ProblemDetailResponseAdapterDTO> constraint(ConstraintViolationException ex) {
        var dto = exception(ErrorKeys.CONSTRAINT_VIOLATIONS.name(), ex.getMessage());
        dto.setInvalidParams(createErrorValidationResponse(ex.getConstraintViolations()));
        return RestResponse.status(Response.Status.BAD_REQUEST, dto);
    }

    default Response clientException(ClientWebApplicationException ex) {
        if (ex.getResponse().getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return providerException(Response.Status.BAD_REQUEST, ErrorKeys.PROVIDER_ERROR.name(),
                "n8n provider returned status " + ex.getResponse().getStatus());
    }

    default Response timeoutException(Exception ex) {
        return providerException(Response.Status.BAD_REQUEST, ErrorKeys.PROVIDER_TIMEOUT.name(),
                "n8n provider did not answer in time");
    }

    /**
     * Convenience method for building a ProblemDetailResponse
     */
    default Response providerException(Response.Status status, String errorCode, String detail) {
        return Response.status(status).type(APPLICATION_JSON).entity(exception(errorCode, detail)).build();
    }

    @Mapping(target = "removeParamsItem", ignore = true)
    @Mapping(target = "params", ignore = true)
    @Mapping(target = "invalidParams", ignore = true)
    @Mapping(target = "removeInvalidParamsItem", ignore = true)
    ProblemDetailResponseAdapterDTO exception(String errorCode, String detail);

    List<ProblemDetailInvalidParamAdapterDTO> createErrorValidationResponse(
            Set<ConstraintViolation<?>> constraintViolation);

    @Mapping(target = "name", source = "propertyPath")
    @Mapping(target = "message", source = "message")
    ProblemDetailInvalidParamAdapterDTO createError(ConstraintViolation<?> constraintViolation);

    default String mapPath(Path path) {
        return path.toString();
    }

    enum ErrorKeys {
        CONSTRAINT_VIOLATIONS,
        INVALID_PROVIDER_TYPE,
        INVALID_PROVIDER_URL,
        PROVIDER_ERROR,
        PROVIDER_TIMEOUT
    }
}
