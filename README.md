# userService

## Authentication and authorization

UserService is an OAuth2 resource server: it validates Keycloak access tokens (JWT) and never
stores passwords. Credentials and roles live in the separate `authService` (Keycloak + its own
PostgreSQL database).

### Access matrix

| Endpoint | Anonymous | USER | ADMIN |
|---|---|---|---|
| `POST /api/users` (registration) | yes | yes | yes |
| `GET /api/users` | no | no | yes |
| `GET /api/users/{id}` | no | own only | any |
| `PUT /api/users/{id}` | no | own only | any |
| `DELETE /api/users/{id}` | no | no | yes |
| `PATCH /api/users/{id}/activate` / `deactivate` | no | no | yes |
| `/api/cards/**` | no | own only | any |

### How it works

- The JWT `sub` claim is the Keycloak user id. It is also used as the local user id, so
  user-scoped endpoints compare `sub` with the path `{id}`.
- `realm_access.roles` is mapped to `ROLE_ADMIN` / `ROLE_USER`.
- Registration creates the user in Keycloak first (Keycloak generates the id), then stores the
  profile locally with the same id.
- Deleting/activating/deactivating a user is mirrored to Keycloak through the Admin REST API.
  Deactivation also revokes Keycloak sessions, because access tokens are valid for 12 hours.
- `ActiveUserFilter` additionally rejects tokens of locally deactivated users on every request.

### Configuration

```properties
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/jacapp/protocol/openid-connect/certs
keycloak.admin.url=http://localhost:8180
keycloak.admin.realm=jacapp
keycloak.admin.client-id=jacapp-backend
keycloak.admin.client-secret=jacapp-backend-secret
```

Start Keycloak first from the `authService` repository, then UserService. In Docker the services
share the external `jacapp-auth-network`, so Keycloak is reachable as `http://keycloak:8080`.
