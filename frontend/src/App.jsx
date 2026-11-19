import React, { useState } from 'react';
import ChatWindow from './components/ChatWindow';

export default function App() {
  const [currentConversation, setCurrentConversation] = useState({
    id: 'demo-chat',
    title: 'New Chat',
    tone: 'Professional'
  });
  const [messages, setMessages] = useState([]);
  const [selectedTone, setSelectedTone] = useState('Professional');
  const [isStreaming, setIsStreaming] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(true);

  const handleSendMessage = (text) => {
    const userMsg = { role: 'user', content: text, timestamp: new Date().toISOString() };
    setMessages(prev => [...prev, userMsg]);
  };

  const handleStopStream = () => {
    setIsStreaming(false);
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
        onStopStream={handleStopStream}
        onToggleSidebar={() => setSidebarOpen(prev => !prev)}
        sidebarOpen={sidebarOpen}
      />
    </div>
  );
}
