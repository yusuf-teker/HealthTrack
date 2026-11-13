import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';

export const WEB_LANGUAGE_KEY = 'ht_web_language';

export function detectBrowserLanguage() {
  if (typeof navigator !== 'undefined') {
    const raw = (navigator.language || navigator.userLanguage || 'en').toLowerCase();
    return raw.startsWith('tr') ? 'tr' : 'en';
  }
  return 'en';
}

export function getStoredWebLanguage() {
  if (typeof window !== 'undefined' && window.localStorage) {
    try {
      const saved = window.localStorage.getItem(WEB_LANGUAGE_KEY);
      if (saved === 'tr' || saved === 'en') return saved;
    } catch (_) {}
  }
  return detectBrowserLanguage();
}

export function tWeb(tr, en, lang = getStoredWebLanguage()) {
  return lang === 'tr' ? tr : en;
}

const LanguageContext = createContext({
  lang: 'en',
  isTr: false,
  dateLocale: 'en-US',
  setLang: () => {},
  t: (tr, en) => en,
});

export function LanguageProvider({ children }) {
  const [lang, setLangState] = useState(() => getStoredWebLanguage());

  const setLang = useCallback((nextLang) => {
    const normalized = nextLang === 'tr' ? 'tr' : 'en';
    setLangState(normalized);
    if (typeof window !== 'undefined' && window.localStorage) {
      try {
        window.localStorage.setItem(WEB_LANGUAGE_KEY, normalized);
      } catch (_) {}
    }
  }, []);

  useEffect(() => {
    if (typeof document !== 'undefined') {
      document.documentElement.lang = lang;
    }
  }, [lang]);

  const t = useCallback(
    (tr, en) => (lang === 'tr' ? tr : en),
    [lang]
  );

  const dateLocale = lang === 'tr' ? 'tr-TR' : 'en-US';

  return (
    <LanguageContext.Provider value={{ lang, isTr: lang === 'tr', dateLocale, setLang, t }}>
      {children}
    </LanguageContext.Provider>
  );
}

export function useLanguage() {
  return useContext(LanguageContext);
}
