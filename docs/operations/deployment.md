# Deployment

The backend is a Spring Boot service on port `8096`. Use profiles `local`, `test`, `staging`, and `production`; secrets must be supplied through environment variables or a secret manager, not YAML.

TLS is expected at a trusted reverse proxy/load balancer. Production should expose only `/api/v1/**` and selected actuator health/metrics endpoints to trusted networks.

Paystack remains configured for real API calls with test credentials only during this phase.
