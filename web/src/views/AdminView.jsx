import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useLanguage } from '../context/LanguageContext';
import * as api from '../api/client';
import { 
  ShieldCheck, 
  Send, 
  Users, 
  Database, 
  Crown, 
  Search, 
  RefreshCw, 
  CheckCircle2, 
  AlertCircle, 
  Trash2, 
  Key, 
  Smartphone, 
  Bell, 
  Sparkles, 
  Flame, 
  CheckSquare, 
  Heart,
  BarChart3,
  ExternalLink,
  ChevronRight,
  UserCheck,
  UserX,
  X
} from 'lucide-react';

export default function AdminView() {
  const { token, adminSecret, setAdminSecret, user } = useAuth();
  const { t, dateLocale } = useLanguage();
  const [activeTab, setActiveTab] = useState('push'); // 'push', 'users', 'system'

  // Push State
  const [pushMode, setPushMode] = useState('BROADCAST'); // 'BROADCAST', 'SPECIFIC'
  const [pushTitle, setPushTitle] = useState('');
  const [pushBody, setPushBody] = useState('');
  const [pushUserQuery, setPushUserQuery] = useState('');
  const [selectedUser, setSelectedUser] = useState(null);
  const [pushTargets, setPushTargets] = useState([]);
  const [searchingTargets, setSearchingTargets] = useState(false);
  const [pushSending, setPushSending] = useState(false);
  const [pushResult, setPushResult] = useState(null);

  // Users Management State
  const [usersList, setUsersList] = useState([]);
  const [usersQuery, setUsersQuery] = useState('');
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [selectedUserTasks, setSelectedUserTasks] = useState(null);
  const [loadingTasksModal, setLoadingTasksModal] = useState(false);
  const [userActionFeedback, setUserActionFeedback] = useState(null);

  // Stats State
  const [stats, setStats] = useState(null);
  const [loadingStats, setLoadingStats] = useState(false);
  const [dbStatus, setDbStatus] = useState(null);

  // Force Update / App Configs State
  const [appConfigs, setAppConfigs] = useState([]);
  const [loadingConfigs, setLoadingConfigs] = useState(false);
  const [savingConfigAppId, setSavingConfigAppId] = useState(null);
  const [configFeedback, setConfigFeedback] = useState(null);

  // Load Initial Data
  useEffect(() => {
    loadStats();
    loadUsers();
    loadAppConfigs();
  }, [token, adminSecret]);

  const loadAppConfigs = async () => {
    setLoadingConfigs(true);
    try {
      const data = await api.getAdminAppConfigs(token, adminSecret);
      setAppConfigs(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('App configs load error:', err);
    } finally {
      setLoadingConfigs(false);
    }
  };

  const handleAppConfigFieldChange = (appId, field, value) => {
    setAppConfigs((prev) =>
      prev.map((cfg) => (cfg.appId === appId ? { ...cfg, [field]: value } : cfg))
    );
  };

  const handleSaveAppConfig = async (cfg) => {
    setSavingConfigAppId(cfg.appId);
    setConfigFeedback(null);
    try {
      const updated = await api.updateAdminAppConfig(token, cfg.appId, cfg, adminSecret);
      setAppConfigs((prev) => prev.map((c) => (c.appId === cfg.appId ? updated : c)));
      setConfigFeedback({
        type: 'success',
        message: `${cfg.appId === 'healthtrack' ? 'HealthTrack' : 'Planora'} ${t('Force Update ayarları kaydedildi.', 'Force Update settings saved.')}`,
      });
    } catch (err) {
      setConfigFeedback({
        type: 'error',
        message: err.message || t('Versiyon ayarları kaydedilemedi.', 'Failed to save version settings.'),
      });
    } finally {
      setSavingConfigAppId(null);
    }
  };

  const loadStats = async () => {
    setLoadingStats(true);
    try {
      const data = await api.getAdminStats(token, adminSecret);
      setStats(data);
      const db = await api.checkDbStatus(token);
      setDbStatus(db);
    } catch (err) {
      console.error('Stats error:', err);
    } finally {
      setLoadingStats(false);
    }
  };

  const loadUsers = async (query = '') => {
    setLoadingUsers(true);
    try {
      const data = await api.getAdminUsers(token, query, adminSecret);
      setUsersList(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Users load error:', err);
    } finally {
      setLoadingUsers(false);
    }
  };

  // Push user search
  const searchPushTargets = async (query) => {
    setPushUserQuery(query);
    if (!query || query.length < 2) {
      setPushTargets([]);
      return;
    }
    setSearchingTargets(true);
    try {
      const data = await api.getAdminPushTargets(token, query, adminSecret);
      setPushTargets(Array.isArray(data) ? data : []);
    } catch (e) {
      console.error(e);
    } finally {
      setSearchingTargets(false);
    }
  };

  // Push Sending
  const handleSendPush = async (e) => {
    e.preventDefault();
    if (!pushTitle.trim() || !pushBody.trim()) {
      alert(t('Lütfen başlık ve mesaj içeriği girin.', 'Please enter a title and message body.'));
      return;
    }

    if (pushMode === 'SPECIFIC' && !selectedUser) {
      alert(t('Lütfen bildirim göndermek için bir kullanıcı seçin.', 'Please select a user to send the notification to.'));
      return;
    }

    setPushSending(true);
    setPushResult(null);

    const payload = {
      title: pushTitle.trim(),
      body: pushBody.trim(),
      broadcastAll: pushMode === 'BROADCAST',
      userId: pushMode === 'SPECIFIC' ? selectedUser.id : null,
      data: {
        sender: 'Planora_HealthTrack_Portal_Admin',
        timestamp: Date.now().toString(),
      }
    };

    try {
      const res = await api.sendAdminPush(token, payload, adminSecret);
      setPushResult({ ok: res.success !== false, message: res.message || t('Bildirim iletildi!', 'Notification sent!') });
      if (res.success !== false) {
        setPushTitle('');
        setPushBody('');
        setSelectedUser(null);
      }
    } catch (err) {
      setPushResult({ ok: false, message: err.message || t('Bildirim gönderilemedi.', 'Failed to send notification.') });
    } finally {
      setPushSending(false);
    }
  };

  // Push Templates
  const applyTemplate = (title, body) => {
    setPushTitle(title);
    setPushBody(body);
  };

  // User Actions: Set Premium
  const handleSetPremium = async (targetUser, days, isPremium = true) => {
    try {
      const res = await api.setAdminUserPremium(token, {
        userId: targetUser.id,
        isPremium,
        days,
      }, adminSecret);
      setUserActionFeedback({ ok: true, message: res.message || t('Premium güncellendi.', 'Premium status updated.') });
      loadUsers(usersQuery);
      loadStats();
    } catch (err) {
      setUserActionFeedback({ ok: false, message: err.message });
    }
  };

  // User Actions: Update Role
  const handleUpdateRole = async (targetUser, newRole) => {
    try {
      await api.updateAdminUserRole(token, targetUser.id, newRole, adminSecret);
      setUserActionFeedback({
        ok: true,
        message: t(
          `'${targetUser.username}' kullanıcısının rolü '${newRole}' yapıldı.`,
          `User '${targetUser.username}' role updated to '${newRole}'.`
        )
      });
      loadUsers(usersQuery);
    } catch (err) {
      setUserActionFeedback({ ok: false, message: err.message });
    }
  };

  // User Actions: Delete User
  const handleDeleteUser = async (targetUser) => {
    if (!confirm(t(
      `DİKKAT: '${targetUser.username}' kullanıcısını ve tüm verilerini kalıcı olarak silmek istediğinizden emin misiniz?`,
      `WARNING: Are you sure you want to permanently delete '${targetUser.username}' and all their data?`
    ))) {
      return;
    }

    try {
      const res = await api.deleteAdminUser(token, targetUser.id, adminSecret);
      setUserActionFeedback({ ok: true, message: res.message || t('Kullanıcı silindi.', 'User deleted.') });
      loadUsers(usersQuery);
      loadStats();
    } catch (err) {
      setUserActionFeedback({ ok: false, message: err.message });
    }
  };

  // User Actions: Inspect Tasks
  const handleInspectTasks = async (targetUser) => {
    setLoadingTasksModal(true);
    setSelectedUserTasks(null);
    try {
      const data = await api.getAdminUserTasks(token, targetUser.id, adminSecret);
      setSelectedUserTasks(data);
    } catch (err) {
      alert(t(`Görevler alınamadı: ${err.message}`, `Failed to load tasks: ${err.message}`));
    } finally {
      setLoadingTasksModal(false);
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-white/5 pb-6">
        <div>
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-amber-500 to-rose-500 flex items-center justify-center text-white shadow-glow-gold">
              <ShieldCheck className="w-4 h-4" />
            </div>
            <h1 className="text-2xl font-extrabold text-white tracking-tight">
              {t('Ekosistem Admin Kontrol İstasyonu', 'Ecosystem Admin Control Station')}
            </h1>
            <span className="text-[10px] font-bold uppercase px-2 py-0.5 rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/30">
              {t('TAM YETKİLİ', 'FULL ACCESS')}
            </span>
          </div>
          <p className="text-xs text-gray-400 mt-1">
            {t('FCM push bildirim konsolu, kullanıcı & premium yönetimi ve Neon DB telemetrisi', 'FCM push notification console, user & premium management, and Neon DB telemetry')}
          </p>
        </div>

        <div className="flex items-center gap-3">
          {/* Secret Key Input if desired */}
          <div className="relative">
            <input 
              type="password"
              value={adminSecret}
              onChange={(e) => setAdminSecret(e.target.value)}
              placeholder={t('X-Admin-Secret (İsteğe bağlı)', 'X-Admin-Secret (Optional)')}
              className="bg-white/5 border border-white/10 rounded-xl px-3 py-1.5 text-xs text-white placeholder-gray-500 outline-none w-48 focus:border-amber-400"
            />
          </div>
        </div>
      </div>

      {/* Action Feedback Banner */}
      {userActionFeedback && (
        <div className={`p-4 rounded-2xl text-xs flex items-center justify-between ${
          userActionFeedback.ok ? 'bg-emerald-500/10 border border-emerald-500/20 text-emerald-300' : 'bg-rose-500/10 border border-rose-500/20 text-rose-300'
        }`}>
          <div className="flex items-center gap-2">
            {userActionFeedback.ok ? <CheckCircle2 className="w-4 h-4" /> : <AlertCircle className="w-4 h-4" />}
            <span>{userActionFeedback.message}</span>
          </div>
          <button onClick={() => setUserActionFeedback(null)} className="text-gray-400 hover:text-white">
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-white/10 pb-2">
        <button
          onClick={() => setActiveTab('push')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all ${
            activeTab === 'push'
              ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30 shadow-sm'
              : 'text-gray-400 hover:text-white hover:bg-white/5'
          }`}
        >
          <Send className="w-3.5 h-3.5" />
          <span>{t('Push Bildirim İstasyonu (FCM)', 'Push Notification Station (FCM)')}</span>
        </button>

        <button
          onClick={() => setActiveTab('users')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all ${
            activeTab === 'users'
              ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30 shadow-sm'
              : 'text-gray-400 hover:text-white hover:bg-white/5'
          }`}
        >
          <Users className="w-3.5 h-3.5" />
          <span>{t('Kullanıcı & Premium Yönetimi', 'User & Premium Management')} ({usersList.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('system')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all ${
            activeTab === 'system'
              ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 shadow-sm'
              : 'text-gray-400 hover:text-white hover:bg-white/5'
          }`}
        >
          <Database className="w-3.5 h-3.5" />
          <span>{t('Sistem & Neon DB Telemetrisi', 'System & Neon DB Telemetry')}</span>
        </button>

        <button
          onClick={() => setActiveTab('versions')}
          className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-2 transition-all ${
            activeTab === 'versions'
              ? 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 shadow-sm'
              : 'text-gray-400 hover:text-white hover:bg-white/5'
          }`}
        >
          <Smartphone className="w-3.5 h-3.5" />
          <span>{t('Force Update & Versiyon', 'Force Update & Versions')}</span>
        </button>
      </div>

      {/* TAB 1: PUSH NOTIFICATION CONSOLE */}
      {activeTab === 'push' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          
          {/* Left Form: 7 cols */}
          <div className="lg:col-span-7 space-y-6">
            
            {/* Target Selector */}
            <div className="glass-panel rounded-3xl p-6 border border-white/10 space-y-5">
              <h3 className="text-xs font-bold uppercase tracking-wider text-rose-400 flex items-center gap-2">
                <Send className="w-3.5 h-3.5" />
                <span>{t('1. Hedef Kitle Belirleme', '1. Select Target Audience')}</span>
              </h3>

              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => { setPushMode('BROADCAST'); setSelectedUser(null); }}
                  className={`p-3.5 rounded-2xl text-left border transition-all ${
                    pushMode === 'BROADCAST' 
                      ? 'bg-rose-500/15 border-rose-500/40 text-white shadow-glow-planora' 
                      : 'bg-white/[0.02] border-white/10 text-gray-400 hover:border-white/20'
                  }`}
                >
                  <span className="font-bold text-xs block text-white">{t('📢 Genel Duyuru (Broadcast)', '📢 General Broadcast')}</span>
                  <span className="text-[11px] text-gray-400 mt-0.5 block">{t('Tüm kayıtlı cihazlara anlık iletilir', 'Sent immediately to all registered devices')}</span>
                </button>

                <button
                  type="button"
                  onClick={() => setPushMode('SPECIFIC')}
                  className={`p-3.5 rounded-2xl text-left border transition-all ${
                    pushMode === 'SPECIFIC' 
                      ? 'bg-amber-500/15 border-amber-500/40 text-white shadow-glow-gold' 
                      : 'bg-white/[0.02] border-white/10 text-gray-400 hover:border-white/20'
                  }`}
                >
                  <span className="font-bold text-xs block text-white">{t('🎯 Tekil Kullanıcı', '🎯 Single User')}</span>
                  <span className="text-[11px] text-gray-400 mt-0.5 block">{t('Kullanıcı adı veya e-posta ile hedefle', 'Target by username or email')}</span>
                </button>
              </div>

              {/* Specific user search */}
              {pushMode === 'SPECIFIC' && (
                <div className="pt-2">
                  <label className="block text-[11px] font-semibold text-gray-400 mb-1.5 uppercase">
                    {t('Kullanıcı Ara', 'Search User')}
                  </label>
                  <div className="relative">
                    <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-500" />
                    <input
                      type="text"
                      value={pushUserQuery}
                      onChange={(e) => searchPushTargets(e.target.value)}
                      placeholder={t('kullanıcı adı, e-posta veya isim yazın...', 'type username, email or name...')}
                      className="w-full pl-9 pr-4 py-2.5 rounded-xl glass-input text-xs text-white"
                    />
                  </div>

                  {/* Search Results */}
                  {pushTargets.length > 0 && (
                    <div className="mt-2 p-2 rounded-xl bg-black/60 border border-white/10 max-h-40 overflow-y-auto space-y-1">
                      {pushTargets.map((target) => (
                        <div
                          key={target.id}
                          onClick={() => { setSelectedUser(target); setPushTargets([]); }}
                          className="p-2 rounded-lg hover:bg-white/10 cursor-pointer flex items-center justify-between text-xs text-white transition-colors"
                        >
                          <div>
                            <span className="font-semibold">{target.name || target.username}</span>
                            <span className="text-[10px] text-gray-400 block">{target.email}</span>
                          </div>
                          <span className="text-[10px] px-2 py-0.5 rounded bg-white/10 text-emerald-400">
                            {target.fcmDeviceCount} {t('Cihaz', 'Devices')}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}

                  {selectedUser && (
                    <div className="mt-2 p-2.5 rounded-xl bg-amber-500/10 border border-amber-500/20 text-xs text-amber-300 flex items-center justify-between">
                      <span>{t('Seçilen Kullanıcı:', 'Selected User:')} <strong>@{selectedUser.username}</strong> ({selectedUser.email})</span>
                      <button onClick={() => setSelectedUser(null)} className="text-gray-400 hover:text-white">
                        <X className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  )}
                </div>
              )}

            </div>

            {/* Notification Content Form */}
            <form onSubmit={handleSendPush} className="glass-panel rounded-3xl p-6 border border-white/10 space-y-5">
              <h3 className="text-xs font-bold uppercase tracking-wider text-rose-400 flex items-center gap-2">
                <Bell className="w-3.5 h-3.5" />
                <span>{t('2. Bildirim İçeriği & Başlık', '2. Notification Title & Body')}</span>
              </h3>

              {/* Ready Templates */}
              <div>
                <label className="block text-[11px] font-semibold text-gray-400 mb-2 uppercase">
                  {t('Hızlı Şablonlar', 'Quick Templates')}
                </label>
                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={() => applyTemplate(
                      t('🥗 Öğününüzü Kaydettiniz mi?', '🥗 Have You Logged Your Meal?'),
                      t('Gününüzün besin değerlerini ve makrolarını HealthTrack ile saniyeler içinde analiz edin!', 'Analyze your daily nutrition and macros in seconds with HealthTrack!')
                    )}
                    className="px-2.5 py-1 rounded-lg bg-white/5 hover:bg-white/10 border border-white/5 text-[11px] text-emerald-300 transition-all"
                  >
                    {t('🍏 HealthTrack Öğün Hatırlatması', '🍏 HealthTrack Meal Reminder')}
                  </button>
                  <button
                    type="button"
                    onClick={() => applyTemplate(
                      t('⚡ Günün Planı Hazır!', '⚡ Today\'s Plan Is Ready!'),
                      t('Bugünkü görevlerinizi kontrol edin ve verimlilik zincirinizi kırmayın.', 'Check today\'s tasks and keep your productivity streak alive.')
                    )}
                    className="px-2.5 py-1 rounded-lg bg-white/5 hover:bg-white/10 border border-white/5 text-[11px] text-indigo-300 transition-all"
                  >
                    {t('⚡ Planora Görev Hatırlatması', '⚡ Planora Task Reminder')}
                  </button>
                  <button
                    type="button"
                    onClick={() => applyTemplate(
                      t('👑 Tebrikler: Premium Erişim Tanımlandı!', '👑 Congrats: Premium Access Granted!'),
                      t('Hesabınıza sınırsız AI ve özel analiz özellikleri tanımlanmıştır. Keyfini çıkarın!', 'Unlimited AI and advanced analytics features have been enabled on your account. Enjoy!')
                    )}
                    className="px-2.5 py-1 rounded-lg bg-white/5 hover:bg-white/10 border border-white/5 text-[11px] text-amber-300 transition-all"
                  >
                    {t('👑 Premium Bildirimi', '👑 Premium Notification')}
                  </button>
                </div>
              </div>

              <div>
                <div className="flex justify-between items-center mb-1">
                  <label className="text-[11px] font-semibold text-gray-400 uppercase">{t('Başlık', 'Title')}</label>
                  <span className="text-[10px] text-gray-500">{pushTitle.length}/50</span>
                </div>
                <input
                  type="text"
                  required
                  value={pushTitle}
                  onChange={(e) => setPushTitle(e.target.value)}
                  placeholder={t('Örn: 🥗 Sağlık Hedeflerinize Bir Adım Daha', 'e.g. 🥗 One Step Closer to Your Health Goals')}
                  className="w-full px-3.5 py-2.5 rounded-xl glass-input text-xs text-white"
                />
              </div>

              <div>
                <div className="flex justify-between items-center mb-1">
                  <label className="text-[11px] font-semibold text-gray-400 uppercase">{t('Mesaj Metni', 'Message Body')}</label>
                  <span className="text-[10px] text-gray-500">{pushBody.length}/150</span>
                </div>
                <textarea
                  rows={3}
                  required
                  value={pushBody}
                  onChange={(e) => setPushBody(e.target.value)}
                  placeholder={t('Bildirim içeriğinizi buraya yazın...', 'Write your notification message here...')}
                  className="w-full px-3.5 py-2.5 rounded-xl glass-input text-xs text-white resize-none"
                />
              </div>

              {pushResult && (
                <div className={`p-3.5 rounded-xl text-xs flex items-center gap-2 ${
                  pushResult.ok ? 'bg-emerald-500/15 border border-emerald-500/30 text-emerald-300' : 'bg-rose-500/15 border border-rose-500/30 text-rose-300'
                }`}>
                  {pushResult.ok ? <CheckCircle2 className="w-4 h-4 shrink-0" /> : <AlertCircle className="w-4 h-4 shrink-0" />}
                  <span>{pushResult.message}</span>
                </div>
              )}

              <button
                type="submit"
                disabled={pushSending}
                className="w-full py-3 rounded-xl font-bold text-xs bg-gradient-to-r from-rose-500 via-pink-500 to-amber-500 text-white shadow-lg hover:shadow-glow-gold hover:opacity-95 transition-all flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {pushSending ? (
                  <>
                    <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    <span>{t('FCM Servisine İletiliyor...', 'Sending to FCM Service...')}</span>
                  </>
                ) : (
                  <>
                    <Send className="w-4 h-4" />
                    <span>{pushMode === 'BROADCAST' ? t('Tüm Cihazlara Anında Gönder (Broadcast)', 'Send Immediately to All Devices (Broadcast)') : t('Hedef Kullanıcıya Gönder', 'Send to Target User')}</span>
                  </>
                )}
              </button>
            </form>

          </div>

          {/* Right Phone Mockup: 5 cols */}
          <div className="lg:col-span-5 flex flex-col items-center justify-start">
            <div className="w-full max-w-xs text-center mb-3">
              <span className="text-[11px] font-bold uppercase text-gray-400 tracking-wider">
                {t('Canlı Mobil Bildirim Önizlemesi', 'Live Mobile Notification Preview')}
              </span>
            </div>

            {/* Smartphone Case */}
            <div className="w-full max-w-[300px] h-[550px] bg-[#121826] border-4 border-[#242D42] rounded-[42px] p-3 shadow-2xl relative flex flex-col justify-between overflow-hidden">
              {/* Dynamic Island / Notch */}
              <div className="w-24 h-4 bg-black rounded-full mx-auto mb-2" />

              {/* Lock Screen Clock */}
              <div className="text-center mt-6">
                <div className="text-4xl font-extralight text-white font-sans">
                  {new Date().toLocaleTimeString(dateLocale, { hour: '2-digit', minute: '2-digit' })}
                </div>
                <div className="text-[11px] text-gray-400 mt-1">
                  {new Date().toLocaleDateString(dateLocale, { weekday: 'long', day: 'numeric', month: 'long' })}
                </div>
              </div>

              {/* Notification Banner */}
              <div className="my-auto">
                <div className="glass-panel p-3.5 rounded-2xl border border-white/20 shadow-2xl bg-[#1E2738]/90 backdrop-blur-xl animate-fade-in">
                  <div className="flex items-center justify-between text-[10px] text-gray-400 mb-1.5">
                    <div className="flex items-center gap-1.5 font-bold text-white">
                      <div className="w-4 h-4 rounded bg-gradient-to-tr from-emerald-500 to-indigo-500 flex items-center justify-center text-[9px] text-white font-black">
                        H
                      </div>
                      <span>HealthTrack & Planora</span>
                    </div>
                    <span>{t('şimdi', 'now')}</span>
                  </div>

                  <div className="font-bold text-xs text-white leading-snug">
                    {pushTitle || t('Örnek Bildirim Başlığı', 'Sample Notification Title')}
                  </div>

                  <p className="text-[11px] text-gray-300 mt-0.5 leading-snug line-clamp-3">
                    {pushBody || t('Bildirim içeriği burada canlı olarak görüntülenecektir...', 'Notification content will preview live here...')}
                  </p>
                </div>
              </div>

              {/* Bottom Home Indicator */}
              <div className="w-28 h-1 bg-white/20 rounded-full mx-auto mb-1" />
            </div>
          </div>

        </div>
      )}

      {/* TAB 2: USER & SUBSCRIPTION MANAGEMENT */}
      {activeTab === 'users' && (
        <div className="space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div className="relative w-full sm:w-80">
              <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-500" />
              <input
                type="text"
                value={usersQuery}
                onChange={(e) => { setUsersQuery(e.target.value); loadUsers(e.target.value); }}
                placeholder={t('İsim, kullanıcı adı veya e-posta ara...', 'Search name, username or email...')}
                className="w-full pl-10 pr-4 py-2.5 rounded-xl glass-input text-xs text-white"
              />
            </div>

            <button
              onClick={() => loadUsers(usersQuery)}
              disabled={loadingUsers}
              className="px-3.5 py-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-semibold text-gray-300 hover:text-white transition-all flex items-center gap-2 self-start sm:self-auto"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loadingUsers ? 'animate-spin' : ''}`} />
              <span>{t('Kullanıcıları Yenile', 'Refresh Users')}</span>
            </button>
          </div>

          {/* Users Table */}
          <div className="glass-panel rounded-3xl border border-white/10 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="border-b border-white/10 bg-white/[0.02] text-gray-400 uppercase font-semibold">
                    <th className="py-3.5 px-4">{t('Kullanıcı', 'User')}</th>
                    <th className="py-3.5 px-4">{t('Rol', 'Role')}</th>
                    <th className="py-3.5 px-4">{t('Premium Durumu', 'Premium Status')}</th>
                    <th className="py-3.5 px-4">{t('Görev & Cihaz', 'Tasks & Devices')}</th>
                    <th className="py-3.5 px-4 text-right">{t('Eylemler / Yetki', 'Actions / Role')}</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {usersList.map((u) => {
                    const isPrem = u.isPremium;
                    return (
                      <tr key={u.id} className="hover:bg-white/[0.02] transition-colors">
                        {/* User Identity */}
                        <td className="py-3.5 px-4">
                          <div className="flex items-center gap-2.5">
                            <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-indigo-500 to-purple-600 flex items-center justify-center font-bold text-white text-xs">
                              {(u.name || u.username || 'U')[0].toUpperCase()}
                            </div>
                            <div>
                              <div className="font-bold text-white text-sm">{u.name || u.username}</div>
                              <div className="text-[11px] text-gray-400">@{u.username} • {u.email}</div>
                            </div>
                          </div>
                        </td>

                        {/* Role */}
                        <td className="py-3.5 px-4">
                          <span className={`text-[10px] font-bold px-2 py-0.5 rounded uppercase ${
                            u.role === 'ADMIN' 
                              ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30' 
                              : u.role === 'NOTIFIER' 
                              ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                              : 'bg-white/10 text-gray-400'
                          }`}>
                            {u.role || 'USER'}
                          </span>
                        </td>

                        {/* Premium Status */}
                        <td className="py-3.5 px-4">
                          {isPrem ? (
                            <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-amber-500/15 border border-amber-500/30 text-amber-300 font-semibold text-[11px]">
                              <Crown className="w-3 h-3 text-amber-400" />
                              <span>{u.premiumUntil ? `${new Date(u.premiumUntil).toLocaleDateString(dateLocale)}` : t('Süresiz / Ömür Boyu', 'Lifetime')}</span>
                            </div>
                          ) : (
                            <span className="text-gray-500 text-[11px]">{t('Ücretsiz Standart', 'Free Standard')}</span>
                          )}
                        </td>

                        {/* Stats */}
                        <td className="py-3.5 px-4 text-gray-300">
                          <div className="flex items-center gap-3">
                            <span title={t('Görev Sayısı', 'Task Count')}>📋 {u.taskCount || 0}</span>
                            <span title={t('Kayıtlı FCM Cihaz', 'Registered FCM Devices')}>📱 {u.fcmDeviceCount || 0}</span>
                          </div>
                        </td>

                        {/* Actions */}
                        <td className="py-3.5 px-4 text-right">
                          <div className="flex items-center justify-end gap-1.5">
                            {/* Inspect Tasks */}
                            <button
                              onClick={() => handleInspectTasks(u)}
                              title={t('Görevlerini Görüntüle', 'View Tasks')}
                              className="px-2 py-1 rounded-lg bg-white/5 hover:bg-white/10 border border-white/5 text-[11px] text-gray-300 hover:text-white transition-all"
                            >
                              {t('Görevler', 'Tasks')}
                            </button>

                            {/* Premium Quick Buttons */}
                            {!isPrem ? (
                              <button
                                onClick={() => handleSetPremium(u, 30, true)}
                                title={t('30 Günlük Premium Tanımla', 'Grant 30-Day Premium')}
                                className="px-2 py-1 rounded-lg bg-amber-500/10 hover:bg-amber-500/20 border border-amber-500/20 text-[11px] text-amber-300 transition-all font-semibold"
                              >
                                {t('+30G Premium', '+30D Premium')}
                              </button>
                            ) : (
                              <button
                                onClick={() => handleSetPremium(u, 0, false)}
                                title={t('Premium İptal Et', 'Revoke Premium')}
                                className="px-2 py-1 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/20 text-[11px] text-rose-300 transition-all"
                              >
                                {t('İptal Et', 'Revoke')}
                              </button>
                            )}

                            {/* Role Switcher */}
                            <button
                              onClick={() => handleUpdateRole(u, u.role === 'ADMIN' ? 'USER' : 'ADMIN')}
                              title={u.role === 'ADMIN' ? t('Admin Yetkisini Al', 'Revoke Admin Role') : t('Admin Yetkisi Ver', 'Grant Admin Role')}
                              className={`p-1.5 rounded-lg border transition-all ${
                                u.role === 'ADMIN'
                                  ? 'bg-rose-500/10 border-rose-500/30 text-rose-400 hover:bg-rose-500/20'
                                  : 'bg-white/5 border-white/10 text-gray-400 hover:text-white'
                              }`}
                            >
                              <ShieldCheck className="w-3.5 h-3.5" />
                            </button>

                            {/* Delete User */}
                            <button
                              onClick={() => handleDeleteUser(u)}
                              title={t('Kullanıcıyı Kalıcı Sil', 'Permanently Delete User')}
                              className="p-1.5 rounded-lg bg-white/5 hover:bg-rose-500/20 border border-white/10 hover:border-rose-500/30 text-gray-400 hover:text-rose-400 transition-all"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* User Tasks Inspection Modal */}
          {selectedUserTasks && (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm">
              <div className="glass-panel w-full max-w-2xl rounded-3xl p-6 border border-white/20 shadow-2xl max-h-[85vh] flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between border-b border-white/10 pb-4 mb-4">
                    <div>
                      <h3 className="text-base font-bold text-white">
                        {t(`@${selectedUserTasks.username} Kullanıcısının Görevleri`, `Tasks for @${selectedUserTasks.username}`)}
                      </h3>
                      <p className="text-xs text-gray-400">{t(`Toplam ${selectedUserTasks.tasks?.length || 0} görev bulundu`, `${selectedUserTasks.tasks?.length || 0} tasks found`)}</p>
                    </div>
                    <button onClick={() => setSelectedUserTasks(null)} className="p-1 text-gray-400 hover:text-white">
                      <X className="w-5 h-5" />
                    </button>
                  </div>

                  <div className="overflow-y-auto max-h-[50vh] space-y-2 pr-1">
                    {selectedUserTasks.tasks?.length === 0 ? (
                      <p className="text-xs text-gray-500 text-center py-6">{t('Kullanıcının kayıtlı görevi yok.', 'No tasks recorded for this user.')}</p>
                    ) : (
                      selectedUserTasks.tasks?.map((task) => (
                        <div key={task.id} className="p-3 rounded-xl bg-white/[0.02] border border-white/5 flex items-center justify-between text-xs">
                          <div>
                            <span className="font-bold text-white block">{task.title}</span>
                            {task.description && <span className="text-[11px] text-gray-400 line-clamp-1">{task.description}</span>}
                          </div>
                          <span className={`px-2 py-0.5 rounded font-bold uppercase text-[10px] ${
                            task.status === 'COMPLETED' ? 'bg-emerald-500/15 text-emerald-300' : 'bg-amber-500/15 text-amber-300'
                          }`}>
                            {task.status}
                          </span>
                        </div>
                      ))
                    )}
                  </div>
                </div>

                <div className="pt-4 border-t border-white/10 text-right">
                  <button
                    onClick={() => setSelectedUserTasks(null)}
                    className="px-4 py-2 rounded-xl bg-white/10 hover:bg-white/20 text-xs font-semibold text-white transition-colors"
                  >
                    {t('Kapat', 'Close')}
                  </button>
                </div>
              </div>
            </div>
          )}

        </div>
      )}

      {/* TAB 3: SYSTEM & NEON DB STATS */}
      {activeTab === 'system' && (
        <div className="space-y-6">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
            
            <div className="glass-panel rounded-3xl p-6 border border-emerald-500/20">
              <span className="text-xs font-bold uppercase tracking-wider text-emerald-400 block mb-1">
                {t('Toplam Kullanıcılar', 'Total Users')}
              </span>
              <div className="text-3xl font-black text-white">{stats?.totalUsers ?? '--'}</div>
              <p className="text-[11px] text-gray-400 mt-2">{t('Neon DB users tablosu kayıtları', 'Neon DB users table records')}</p>
            </div>

            <div className="glass-panel rounded-3xl p-6 border border-amber-500/20">
              <span className="text-xs font-bold uppercase tracking-wider text-amber-400 block mb-1">
                {t('Premium Kullanıcı Sayısı', 'Premium Users')}
              </span>
              <div className="text-3xl font-black text-white">{stats?.totalPremiumUsers ?? '--'}</div>
              <p className="text-[11px] text-gray-400 mt-2">{t('Aktif lisanslı/ücretli üyeler', 'Active licensed/paid members')}</p>
            </div>

            <div className="glass-panel rounded-3xl p-6 border border-indigo-500/20">
              <span className="text-xs font-bold uppercase tracking-wider text-indigo-400 block mb-1">
                {t('Aktif FCM Cihazları', 'Active FCM Devices')}
              </span>
              <div className="text-3xl font-black text-white">{stats?.totalFcmDevices ?? '--'}</div>
              <p className="text-[11px] text-gray-400 mt-2">{t('Push bildirim alabilen cihazlar', 'Devices registered for push notifications')}</p>
            </div>

            <div className="glass-panel rounded-3xl p-6 border border-white/10">
              <span className="text-xs font-bold uppercase tracking-wider text-gray-400 block mb-1">
                {t('Toplam Görevler (Tasks)', 'Total Tasks')}
              </span>
              <div className="text-3xl font-black text-white">{stats?.totalTasks ?? '--'}</div>
              <p className="text-[11px] text-gray-400 mt-2">{t('Planora görev tablosu', 'Planora tasks table')}</p>
            </div>

            <div className="glass-panel rounded-3xl p-6 border border-white/10">
              <span className="text-xs font-bold uppercase tracking-wider text-gray-400 block mb-1">
                {t('Plan Odaları (Rooms)', 'Plan Rooms')}
              </span>
              <div className="text-3xl font-black text-white">{stats?.totalRooms ?? '--'}</div>
              <p className="text-[11px] text-gray-400 mt-2">{t('Grup ve çalışma odaları', 'Group and study rooms')}</p>
            </div>

            <div className="glass-panel rounded-3xl p-6 border border-white/10">
              <span className="text-xs font-bold uppercase tracking-wider text-gray-400 block mb-1">
                {t('Topluluk Gönderileri (Posts)', 'Community Posts')}
              </span>
              <div className="text-3xl font-black text-white">{stats?.totalPosts ?? '--'}</div>
              <p className="text-[11px] text-gray-400 mt-2">{t('Sosyal akış paylaşımları', 'Social feed posts')}</p>
            </div>

          </div>

          {/* Database & Render Health Box */}
          <div className="glass-panel rounded-3xl p-6 border border-white/10 space-y-4">
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              <Database className="w-4 h-4 text-emerald-400" />
              <span>{t('Neon PostgreSQL & Render Altyapı Durumu', 'Neon PostgreSQL & Render Infrastructure Status')}</span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
              <div className="p-4 rounded-2xl bg-white/[0.02] border border-white/5 space-y-1">
                <span className="text-gray-400">{t('Veritabanı Sağlayıcısı:', 'Database Provider:')}</span>
                <p className="font-bold text-white">Neon Serverless PostgreSQL (AWS Frankfurt / eu-central-1)</p>
                <p className="text-[11px] text-emerald-400">{t('SSL Mode: Require | HikariCP Havuzu Aktif', 'SSL Mode: Require | HikariCP Pool Active')}</p>
              </div>

              <div className="p-4 rounded-2xl bg-white/[0.02] border border-white/5 space-y-1">
                <span className="text-gray-400">{t('Sunucu Hosting Servisi:', 'Server Hosting Service:')}</span>
                <p className="font-bold text-white">Render.com Web Service (Ktor JVM Engine)</p>
                <p className="text-[11px] text-indigo-400">{t('Uptime Robot 5 dk aralıklı canlandırma aktif', 'Uptime Robot 5-min keep-alive active')}</p>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 4: FORCE UPDATE & VERSION MANAGEMENT */}
      {activeTab === 'versions' && (
        <div className="space-y-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg font-bold text-white flex items-center gap-2">
                <Smartphone className="w-5 h-5 text-indigo-400" />
                <span>{t('Force Update & Uzaktan Versiyon Yönetimi', 'Force Update & Remote Version Management')}</span>
              </h2>
              <p className="text-xs text-gray-400 mt-1">
                {t('Minimum desteklenen sürümü yükselterek eski sürümleri zorunlu güncellemeye yönlendirebilirsiniz.', 'Increase the minimum supported version to prompt older app versions with a forced update.')}
              </p>
            </div>
            <button
              onClick={loadAppConfigs}
              disabled={loadingConfigs}
              className="px-3 py-2 rounded-xl bg-white/5 hover:bg-white/10 text-xs font-semibold text-gray-300 flex items-center gap-1.5 border border-white/10"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loadingConfigs ? 'animate-spin' : ''}`} />
              <span>{t('Yenile', 'Refresh')}</span>
            </button>
          </div>

          {configFeedback && (
            <div
              className={`p-3.5 rounded-2xl border text-xs flex items-center gap-2 ${
                configFeedback.type === 'success'
                  ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
                  : 'bg-rose-500/10 border-rose-500/30 text-rose-300'
              }`}
            >
              {configFeedback.type === 'success' ? (
                <CheckCircle2 className="w-4 h-4 shrink-0" />
              ) : (
                <AlertCircle className="w-4 h-4 shrink-0" />
              )}
              <span>{configFeedback.message}</span>
            </div>
          )}

          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {appConfigs.map((cfg) => {
              const isHealth = cfg.appId === 'healthtrack';
              return (
                <div
                  key={cfg.appId}
                  className="glass-panel rounded-3xl p-6 border border-white/10 space-y-4"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <span
                        className={`px-2.5 py-1 rounded-lg text-xs font-bold ${
                          isHealth
                            ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                            : 'bg-indigo-500/20 text-indigo-300 border border-indigo-500/30'
                        }`}
                      >
                        {isHealth ? 'HealthTrack' : 'Planora'}
                      </span>
                      <span className="text-xs text-gray-500 font-mono">
                        /api/app-config?app={cfg.appId}
                      </span>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="block text-[11px] font-semibold text-gray-400 mb-1">
                        {t('Minimum Zorunlu Sürüm', 'Minimum Required Version')}
                      </label>
                      <input
                        type="text"
                        value={cfg.minSupportedVersion || ''}
                        onChange={(e) =>
                          handleAppConfigFieldChange(cfg.appId, 'minSupportedVersion', e.target.value)
                        }
                        placeholder="1.0.0"
                        className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-xs text-white font-mono"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] font-semibold text-gray-400 mb-1">
                        {t('En Güncel Sürüm (Opsiyonel Uyarı)', 'Latest Version (Optional Prompt)')}
                      </label>
                      <input
                        type="text"
                        value={cfg.latestVersion || ''}
                        onChange={(e) =>
                          handleAppConfigFieldChange(cfg.appId, 'latestVersion', e.target.value)
                        }
                        placeholder="1.1.0"
                        className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-xs text-white font-mono"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                      <label className="block text-[11px] font-semibold text-gray-400 mb-1">
                        Android Store URL
                      </label>
                      <input
                        type="text"
                        value={cfg.storeUrlAndroid || ''}
                        onChange={(e) =>
                          handleAppConfigFieldChange(cfg.appId, 'storeUrlAndroid', e.target.value)
                        }
                        placeholder="https://play.google.com/store/apps/details?id=..."
                        className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-xs text-white"
                      />
                    </div>
                    <div>
                      <label className="block text-[11px] font-semibold text-gray-400 mb-1">
                        iOS App Store URL
                      </label>
                      <input
                        type="text"
                        value={cfg.storeUrlIos || ''}
                        onChange={(e) =>
                          handleAppConfigFieldChange(cfg.appId, 'storeUrlIos', e.target.value)
                        }
                        placeholder="https://apps.apple.com/app/..."
                        className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-xs text-white"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-[11px] font-semibold text-gray-400 mb-1">
                      {t('Zorunlu Güncelleme Başlığı (TR)', 'Force Update Title (TR)')}
                    </label>
                    <input
                      type="text"
                      value={cfg.forceUpdateTitleTr || ''}
                      onChange={(e) =>
                        handleAppConfigFieldChange(cfg.appId, 'forceUpdateTitleTr', e.target.value)
                      }
                      className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-xs text-white"
                    />
                  </div>

                  <div>
                    <label className="block text-[11px] font-semibold text-gray-400 mb-1">
                      {t('Zorunlu Güncelleme Mesajı (TR)', 'Force Update Message (TR)')}
                    </label>
                    <textarea
                      rows={2}
                      value={cfg.forceUpdateMessageTr || ''}
                      onChange={(e) =>
                        handleAppConfigFieldChange(cfg.appId, 'forceUpdateMessageTr', e.target.value)
                      }
                      className="w-full px-3 py-2 rounded-xl bg-white/5 border border-white/10 text-xs text-white"
                    />
                  </div>

                  <button
                    onClick={() => handleSaveAppConfig(cfg)}
                    disabled={savingConfigAppId === cfg.appId}
                    className="w-full py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white text-xs font-bold transition-colors"
                  >
                    {savingConfigAppId === cfg.appId ? t('Kaydediliyor...', 'Saving...') : t('Ayarları Kaydet', 'Save Settings')}
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      )}

    </div>
  );
}

