import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import { getHealthData, seedSampleHealthData } from '../api/client';
import { 
  Heart, 
  Flame, 
  Activity, 
  Utensils, 
  Scale, 
  Calendar, 
  Printer, 
  Download, 
  RefreshCw, 
  AlertCircle, 
  CheckCircle2, 
  ChevronRight,
  Sparkles,
  TrendingDown,
  TrendingUp,
  Clock,
  ShieldCheck,
  PlusCircle,
  CalendarDays
} from 'lucide-react';

export default function HealthTrackView() {
  const { token, user } = useAuth();
  const { t, lang, dateLocale } = useLanguage();
  const cacheKey = user?.id ? `health_data_cache_${user.id}` : 'health_data_cache_default';

  // Initialize from localStorage cache to prevent data loss on tab switches
  const [healthData, setHealthData] = useState(() => {
    try {
      const cached = localStorage.getItem(cacheKey);
      if (cached) return JSON.parse(cached);
    } catch (_) {}
    return {
      foods: [],
      goal: null,
      workouts: [],
      weights: [],
    };
  });

  const [loading, setLoading] = useState(healthData.foods.length === 0 && healthData.workouts.length === 0);
  const [seeding, setSeeding] = useState(false);
  const [error, setError] = useState('');
  const [infoMessage, setInfoMessage] = useState('');
  const [activeTab, setActiveTab] = useState('foods'); // 'foods', 'workouts', 'weights', 'report'
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [foodViewMode, setFoodViewMode] = useState('DATE'); // 'DATE' or 'ALL'

  const loadData = async (isManualRefresh = false) => {
    if (isManualRefresh || (healthData.foods.length === 0 && healthData.workouts.length === 0)) {
      setLoading(true);
    }
    setError('');
    try {
      const data = await getHealthData(token, 0);
      const parsedData = {
        foods: (data.foods || []).filter(f => !f.isDeleted),
        goal: data.nutritionGoals || data.goal || null,
        workouts: (data.workouts || []).filter(w => !w.isDeleted),
        weights: (data.weights || []).filter(wt => !wt.isDeleted),
      };
      setHealthData(parsedData);
      try {
        localStorage.setItem(cacheKey, JSON.stringify(parsedData));
      } catch (_) {}
    } catch (err) {
      console.error('Failed to load health data:', err);
      // Keep displaying cached data if available; only show error if no data at all
      if (healthData.foods.length === 0 && healthData.workouts.length === 0) {
        setError(err.message || t('Sağlık verileri yüklenirken bir hata oluştu.', 'An error occurred while loading health data.'));
      } else {
        setInfoMessage(t('Çevrimdışı / Önbelleğe alınmış sağlık verileri görüntüleniyor.', 'Showing offline / cached health data.'));
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (token) {
      loadData(false);
    }
  }, [token]);

  const handleSeedData = async () => {
    setSeeding(true);
    setError('');
    setInfoMessage('');
    try {
      await seedSampleHealthData(token, selectedDate);
      await loadData(true);
      setInfoMessage(t('Örnek sağlık verileri başarıyla sunucuya kaydedildi ve yüklendi.', 'Sample health data saved to server and loaded successfully.'));
    } catch (err) {
      console.error('Seed error:', err);
      setError(err.message || t('Örnek veriler yüklenirken hata oluştu.', 'An error occurred while loading sample data.'));
    } finally {
      setSeeding(false);
    }
  };

  // Derived Stats
  const targetCalories = healthData.goal?.calories || 2150;
  const targetProtein = healthData.goal?.proteinG || 135;
  const targetCarbs = healthData.goal?.carbsG || 210;
  const targetFat = healthData.goal?.fatG || 65;

  // Selected date food stats
  const dateFoods = healthData.foods.filter(f => f.date === selectedDate);
  const totalCaloriesDate = dateFoods.reduce((acc, f) => acc + (f.totalCalories || 0), 0);

  // Parse items from dateFoods to compute date macros
  let dateProtein = 0;
  let dateCarbs = 0;
  let dateFat = 0;

  dateFoods.forEach(food => {
    if (food.itemsJson) {
      try {
        const items = typeof food.itemsJson === 'string' ? JSON.parse(food.itemsJson) : food.itemsJson;
        if (Array.isArray(items)) {
          items.forEach(it => {
            dateProtein += it.protein || it.proteinG || 0;
            dateCarbs += it.carbs || it.carbsG || 0;
            dateFat += it.fat || it.fatG || 0;
          });
        }
      } catch (_) {}
    }
  });

  // Latest weight
  const sortedWeights = [...healthData.weights].sort((a, b) => (b.date || '').localeCompare(a.date || ''));
  const latestWeight = sortedWeights[0]?.weightKg || null;
  const previousWeight = sortedWeights[1]?.weightKg || null;
  const weightChange = (latestWeight && previousWeight) ? (latestWeight - previousWeight).toFixed(1) : null;

  // Total workouts burn
  const totalWorkoutBurn = healthData.workouts.reduce((acc, w) => acc + (w.caloriesBurned || 0), 0);
  const totalWorkoutMinutes = healthData.workouts.reduce((acc, w) => acc + (w.durationMinutes || 0), 0);

  // Export CSV
  const handleExportCSV = () => {
    let csv = t('Tür,Tarih,Açıklama / Detay,Kalori / Değer\n', 'Type,Date,Description / Detail,Calories / Value\n');
    healthData.foods.forEach(f => {
      csv += `${t('Öğün', 'Meal')},"${f.date}","${(f.summary || f.rawText || '').replace(/"/g, '""')}",${f.totalCalories || 0}\n`;
    });
    healthData.workouts.forEach(w => {
      csv += `${t('Antrenman', 'Workout')},"${w.date}","${(w.rawText || '').replace(/"/g, '""')}",${w.caloriesBurned || 0} kcal (${w.durationMinutes || 0} ${t('dk', 'min')})\n`;
    });
    healthData.weights.forEach(wt => {
      csv += `${t('Kilo', 'Weight')},"${wt.date}","${t('Tartı Kaydı', 'Weight Entry')}",${wt.weightKg} kg\n`;
    });

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `${t('HealthTrack_Rapor', 'HealthTrack_Report')}_${user?.username || 'user'}_${selectedDate}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const isDataEmpty = healthData.foods.length === 0 && healthData.workouts.length === 0 && healthData.weights.length === 0;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      
      {/* Header & Date Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/5 pb-6">
        <div>
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
              <Heart className="w-4 h-4" />
            </div>
            <h1 className="text-2xl font-extrabold text-white tracking-tight">
              {t('HealthTrack Biyo-Telemetri Portalı', 'HealthTrack Bio-Telemetry Portal')}
            </h1>
          </div>
          <p className="text-xs text-gray-400 mt-1">
            {t('Günlük besin dökümü, kalori hedefleri, antrenmanlar ve vücut kompozisyonu geçmişiniz', 'Your daily nutrition log, calorie goals, workouts, and body composition history')}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          {/* Quick Today Button */}
          <button
            onClick={() => setSelectedDate(new Date().toISOString().split('T')[0])}
            className={`px-3 py-1.5 rounded-xl text-xs font-semibold border transition-all ${
              selectedDate === new Date().toISOString().split('T')[0]
                ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30'
                : 'bg-white/5 text-gray-400 border-white/10 hover:text-white'
            }`}
          >
            {t('Bugün', 'Today')}
          </button>

          {/* Date Picker */}
          <div className="flex items-center gap-2 bg-white/5 px-3 py-1.5 rounded-xl border border-white/10">
            <span className="text-xs text-gray-400">{t('Tarih:', 'Date:')}</span>
            <input 
              type="date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="bg-transparent text-xs text-white outline-none cursor-pointer"
            />
          </div>

          {/* Refresh Button */}
          <button
            onClick={() => loadData(true)}
            title={t('Verileri Yenile', 'Refresh Data')}
            disabled={loading}
            className="p-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-gray-300 hover:text-white transition-all disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Info Message */}
      {infoMessage && (
        <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 text-xs flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0" />
            <span>{infoMessage}</span>
          </div>
          <button onClick={() => setInfoMessage('')} className="text-emerald-400 hover:text-white text-xs">{t('Kapat', 'Close')}</button>
        </div>
      )}

      {/* Error Notice */}
      {error && (
        <div className="p-4 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-400 shrink-0" />
            <span>{error}</span>
          </div>
          <button onClick={() => setError('')} className="text-rose-400 hover:text-white text-xs">{t('Kapat', 'Close')}</button>
        </div>
      )}

      {/* Empty Database Helper Banner */}
      {isDataEmpty && !loading && (
        <div className="glass-panel rounded-3xl p-6 sm:p-8 border border-emerald-500/30 bg-gradient-to-r from-emerald-500/10 via-teal-500/5 to-transparent flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div className="space-y-1.5 max-w-2xl">
            <div className="flex items-center gap-2 text-emerald-400 text-xs font-bold uppercase tracking-wider">
              <Sparkles className="w-4 h-4" />
              <span>{t('Veri Senkronizasyonu Bekleniyor', 'Waiting for Data Sync')}</span>
            </div>
            <h2 className="text-lg font-bold text-white">
              {t('Henüz bu hesap için kayıtlı sağlık verisi bulunmuyor', 'No health data recorded for this account yet')}
            </h2>
            <p className="text-xs text-gray-400 leading-relaxed">
              {t('HealthTrack mobil uygulamasından öğün, kilo veya antrenman ekleyip senkronize ettiğinizde burada anlık olarak görünecektir. Dilerseniz panoyu ve grafikleri hemen test etmek için bir tıkla örnek verileri yükleyebilirsiniz.', 'When you log and sync meals, weight, or workouts from the HealthTrack mobile app, they will appear here in real time. You can also load sample data in one click to preview the dashboard and charts right away.')}
            </p>
          </div>

          <button
            onClick={handleSeedData}
            disabled={seeding}
            className="px-5 py-2.5 rounded-2xl bg-gradient-to-r from-emerald-500 to-teal-500 text-white text-xs font-bold flex items-center gap-2 shadow-lg shadow-emerald-500/20 hover:scale-105 active:scale-95 transition-all disabled:opacity-50 shrink-0"
          >
            <PlusCircle className={`w-4 h-4 ${seeding ? 'animate-spin' : ''}`} />
            <span>{seeding ? t('Veriler Yükleniyor...', 'Loading Data...') : t('Örnek Sağlık Verilerini Yükle', 'Load Sample Health Data')}</span>
          </button>
        </div>
      )}

      {/* Telemetry Hero Bento Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5">
        
        {/* Metric 1: Calories Ring */}
        <div className="glass-panel rounded-3xl p-6 relative overflow-hidden border border-emerald-500/20">
          <div className="flex items-center justify-between mb-4">
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-400">
              {t('Günlük Kalori', 'Daily Calories')}
            </span>
            <Flame className="w-4 h-4 text-orange-400" />
          </div>

          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-black text-white">
              {totalCaloriesDate}
            </span>
            <span className="text-xs text-gray-400 font-medium">
              / {targetCalories} kcal
            </span>
          </div>

          {/* Progress Bar */}
          <div className="w-full bg-white/5 h-2 rounded-full mt-4 overflow-hidden">
            <div 
              className={`h-full rounded-full transition-all duration-500 ${
                totalCaloriesDate > targetCalories ? 'bg-orange-500' : 'bg-gradient-to-r from-emerald-500 to-teal-400'
              }`}
              style={{ width: `${Math.min(100, Math.round((totalCaloriesDate / (targetCalories || 1)) * 100))}%` }}
            />
          </div>

          <div className="mt-3 flex items-center justify-between text-[11px] text-gray-400">
            <span>{t('Kalan', 'Remaining')}: {Math.max(0, targetCalories - totalCaloriesDate)} kcal</span>
            <span>{lang === 'tr' ? `%${Math.round((totalCaloriesDate / (targetCalories || 1)) * 100)}` : `${Math.round((totalCaloriesDate / (targetCalories || 1)) * 100)}%`}</span>
          </div>
        </div>

        {/* Metric 2: Macro Nutrients */}
        <div className="glass-panel rounded-3xl p-6 relative overflow-hidden border border-teal-500/20">
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold uppercase tracking-wider text-teal-400">
              {t('Makro Dağılımı', 'Macro Breakdown')} ({selectedDate})
            </span>
            <Utensils className="w-4 h-4 text-teal-400" />
          </div>

          <div className="space-y-2 text-xs">
            {/* Protein */}
            <div>
              <div className="flex justify-between text-[11px] mb-1">
                <span className="text-gray-400">Protein</span>
                <span className="text-gray-300 font-mono">{Math.round(dateProtein)}g / {targetProtein}g</span>
              </div>
              <div className="w-full bg-white/5 h-1.5 rounded-full overflow-hidden">
                <div 
                  className="bg-indigo-400 h-full rounded-full" 
                  style={{ width: `${Math.min(100, Math.round((dateProtein / (targetProtein || 1)) * 100))}%` }}
                />
              </div>
            </div>

            {/* Carbs */}
            <div>
              <div className="flex justify-between text-[11px] mb-1">
                <span className="text-gray-400">{t('Karbonhidrat', 'Carbs')}</span>
                <span className="text-gray-300 font-mono">{Math.round(dateCarbs)}g / {targetCarbs}g</span>
              </div>
              <div className="w-full bg-white/5 h-1.5 rounded-full overflow-hidden">
                <div 
                  className="bg-amber-400 h-full rounded-full" 
                  style={{ width: `${Math.min(100, Math.round((dateCarbs / (targetCarbs || 1)) * 100))}%` }}
                />
              </div>
            </div>

            {/* Fat */}
            <div>
              <div className="flex justify-between text-[11px] mb-1">
                <span className="text-gray-400">{t('Yağ', 'Fat')}</span>
                <span className="text-gray-300 font-mono">{Math.round(dateFat)}g / {targetFat}g</span>
              </div>
              <div className="w-full bg-white/5 h-1.5 rounded-full overflow-hidden">
                <div 
                  className="bg-rose-400 h-full rounded-full" 
                  style={{ width: `${Math.min(100, Math.round((dateFat / (targetFat || 1)) * 100))}%` }}
                />
              </div>
            </div>
          </div>
        </div>

        {/* Metric 3: Weight Scale */}
        <div className="glass-panel rounded-3xl p-6 relative overflow-hidden border border-emerald-500/20">
          <div className="flex items-center justify-between mb-4">
            <span className="text-xs font-bold uppercase tracking-wider text-emerald-400">
              {t('Vücut Ağırlığı', 'Body Weight')}
            </span>
            <Scale className="w-4 h-4 text-emerald-400" />
          </div>

          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-black text-white">
              {latestWeight ? `${latestWeight}` : '--'}
            </span>
            <span className="text-sm text-gray-400 font-medium">kg</span>
          </div>

          <div className="mt-4 flex items-center gap-2">
            {weightChange !== null ? (
              <span className={`inline-flex items-center gap-1 text-xs font-semibold px-2 py-0.5 rounded-full ${
                Number(weightChange) < 0 
                  ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/30' 
                  : 'bg-orange-500/15 text-orange-300 border border-orange-500/30'
              }`}>
                {Number(weightChange) < 0 ? <TrendingDown className="w-3 h-3" /> : <TrendingUp className="w-3 h-3" />}
                {Math.abs(weightChange)} kg ({sortedWeights[1]?.date ? t('önceki tartıya göre', 'vs previous weigh-in') : ''})
              </span>
            ) : (
              <span className="text-xs text-gray-500">{t('Kilo kaydı bekleniyor', 'Waiting for weight log')}</span>
            )}
          </div>

          <p className="text-[11px] text-gray-400 mt-2">
            {t(`Toplam ${healthData.weights.length} tartı kaydı mevcut`, `${healthData.weights.length} weight entries recorded`)}
          </p>
        </div>

        {/* Metric 4: Workouts & Burns */}
        <div className="glass-panel rounded-3xl p-6 relative overflow-hidden border border-white/10">
          <div className="flex items-center justify-between mb-4">
            <span className="text-xs font-bold uppercase tracking-wider text-indigo-400">
              {t('Antrenman & Aktivite', 'Workouts & Activity')}
            </span>
            <Activity className="w-4 h-4 text-indigo-400" />
          </div>

          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-black text-white">
              {totalWorkoutBurn}
            </span>
            <span className="text-xs text-gray-400 font-medium">{t('kcal yakıldı', 'kcal burned')}</span>
          </div>

          <div className="mt-4 flex items-center justify-between text-xs text-gray-400">
            <span>{t('Toplam Süre:', 'Total Duration:')}</span>
            <span className="text-white font-semibold">{totalWorkoutMinutes} {t('dakika', 'min')}</span>
          </div>

          <p className="text-[11px] text-gray-500 mt-2">
            {t(`Toplam ${healthData.workouts.length} kayıtlı egzersiz`, `${healthData.workouts.length} recorded workouts`)}
          </p>
        </div>

      </div>

      {/* Tabs Navigation */}
      <div className="flex items-center gap-2 border-b border-white/10 pb-2 overflow-x-auto">
        <button
          onClick={() => setActiveTab('foods')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all whitespace-nowrap ${
            activeTab === 'foods'
              ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
              : 'text-gray-400 hover:text-white hover:bg-white/5'
          }`}
        >
          <Utensils className="w-3.5 h-3.5" />
          <span>{t('Öğün Günlüğü', 'Food Log')} ({healthData.foods.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('workouts')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all whitespace-nowrap ${
            activeTab === 'workouts'
              ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
              : 'text-gray-400 hover:text-white hover:bg-white/5'
          }`}
        >
          <Activity className="w-3.5 h-3.5" />
          <span>{t('Antrenmanlar', 'Workouts')} ({healthData.workouts.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('weights')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all whitespace-nowrap ${
            activeTab === 'weights'
              ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
              : 'text-gray-400 hover:text-white hover:bg-white/5'
          }`}
        >
          <Scale className="w-3.5 h-3.5" />
          <span>{t('Kilo Takibi', 'Weight Tracking')} ({healthData.weights.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('report')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all whitespace-nowrap ${
            activeTab === 'report'
              ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30'
              : 'text-gray-400 hover:text-white hover:bg-white/5'
          }`}
        >
          <Printer className="w-3.5 h-3.5" />
          <span>{t('Sağlık Raporu & Çıktı', 'Health Report & Export')}</span>
        </button>
      </div>

      {/* Tab 1: Food Logs Content */}
      {activeTab === 'foods' && (
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <h2 className="text-sm font-bold uppercase tracking-wider text-gray-300">
                {foodViewMode === 'DATE' 
                  ? t(`Seçili Günün Öğünleri (${dateFoods.length} öğün: ${selectedDate})`, `Meals for Selected Date (${dateFoods.length} meals: ${selectedDate})`)
                  : t(`Tüm Kayıtlı Öğünler (${healthData.foods.length} öğün)`, `All Recorded Meals (${healthData.foods.length} meals)`)}
              </h2>
              <span className="text-xs text-gray-500">
                {t('Toplam kalori ve besin değeri dağılımı', 'Total calorie and nutrient breakdown')}
              </span>
            </div>

            {/* Toggle: Seçili Gün vs Tüm Tarihler */}
            <div className="flex items-center gap-1.5 bg-white/5 p-1 rounded-xl border border-white/5">
              <button
                onClick={() => setFoodViewMode('DATE')}
                className={`px-3 py-1 rounded-lg text-xs font-semibold transition-all ${
                  foodViewMode === 'DATE' ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30' : 'text-gray-400 hover:text-white'
                }`}
              >
                {t('Seçili Tarih', 'Selected Date')} ({selectedDate})
              </button>
              <button
                onClick={() => setFoodViewMode('ALL')}
                className={`px-3 py-1 rounded-lg text-xs font-semibold transition-all ${
                  foodViewMode === 'ALL' ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30' : 'text-gray-400 hover:text-white'
                }`}
              >
                {t('Tüm Öğünler', 'All Meals')} ({healthData.foods.length})
              </button>
            </div>
          </div>

          {/* Food List */}
          {((foodViewMode === 'DATE' ? dateFoods : healthData.foods).length === 0) ? (
            <div className="glass-panel rounded-3xl p-10 text-center border border-white/5 space-y-3">
              <Utensils className="w-8 h-8 text-gray-600 mx-auto" />
              <p className="text-sm font-semibold text-gray-300">
                {foodViewMode === 'DATE' 
                  ? t(`Bu tarihte (${selectedDate}) kayıtlı öğün bulunamadı.`, `No meals recorded on ${selectedDate}.`)
                  : t('Henüz hiçbir tarihte kayıtlı öğün bulunmuyor.', 'No meals recorded on any date yet.')}
              </p>
              <p className="text-xs text-gray-500 max-w-md mx-auto">
                {t('HealthTrack mobil uygulamasından fotoğraf veya serbest metinle öğün ekleyebilirsiniz.', 'You can log meals via photo or free text from the HealthTrack mobile app.')}
              </p>
              {healthData.foods.length > 0 && foodViewMode === 'DATE' && (
                <button
                  onClick={() => setFoodViewMode('ALL')}
                  className="mt-2 px-4 py-1.5 rounded-xl bg-white/10 hover:bg-white/15 text-xs text-emerald-300 font-semibold transition-all"
                >
                  {t(`Diğer Günlerdeki Öğünleri Gör (${healthData.foods.length} öğün)`, `View Meals on Other Days (${healthData.foods.length} meals)`)}
                </button>
              )}
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {(foodViewMode === 'DATE' ? dateFoods : healthData.foods).map((food) => {
                let parsedItems = [];
                if (food.itemsJson) {
                  try {
                    parsedItems = typeof food.itemsJson === 'string' ? JSON.parse(food.itemsJson) : food.itemsJson;
                  } catch (_) {}
                }

                return (
                  <div key={food.id} className="glass-panel rounded-2xl p-5 border border-white/10 hover:border-emerald-500/30 transition-all">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <span className="text-[10px] font-bold uppercase px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                          {food.date}
                        </span>
                        <h3 className="text-base font-bold text-white mt-1.5">
                          {food.summary || food.rawText}
                        </h3>
                        {food.summary && food.rawText && food.summary !== food.rawText && (
                          <p className="text-xs text-gray-400 mt-0.5 line-clamp-2">"{food.rawText}"</p>
                        )}
                      </div>

                      <div className="text-right shrink-0">
                        <span className="text-lg font-black text-orange-400">
                          {food.totalCalories || 0}
                        </span>
                        <span className="text-[10px] text-gray-400 block -mt-1">kcal</span>
                      </div>
                    </div>

                    {/* Breakdown items */}
                    {Array.isArray(parsedItems) && parsedItems.length > 0 && (
                      <div className="mt-4 pt-3 border-t border-white/5 space-y-2">
                        <span className="text-[11px] font-bold uppercase text-gray-400 tracking-wider block">
                          {t('İçerik & Makro Detayları', 'Items & Macro Details')}
                        </span>
                        <div className="divide-y divide-white/5">
                          {parsedItems.map((item, idx) => (
                            <div key={idx} className="py-1.5 flex items-center justify-between text-xs">
                              <span className="text-gray-300">{item.name || item.foodName}</span>
                              <div className="flex items-center gap-3">
                                {(item.protein || item.proteinG) && (
                                  <span className="text-[10px] text-indigo-300 font-mono">
                                    {item.protein || item.proteinG}g P
                                  </span>
                                )}
                                {(item.carbs || item.carbsG) && (
                                  <span className="text-[10px] text-amber-300 font-mono">
                                    {item.carbs || item.carbsG}g {t('K', 'C')}
                                  </span>
                                )}
                                {(item.fat || item.fatG) && (
                                  <span className="text-[10px] text-rose-300 font-mono">
                                    {item.fat || item.fatG}g {t('Y', 'F')}
                                  </span>
                                )}
                                <span className="font-semibold text-gray-200 font-mono">
                                  {item.calories} kcal
                                </span>
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Tab 2: Workouts Content */}
      {activeTab === 'workouts' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold uppercase tracking-wider text-gray-300">
              {t('Egzersiz & Spor Kayıtları', 'Exercise & Workout Logs')}
            </h2>
            <span className="text-xs text-gray-500">{t(`Toplam ${healthData.workouts.length} kayıt`, `${healthData.workouts.length} total entries`)}</span>
          </div>

          {healthData.workouts.length === 0 ? (
            <div className="glass-panel rounded-3xl p-10 text-center border border-white/5">
              <Activity className="w-8 h-8 text-gray-600 mx-auto mb-3" />
              <p className="text-sm font-semibold text-gray-300">{t('Henüz kayıtlı egzersiz bulunmuyor.', 'No workouts recorded yet.')}</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {healthData.workouts.map((w) => (
                <div key={w.id} className="glass-panel rounded-2xl p-5 border border-white/10 hover:border-indigo-500/30 transition-all">
                  <div className="flex items-center justify-between text-xs text-gray-400 mb-2">
                    <span className="font-semibold">{w.date}</span>
                    <span className="text-indigo-400 font-semibold">{w.durationMinutes || 0} {t('dakika', 'min')}</span>
                  </div>

                  <h3 className="text-base font-bold text-white mb-2">
                    {w.rawText}
                  </h3>

                  {w.aiAnalysis && (
                    <p className="text-xs text-gray-400 bg-white/[0.03] p-2.5 rounded-xl border border-white/5 mb-3">
                      {w.aiAnalysis}
                    </p>
                  )}

                  <div className="flex items-center justify-between pt-3 border-t border-white/5 text-xs">
                    <span className="text-gray-400">{t('Yakılan Enerji:', 'Calories Burned:')}</span>
                    <span className="text-base font-black text-orange-400">{w.caloriesBurned || 0} kcal</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Tab 3: Weights Content */}
      {activeTab === 'weights' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-sm font-bold uppercase tracking-wider text-gray-300">
              {t('Tartı & Kilo Geçmişi', 'Weigh-in & Weight History')}
            </h2>
            <span className="text-xs text-gray-500">{t(`Toplam ${healthData.weights.length} kayıt`, `${healthData.weights.length} total entries`)}</span>
          </div>

          {healthData.weights.length === 0 ? (
            <div className="glass-panel rounded-3xl p-10 text-center border border-white/5">
              <Scale className="w-8 h-8 text-gray-600 mx-auto mb-3" />
              <p className="text-sm font-semibold text-gray-300">{t('Henüz kayıtlı kilo geçmişi bulunmuyor.', 'No weight history recorded yet.')}</p>
            </div>
          ) : (
            <div className="glass-panel rounded-3xl p-6 border border-white/10 overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="border-b border-white/10 text-gray-400">
                  <tr>
                    <th className="pb-3 font-semibold">{t('Tarih', 'Date')}</th>
                    <th className="pb-3 font-semibold">{t('Ölçülen Kilo', 'Measured Weight')}</th>
                    <th className="pb-3 font-semibold">{t('Hedef Kilo', 'Target Weight')}</th>
                    <th className="pb-3 font-semibold">{t('Fark', 'Diff')}</th>
                    <th className="pb-3 font-semibold">{t('Not / Açıklama', 'Note / Details')}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {sortedWeights.map((wt) => {
                    const diff = wt.targetWeightKg ? (wt.weightKg - wt.targetWeightKg).toFixed(1) : null;
                    return (
                      <tr key={wt.id} className="hover:bg-white/[0.02] transition-colors">
                        <td className="py-3 text-gray-300 font-medium">{wt.date}</td>
                        <td className="py-3">
                          <span className="text-sm font-bold text-white font-mono">{wt.weightKg} kg</span>
                        </td>
                        <td className="py-3 text-gray-400 font-mono">
                          {wt.targetWeightKg ? `${wt.targetWeightKg} kg` : '--'}
                        </td>
                        <td className="py-3">
                          {diff !== null ? (
                            <span className={`text-[11px] font-bold px-2 py-0.5 rounded ${
                              Number(diff) <= 0 
                                ? 'bg-emerald-500/15 text-emerald-300' 
                                : 'bg-orange-500/15 text-orange-300'
                            }`}>
                              {Number(diff) > 0 ? `+${diff}` : diff} kg
                            </span>
                          ) : '--'}
                        </td>
                        <td className="py-3 text-gray-400 italic">
                          {wt.note || t('Ölçüm kaydı', 'Weight entry')}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Tab 4: Health Report & Export */}
      {activeTab === 'report' && (
        <div className="space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h2 className="text-lg font-bold text-white">{t('Biyo-Telemetri & Beslenme Raporu', 'Bio-Telemetry & Nutrition Report')}</h2>
              <p className="text-xs text-gray-400">
                {t('Seçili tarih ve genel sağlık geçmişinizin detaylı dökümü', 'Detailed summary of your selected date and overall health history')}
              </p>
            </div>

            <div className="flex items-center gap-3">
              <button
                onClick={handleExportCSV}
                className="px-3.5 py-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-semibold text-gray-200 flex items-center gap-2 transition-all"
              >
                <Download className="w-4 h-4 text-emerald-400" />
                <span>{t('CSV Olarak İndir', 'Download as CSV')}</span>
              </button>

              <button
                onClick={() => window.print()}
                className="px-3.5 py-2 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 text-xs font-semibold text-white flex items-center gap-2 shadow-lg hover:shadow-glow-health transition-all"
              >
                <Printer className="w-4 h-4" />
                <span>{t('Raporu Yazdır / PDF Kaydet', 'Print Report / Save PDF')}</span>
              </button>
            </div>
          </div>

          {/* Printable Document Box */}
          <div className="glass-panel rounded-3xl p-8 border border-white/10 space-y-6 print:border-none print:p-0">
            <div className="border-b border-white/10 pb-4 flex justify-between items-start">
              <div>
                <h1 className="text-xl font-bold text-white">{t('Bireysel Sağlık & Beslenme İlerleme Raporu', 'Personal Health & Nutrition Progress Report')}</h1>
                <p className="text-xs text-gray-400 mt-1">
                  {t('Kullanıcı:', 'User:')} <strong className="text-white">{user?.name || user?.username}</strong> | {t('Rapor Tarihi:', 'Report Date:')} {new Date().toLocaleDateString(dateLocale)}
                </p>
              </div>
              <div className="text-right text-xs text-gray-400">
                <span className="font-bold text-emerald-400">{t('HealthTrack Ekosistemi', 'HealthTrack Ecosystem')}</span>
                <p className="text-[10px]">Neon Cloud Database Verified</p>
              </div>
            </div>

            {/* Summary Grid */}
            <div className="grid grid-cols-3 gap-4 text-xs">
              <div className="p-3 rounded-xl bg-white/5">
                <span className="text-gray-400 block">{t('Güncel Kilo', 'Current Weight')}</span>
                <span className="text-base font-bold text-white">{latestWeight ? `${latestWeight} kg` : t('Veri yok', 'No data')}</span>
              </div>
              <div className="p-3 rounded-xl bg-white/5">
                <span className="text-gray-400 block">{t('Günlük Kalori Hedefi', 'Daily Calorie Goal')}</span>
                <span className="text-base font-bold text-white">{targetCalories} kcal</span>
              </div>
              <div className="p-3 rounded-xl bg-white/5">
                <span className="text-gray-400 block">{t('Toplam Egzersiz Kaydı', 'Total Workout Sessions')}</span>
                <span className="text-base font-bold text-white">{healthData.workouts.length} {t('seans', 'sessions')}</span>
              </div>
            </div>

            {/* Recent Foods Table */}
            <div>
              <h3 className="text-xs font-bold uppercase tracking-wider text-gray-300 mb-2">
                {t('Son Kaydedilen Öğünler', 'Recently Recorded Meals')}
              </h3>
              <div className="rounded-xl border border-white/10 overflow-hidden">
                <table className="w-full text-left text-xs">
                  <thead className="bg-white/5 text-gray-400">
                    <tr>
                      <th className="p-2.5">{t('Tarih', 'Date')}</th>
                      <th className="p-2.5">{t('Öğün Özeti', 'Meal Summary')}</th>
                      <th className="p-2.5 text-right">{t('Kalori', 'Calories')}</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5">
                    {healthData.foods.slice(0, 10).map((f) => (
                      <tr key={f.id}>
                        <td className="p-2.5 text-gray-400">{f.date}</td>
                        <td className="p-2.5 text-white font-medium">{f.summary || f.rawText}</td>
                        <td className="p-2.5 text-right text-orange-400 font-mono font-semibold">{f.totalCalories} kcal</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Disclaimer */}
            <div className="p-3 rounded-xl bg-white/[0.02] border border-white/5 text-[11px] text-gray-400">
              * {t('Bu rapor, kullanıcının HealthTrack mobil uygulaması üzerinden kaydettiği biyo-telemetri ve besin tüketim verilerini içerir. Tıbbi tanı yerine geçmez, danışman hekim ve diyetisyen değerlendirmesi için hazırlanmıştır.', 'This report contains bio-telemetry and nutrition intake data recorded by the user via the HealthTrack mobile app. It is not a medical diagnosis and is intended for review by a physician or dietitian.')}
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
