# J.A.R.V.I.S. v2.5-TITAN update

## Perbaikan
- Provider kini berupa preset yang dapat dipilih; Base URL otomatis.
- Migrasi konfigurasi v2.x: URL lama `/chat/completions` dinormalisasi menjadi base URL.
- API key selalu dibaca dari field UI saat Validate, bukan hanya dari SharedPreferences.
- Sanitasi clipboard diperluas untuk LRM/RLM/BOM/zero-width dan whitespace umum.
- Model catalog dideteksi langsung dari provider API key.
- OpenRouter/OpenAI-compatible: GET `/models`.
- Google Gemini: GET `/v1beta/models?key=...`.
- Model list memiliki pencarian dan dapat dipilih langsung.
- Error 403 agentic-only dibuat menjadi pesan yang dapat dipahami.
- HTML 404 tidak lagi ditampilkan mentah ke user.
- API key tidak pernah ditulis ke log oleh networking layer.
- Base URL tidak lagi menyimpan `/chat/completions`; request layer menambah endpoint tepat satu kali.
- Version bumped to 2.5-TITAN.

## Build
Project memakai Android Gradle Plugin 8.2.0 / Gradle 8.5, compileSdk 34, Java 17.
