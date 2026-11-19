import { useState, useRef, useCallback } from 'react';
import { getStreamUrl } from '../services/api';

/**
 * Custom React hook to stream Gemini chat responses using fetch + ReadableStream.
 * Handles SSE parsing (events: token, done, error) and supports aborting via AbortController.
 */
export function useChatStream() {
  const [isStreaming, setIsStreaming] = useState(false);
  const [streamError, setStreamError] = useState(null);
  const abortControllerRef = useRef(null);

  const startStream = useCallback(async ({
    conversationId,
    content,
    tone,
    onChunk,
    onComplete,
    onError
  }) => {
    if (!conversationId || !content) return;

    // Reset previous abort controller if any
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }

    const controller = new AbortController();
    abortControllerRef.current = controller;

    setIsStreaming(true);
    setStreamError(null);

    try {
      const response = await fetch(getStreamUrl(conversationId), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream'
        },
        body: JSON.stringify({ content, tone }),
        signal: controller.signal
      });

      if (!response.ok) {
        let errMessage = `Server error (${response.status})`;
        try {
          const errJson = await response.json();
          if (errJson && errJson.message) {
            errMessage = errJson.message;
          }
        } catch (_) {}
        throw new Error(errMessage);
      }

      if (!response.body) {
        throw new Error('ReadableStream not supported by the response.');
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let buffer = '';

      while (true) {
        const { value, done } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        // SSE messages are separated by double newlines
        const blocks = buffer.split('\n\n');
        // Keep the last partial block in buffer
        buffer = blocks.pop() || '';

        for (const block of blocks) {
          if (!block.trim()) continue;

          const lines = block.split('\n');
          let eventType = 'message';
          const dataParts = [];

          for (const line of lines) {
            if (line.startsWith('event:')) {
              eventType = line.slice(6).trim();
            } else if (line.startsWith('data:')) {
              // Strip "data:" prefix and optional leading single space
              const dataContent = line.slice(5).replace(/^ /, '');
              dataParts.push(dataContent);
            }
          }

          const eventData = dataParts.join('\n');

          if (eventType === 'token') {
            onChunk?.(eventData);
          } else if (eventType === 'done') {
            onComplete?.(eventData);
          } else if (eventType === 'error') {
            const errorMsg = eventData || 'Error received from streaming endpoint.';
            setStreamError(errorMsg);
            onError?.(errorMsg);
          }
        }
      }

      onComplete?.();
    } catch (err) {
      if (err.name === 'AbortError') {
        // User aborted the stream via Stop button
        console.log('Chat stream aborted by user.');
      } else {
        console.error('Chat stream failure:', err);
        const msg = err.message || 'Stream connection failed.';
        setStreamError(msg);
        onError?.(msg);
      }
    } finally {
      setIsStreaming(false);
      abortControllerRef.current = null;
    }
  }, []);

  const stopStream = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    setIsStreaming(false);
  }, []);

  return {
    isStreaming,
    streamError,
    startStream,
    stopStream
  };
}
