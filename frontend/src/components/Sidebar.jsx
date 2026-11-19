import React from 'react';
import { Plus, Trash2, MessageSquare, Search, Sparkles, X } from 'lucide-react';

export default function Sidebar({
  conversations = [],
  activeConversationId,
  onSelectConversation,
  onNewChat,
  onDeleteConversation,
  isOpen,
  onClose,
  searchQuery,
  onSearchChange,
  isLoading = false
}) {
  const formatTime = (isoString) => {
    if (!isoString) return '';
    try {
      const date = new Date(isoString);
      const now = new Date();
      const diffMs = now - date;
      const diffMins = Math.floor(diffMs / (1000 * 60));
      const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
      const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

      if (diffMins < 1) return 'Just now';
      if (diffMins < 60) return `${diffMins}m ago`;
      if (diffHours < 24) return `${diffHours}h ago`;
      if (diffDays < 7) return `${diffDays}d ago`;
      return date.toLocaleDateString([], { month: 'short', day: 'numeric' });
    } catch {
      return '';
    }
  };

  return (
    <aside className={`sidebar ${isOpen ? 'open' : 'closed'}`}>
      {/* Top Header */}
      <div className="sidebar-header">
        <div className="app-brand">
          <div className="brand-icon">
            <Sparkles size={18} />
          </div>
          <span className="brand-name">AI Assistant</span>
        </div>
        <button className="mobile-close-btn" onClick={onClose} title="Close sidebar">
          <X size={18} />
        </button>
      </div>

      {/* New Chat Button */}
      <div className="sidebar-actions">
        <button className="new-chat-btn" onClick={onNewChat}>
          <Plus size={18} />
          <span>New Chat</span>
        </button>
      </div>

      {/* Search Input */}
      <div className="sidebar-search-box">
        <Search size={15} className="search-icon" />
        <input
          type="text"
          placeholder="Search threads..."
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          className="search-input"
        />
        {searchQuery && (
          <button
            className="clear-search-btn"
            onClick={() => onSearchChange('')}
            title="Clear search"
          >
            <X size={13} />
          </button>
        )}
      </div>

      {/* Conversations List */}
      <div className="threads-list">
        {isLoading ? (
          <div className="sidebar-loading">Loading chats...</div>
        ) : conversations.length === 0 ? (
          <div className="sidebar-empty">
            {searchQuery ? 'No matching chats found' : 'No past conversations'}
          </div>
        ) : (
          conversations.map((conv) => {
            const isActive = conv.id === activeConversationId;
            return (
              <div
                key={conv.id}
                className={`thread-item ${isActive ? 'active' : ''}`}
                onClick={() => onSelectConversation(conv.id)}
              >
                <div className="thread-icon">
                  <MessageSquare size={16} />
                </div>
                <div className="thread-info">
                  <div className="thread-title" title={conv.title}>
                    {conv.title || 'Untitled Chat'}
                  </div>
                  <div className="thread-meta">
                    <span>{formatTime(conv.updatedAt || conv.createdAt)}</span>
                    {conv.tone && <span className="thread-tone-badge">{conv.tone}</span>}
                  </div>
                </div>
                <button
                  className="delete-thread-btn"
                  title="Delete chat"
                  onClick={(e) => {
                    e.stopPropagation();
                    if (window.confirm(`Delete "${conv.title}"?`)) {
                      onDeleteConversation(conv.id);
                    }
                  }}
                >
                  <Trash2 size={14} />
                </button>
              </div>
            );
          })
        )}
      </div>

      {/* Sidebar Footer */}
      <div className="sidebar-footer">
        <div className="model-badge">
          <span className="status-dot"></span>
          <span>Gemini 3.5 Flash Lite</span>
        </div>
      </div>
    </aside>
  );
}
