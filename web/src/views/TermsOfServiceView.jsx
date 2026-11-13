import React from 'react';
import { FileText, ArrowLeft, AlertTriangle, CheckCircle2 } from 'lucide-react';
import { useLanguage } from '../context/LanguageContext';
import LanguageToggle from '../components/LanguageToggle';

export default function TermsOfServiceView({ onBack }) {
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
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 text-xs font-bold mb-3">
              <FileText className="w-3.5 h-3.5" />
              {t('Kullanım Koşulları', 'Terms of Service')}
            </div>
            <h1 className="text-2xl sm:text-3xl font-black text-white tracking-tight">
              {t('Planora & HealthTrack Kullanım Koşulları', 'Planora & HealthTrack Terms of Service')}
            </h1>
            <p className="text-xs text-gray-400 mt-2">
              {t('Son Güncelleme: 13 Kasım 2026 • Geliştirici: Yusuf Teker', 'Last Updated: November 13, 2026 • Developer: Yusuf Teker')}
            </p>
          </div>

          <section className="space-y-3 text-sm text-gray-300 leading-relaxed">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-amber-400" />
              {t('1. Tıbbi Sorumluluk Reddi (HealthTrack)', '1. Medical Disclaimer (HealthTrack)')}
            </h2>
            <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/20 text-xs text-amber-200 space-y-1.5">
              <p className="font-bold text-amber-400">{t('Önemli Uyarı: Tıbbi Tavsiye Değildir', 'Important Notice: Not Medical Advice')}</p>
              <p>
                {t(
                  'HealthTrack yalnızca kişisel beslenme, egzersiz, kilo ve randevu takibi amacıyla sunulan bir yaşam tarzı ve biyo-telemetri uygulamasıdır. Uygulama içindeki kalori, makro/mikro besin hedefleri, VKİ (BMI) hesaplamaları ve yapay zeka tabanlı analizler tıbbi tavsiye, teşhis veya tedavi yerine geçmez. Sağlıkla ilgili kararlarınızda mutlaka bir hekim veya uzman diyetisyene danışınız.',
                  'HealthTrack is a lifestyle and bio-telemetry companion offered solely for personal nutrition, workout, weight, and appointment tracking. Calorie, macro/micronutrient targets, BMI calculations, and AI-powered insights do not constitute medical advice, diagnosis, or treatment. Always consult a qualified physician or dietitian for medical decisions.'
                )}
              </p>
            </div>
          </section>

          <section className="space-y-3 text-sm text-gray-300 leading-relaxed">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              {t('2. Hesap Kullanımı ve Sorumluluk', '2. Account Usage & Responsibilities')}
            </h2>
            <ul className="list-disc pl-5 space-y-1.5 text-gray-400">
              <li>{t('Hesap bilgilerinizin gizliliğini korumak sizin sorumluluğunuzdadır.', 'You are responsible for maintaining the confidentiality of your account credentials.')}</li>
              <li>{t('Planora topluluk akışı ve ortak plan odalarında hukuka aykırı, taciz edici veya spam içerik paylaşılamaz.', 'Unlawful, harassing, or spam content may not be shared in Planora community feeds or shared plan rooms.')}</li>
              <li>{t('Yapay zeka kotasını aşmaya yönelik otomatik veya kötüye kullanım girişimlerinde hesap erişimi kısıtlanabilir.', 'Account access may be restricted if automated or abusive attempts to bypass AI quotas are detected.')}</li>
            </ul>
          </section>

          <section className="space-y-3 text-sm text-gray-300 leading-relaxed">
            <h2 className="text-base font-bold text-white">{t('3. Hesap Sonlandırma ve Veri Silme', '3. Account Termination & Data Deletion')}</h2>
            <p className="text-gray-400">
              {t('Kullanıcılar diledikleri zaman uygulama ayarlarından veya', 'Users can permanently delete their accounts at any time from in-app settings or via the')}{' '}
              <a href="/delete-account" className="text-emerald-400 underline hover:text-emerald-300">
                {t('Hesap Silme Sayfası', 'Delete Account Page')}
              </a>{' '}
              {t(
                'üzerinden hesaplarını kalıcı olarak silebilirler. Silinen hesaplar ve veriler geri getirilemez.',
                'with permanent erasure of all associated data. Deleted accounts and data cannot be recovered.'
              )}
            </p>
          </section>

          <section className="space-y-2 text-sm text-gray-300 border-t border-white/10 pt-6">
            <h2 className="text-base font-bold text-white">{t('4. İletişim', '4. Contact')}</h2>
            <p className="text-gray-400">
              {t('Sorularınız için:', 'For questions:')}{' '}
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

