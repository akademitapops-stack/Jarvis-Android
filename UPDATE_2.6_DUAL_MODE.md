# JARVIS v2.6 TITAN — Dual Chat + Agent Mode

## Perubahan
- Model yang dipilih tetap satu dan dipakai untuk dua fungsi: normal chat dan agent tasks.
- Ditambahkan tombol `⚡ AGENT` / `💬 CHAT` di halaman utama.
- Agent mode memakai agent harness internal JARVIS: model tetap dipanggil melalui provider API biasa, lalu JARVIS mengeksekusi tool/action dan mengirim feedback ke model pada putaran berikutnya.
- Chat mode mempunyai system prompt khusus tanpa tools.
- Ada safety boundary di AIManager: pada Chat mode seluruh action/tool fields dibersihkan walaupun model salah mengikuti instruksi.
- Mode tersimpan di SharedPreferences dan default-nya AGENT agar perilaku v2.5 tetap kompatibel.
- Model tidak diganti saat berpindah mode.

## Catatan penting tentang model
Model agentic-only seperti `thinkingmachines/inkling:free` yang ditolak provider dengan HTTP 403 tidak dapat dipaksa oleh aplikasi menjadi model biasa. JARVIS dapat menjadi harness agent sendiri untuk model yang menerima endpoint chat API normal, tetapi provider tetap dapat membatasi model tertentu.
