# JARVIS v2.5 TITAN+

- Multi AI profiles: provider, base URL, model and API key persist per profile.
- API secrets use Android Keystore-backed encryption where available.
- Legacy single-key settings are migrated into the first profile.
- Persistent chat sessions with new-chat and session switching.
- Editable `memory.md`, `soul.md`, `rules.md`, `profile.md` in the private app workspace.
- Custom tool builder; AI can persist a tool definition and invoke it through the existing command safety validator.
- Accessibility UI control foundation: tap text, type, back, home, recents and scroll.
- Google Calendar event creation when calendar permission is granted.
- GitHub repository browsing through the GitHub API.
- Image Studio using OpenAI-compatible `/images/generations` endpoints; image model compatibility depends on the provider.
- Telegram long-poll agent with allowlist protection.
- Optional WhatsApp Baileys bridge under `bridge/` for pairing-code flow.
- Root mode is capability-based: JARVIS cannot grant root; a rooted device/Magisk-style `su` is required.
- Modern black/white chat presentation and quick actions.

## Security notes

Credentials are never printed by the app's normal UI. Telegram, GitHub and WhatsApp credentials are stored through the same local secret store. Remote bridges should be protected with HTTPS/authentication and strict allowlists.
