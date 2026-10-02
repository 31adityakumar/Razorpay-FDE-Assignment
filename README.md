# Freshdesk Agent Connector

## 1. Overview

This repository implements Assignment 3: a private connector for a merchant support tool.

The selected tool is **Freshdesk**. The connector gives an Agent Studio agent safe, read-only access to support data through normalized REST resources and an MCP-equivalent tool manifest.

Example agent request:

> Find all high-priority open tickets created after yesterday.

The agent can call `search_tickets` with structured filters instead of reasoning over Freshdesk's vendor-specific response format.

### What the agent can do

- List tickets with bounded pagination
- Fetch one ticket by ID
- Search tickets by status, priority, creation time, and subject text
- Read a ticket's conversation timeline
- Search contacts by name or email

### What the agent cannot do

- Create, update, or delete tickets
- Send replies or messages
- Modify contacts
- Trigger refunds or billing actions
- Read API keys or other credentials

The connector is intentionally read-only to reduce the risk of an agent making irreversible changes in a merchant account.

## 2. Architecture

```text
Agent Studio / MCP adapter
             |
       GET /mcp/tools
             |
       Agent-facing REST API
             |
          Controllers
             |
      FreshdeskClient interface
          /             \
 MockFreshdeskClient   HttpFreshdeskClient
   fictional data       Freshdesk REST API
```

The `FreshdeskClient` interface separates the agent-facing contract from the vendor integration. This allows the same controllers and response models to run against fictional data during evaluation or against a real Freshdesk account when configured.

## 3. Technology

- Java 25
- Spring Boot 3.4.13
- Maven
- Spring MVC / REST Client
- JUnit 5 and Spring Boot Test
- Docker-compatible runtime

No database is required. The mock implementation stores a small set of fictional records in memory.

## 4. Quick start: safe demo mode

Requirements:

- Java 25+
- Maven 3.9+

Start the service:

```bash
mvn spring-boot:run
```

The default configuration is `FRESHDESK_MOCK=true`, so no Freshdesk account, API key, or customer data is required.

Inspect the tool manifest:

```bash
curl http://localhost:8080/mcp/tools
```

Try the connector APIs:

```bash
# List the first page of tickets
curl "http://localhost:8080/api/tickets?page=1&perPage=2"

# Get one ticket
curl http://localhost:8080/api/tickets/1001

# Search by status and priority
curl "http://localhost:8080/api/tickets/search?status=open&priority=high"

# Read the conversation timeline
curl http://localhost:8080/api/tickets/1001/conversations

# Search contacts
curl "http://localhost:8080/api/contacts/search?query=asha"
```

There is also a shell walkthrough in [`examples/sample-requests.sh`](examples/sample-requests.sh). It requires `curl` and `jq`.

## 5. Live Freshdesk mode

The live client uses Freshdesk API-key authentication over HTTPS. Configure the following environment variables before starting the application:

```bash
FRESHDESK_MOCK=false
FRESHDESK_DOMAIN=your-subdomain.freshdesk.com
FRESHDESK_API_KEY=your-api-key
```

Freshdesk API-key authentication is HTTP Basic Auth with the API key as the username and `X` as the placeholder password. The key is read from the environment only and is never included in tool responses.

For PowerShell:

```powershell
$env:FRESHDESK_MOCK="false"
$env:FRESHDESK_DOMAIN="your-subdomain.freshdesk.com"
$env:FRESHDESK_API_KEY="your-api-key"
mvn spring-boot:run
```

Do not commit API keys or real customer data.

## 6. Tool manifest

`GET /mcp/tools` returns the agent-facing tool specification. It is deliberately transport-neutral so it can be connected to an Agent Studio HTTP/MCP adapter.

| Tool | Required input | Behavior |
|---|---|---|
| `list_tickets` | `page`, `perPage` optional | Returns a bounded page of normalized tickets |
| `get_ticket` | `id` | Returns one normalized ticket or a structured 404 |
| `search_tickets` | All filters optional | Searches by status, priority, creation time, and subject text |
| `get_ticket_conversations` | `ticketId` | Returns the ticket conversation timeline |
| `search_contacts` | `query` | Searches contacts by name or email |

Every tool is marked `readOnly: true`. The JSON schemas expose required fields, types, and pagination limits to an agent runtime.

