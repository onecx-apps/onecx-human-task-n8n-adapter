package org.tkit.onecx.human.task.adapter.rs.internal.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.tkit.onecx.human.task.adapter.client.model.N8nTaskDecisionRequest;

import gen.org.tkit.onecx.human.task.adapter.rs.internal.model.ProcessTaskRequestAdapterDTO;

@Mapper
public interface TaskAdapterMapper {

    @Mapping(target = "accepted", constant = "true")
    N8nTaskDecisionRequest toAcceptTaskRequest(ProcessTaskRequestAdapterDTO processTaskRequestAdapterDTO);

    @Mapping(target = "accepted", constant = "false")
    N8nTaskDecisionRequest toDeclineTaskRequest(ProcessTaskRequestAdapterDTO processTaskRequestAdapterDTO);
}
