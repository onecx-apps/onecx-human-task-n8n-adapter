# onecx-human-task-n8n-adapter

OneCX Human Task n8n provider adapter.

The adapter uses its own OIDC service-account token for n8n callbacks.
`apm-principal-token` is propagated separately as tenant/principal context.
Its Keycloak client scopes in `src/main/helm/values.yaml` cover the workflow's
downstream operations: `ocx-ev:all`, `ocx-ai:all`, `ocx-ht:write`,
`ocx-mail:read`, `ocx-mail:write`, and `ocx-tk:write`.
