const BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

export async function fetchConversations(search = '') {
  const url = search ? `${BASE_URL}/api/conversations?search=${encodeURIComponent(search)}` : `${BASE_URL}/api/conversations`;
  const res = await fetch(url);
  if (!res.ok) {
    throw new Error(`Failed to fetch conversations: ${res.statusText}`);
  }
  return res.json();
}

export async function createConversation(title = 'New Chat', tone = 'Professional') {
  const res = await fetch(`${BASE_URL}/api/conversations`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ title, tone })
  });
  if (!res.ok) {
    throw new Error(`Failed to create conversation: ${res.statusText}`);
  }
  return res.json();
}

export async function getConversation(id) {
  const res = await fetch(`${BASE_URL}/api/conversations/${id}`);
  if (!res.ok) {
    throw new Error(`Failed to fetch conversation: ${res.statusText}`);
  }
  return res.json();
}

export async function deleteConversation(id) {
  const res = await fetch(`${BASE_URL}/api/conversations/${id}`, {
    method: 'DELETE'
  });
  if (!res.ok) {
    throw new Error(`Failed to delete conversation: ${res.statusText}`);
  }
  return true;
}

export function getStreamUrl(id) {
  return `${BASE_URL}/api/conversations/${id}/messages/stream`;
}