## 7. REST endpoint reference

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/tickets?page=1&perPage=30` | List tickets; `perPage` is limited to 100 |
| `GET` | `/api/tickets/{id}` | Get one ticket |
| `GET` | `/api/tickets/search` | Search using `status`, `priority`, `createdAfter`, and `query` |
| `GET` | `/api/tickets/{id}/conversations` | Get ticket conversations |
| `GET` | `/api/contacts/search?query=...` | Search contacts |
| `GET` | `/mcp/tools` | Get the tool manifest |

Successful responses use normalized models rather than returning arbitrary Freshdesk payloads. Errors use a structured shape such as:

```json
{
  "error": "UPSTREAM_ERROR",
  "message": "Freshdesk request failed: Too Many Requests",
  "retryable": true,
  "timestamp": "2026-10-02T10:00:00Z"
}
```

## 8. Reliability decisions

### Authentication

The API key is configured through environment variables and applied only by the live client. It is never accepted as an agent tool argument and never returned in a response.

### Retries and rate limits

The live client retries HTTP 429 and 5xx responses with bounded exponential backoff. When Freshdesk sends `Retry-After`, that value is honored. After the retry budget is exhausted, the connector returns an explicit structured error instead of fabricating a result.

### Timeouts

Connect and read timeouts are configured through `FRESHDESK_REQUEST_TIMEOUT_MS`, defaulting to 5000 milliseconds.

### Pagination

The public list endpoint validates positive page numbers and limits page size to 100. The live client maps Freshdesk's pagination into the normalized `Page<T>` response.

### Response normalization

Freshdesk uses numeric status and priority values and snake_case fields. The live client maps these into agent-friendly values such as `"open"`, `"high"`, `requesterId`, and ISO-8601 timestamps.

### Failure behavior

- Missing records return 404-style structured errors.
- Invalid request parameters return a client error.
- Upstream 429 and 5xx failures are marked retryable.
- The service never returns an empty success response to hide an upstream failure.

## 9. Testing

Run the complete test suite:

```bash
mvn test
```

The tests cover:

- Normalized mock ticket search
- Tool-manifest shape and read-only flags
- Missing-ticket error behavior
- Live Freshdesk field mapping
- Freshdesk status and priority filter construction
- 429 handling with `Retry-After` and a successful retry

Build the deployable JAR:

```bash
mvn package -DskipTests
```

## 10. Docker

Build and run the mock demo:

```bash
mvn package -DskipTests
docker compose up --build
```

The service listens on port 8080. The Docker image does not contain credentials; live credentials should be injected by the deployment environment.

## 11. Security and deployment assumptions

This sample assumes that the deployment boundary provides:

- Authentication of callers
- Authorization and tenant routing
- TLS termination
- Secret storage and rotation
- Request logging and monitoring

The service should not be exposed directly to the public internet without those controls. Production logs should redact authorization headers and ticket conversation bodies.

## 12. Limitations and long-term improvements

This is a read-only connector demonstration, not a full Freshdesk synchronization platform. It does not currently provide:

- Webhook or event synchronization
- A persistent cache
- Multi-tenant secret provisioning
- Write operations
- A full stateful MCP session/transport implementation
- Production metrics and distributed rate-limit coordination

The next production steps would be to add tenant-scoped secret management, correlation IDs and metrics, a shared rate-limit budget, WireMock contract coverage for more Freshdesk response variants, and an MCP transport adapter selected by the Agent Studio deployment.

## 13. Assignment requirement checklist

| Requirement | Implementation |
|---|---|
| Working authentication flow | Freshdesk API-key Basic Auth from environment variables |
| List primitive | `list_tickets` and `GET /api/tickets` |
| Get primitive | `get_ticket` and `GET /api/tickets/{id}` |
| Search primitive | `search_tickets` and `GET /api/tickets/search` |
| Rate-limit handling | 429 detection, `Retry-After`, bounded backoff, structured failure |
| Pagination | Validated page/per-page inputs and normalized page response |
| MCP specification or equivalent | `GET /mcp/tools` tool manifest with JSON-like schemas |
| Agent boundaries | Explicit read-only tools and documented limitations |
| Working demonstration | Mock mode, curl examples, tests, and Docker setup |
| No real credentials/customer data | Synthetic `example.test` records and environment-only secrets |
