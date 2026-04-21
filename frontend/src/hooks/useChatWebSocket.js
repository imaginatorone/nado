import { useEffect, useRef, useCallback, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { getKeycloak } from '../auth/keycloak';

/**
 * хук для websocket-based realtime чата.
 * если ws недоступен - возвращает connected=false,
 * и ChatPage переключается на polling fallback.
 */
export function useChatWebSocket(roomId, onMessage) {
  const clientRef = useRef(null);
  const [connected, setConnected] = useState(false);

  const getToken = useCallback(() => {
    const kc = getKeycloak();
    if (kc?.authenticated) return kc.token;
    return localStorage.getItem('token');
  }, []);

  useEffect(() => {
    if (!roomId) return;

    const token = getToken();
    if (!token) return;

    const client = new Client({
      webSocketFactory: () => new SockJS('/api/ws'),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/chat/${roomId}`, (frame) => {
          try {
            const msg = JSON.parse(frame.body);
            onMessage?.(msg);
          } catch {}
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: () => setConnected(false),
      onWebSocketClose: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
      setConnected(false);
    };
  }, [roomId, getToken]);

  const sendMessage = useCallback((content) => {
    if (!clientRef.current?.connected || !roomId) return false;
    clientRef.current.publish({
      destination: `/app/chat.send.${roomId}`,
      body: JSON.stringify({ content }),
    });
    return true;
  }, [roomId]);

  return { connected, sendMessage };
}
