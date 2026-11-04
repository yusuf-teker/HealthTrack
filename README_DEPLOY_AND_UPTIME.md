# HealthTrack & Planora Backend Dağıtım ve Uptime Robot Kılavuzu

Bu kılavuz, HealthTrack modülünü içeren Planora backend'inin **Render** üzerinde dağıtımını, gerekli ortam değişkenlerini (environment variables) ve sunucunun uyku moduna geçmesini engelleyen **Uptime Robot** yapılandırmasını içerir.

---

## 1. Mimari Kararı ve Veritabanı (Seçenek A)

HealthTrack, kullanıcının mevcut **Planora** projesinin backend servisine (`Pulse/server`) yeni bir `health` modülü olarak entegre edilmiştir:
- **Tek Kullanıcı Havuzu:** Planora kullanıcı adı ve şifresi ile HealthTrack'e giriş yapılır.
- **Aynı JWT Secret:** Planora'nın ürettiği JWT token'ları HealthTrack API'larında doğrudan geçerlidir.
- **Neon PostgreSQL:** Mevcut veritabanı tablolarına dokunulmamış, sadece bağımsız `health_*` tabloları eklenmiştir.
- **Planora Takvim Entegrasyonu:** HealthTrack üzerinden kaydedilen doktor randevuları, Planora'nın mevcut `tasks` tablosuna `EVENT` türünde otomatik olarak yansıtılır.

---

## 2. Veritabanı Migrasyonu (Flyway)

Backend başlatıldığında Flyway migrasyonu otomatik olarak çalışır:
- Migrasyon dosyası: `Pulse/server/src/main/resources/db/migration/V25__Create_health_tables.sql`
- Oluşturulan tablolar:
  1. `health_food_logs` (Yemek kayıtları, serbest metin, yapılandırılmış besin değerleri JSON)
  2. `health_nutrition_goals` (Kullanıcıya özel veya varsayılan günlük besin hedefleri)
  3. `health_workouts` (Spor/egzersiz kayıtları, süre ve yakılan kalori)
  4. `health_weights` (Kilo takibi kayıtları)
  5. `health_appointments` (Doktor randevuları ve hatırlatıcılar)
  6. `health_appointment_attachments` (Yerel reçete/rapor dosya referansları ve metadata)

Tüm tablolarda offline-first gereksinimi için `updated_at`, `is_deleted` ve UUID `id` alanları standart olarak bulunmaktadır.

---

## 3. Render Ortam Değişkenleri (Environment Variables)

Render Dashboard üzerinde backend servisinizin **Environment** sekmesine şu değişkenleri tanımlayın:

| Değişken Adı | Açıklama | Örnek Değer |
|---|---|---|
| `GEMINI_API_KEY` | Google AI Studio'dan alınan Gemini API anahtarı (Yemek ve Spor analizleri için zorunludur). İstemcilere asla gömülmez. | `AIzaSy...` |
| `DB_URL` | Neon PostgreSQL bağlantı URL'si (SSL mode require). | `jdbc:postgresql://ep-xyz.eu-central-1.aws.neon.tech/neondb?sslmode=require` |
| `DB_USER` | Neon veritabanı kullanıcı adı. | `neondb_owner` |
| `DB_PASSWORD` | Neon veritabanı şifresi. | `••••••••••••` |
| `JWT_SECRET` | Planora ile ortak kullanılan JWT imzalama anahtarı. | `your-very-secure-jwt-secret-key` |
| `JWT_ISSUER` | JWT issuer değeri (Planora ile aynı). | `https://planora.com` |
| `PORT` | Render tarafından otomatik atanır (isteğe bağlı elle verilebilir). | `8080` |

---

## 4. Render Dağıtım Adımları

1. Kodları Git repository'nize push edin:
   ```bash
   git push origin main
   ```
2. Render Dashboard'da Planora Web Service'inize gidin.
3. Otomatik Deploy (Auto-Deploy) açıksa derleme kendiliğinden başlar. Manuel tetiklemek için **Manual Deploy -> Deploy latest commit** butonuna basın.
4. Derleme komutu ve başlangıç komutu (Dockerfile veya Gradle):
   - **Build Command:** `./gradlew :server:installDist` (veya Dockerfile tabanlı ise Docker build)
   - **Start Command:** `./server/build/install/server/bin/server`
5. Dağıtım tamamlandığında loglarda şu satırı görmelisiniz:
   ```
   Application started in ... - Responding at http://0.0.0.0:8080
   ```

---

## 5. Uptime Robot Kurulumu (Render'ın Uyumasını Önleme)

Render Free Tier servisleri 15 dakika boyunca HTTP trafiği almazsa uyku moduna (spin down) geçer. İlk istek geldiğinde sunucunun uyanması 30-50 saniye sürebilir. Bunu engellemek ve servisi 7/24 uyanık tutmak için hafif bir sağlık kontrolü eklenmiştir:

### Health Endpoint Özellikleri:
- `GET /health` veya `HEAD /health`
- **Kimlik Doğrulama:** Gerektirmez (Auth-free).
- **Veritabanı Yükü:** Sıfır (DB bağlantısı sorgulamaz, anında `{"status":"UP","timestamp":"..."}` döner).
- Veritabanını kontrol etmek isterseniz ayrı olarak `GET /health/db` endpoint'i mevcuttur.

### Uptime Robot Yapılandırma Adımları:
1. [UptimeRobot.com](https://uptimerobot.com) adresine gidin ve giriş yapın.
2. **"+ Add New Monitor"** butonuna tıklayın.
3. Ayarları şu şekilde doldurun:
   - **Monitor Type:** `HTTP(s)`
   - **Friendly Name:** `Planora & HealthTrack API`
   - **URL (or IP):** `https://<render-servis-adiniz>.onrender.com/health` (örn: `https://planora-server.onrender.com/health`)
   - **Monitoring Interval:** `Every 5 minutes` (Free planda 5 dakika seçeneği mevcuttur ve Render'ın 15 dakikalık sınırını rahatlıkla aşar).
   - **Monitor Timeout:** `30 seconds`
   - **HTTP Method:** `HEAD` veya `GET` (`HEAD` daha az bant genişliği kullanır).
4. **"Create Monitor"** butonuna tıklayarak kaydedin.

Artık Uptime Robot her 5 dakikada bir `/health` adresine istek göndererek servisi daima sıcak ve çalışır durumda tutacaktır.

---

## 6. API Endpoint Özeti

| Metot | Yol | Kimlik Doğrulama | Açıklama |
|---|---|---|---|
| `GET / HEAD` | `/health` | Hayır | Uptime Robot için hafif canlılık kontrolü |
| `GET` | `/health/db` | Evet | Canlı veritabanı bağlantı testi |
| `POST` | `/health/ai/food` | Evet | Gemini ile serbest metinden yapılandırılmış besin analizi |
| `POST` | `/health/ai/workout` | Evet | Gemini ile serbest metin + kullanıcı kilosundan kalori analizi |
| `POST` | `/health/sync/push` | Evet | Çift yönlü çevrimdışı senkronizasyon (push) |
| `GET` | `/health/sync/pull` | Evet | Belirli bir zamandan sonraki değişiklikleri çekme (pull) |
| `GET` | `/health/nutrition/goals`| Evet | Varsayılan günlük besin hedefleri |
