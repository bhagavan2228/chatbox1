import React, { useState, useRef, useEffect } from 'react';
import { Send, Square } from 'lucide-react';

export default function ChatInput({ onSendMessage, isStreaming, onStopStream }) {
  const [input, setInput] = useState('');
  const textareaRef = useRef(null);

  // Auto-resize textarea height as user types
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 180)}px`;
    }
  }, [input]);

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleSend = () => {
    if (!input.trim() || isStreaming) return;
    onSendMessage(input.trim());
    setInput('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }
  };

  return (
    <div className="chat-input-container">
      <div className="chat-input-box">
        <textarea
          ref={textareaRef}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={isStreaming ? 'AI is replying...' : 'Ask Gemini anything... (Enter to send, Shift+Enter for newline)'}
          disabled={isStreaming}
          rows={1}
          className="chat-textarea"
        />

        <div className="chat-actions">
          {isStreaming ? (
            <button
              type="button"
              className="stop-btn"
              onClick={onStopStream}
              title="Stop generating response"
            >
              <Square size={14} fill="currentColor" />
              <span>Stop generating</span>
            </button>
          ) : (
            <button
              type="button"
              className="send-btn"
              onClick={handleSend}
              disabled={!input.trim()}
              title="Send message (Enter)"
            >
              <Send size={16} />
            </button>
          )}
        </div>
      </div>
      <div className="chat-footer-hint">
        Gemini 3.5 Flash Lite • Context maintained throughout conversation
      </div>
    </div>
  );
}
