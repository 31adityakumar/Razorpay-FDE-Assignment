#!/usr/bin/env bash
set -euo pipefail

# Starts in mock mode; no credentials or real customer data are required.
curl -s http://localhost:8080/mcp/tools | jq .
curl -s 'http://localhost:8080/api/tickets?page=1&perPage=2' | jq .
curl -s 'http://localhost:8080/api/tickets/search?status=open&priority=high' | jq .
curl -s http://localhost:8080/api/tickets/1001/conversations | jq .
curl -s 'http://localhost:8080/api/contacts/search?query=asha' | jq .
