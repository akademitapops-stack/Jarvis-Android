# JARVIS v2.9 fixes

- Provider field is authoritative; profile names can no longer force Gemini.
- Base URL is automatically derived from the selected provider, including runtime API access.
- Model picker is visible beside Load Models and retains the active model.
- OpenRouter model catalog keeps the selected model even when filtering.
- Chat response parsing now accepts string and structured/array `content`, preventing `Parse API gagal: JsonObject`.
- Image Studio has explicit validation, request timeouts, better error handling, and URL image display.
- Web Tools restores previously saved Brave/custom settings when opened.

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
