import React, { useRef, useEffect } from 'react';
import ToneToggle from './ToneToggle';
import MessageBubble from './MessageBubble';
import ChatInput from './ChatInput';
import { Sparkles, MessageSquare } from 'lucide-react';

export default function ChatWindow({
  conversation,
  messages,
  selectedTone,
  onSelectTone,
  onSendMessage,
  isStreaming,
  onStopStream,
  onToggleSidebar,
  sidebarOpen
}) {
  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isStreaming]);

  const starterPrompts = [
    { title: 'Explain a concept', prompt: 'Explain how Server-Sent Events work compared to WebSockets in 3 simple paragraphs.' },
    { title: 'Write code', prompt: 'Write a Java method to validate email addresses using regex and unit test it.' },
    { title: 'System Design', prompt: 'Design a real-time collaborative chat system with WebSockets and Redis pub/sub.' },
    { title: 'AI Architecture', prompt: 'Compare fine-tuning vs RAG for building domain-specific enterprise AI assistants.' },
  ];

  return (
    <div className="chat-window">
      {/* Header */}
      <header className="chat-header">
        <div className="header-left">
          <button
            className="sidebar-toggle-btn"
            onClick={onToggleSidebar}
            title={sidebarOpen ? 'Hide sidebar' : 'Show sidebar'}
          >
            <MessageSquare size={18} />
          </button>
          <div className="header-title-box">
            <h2 className="header-title">{conversation?.title || 'New Chat'}</h2>
            <span className="header-meta">
              {messages.length} message{messages.length === 1 ? '' : 's'}
            </span>
          </div>
        </div>

        <div className="header-right">
          <ToneToggle
            selectedTone={selectedTone}
            onSelectTone={onSelectTone}
            disabled={isStreaming}
          />
        </div>
      </header>

      {/* Messages area */}
      <div className="messages-container">
        {messages.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon-box">
              <Sparkles size={36} className="sparkle-icon" />
            </div>
            <h3>How can I help you today?</h3>
            <p>Choose your tone above and ask any question to get started.</p>

            <div className="starter-grid">
              {starterPrompts.map((item, idx) => (
                <button
                  key={idx}
                  className="starter-card"
                  onClick={() => onSendMessage(item.prompt)}
                  disabled={isStreaming}
                >
                  <span className="starter-title">{item.title}</span>
                  <span className="starter-text">{item.prompt}</span>
                </button>
              ))}
            </div>
          </div>
        ) : (
          <div className="messages-list">
            {messages.map((msg, index) => {
              const isLast = index === messages.length - 1;
              const isAiStreaming = isStreaming && isLast && msg.role === 'assistant';
              return (
                <MessageBubble
                  key={index}
                  message={msg}
                  isStreaming={isAiStreaming}
                />
              );
            })}
            <div ref={messagesEndRef} />
          </div>
        )}
      </div>

      {/* Footer Chat Input */}
      <ChatInput
        onSendMessage={onSendMessage}
        isStreaming={isStreaming}
        onStopStream={onStopStream}
      />
    </div>
  );
}
