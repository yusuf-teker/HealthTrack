import React from 'react';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import { 
  Heart, 
  CheckSquare, 
  ShieldCheck, 
  Sparkles, 
  Flame, 
  Calendar, 
  BarChart3, 
  Send, 
  Users, 
  Database,
  ArrowRight,
  Crown
} from 'lucide-react';

export default function AppSelectorView() {
  const { user, isAdmin, setActiveWorkspace } = useAuth();
  const { t } = useLanguage();

  return (
    <div className="max-w-6xl mx-auto px-4 sm:px-6 py-10 sm:py-16">
      
      {/* Welcome Banner */}
      <div className="text-center mb-12 sm:mb-16">
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/5 border border-white/10 text-xs text-gray-300 mb-4">
          <Sparkles className="w-3.5 h-3.5 text-emerald-400" />
          <span>{t('Ekosistem Merkezi & Analizler', 'Ecosystem Hub & Analytics')}</span>
        </div>
        <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
          {t('Hoş Geldiniz,', 'Welcome,')} <span className="bg-gradient-to-r from-emerald-400 via-teal-300 to-indigo-400 bg-clip-text text-transparent">{user?.name || user?.username}</span> 👋
        </h1>
        <p className="text-sm sm:text-base text-gray-400 mt-2 max-w-xl mx-auto">
          {t(
            'Sağlık raporlarınızı incelemek, Planora görevlerinizi yönetmek veya sistem denetimi gerçekleştirmek için bir alan seçin.',
            'Choose a workspace to inspect your health reports, manage your Planora tasks, or administer the platform.'
          )}
        </p>
      </div>

      {/* Grid of Workspace Cards */}
      <div className={`grid grid-cols-1 md:grid-cols-2 ${isAdmin ? 'lg:grid-cols-3' : 'max-w-4xl mx-auto'} gap-6 sm:gap-8`}>
        
        {/* Card 1: HealthTrack */}
        <div 
          onClick={() => setActiveWorkspace('healthtrack')}
          className="group relative glass-panel rounded-3xl p-7 border border-emerald-500/20 hover:border-emerald-500/50 hover:shadow-glow-health transition-all duration-300 cursor-pointer flex flex-col justify-between overflow-hidden"
        >
          {/* Ambient Corner Glow */}
          <div className="pointer-events-none absolute -top-12 -right-12 w-40 h-40 bg-emerald-500/10 rounded-full blur-3xl group-hover:bg-emerald-500/20 transition-all" />

          <div>
            <div className="w-14 h-14 rounded-full bg-emerald-500/15 border border-emerald-500/30 flex items-center justify-center text-emerald-400 mb-6 group-hover:scale-110 transition-transform shadow-lg shadow-emerald-500/10">
              <Heart className="w-7 h-7" />
            </div>

            <div className="flex items-center gap-2 mb-2">
              <span className="text-xs font-bold uppercase tracking-wider text-emerald-400">
                {t('Sağlık & Biyo-Telemetri', 'Health & Bio-Telemetry')}
              </span>
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-300 border border-emerald-500/20 font-semibold">
                {t('Kişisel Portal', 'Personal Portal')}
              </span>
            </div>

            <h2 className="text-2xl font-bold text-white group-hover:text-emerald-300 transition-colors">
              HealthTrack
            </h2>

            <p className="text-xs text-gray-400 mt-2 leading-relaxed">
              {t(
                'Beslenme, kalori, makro hedefleri, tartı geçmişi, egzersizler ve doktora sunulabilir sağlık raporlarınızı görüntüleyin.',
                'Inspect your nutrition, calories, macro targets, weight trends, workouts, and doctor-ready health summaries.'
              )}
            </p>

            <ul className="mt-5 space-y-2 text-xs text-gray-300">
              <li className="flex items-center gap-2">
                <Flame className="w-3.5 h-3.5 text-orange-400 shrink-0" />
                <span>{t('Kalori ve Makro (Protein/Karb/Yağ) Dökümü', 'Calories & Macros (Protein/Carbs/Fat) Breakdown')}</span>
              </li>
              <li className="flex items-center gap-2">
                <BarChart3 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                <span>{t('Kilo Yolculuğu, BMI & Hedef Takibi', 'Weight Journey, BMI & Goal Tracking')}</span>
              </li>
              <li className="flex items-center gap-2">
                <Calendar className="w-3.5 h-3.5 text-teal-400 shrink-0" />
                <span>{t('Doktor Randevuları & Reçete Kayıtları', 'Doctor Appointments & Prescriptions')}</span>
              </li>
            </ul>
          </div>

          <div className="mt-8 pt-5 border-t border-white/5 flex items-center justify-between text-xs font-semibold text-emerald-400 group-hover:text-emerald-300">
            <span>{t('Raporları ve Verileri Görüntüle', 'View Reports & Data')}</span>
            <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
          </div>
        </div>

        {/* Card 2: Planora */}
        <div 
          onClick={() => setActiveWorkspace('planora')}
          className="group relative glass-panel rounded-3xl p-7 border border-indigo-500/20 hover:border-indigo-500/50 hover:shadow-glow-planora transition-all duration-300 cursor-pointer flex flex-col justify-between overflow-hidden"
        >
          {/* Ambient Corner Glow */}
          <div className="pointer-events-none absolute -top-12 -right-12 w-40 h-40 bg-indigo-500/10 rounded-full blur-3xl group-hover:bg-indigo-500/20 transition-all" />

          <div>
            <div className="w-14 h-14 rounded-full bg-indigo-500/15 border border-indigo-500/30 flex items-center justify-center text-indigo-400 mb-6 group-hover:scale-110 transition-transform shadow-lg shadow-indigo-500/10">
              <CheckSquare className="w-7 h-7" />
            </div>

            <div className="flex items-center gap-2 mb-2">
              <span className="text-xs font-bold uppercase tracking-wider text-indigo-400">
                {t('Verimlilik & Planlama', 'Productivity & Planning')}
              </span>
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-indigo-500/10 text-indigo-300 border border-indigo-500/20 font-semibold">
                {t('Görev Panosu', 'Task Board')}
              </span>
            </div>

            <h2 className="text-2xl font-bold text-white group-hover:text-indigo-300 transition-colors">
              Planora
            </h2>

            <p className="text-xs text-gray-400 mt-2 leading-relaxed">
              {t(
                'Günlük to-do listeleriniz, plan odalarınız, takvim görevleriniz ve alışkanlık zincirlerinizi masaüstünden inceleyin.',
                'Review your daily to-do lists, plan rooms, calendar tasks, and habit streaks from the desktop.'
              )}
            </p>

            <ul className="mt-5 space-y-2 text-xs text-gray-300">
              <li className="flex items-center gap-2">
                <CheckSquare className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                <span>{t('Görev & To-Do Listesi Durumları', 'Tasks & To-Do List Statuses')}</span>
              </li>
              <li className="flex items-center gap-2">
                <BarChart3 className="w-3.5 h-3.5 text-violet-400 shrink-0" />
                <span>{t('Verimlilik Oranı & Tamamlanan Hedefler', 'Productivity Rate & Completed Goals')}</span>
              </li>
              <li className="flex items-center gap-2">
                <Users className="w-3.5 h-3.5 text-purple-400 shrink-0" />
                <span>{t('Plan Odaları & Paylaşımlı Etkinlikler', 'Plan Rooms & Shared Events')}</span>
              </li>
            </ul>
          </div>

          <div className="mt-8 pt-5 border-t border-white/5 flex items-center justify-between text-xs font-semibold text-indigo-400 group-hover:text-indigo-300">
            <span>{t('Görevleri İncele', 'Explore Tasks')}</span>
            <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
          </div>
        </div>

        {/* Card 3: Admin Console (Visible ONLY if isAdmin) */}
        {isAdmin && (
          <div 
            onClick={() => setActiveWorkspace('admin')}
            className="group relative glass-panel rounded-3xl p-7 border border-amber-500/30 hover:border-amber-500/60 hover:shadow-glow-gold transition-all duration-300 cursor-pointer flex flex-col justify-between overflow-hidden"
          >
            {/* Ambient Corner Glow */}
            <div className="pointer-events-none absolute -top-12 -right-12 w-40 h-40 bg-amber-500/10 rounded-full blur-3xl group-hover:bg-amber-500/20 transition-all" />

            <div>
              <div className="w-14 h-14 rounded-full bg-amber-500/15 border border-amber-500/30 flex items-center justify-center text-amber-400 mb-6 group-hover:scale-110 transition-transform shadow-lg shadow-amber-500/10">
                <ShieldCheck className="w-7 h-7" />
              </div>

              <div className="flex items-center gap-2 mb-2">
                <span className="text-xs font-bold uppercase tracking-wider text-amber-400">
                  {t('Yönetim & Kontrol', 'Management & Control')}
                </span>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/30 font-bold">
                  {t('SÜPER ADMİN', 'SUPER ADMIN')}
                </span>
              </div>

              <h2 className="text-2xl font-bold text-white group-hover:text-amber-300 transition-colors">
                {t('Admin Konsolu', 'Admin Console')}
              </h2>

              <p className="text-xs text-gray-400 mt-2 leading-relaxed">
                {t(
                  'Tüm ekosistemi tek merkezden yönetin. Canlı push bildirimleri gönderin, kullanıcıları ve abonelikleri denetleyin.',
                  'Manage the entire ecosystem from one hub. Send live push notifications and audit users and subscriptions.'
                )}
              </p>

              <ul className="mt-5 space-y-2 text-xs text-gray-300">
                <li className="flex items-center gap-2">
                  <Send className="w-3.5 h-3.5 text-rose-400 shrink-0" />
                  <span>{t('Canlı Push Bildirim İstasyonu (FCM)', 'Live Push Notification Station (FCM)')}</span>
                </li>
                <li className="flex items-center gap-2">
                  <Crown className="w-3.5 h-3.5 text-amber-400 shrink-0" />
                  <span>{t('Kullanıcı Yönetimi & Süresiz/Süreli Premium', 'User Management & Lifetime/Timed Premium')}</span>
                </li>
                <li className="flex items-center gap-2">
                  <Database className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                  <span>{t('Neon DB Durumu & Ekosistem İstatistikleri', 'Neon DB Status & Ecosystem Metrics')}</span>
                </li>
              </ul>
            </div>

            <div className="mt-8 pt-5 border-t border-white/5 flex items-center justify-between text-xs font-semibold text-amber-400 group-hover:text-amber-300">
              <span>{t('Yönetim Merkezine Git', 'Open Admin Console')}</span>
              <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </div>
          </div>
        )}

      </div>

    </div>
  );
}

