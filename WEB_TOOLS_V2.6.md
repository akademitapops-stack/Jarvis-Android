# JARVIS v2.7 WEB+

Web layer is fully wired into the agent loop.

- Brave Search API: web, news and image search.
- Generic custom JSON search provider.
- `web_open[]` reads public HTTP/HTTPS pages with bounded text extraction.
- Search/news URLs are clickable in chat.
- Image results render inline with a **Download gambar** button. Downloads go to `Download/JARVIS`.
- Image search uses Brave SafeSearch strict by default.
- Scraper blocks localhost/private/link-local destinations and limits page size.
- Configure from **Settings → Web Tools**.

Brave API documentation: https://api-dashboard.search.brave.com/documentation/services/image-search
