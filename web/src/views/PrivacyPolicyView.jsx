import React from 'react';
import { Shield, ArrowLeft, Lock, HeartPulse, Mic, Sparkles, Trash2, Mail } from 'lucide-react';
import { useLanguage } from '../context/LanguageContext';
import LanguageToggle from '../components/LanguageToggle';

export default function PrivacyPolicyView({ onBack }) {
  const { t } = useLanguage();

  return (
    <div className="min-h-screen bg-[#0B0F19] text-[#F8FAFC] py-10 px-4 sm:px-6">
      <div className="max-w-3xl mx-auto space-y-6">
        <div className="flex items-center justify-between gap-3">
          <button
            onClick={onBack}
            className="inline-flex items-center gap-2 text-xs font-semibold text-emerald-400 hover:text-emerald-300 bg-emerald-500/10 border border-emerald-500/20 px-3.5 py-2 rounded-xl transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            {t('Portala Dön', 'Back to Portal')}
          </button>
          <LanguageToggle />
        </div>

        <div className="glass-panel rounded-3xl p-6 sm:p-10 border border-white/10 space-y-8">
          <div className="border-b border-white/10 pb-6">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-xs font-bold mb-3">
              <Shield className="w-3.5 h-3.5" />
              {t('Gizlilik Politikası', 'Privacy Policy')}
            </div>
            <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight">
              {t('Planora & HealthTrack Gizlilik Politikası', 'Planora & HealthTrack Privacy Policy')}
            </h1>
            <p className="text-xs text-gray-400 mt-2">
              {t('Son Güncelleme: 26 Kasım 2026 • Geliştirici: Yusuf Teker', 'Last Updated: November 26, 2026 • Developer: Yusuf Teker')}
            </p>
          </div>

          <section className="space-y-3 text-sm text-gray-300 leading-relaxed">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <Lock className="w-4 h-4 text-emerald-400" />
              {t('1. Toplanan Veriler ve Kullanım Amacı', '1. Data Collected & Purpose')}
            </h2>
            <p>
              {t(
                'Planora ve HealthTrack uygulamaları, hesap oluşturma, cihazlar arası senkronizasyon ve kişiselleştirilmiş planlama/sağlık takibi deneyimi sunmak amacıyla yalnızca gerekli verileri işler:',
                'Planora and HealthTrack process only the data strictly necessary for account creation, cross-device synchronization, and personalized planning/health tracking:'
              )}
            </p>
            <ul className="list-disc pl-5 space-y-1.5 text-gray-400">
              <li>
                <strong className="text-gray-200">{t('Hesap Bilgileri:', 'Account Info:')}</strong>{' '}
                {t('Kullanıcı adı, e-posta adresi, ad-soyad ve şifrelenmiş (hash) parola bilgileri.', 'Username, email address, full name, and hashed password credentials.')}
              </li>
              <li>
                <strong className="text-gray-200">{t('Planora Verileri:', 'Planora Data:')}</strong>{' '}
                {t('Görevler, notlar, takvim etkinlikleri, odaklanma kayıtları, ortak plan odaları ve topluluk paylaşımları.', 'Tasks, notes, calendar events, focus logs, shared plan rooms, and community posts.')}
              </li>
              <li>
                <strong className="text-gray-200">{t('HealthTrack Verileri:', 'HealthTrack Data:')}</strong>{' '}
                {t('Beslenme/kalori kayıtları, makro ve mikro besin hedefleri, antrenman geçmişi ve kilo ölçümleri.', 'Nutrition/calorie logs, macro and micronutrient targets, workout history, and weight measurements.')}
              </li>
            </ul>
          </section>

          <section className="space-y-3 text-sm text-gray-300 leading-relaxed">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <HeartPulse className="w-4 h-4 text-rose-400" />
              {t('2. Health Connect, Apple HealthKit ve Huawei Health Kit Verileri', '2. Health Connect, Apple HealthKit & Huawei Health Kit Data')}
            </h2>
            <p>
              {t(
                'HealthTrack; açık rızanız doğrultusunda Android Health Connect, iOS Apple HealthKit veya Huawei Health Service Kit (OAuth 2.0 REST API) üzerinden yalnızca salt-okunur (read-only) olarak adım sayısı, mesafe/irtifa, aktif ve toplam kalori, aktif saatler, egzersiz/aktivite kayıtları, kilo ve vücut yağ oranı, kalp atış hızı, stres ve uyku özet verilerinizi okuyabilir.',
                'With your explicit consent, HealthTrack can read—strictly on a read-only basis—your steps, distance/ascent, active and total calories, active hours, workout/activity records, body weight and body fat, heart rate, stress, and sleep summaries via Android Health Connect, iOS Apple HealthKit, or Huawei Health Service Kit (OAuth 2.0 REST API).'
              )}
            </p>
            <p className="text-gray-400">
              {t(
                'Huawei Health Service Kit bağlantısı yalnızca sizin HUAWEI ID OAuth 2.0 onayınızla kurulur. Dilediğiniz zaman uygulama içinden veya Huawei Hesap/Gizlilik ayarlarından erişim iznini iptal edebilir, hesabınızı ve senkronize edilmiş tüm verilerinizi kalıcı olarak silebilirsiniz.',
                'Huawei Health Service Kit access is enabled only after your explicit HUAWEI ID OAuth 2.0 authorization. You may revoke consent or permanently delete your account and all synced health data at any time from within HealthTrack or your Huawei Account privacy settings.'
              )}
            </p>
            <div className="p-4 rounded-2xl bg-emerald-500/5 border border-emerald-500/20 text-xs text-emerald-200 space-y-1">
              <p className="font-bold text-emerald-400">{t('Sınırlı Kullanım Taahhüdü (Limited Use)', 'Limited Use Commitment')}</p>
              <p>
                {t(
                  'Health Connect, Apple HealthKit ve Huawei Health Service Kit üzerinden alınan hiçbir sağlık ve aktivite verisi üçüncü taraflara satılmaz, reklam/pazarlama amacıyla kullanılmaz, veri simsarlarıyla paylaşılmaz ve tıbbi teşhis/tedavi amacıyla kullanılmaz. Veriler yalnızca kişisel fitness, beslenme ve yaşam tarzı takibinizi görüntülemeniz için kullanılır.',
                  'No health or wellness data obtained from Health Connect, Apple HealthKit, or Huawei Health Service Kit is ever sold to third parties, used for advertising/marketing, shared with data brokers, or used for medical diagnosis or treatment. Data is used solely to display your personal fitness, nutrition, and lifestyle insights inside the app.'
                )}
              </p>
            </div>
          </section>

          <section className="space-y-3 text-sm text-gray-300 leading-relaxed">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <Mic className="w-4 h-4 text-cyan-400" />
              {t('3. Kamera, Galeri, Mikrofon ve Takvim İzinleri', '3. Camera, Photo Library, Microphone & Calendar Permissions')}
            </h2>
            <ul className="list-disc pl-5 space-y-1.5 text-gray-400">
              <li>
                <strong className="text-gray-200">{t('Kamera ve Galeri:', 'Camera & Photo Library:')}</strong>{' '}
                {t('Yemek/besin etiketi fotoğraflarının analizi ve profil fotoğrafı yükleme işlemleri için yalnızca sizin seçiminizle kullanılır.', 'Used only when you explicitly choose to analyze food/nutrition label photos or upload a profile avatar.')}
              </li>
              <li>
                <strong className="text-gray-200">{t('Mikrofon ve Konuşma Tanıma:', 'Microphone & Speech Recognition:')}</strong>{' '}
                {t('Sesli yemek veya görev kaydı oluşturmak istediğinizde anlık ses-metin dönüşümü için kullanılır; ham ses kayıtları sunucuda saklanmaz.', 'Used for instant speech-to-text when you choose voice entry for meals or tasks; raw audio recordings are not stored on servers.')}
              </li>
              <li>
                <strong className="text-gray-200">{t('Takvim ve Bildirimler:', 'Calendar & Notifications:')}</strong>{' '}
                {t('Planora görev hatırlatıcıları, odak bildirimleri ve HealthTrack günlük ölçüm/egzersiz hatırlatıcıları için kullanılır.', 'Used for Planora task reminders, focus alerts, and HealthTrack weigh-in/workout reminders.')}
              </li>
            </ul>
          </section>

          <section className="space-y-3 text-sm text-gray-300 leading-relaxed">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-amber-400" />
              {t('4. Yapay Zeka ve Üçüncü Taraf Servisler', '4. AI & Third-Party Services')}
            </h2>
            <p className="text-gray-400">
              {t(
                'Doğal dil ile girdiğiniz yemek/egzersiz metinleri veya planlama talepleri, besin/kalori tahmini ve akıllı öneriler üretmek amacıyla Google Gemini API üzerinden işlenebilir. Hata takibi ve anlık bildirim teslimatı için Google Firebase (Crashlytics & Cloud Messaging) altyapısından faydalanılır. Tüm sunucu iletişimi HTTPS (TLS) ile şifrelenir.',
                'Natural-language meal/workout logs or planning requests you submit may be processed via Google Gemini API to generate nutrition estimates and smart suggestions. Google Firebase (Crashlytics & Cloud Messaging) is used for crash diagnostics and push delivery. All server communication is encrypted over HTTPS (TLS).'
              )}
            </p>
          </section>

          <section className="space-y-3 text-sm text-gray-300 leading-relaxed">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <Trash2 className="w-4 h-4 text-rose-400" />
              {t('5. Veri Saklama ve Hesap Silme Hakkı', '5. Data Retention & Account Deletion Rights')}
            </h2>
            <p className="text-gray-400">
              {t('Hesabınız aktif olduğu sürece verileriniz bulut veritabanında güvenli şekilde saklanır. Dilediğiniz zaman:', 'Your data is securely retained while your account remains active. At any time you may use:')}
            </p>
            <ul className="list-disc pl-5 space-y-1 text-gray-400">
              <li><strong>Planora:</strong> <em>{t('Ayarlar → Hesabı Sil', 'Settings → Delete Account')}</em></li>
              <li><strong>HealthTrack:</strong> <em>{t('Profil → Hesabı Sil', 'Profile → Delete Account')}</em></li>
              <li>
                <strong>{t('Web üzerinden:', 'On Web:')}</strong>{' '}
                <a href="/delete-account" className="text-emerald-400 underline hover:text-emerald-300">
                  {t('Hesap Silme Sayfası', 'Delete Account Page')}
                </a>
              </li>
            </ul>
            <p className="text-gray-400">
              {t(
                'adımlarını kullanarak hesabınızı ve tüm ilişkili Planora & HealthTrack verilerinizi anında ve kalıcı olarak silebilirsiniz.',
                'to permanently erase your account and all associated Planora & HealthTrack data immediately.'
              )}
            </p>
          </section>

          <section className="space-y-2 text-sm text-gray-300 border-t border-white/10 pt-6">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <Mail className="w-4 h-4 text-indigo-400" />
              {t('6. İletişim', '6. Contact')}
            </h2>
            <p className="text-gray-400">
              {t('Gizlilik politikası veya verilerinizle ilgili her türlü soru ve talep için:', 'For any questions or requests regarding this policy or your data:')}{' '}
              <a href="mailto:yusufteker.dev@gmail.com" className="text-emerald-400 underline">
                yusufteker.dev@gmail.com
              </a>
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}

