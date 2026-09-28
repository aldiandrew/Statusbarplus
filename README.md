# Statusbarplus

Statusbarplus adalah aplikasi Android yang menampilkan informasi hari dan tanggal langsung di area status bar melalui mekanisme notifikasi Android. Aplikasi dibuat untuk perangkat Android modern dan dapat digunakan tanpa root, tanpa Xposed, tanpa Accessibility Service, dan tanpa overlay.

## Kegunaan

Statusbarplus membantu Anda melihat informasi kalender tanpa harus membuka aplikasi jam atau kalender.

Informasi yang dapat ditampilkan:

- **Hari saja**
- **Hari + tanggal**
- **Hari + tanggal + bulan**
- **Tanggal + bulan**

Untuk tampilan yang menggunakan tanggal atau bulan, informasi disusun dalam **dua baris** agar lebih mudah dibaca pada area status bar.

Aplikasi juga menyediakan:

- Pilihan hari singkat atau nama hari lengkap.
- Mengikuti bahasa/locale perangkat.
- Pengaturan ukuran teks.
- Pilihan font sistem atau font TTF/OTF milik pengguna.
- Tema aplikasi: mengikuti sistem, terang, atau gelap.
- Pembaruan otomatis saat pergantian hari.
- Dukungan perangkat beresolusi tinggi dengan ukuran teks yang disesuaikan agar tetap terbaca.
- Informasi kalender lengkap dapat dilihat dari notifikasi ketika notifikasi dibuka.
- Permintaan izin notifikasi menggunakan mekanisme Android resmi.
- Dirancang agar penggunaan baterai tetap rendah.

## Cara menggunakan

1. Instal **Statusbarplus**.
2. Buka aplikasi.
3. Aktifkan **Tampilkan hari**.
4. Izinkan notifikasi jika Android memintanya.
5. Pilih **Isi status bar** sesuai informasi yang ingin ditampilkan.
6. Pilih **Hari singkat** jika ingin nama hari yang lebih ringkas.
7. Atur **Ukuran teks status bar**.
8. Pilih **Font sistem** atau impor font **TTF/OTF** sendiri.
9. Jika Android/perangkat Anda agresif menghentikan aplikasi di latar belakang, gunakan pilihan pengecualian optimasi baterai.
10. Setelah aktif, informasi akan muncul sebagai ikon teks pada status bar dan diperbarui ketika tanggal berubah.

## Setelah restart

Statusbarplus dirancang untuk dapat kembali bekerja setelah perangkat dinyalakan ulang. Jika status bar belum menampilkan informasi setelah restart, buka aplikasi sekali untuk memastikan izin notifikasi masih aktif dan aplikasi tidak dibatasi oleh sistem.

## Cara kerja

Statusbarplus menggunakan **notification small icon** Android. Teks hari/tanggal digambar menjadi bitmap dan digunakan sebagai ikon notifikasi. Dengan cara ini aplikasi tidak perlu menggambar langsung di atas System UI menggunakan overlay atau Accessibility Service.

Android/System UI tetap menentukan slot, posisi, dan sebagian karakteristik visual ikon notifikasi. Karena itu ukuran akhir di status bar dapat sedikit berbeda antarperangkat.

## Privasi

Statusbarplus tidak memerlukan akses internet untuk fungsi utama. Informasi tanggal dan hari dibuat berdasarkan waktu serta locale perangkat.

## Persyaratan

- Android 11 atau lebih baru.
- Izin notifikasi pada versi Android yang memerlukannya.
- Tidak membutuhkan root.
- Tidak membutuhkan Xposed.
- Tidak membutuhkan Accessibility Service.
- Tidak membutuhkan overlay.

## Build

Proyek ini menggunakan Gradle dan GitHub Actions.

Setiap push ke branch `main` menjalankan **debug build** dan menghasilkan `app-debug.apk` sebagai artifact. Build debug tidak membutuhkan Anda menyediakan atau mengelola keystore release.

## Catatan

Statusbarplus berfokus pada tampilan informasi kalender yang ringan dan sederhana di status bar. Ketersediaan serta ukuran ruang yang diberikan System UI untuk ikon notifikasi dapat berbeda antara perangkat dan versi Android.
