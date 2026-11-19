import React, { useState, useEffect, useCallback } from 'react';
import Sidebar from './components/Sidebar';
import ChatWindow from './components/ChatWindow';
import { useChatStream } from './hooks/useChatStream';
import {
  fetchConversations,
  createConversation,
  getConversation,
  deleteConversation
} from './services/api';

export default function App() {
  const [conversations, setConversations] = useState([]);
  const [currentConversation, setCurrentConversation] = useState(null);
  const [messages, setMessages] = useState([]);
  const [selectedTone, setSelectedTone] = useState('Professional');
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoadingList, setIsLoadingList] = useState(false);

  const { isStreaming, startStream, stopStream } = useChatStream();

  // Load conversations from backend
  const loadConversations = useCallback(async (search = '', selectFirst = false) => {
    setIsLoadingList(true);
    try {
      const data = await fetchConversations(search);
      setConversations(data);

      if (selectFirst && data.length > 0) {
        await loadConversationDetails(data[0].id);
      } else if (selectFirst && data.length === 0) {
        // Create initial default conversation
        const newConv = await createConversation('New Chat', 'Professional');
        setConversations([newConv]);
        setCurrentConversation(newConv);
        setMessages([]);
        setSelectedTone(newConv.tone || 'Professional');
      }
    } catch (err) {
      console.error('Failed to load conversations:', err);
    } finally {
      setIsLoadingList(false);
    }
  }, []);

  // Initial load
  useEffect(() => {
    loadConversations('', true);
  }, [loadConversations]);

  // Load full messages for a specific conversation
  const loadConversationDetails = async (id) => {
    try {
      const conv = await getConversation(id);
      setCurrentConversation(conv);
      setMessages(conv.messages || []);
      if (conv.tone) {
        setSelectedTone(conv.tone);
      }
    } catch (err) {
      console.error(`Failed to load conversation ${id}:`, err);
    }
  };

  const handleSelectConversation = (id) => {
    if (currentConversation?.id === id) return;
    loadConversationDetails(id);
  };

  const handleNewChat = async () => {
    try {
      const newConv = await createConversation('New Chat', selectedTone);
      setConversations((prev) => [newConv, ...prev]);
      setCurrentConversation(newConv);
      setMessages([]);
    } catch (err) {
      console.error('Failed to create new conversation:', err);
    }
  };

  const handleDeleteConversation = async (id) => {
    try {
      await deleteConversation(id);
      const remaining = conversations.filter((c) => c.id !== id);
      setConversations(remaining);

      if (currentConversation?.id === id) {
        if (remaining.length > 0) {
          loadConversationDetails(remaining[0].id);
        } else {
          handleNewChat();
        }
      }
    } catch (err) {
      console.error('Failed to delete conversation:', err);
    }
  };

  const handleSendMessage = async (text) => {
    if (!text.trim() || isStreaming) return;

    let convId = currentConversation?.id;
    if (!convId) {
      const created = await createConversation('New Chat', selectedTone);
      setConversations((prev) => [created, ...prev]);
      setCurrentConversation(created);
      convId = created.id;
    }

    const userMessage = {
      role: 'user',
      content: text,
      timestamp: new Date().toISOString()
    };

    const aiMessage = {
      role: 'assistant',
      content: '',
      timestamp: new Date().toISOString()
    };

    setMessages((prev) => [...prev, userMessage, aiMessage]);

    startStream({
      conversationId: convId,
      content: text,
      tone: selectedTone,
      onChunk: (token) => {
        setMessages((prev) => {
          if (prev.length === 0) return prev;
          const updated = [...prev];
          const lastIndex = updated.length - 1;
          const lastMsg = updated[lastIndex];
          if (lastMsg.role === 'assistant') {
            updated[lastIndex] = {
              ...lastMsg,
              content: lastMsg.content + token
            };
          }
          return updated;
        });
      },
      onComplete: () => {
        // Refresh conversations to get updated title & timestamp
        loadConversations(searchQuery, false);
      },
      onError: (errorMsg) => {
        setMessages((prev) => {
          if (prev.length === 0) return prev;
          const updated = [...prev];
          const lastIndex = updated.length - 1;
          const lastMsg = updated[lastIndex];
          if (lastMsg.role === 'assistant') {
            updated[lastIndex] = {
              ...lastMsg,
              content: lastMsg.content
                ? `${lastMsg.content}\n\n⚠️ [Error: ${errorMsg}]`
                : `⚠️ [Error: ${errorMsg}]`
            };
          }
          return updated;
        });
        loadConversations(searchQuery, false);
      }
    });
  };

  return (
    <div className="app-container">
      <Sidebar
        conversations={conversations}
        activeConversationId={currentConversation?.id}
        onSelectConversation={handleSelectConversation}
        onNewChat={handleNewChat}
        onDeleteConversation={handleDeleteConversation}
        isOpen={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        isLoading={isLoadingList}
      />

      <ChatWindow
        conversation={currentConversation}
        messages={messages}
        selectedTone={selectedTone}
        onSelectTone={setSelectedTone}
        onSendMessage={handleSendMessage}
        isStreaming={isStreaming}
        onStopStream={stopStream}
        onToggleSidebar={() => setSidebarOpen((prev) => !prev)}
        sidebarOpen={sidebarOpen}
      />
    </div>
  );
}
