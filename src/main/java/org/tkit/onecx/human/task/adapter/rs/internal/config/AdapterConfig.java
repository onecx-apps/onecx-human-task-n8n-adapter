package org.tkit.onecx.human.task.adapter.rs.internal.config;

import java.net.URI;

import io.quarkus.runtime.annotations.ConfigDocFilename;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigDocFilename("onecx-human-task-n8n-adapter.adoc")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "onecx.ht.adapter.n8n")
public interface AdapterConfig {

    /**
     * Base URL of the n8n instance, used to validate providerURL
     */
    @WithName("base-url")
    @WithDefault("http://localhost:5678")
    URI baseUrl();
}
