import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getLetterDetail, sendLetter } from '../api/messages';
import { useAuth } from '../contexts/AuthContext';

export default function MessageDetailPage() {
  const { conversationId } = useParams<{ conversationId: string }>();
  const { user: currentUser } = useAuth();
  const [messages, setMessages] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [reply, setReply] = useState('');

  const load = () => {
    if (!conversationId) return;
    setLoading(true);
    getLetterDetail(conversationId, 0, 100)
      .then((data) => setMessages(data.content))
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(load, [conversationId]);

  const otherUser = messages.length > 0
    ? messages[0].fromUser.id === currentUser?.id
      ? null
      : messages[0].fromUser
    : null;

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

      <div className="chat-list">
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
