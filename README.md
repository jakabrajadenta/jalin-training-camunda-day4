# Jalin Training Camunda Day 4 — Payment Switching

Proyek pelatihan integrasi **Camunda 8 (Zeebe)** dengan **Spring Boot** untuk mensimulasikan alur *payment switching* dan otorisasi transaksi kartu. Proyek ini merupakan bagian dari seri pelatihan Camunda di Jalin Pembayaran Nusantara.

---

## Tujuan Pelatihan

- Memahami cara mengintegrasikan Camunda 8 (Zeebe) dengan Spring Boot
- Membuat dan mendaftarkan **Job Worker** untuk menangani service task pada BPMN
- Menerapkan alur proses bisnis payment switching: validasi → routing → otorisasi
- Menyimpan hasil transaksi ke database menggunakan Spring Data JPA
- Memanggil external API dari dalam Job Worker

---

## Teknologi yang Digunakan

| Teknologi | Versi |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.6 |
| Camunda Spring Boot Starter | 8.8.0 |
| Spring Data JPA | (via Spring Boot) |
| MySQL Connector/J | (via Spring Boot) |
| Lombok | (via Spring Boot) |
| Spring WebFlux | (via Spring Boot) |

---

## Arsitektur Sistem

```
┌─────────────┐     POST /api/payment/start     ┌──────────────────────┐
│   Client    │ ──────────────────────────────▶ │  PaymentController   │
└─────────────┘                                  └──────────┬───────────┘
                                                            │ createProcessInstance
                                                            ▼
                                              ┌─────────────────────────┐
                                              │   Camunda 8 / Zeebe     │
                                              │  BPM_Payment_Switching  │
                                              └────────────┬────────────┘
                                                           │
                          ┌────────────────────────────────┤
                          │                │               │
                          ▼                ▼               ▼
               ┌──────────────────┐ ┌──────────────┐ ┌────────────────────┐
               │ ValidationWorker │ │RoutingWorker │ │ AuthorizationWorker│
               │validate-transact.│ │route-transact│ │authorize-transact. │
               └──────────────────┘ └──────────────┘ └────────┬───────────┘
                                                               │
                                                    ┌──────────┴──────────┐
                                                    │  External Issuer API│
                                                    │  + MySQL Database   │
                                                    └─────────────────────┘
```

---

## Alur Proses BPMN (`BPM_Payment_Switching`)

1. **Validasi Transaksi** (`validate-transaction`)
   - Memeriksa `cardNumber` tidak null dan `amount` lebih dari 0
   - Jika tidak valid → `status = DECLINE`
   - Jika valid → `status = VALID`, lanjut ke tahap berikutnya

2. **Routing** (`route-transaction`)
   - Menentukan issuer bank tujuan berdasarkan variabel `issuer`
   - Jika issuer kosong → default ke `BANK_DEFAULT`

3. **Otorisasi** (`authorize-transaction`)
   - Memanggil external API issuer di `http://localhost/issuer_api/authorize.php`
   - Menginterpretasi respons: `APPROVED`, `DECLINED`, atau `ERROR`
   - Menyimpan hasil transaksi ke tabel `payment_transaction` di MySQL

---

## Struktur Proyek

```
src/main/java/co/id/jalin/camunda/training/day4/
├── Application.java                         # Entry point Spring Boot
├── controller/
│   └── PaymentController.java               # REST endpoint untuk trigger proses
├── entity/
│   └── PaymentTransaction.java              # Entity JPA untuk tabel transaksi
├── repository/
│   └── PaymentTransactionRepository.java    # Spring Data JPA repository
└── worker/
    ├── ValidationWorker.java                # Job worker: validasi transaksi
    ├── RoutingWorker.java                   # Job worker: routing ke issuer
    └── AuthorizationWorker.java             # Job worker: otorisasi + simpan DB
```

---

## Prasyarat

Pastikan hal-hal berikut sudah tersedia sebelum menjalankan proyek:

- **Java 21** atau lebih baru
- **Maven 3.8+** (atau gunakan `mvnw` yang sudah tersedia)
- **Camunda 8 Self-Managed** berjalan di lokal (Docker Compose tersedia di dokumentasi Camunda)
  - Zeebe gRPC: `localhost:26500`
  - Zeebe REST: `localhost:8080`
