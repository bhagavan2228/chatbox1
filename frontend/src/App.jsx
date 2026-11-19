import React, { useState } from 'react';
import ChatWindow from './components/ChatWindow';
import { useChatStream } from './hooks/useChatStream';

export default function App() {
  const [currentConversation, setCurrentConversation] = useState({
    id: 'demo-conversation-id',
    title: 'New Chat',
    tone: 'Professional'
  });
  const [messages, setMessages] = useState([]);
  const [selectedTone, setSelectedTone] = useState('Professional');
  const [sidebarOpen, setSidebarOpen] = useState(true);

  const { isStreaming, startStream, stopStream } = useChatStream();

  const handleSendMessage = (text) => {
    if (!text.trim() || isStreaming) return;

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
      conversationId: currentConversation.id,
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
      }
    });
  };

  return (
    <div className="app-container">
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
