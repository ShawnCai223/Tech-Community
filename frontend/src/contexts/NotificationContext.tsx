import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { useAuth } from './AuthContext';
import { getMessageSummary } from '../api/messages';
import type { MessageSummary } from '../types/api';

const DEFAULT_SUMMARY: MessageSummary = {
  directMessageUnreadCount: 0,
  likeUnreadCount: 0,
  commentUnreadCount: 0,
  replyUnreadCount: 0,
  followUnreadCount: 0,
  noticeUnreadCount: 0,
  totalUnreadCount: 0,
};

interface NotificationContextType {
  summary: MessageSummary;
  connected: boolean;
  refreshSummary: () => Promise<void>;
}

const NotificationContext = createContext<NotificationContextType | null>(null);

function createSocketUrl(token: string) {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/community/ws/messages?token=${encodeURIComponent(token)}`;
}

export function NotificationProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated, accessToken } = useAuth();
  const [summary, setSummary] = useState<MessageSummary>(DEFAULT_SUMMARY);
  const [connected, setConnected] = useState(false);
  const socketRef = useRef<WebSocket | null>(null);
  const reconnectTimerRef = useRef<number | null>(null);

  const refreshSummary = async () => {
    if (!isAuthenticated) {
      setSummary(DEFAULT_SUMMARY);
      return;
    }
    try {
      const data = await getMessageSummary();
      setSummary(data);
    } catch {
      // Ignore transient summary refresh failures.
    }
  };

  useEffect(() => {
    if (!isAuthenticated || !accessToken) {
      setConnected(false);
      setSummary(DEFAULT_SUMMARY);
      if (reconnectTimerRef.current) {
        window.clearTimeout(reconnectTimerRef.current);
        reconnectTimerRef.current = null;
      }
      socketRef.current?.close();
      socketRef.current = null;
      return;
    }

    let active = true;

    const connect = () => {
      if (!active) {
        return;
      }

      const socket = new WebSocket(createSocketUrl(accessToken));
      socketRef.current = socket;

      socket.onopen = () => {
        setConnected(true);
        refreshSummary();
      };

      socket.onmessage = (event) => {
        try {
          const payload = JSON.parse(event.data);
          if (payload.summary) {
            setSummary(payload.summary);
          }
          window.dispatchEvent(new CustomEvent('community:ws-event', { detail: payload }));
        } catch {
          // Ignore malformed realtime payloads.
        }
      };

      socket.onclose = () => {
        setConnected(false);
        if (!active) {
          return;
        }
        reconnectTimerRef.current = window.setTimeout(connect, 3000);
      };

      socket.onerror = () => {
        socket.close();
      };
    };

    refreshSummary();
    connect();

    return () => {
      active = false;
      setConnected(false);
      if (reconnectTimerRef.current) {
        window.clearTimeout(reconnectTimerRef.current);
        reconnectTimerRef.current = null;
      }
      socketRef.current?.close();
      socketRef.current = null;
    };
  }, [isAuthenticated, accessToken]);

  const value = useMemo(
    () => ({
      summary,
      connected,
      refreshSummary,
    }),
    [summary, connected, refreshSummary],
  );

  return (
    <NotificationContext.Provider value={value}>
      {children}
    </NotificationContext.Provider>
  );
}

export function useNotifications() {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotifications must be used within a NotificationProvider');
  }
  return context;
}