- **MySQL** berjalan di `localhost:3306`
- Database `camunda_payments` sudah dibuat di MySQL

---

## Cara Instalasi & Menjalankan

### 1. Clone repositori

```bash
git clone https://github.com/jalin-pembayaran-nusantara/jalin-training-camunda-day4.git
cd jalin-training-camunda-day4
```

### 2. Buat database MySQL

```sql
CREATE DATABASE camunda_payments;
```

### 3. Konfigurasi koneksi

Edit file `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/camunda_payments?useSSL=false
    username: root
    password: your_password   # sesuaikan dengan password MySQL Anda
```

### 4. Deploy BPMN ke Camunda

Upload file BPMN `BPM_Payment_Switching` ke Camunda Modeler atau via Operate/REST API.

### 5. Build dan jalankan aplikasi

```bash
# Menggunakan Maven Wrapper (rekomendasi)
./mvnw spring-boot:run

# Atau build JAR terlebih dahulu
./mvnw clean package
java -jar target/camunda.training.day4-0.0.1-SNAPSHOT.jar
```

Aplikasi akan berjalan di **`http://localhost:8081`**.

---

## API Reference

### POST `/api/payment/start`

Memulai proses payment switching baru di Camunda.

**Request Body:**

```json
{
  "cardNumber": "4111111111111111",
  "amount": 150000,
  "merchant": "MERCHANT_ABC",
  "issuer": "BANK_BCA"
}
```

| Field | Tipe | Keterangan |
|---|---|---|
| `cardNumber` | String | Nomor kartu pembayaran |
| `amount` | Number | Nominal transaksi (harus > 0) |
| `merchant` | String | Kode merchant |
| `issuer` | String | Kode bank penerbit kartu |

**Response sukses (200 OK):**

```json
{
  "processInstanceKey": 2251799813685281
}
```

**Response error (400 Bad Request):**

```json
{
  "message": "Process definition with key 'BPM_Payment_Switching' not found"
}
```

---

## Skema Database

Tabel `payment_transaction` dibuat otomatis oleh Hibernate (`ddl-auto: update`):

| Kolom | Tipe | Keterangan |
|---|---|---|
| `id` | BIGINT (PK, AI) | ID unik transaksi |
| `process_instance_key` | BIGINT | Key proses Camunda |
| `card_number` | VARCHAR | Nomor kartu |
| `merchant` | VARCHAR | Kode merchant |
| `issuer` | VARCHAR | Kode issuer bank |
| `amount` | DOUBLE | Nominal transaksi |
| `status` | VARCHAR | `VALID`, `APPROVED`, `DECLINED`, `ERROR` |
| `message` | VARCHAR(500) | Pesan hasil otorisasi |
| `created_at` | DATETIME | Waktu transaksi dibuat |

---

## Konfigurasi Aplikasi (`application.yaml`)

```yaml
server:
  port: 8081                      # Port aplikasi

camunda:
  client:
    mode: self-managed            # Mode koneksi Camunda
    security:
      plaintext: true             # Non-TLS untuk development
    zeebe:
      grpc-address: http://127.0.0.1:26500   # Zeebe gRPC Gateway
      rest-address: http://127.0.0.1:8080    # Zeebe REST Gateway

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/camunda_payments?useSSL=false
  jpa:
    hibernate:
      ddl-auto: update            # Auto-create/update tabel
    show-sql: true                # Log query SQL ke console
```

---

## Menjalankan Tes

```bash
./mvnw test
```

---

## Materi Terkait

- [Camunda 8 Documentation](https://docs.camunda.io)
- [Camunda Spring Boot Starter](https://docs.camunda.io/docs/apis-tools/spring-zeebe-sdk/getting-started/)
- [Zeebe Job Worker Concept](https://docs.camunda.io/docs/components/concepts/job-workers/)
- [Camunda Self-Managed Docker Compose](https://github.com/camunda/camunda-distributions)

---

## Lisensi

Proyek ini dibuat untuk keperluan internal pelatihan Jalin Pembayaran Nusantara.
