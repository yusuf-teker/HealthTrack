import React, { useState } from 'react';
import { Trash2, ArrowLeft, AlertTriangle, CheckCircle2, Lock, User } from 'lucide-react';
import { login, deleteAccount } from '../api/client';
import { useLanguage } from '../context/LanguageContext';
import LanguageToggle from '../components/LanguageToggle';

export default function DeleteAccountView({ onBack }) {
  const { t } = useLanguage();
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [confirmed, setConfirmed] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!identifier.trim() || !password) {
      setError(t('Lütfen kullanıcı adı/e-posta ve şifrenizi girin.', 'Please enter your username/email and password.'));
      return;
    }
    if (!confirmed) {
      setError(t('Devam etmek için kalıcı silme onay kutusunu işaretlemelisiniz.', 'Please check the confirmation box to continue with permanent deletion.'));
      return;
    }

    setLoading(true);
    try {
      const authData = await login(identifier.trim(), password);
      const token = authData?.token || authData?.accessToken;
      if (!token) {
        throw new Error(t('Kimlik doğrulama jetonu alınamadı.', 'Could not obtain authentication token.'));
      }
      await deleteAccount(token);
      try {
        localStorage.removeItem('ht_token');
        localStorage.removeItem('ht_user');
      } catch (_) {}
      setSuccess(true);
    } catch (err) {
      setError(err.message || t('Hesap silme işlemi sırasında bir hata oluştu.', 'An error occurred while deleting your account.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#0B0F19] text-[#F8FAFC] py-10 px-4 sm:px-6">
      <div className="max-w-xl mx-auto space-y-6">
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

        <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-white/10 space-y-6">
          <div className="border-b border-white/10 pb-5">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs font-bold mb-3">
              <Trash2 className="w-3.5 h-3.5" />
              {t('Hesap ve Veri Silme', 'Account & Data Deletion')}
            </div>
            <h1 className="text-2xl font-black text-white tracking-tight">
              {t('Planora & HealthTrack Hesabını Sil', 'Delete Planora & HealthTrack Account')}
            </h1>
            <p className="text-xs text-gray-400 mt-1.5">
              {t(
                'Planora ve HealthTrack aynı ortak hesabı kullanır. Hesabınızı sildiğinizde her iki uygulamadaki verileriniz de kalıcı olarak silinir.',
                'Planora and HealthTrack share the same unified account. Deleting your account permanently erases your data from both apps.'
              )}
            </p>
          </div>

          {success ? (
            <div className="p-5 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 space-y-3">
              <div className="flex items-center gap-2 text-emerald-400 font-bold text-sm">
                <CheckCircle2 className="w-5 h-5" />
                {t('Hesabınız Kalıcı Olarak Silindi', 'Your Account Has Been Permanently Deleted')}
              </div>
              <p className="text-xs text-gray-300 leading-relaxed">
                {t(
                  'Hesabınız ve ilişkili tüm Planora & HealthTrack verileriniz sistemlerimizden tamamen kaldırılmıştır. Bizi tercih ettiğiniz için teşekkür ederiz.',
                  'Your account and all associated Planora & HealthTrack data have been completely removed from our systems. Thank you for trying our apps.'
                )}
              </p>
            </div>
          ) : (
            <>
              <div className="p-4 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-xs text-rose-200 space-y-2">
                <div className="flex items-center gap-2 font-bold text-rose-400">
                  <AlertTriangle className="w-4 h-4 shrink-0" />
                  {t('Kalıcı Olarak Silinecek Veriler:', 'Data That Will Be Permanently Deleted:')}
                </div>
                <ul className="list-disc pl-5 space-y-1 text-rose-200/80">
                  <li>{t('Profil bilgileriniz, oturum jetonlarınız ve bildirim kayıtlarınız', 'Your profile details, session tokens, and notification registrations')}</li>
                  <li>{t('HealthTrack beslenme, antrenman, kilo, hedef ve doktor randevu kayıtlarınız', 'Your HealthTrack nutrition, workout, weight, goal, and doctor appointment records')}</li>
                  <li>{t('Planora görevleriniz, notlarınız, paylaşımlarınız ve oda üyelikleriniz', 'Your Planora tasks, notes, community posts, and room memberships')}</li>
                </ul>
                <p className="text-[11px] text-rose-300/80 pt-1">
                  {t(
                    'İşlem anında gerçekleştirilir ve geri alınamaz. Uygulama içinden silmek için: Planora → Ayarlar → Hesabı Sil veya HealthTrack → Profil → Hesabı Sil.',
                    'Deletion takes effect immediately and cannot be undone. To delete inside the apps: Planora → Settings → Delete Account or HealthTrack → Profile → Delete Account.'
                  )}
                </p>
              </div>

              {error && (
                <div className="p-3.5 rounded-xl bg-rose-500/15 border border-rose-500/30 text-xs text-rose-300">
                  {error}
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-300 mb-1.5">
                    {t('Kullanıcı Adı veya E-posta', 'Username or Email')}
                  </label>
                  <div className="relative">
                    <User className="w-4 h-4 text-gray-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      value={identifier}
                      onChange={(e) => setIdentifier(e.target.value)}
                      placeholder={t('kullaniciadi veya ornek@mail.com', 'username or name@mail.com')}
                      className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-sm text-white placeholder-gray-500 focus:outline-none focus:border-rose-500/50"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-gray-300 mb-1.5">
                    {t('Şifre', 'Password')}
                  </label>
                  <div className="relative">
                    <Lock className="w-4 h-4 text-gray-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
                    <input
                      type="password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="••••••••"
                      className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-white/5 border border-white/10 text-sm text-white placeholder-gray-500 focus:outline-none focus:border-rose-500/50"
                    />
                  </div>
                </div>

                <label className="flex items-start gap-2.5 text-xs text-gray-300 cursor-pointer select-none pt-1">
                  <input
                    type="checkbox"
                    checked={confirmed}
                    onChange={(e) => setConfirmed(e.target.checked)}
                    className="mt-0.5 rounded border-white/20 bg-white/5 text-rose-500 focus:ring-rose-500"
                  />
                  <span>
                    {t(
                      'Hesabımın ve tüm Planora & HealthTrack verilerimin geri döndürülemez şekilde silineceğini onaylıyorum.',
                      'I confirm that my account and all Planora & HealthTrack data will be permanently deleted.'
                    )}
                  </span>
                </label>

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full py-3 rounded-xl bg-rose-600 hover:bg-rose-500 disabled:opacity-50 text-white text-sm font-bold transition-colors flex items-center justify-center gap-2"
                >
                  <Trash2 className="w-4 h-4" />
                  {loading ? t('Hesap Siliniyor...', 'Deleting Account...') : t('Hesabımı Kalıcı Olarak Sil', 'Permanently Delete My Account')}
                </button>
              </form>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

