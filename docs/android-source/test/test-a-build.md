# Test a Build

**Source:** https://source.android.com/docs/setup/test
**Scrape Date:** 2025-10-23
**Note:** Content was scraped in Indonesian.

---

**(Verbatim content in Indonesian)**

Untuk menguji build, Anda dapat menjalankan build di emulator atau menjalankan (mem-flash) build di perangkat yang sebenarnya.

Catatan: Jika Anda ingin menguji build di perangkat, jangan lupa untuk mendapatkan biner eksklusif atau build Anda tidak akan berhasil di-boot. Terkadang sumber mungkin memiliki biner yang berbeda untuk build dan cabang yang berbeda. Jika Anda mendapatkan blob biner pada tahap ini, Anda perlu mengekstraknya, m installclean, dan membangun ulang. Untuk mengetahui informasi selengkapnya tentang proses ini, lihat Mendownload biner eksklusif.


## MENGUJI BUILD ANDA DI EMULATOR

Cuttlefish adalah emulator perangkat virtual yang digunakan untuk menguji build Anda. Untuk informasi tentang menjalankan build di Cuttlefish, lihat panduan Memulai.

Untuk pengujian aplikasi, termasuk aplikasi Google Automotive Services (GAS), gunakan Android Emulator.


## MENGUJI BUILD DI PERANGKAT

Alat fastboot memungkinkan Anda mem-flash lebih banyak jenis perangkat daripada Android Flash Tool dan dapat digunakan untuk mem-flash build lokal Anda sendiri ke perangkat untuk pengujian.

Untuk mengetahui informasi tentang penggunaan alat command line fastboot, lihat Mem-flash dengan Fastboot

Catatan: Google menyediakan alat flash tambahan, yang disebut Android Flash Tool yang ditujukan untuk digunakan hanya dengan build yang telah dibuat sebelumnya. Android Flash Tool lebih mudah digunakan daripada fastboot, tetapi mendukung lebih sedikit perangkat.
