# JARVIS v2.8 TITAN+

## Provider/API reliability
- Base URL is now stored normalized without `/chat/completions`.
- Legacy full endpoints are automatically migrated/normalized.
- Chat requests append `/chat/completions` exactly once.
- API keys sanitize invisible Unicode/control characters and clipboard wrappers.
- Added live API test with model-count feedback.
- Added dynamic model discovery for OpenAI-compatible providers.
- Added native Gemini model discovery with `generateContent` filtering.
- Added HTML-404 detection and safer API error messages.

## UI
- Reworked Control Center into compact card-based sections.
- Provider selection automatically sets the correct base URL.
- Added `🔧 CUSTOM API` for arbitrary OpenAI-compatible endpoints.
- Added clipboard paste for API keys.
- Added model loading/searchable dropdown.
- Added API/model status indicators.
- Added Chat/Agent toggle on the main screen.

## Chat / Agent
- The same selected model is used in both modes.
- Chat mode explicitly disables tool/action execution.
- Agent mode preserves the existing tool execution pipeline.
