package org.tkit.onecx.human.task.adapter.client.model;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class N8nTaskDecisionRequest {
    private boolean accepted;
    private Map<String, String> customInput;
}
