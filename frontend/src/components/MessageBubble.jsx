import React from 'react';
import { User, Bot, Clock } from 'lucide-react';

export default function MessageBubble({ message, isStreaming = false }) {
  const isUser = message.role === 'user';
  const timeFormatted = message.timestamp
    ? new Date(message.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    : '';

  return (
    <div className={`message-row ${isUser ? 'user-row' : 'assistant-row'}`}>
      <div className="message-avatar">
        {isUser ? (
          <div className="avatar user-avatar" title="You">
            <User size={16} />
          </div>
        ) : (
          <div className="avatar ai-avatar" title="Gemini Assistant">
            <Bot size={16} />
          </div>
        )}
      </div>

      <div className={`message-bubble ${isUser ? 'user-bubble' : 'ai-bubble'}`}>
        <div className="message-header">
          <span className="sender-name">{isUser ? 'You' : 'Gemini AI'}</span>
          {timeFormatted && (
            <span className="message-time">
              <Clock size={11} style={{ marginRight: '3px', verticalAlign: 'middle' }} />
              {timeFormatted}
            </span>
          )}
        </div>

        <div className="message-content">
          <div className="text-content" style={{ whiteSpace: 'pre-wrap' }}>
            {message.content}
            {isStreaming && <span className="streaming-cursor" />}
          </div>
        </div>
      </div>
    </div>
  );
}
