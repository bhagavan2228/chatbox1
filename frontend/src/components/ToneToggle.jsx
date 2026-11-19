import React from 'react';
import { Briefcase, MessageCircle, Zap } from 'lucide-react';

const TONES = [
  { id: 'Professional', label: 'Professional', icon: Briefcase, desc: 'Structured & articulate' },
  { id: 'Casual', label: 'Casual', icon: MessageCircle, desc: 'Friendly & conversational' },
  { id: 'Concise', label: 'Concise', icon: Zap, desc: 'Direct & to the point' },
];

export default function ToneToggle({ selectedTone, onSelectTone, disabled = false }) {
  return (
    <div className="tone-toggle-wrapper">
      <span className="tone-toggle-label">Vibe Check:</span>
      <div className="tone-toggle-group">
        {TONES.map(({ id, label, icon: Icon, desc }) => {
          const isActive = selectedTone?.toLowerCase() === id.toLowerCase();
          return (
            <button
              key={id}
              type="button"
              className={`tone-btn ${isActive ? 'active' : ''}`}
              onClick={() => onSelectTone(id)}
              disabled={disabled}
              title={desc}
            >
              <Icon size={14} className="tone-icon" />
              <span>{label}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
