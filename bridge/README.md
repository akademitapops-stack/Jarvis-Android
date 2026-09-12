# JARVIS WhatsApp Baileys Bridge

This optional Node.js service runs Baileys outside the Android app and exposes a small pairing endpoint.

## Run

```bash
cd bridge
npm install
node server.js
```

Then put the public bridge URL into **Settings → WhatsApp / Baileys Bridge** in JARVIS and enter the WhatsApp number in international format.

The Android app requests `POST /pair/code` and displays the returned pairing code.

**Important:** Baileys requires a persistent Node.js process and WhatsApp session storage. Do not deploy this as a stateless edge function. Protect the bridge with HTTPS and authentication before exposing it to the internet.
