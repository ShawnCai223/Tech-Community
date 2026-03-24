import { useEffect, useRef, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getLetterDetail, sendLetter } from '../api/messages';
import { useAuth } from '../contexts/AuthContext';
import { getUserProfile } from '../api/users';
import type { User } from '../types/api';

export default function MessageDetailPage() {
  const POLL_INTERVAL_MS = 5000;
  const { conversationId } = useParams<{ conversationId: string }>();
  const { user: currentUser } = useAuth();
  const [messages, setMessages] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [reply, setReply] = useState('');
  const [otherUser, setOtherUser] = useState<User | null>(null);
  const chatListRef = useRef<HTMLDivElement>(null);

  const load = (silent = false) => {
    if (!conversationId) return;
    if (!silent) {
      setLoading(true);
    }
    getLetterDetail(conversationId, 0, 100)
      .then((data) => setMessages([...data.content].reverse()))
      .catch(() => {})
      .finally(() => {
        if (!silent) {
          setLoading(false);
        }
      });
  };

  useEffect(load, [conversationId]);

  useEffect(() => {
    if (!conversationId) return;

    const intervalId = window.setInterval(() => {
      if (document.visibilityState === 'visible') {
        load(true);
      }
    }, POLL_INTERVAL_MS);

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        load(true);
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      window.clearInterval(intervalId);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [conversationId]);

  useEffect(() => {
    const node = chatListRef.current;
    if (!node) return;
    node.scrollTop = node.scrollHeight;
  }, [messages]);

  useEffect(() => {
    const participant = messages.find((item: any) => item.fromUser.id !== currentUser?.id)?.fromUser ?? null;
    if (participant) {
      setOtherUser(participant);
      return;
    }

    if (!conversationId || !currentUser) {
      setOtherUser(null);
      return;
    }

    const ids = conversationId.split('_').map(Number).filter((id) => !Number.isNaN(id));
    const counterpartId = ids.find((id) => id !== currentUser.id);
    if (!counterpartId) {
      setOtherUser(null);
      return;
    }

    getUserProfile(counterpartId)
      .then((user) => setOtherUser(user))
      .catch(() => setOtherUser(null));
  }, [conversationId, currentUser, messages]);

  const handleSend = async () => {
    if (!reply.trim() || !otherUser) return;
    try {
      await sendLetter(otherUser.username, reply);
      setReply('');
      load();
    } catch {}
  };

  if (loading) return <div className="loading">Loading...</div>;

  return (
    <div>
      <Link to="/community/app/messages" className="page-backlink">&larr; Back to messages</Link>

      <div ref={chatListRef} className="chat-list">
        {messages.map((item: any) => {
          const isSent = item.fromUser.id === currentUser?.id;
          return (
            <div key={item.letter.id} style={{ display: 'flex', flexDirection: 'column', alignItems: isSent ? 'flex-end' : 'flex-start' }}>
              <div className={`chat-bubble ${isSent ? 'chat-bubble-sent' : 'chat-bubble-received'}`}>
                {item.letter.content}
              </div>
              <div className="chat-meta">
                {item.fromUser.username} · {new Date(item.letter.createTime).toLocaleString()}
              </div>
            </div>
          );
        })}
      </div>

      {otherUser && (
        <div className="chat-compose">
          <textarea
            className="form-textarea"
            placeholder={`Reply to ${otherUser.username}...`}
            value={reply}
            onChange={(e) => setReply(e.target.value)}
          />
          <button className="btn btn-primary" onClick={handleSend} style={{ alignSelf: 'flex-end' }}>Send</button>
        </div>
      )}
    </div>
  );
}
