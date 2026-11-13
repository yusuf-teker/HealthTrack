import React from 'react';
import { Globe } from 'lucide-react';
import { useLanguage } from '../context/LanguageContext';

export default function LanguageToggle({ className = '' }) {
  const { lang, setLang } = useLanguage();

  return (
    <div
      className={`inline-flex items-center gap-0.5 p-0.5 rounded-xl bg-white/[0.04] border border-white/10 ${className}`}
      role="group"
      aria-label="Language"
    >
      <Globe className="w-3 h-3 text-gray-400 ml-1.5 mr-0.5 shrink-0" />
      <button
        type="button"
        onClick={() => setLang('tr')}
        className={`px-2 py-1 rounded-lg text-[10px] font-bold transition-all ${
          lang === 'tr'
            ? 'bg-emerald-500/25 text-emerald-300 border border-emerald-500/40'
            : 'text-gray-400 hover:text-gray-200 border border-transparent'
        }`}
      >
        TR
      </button>
      <button
        type="button"
        onClick={() => setLang('en')}
        className={`px-2 py-1 rounded-lg text-[10px] font-bold transition-all ${
          lang === 'en'
            ? 'bg-emerald-500/25 text-emerald-300 border border-emerald-500/40'
            : 'text-gray-400 hover:text-gray-200 border border-transparent'
        }`}
      >
        EN
      </button>
    </div>
  );
}
