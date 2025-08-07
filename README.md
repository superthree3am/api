[API Autentikasi Spring Boot](https://github.com/superthree3am/api)

Proyek api ini adalah contoh aplikasi Spring Boot yang menyediakan fungsionalitas autentikasi pengguna yang kuat, termasuk pendaftaran, login berbasis kredensial, verifikasi Firebase ID Token, manajemen sesi JWT dan Refresh Token, serta profil pengguna.

1. Deskripsi Proyek

Aplikasi ini adalah API berbasis Spring Boot yang berfungsi sebagai backend untuk sistem autentikasi. Ini dirancang untuk mengelola pengguna, pendaftaran, login, verifikasi identitas eksternal (melalui Firebase), dan pengelolaan sesi yang aman menggunakan JSON Web Tokens (JWT) dan Refresh Token yang disimpan di Redis. Ini juga mencakup fitur keamanan seperti pembatasan upaya login yang gagal

2. Fitur API (Endpoints)

```POST /api/v1/register```: Mendaftarkan pengguna baru dengan username, email, nomor telepon, nama lengkap, dan password.

```POST /api/v1/login```: Mengautentikasi pengguna dengan username dan password. Jika berhasil, akan memicu proses verifikasi OTP (melalui nomor telepon yang terdaftar).

```POST /api/v1/verify```: Memverifikasi Firebase ID Token yang diterima dari klien setelah verifikasi OTP berhasil, dan mengembalikan Access Token serta Refresh Token aplikasi.

```GET /api/v1/profile```: Mengambil detail profil pengguna yang sedang diautentikasi. Membutuhkan Access Token yang valid.

```POST /api/v1/refresh-token```: Menggunakan Refresh Token yang valid untuk mendapatkan Access Token baru dan Refresh Token yang baru (untuk rotasi token).

```POST /api/v1/logout```: Mengakhiri sesi pengguna dengan menghapus Refresh Token dari Redis.

3. Konfigurasi Database & ORM

Aplikasi menggunakan Spring Boot dengan Spring Data JPA dan Hibernate sebagai penyedia ORM. Konfigurasi utama diatur dalam src/main/resources/application.properties.example.

Konfigurasi Database (DataSource)

Tipe Database: PostgreSQL

-- URL Koneksi: ```jdbc:postgresql://localhost:5432/springdb```

-- Username: postgres

-- Password: postgres

-- Driver Class: org.postgresql.Driver

Konfigurasi JPA/Hibernate

-- Dialek Database: org.hibernate.dialect.PostgreSQLDialect

-- DDU Auto: validate (Hibernate akan memvalidasi skema database yang ada)

-- SQL Logging: SQL yang dihasilkan akan ditampilkan dan diformat di konsol.

Pemetaan Objek-Relasional (ORM)

Entitas: Kelas-kelas Java seperti User.java dan LoginAttempt.java berfungsi sebagai entitas ORM. Mereka dianotasi dengan @Entity untuk menandai bahwa mereka adalah objek yang akan dipetakan ke tabel database, dan @Table untuk menentukan nama tabel.

Kolom: Properti dalam entitas (misalnya username, email, phone di User.java) dianotasi dengan @Column untuk memetakan mereka ke kolom database, termasuk properti seperti nullable, unique, dan length.

Primary Key: Bidang id dalam entitas ditandai dengan @Id sebagai primary key menggunakan ULID (String).

Repository: Aplikasi menggunakan Spring Data JPA JpaRepository (misalnya UserRepository.java dan LoginAttemptRepository.java) untuk menyediakan operasi CRUD dasar dan query khusus tanpa perlu menulis implementasi SQL. Spring Data JPA secara otomatis membuat implementasi repository pada runtime.

```entity/User.java```
|Atribut Java	|Tipe Data Java	|Kolom Database	|Properti Kolom	|Deskripsi|
|-------------|---------------|---------------|---------------|--------------------------|
|id	|String|	id|	Primary Key|	Pengidentifikasi unik untuk pengguna, menggunakan ULID (Universal Unique Lexicographically Sortable Identifier)|
|username	|String	|username	|Not Null, Unique, Panjang maks 20	|Nama pengguna unik yang digunakan untuk login|
|email	|String	|email	|Not Null, Unique |Alamat email unik pengguna.|
|phone	|String	|phone_number	|Not Null, Unique, Panjang maks 15	|Nomor telepon unik pengguna|
|full_name	|String	|full_name	|Not Null, Panjang maks 50	|Nama lengkap pengguna|
|password	|String	|password	|Not Null	|Hash password pengguna|
|createdAt	|LocalDateTime	|created_at	|Not Null	|Timestamp saat akun pengguna dibuat. Ditetapkan secara otomatis saat persistensi (@PrePersist)|

```entity/LoginAttemp.java```
|Atribut Java	|Tipe Data Java	|Kolom Database	|Properti Kolom	|Deskripsi|
|-------------|---------------|---------------|---------------|--------------------------|
|id	|String	|id	|Primary Key	|Pengidentifikasi unik untuk upaya login, menggunakan ULID|
|userId	|String	|user_id	|Not Null, Unique	|ID pengguna yang terkait dengan upaya login ini (merujuk ke User.id). Setiap pengguna hanya memiliki satu catatan upaya login|
|failedAttempts	|int	|failed_attempts	|Not Null	|Jumlah upaya login yang gagal berturut-turut|
|lockedUntil	|LocalDateTime	|locked_until	|Nullable	|Timestamp hingga kapan akun dikunci. Null jika tidak dikunci|
|lastFailedAttempt	|LocalDateTime	|last_failed_attempt	|Nullable	|Timestamp upaya login gagal terakhir|

4. Cara Menjalankan Proyek
   
- Prerequisites

```Java 21 JDK```

```Maven```

```PostgreSQL```

```Redis```

```Firebase Service Account Key (untuk src/main/resources/firebase/serviceAccountKey.json)```

- Langkah-langkah

Clone Repository:

Bash

```git clone https://github.com/superthree3am/project3am.git```

```cd project3am/api```

Konfigurasi Database:

Pastikan PostgreSQL dan Redis berjalan.

Buat database bernama springdb.

Salin src/main/resources/application.properties.example ke src/main/resources/application.properties dan perbarui spring.datasource.password serta jwt.secret (pastikan secret JWT sangat panjang dan kuat).

Pastikan skema database Anda sesuai dengan entitas Java (terutama tipe ID sebagai VARCHAR(36) atau TEXT untuk ULID).

Firebase Configuration:

Unduh Firebase Service Account Key (file JSON) dari konsol Firebase Anda.

Simpan file JSON tersebut di src/main/resources/firebase/serviceAccountKey.json.

Jalankan Aplikasi:

Anda dapat menjalankan aplikasi menggunakan Maven:

Bash

```./mvnw spring-boot:run```

Atau Anda dapat membangun JAR yang dapat dieksekusi dan menjalankannya:

Bash

```./mvnw clean install```

java -jar target/api-0.0.1-SNAPSHOT.jar

Menjalankan dengan Docker Compose (Lingkungan Pengembangan)

File docker-compose.yml disediakan untuk pengaturan lingkungan pengembangan yang cepat, termasuk Jenkins, SonarQube, dan PostgreSQL.

Bash

```docker-compose up -d```
