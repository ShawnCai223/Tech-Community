import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getLetters, getNotices, sendLetter } from '../api/messages';

export default function MessagesPage() {
  const [tab, setTab] = useState<'letters' | 'notices'>('letters');
  const [letters, setLetters] = useState<any[]>([]);
  const [notices, setNotices] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [showCompose, setShowCompose] = useState(false);
  const [toName, setToName] = useState('');
  const [content, setContent] = useState('');
  const [sendError, setSendError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    setLoading(true);
    if (tab === 'letters') {
      getLetters().then((data) => setLetters(data.content)).catch(() => {}).finally(() => setLoading(false));
    } else {
      getNotices().then(setNotices).catch(() => {}).finally(() => setLoading(false));
    }
  }, [tab]);

  const handleSend = async () => {
    setSendError('');
    try {
      const res = await sendLetter(toName, content);
      if (res.code === 0) {
        setShowCompose(false);
        setToName('');
        setContent('');
        getLetters().then((data) => setLetters(data.content));
      } else {
        setSendError(res.message);
      }
    } catch (err: any) {
      setSendError(err.response?.data?.message || 'Failed to send.');
    }
  };

  return (
    <div>
      <div className="section-heading">
        <h2>Messages</h2>
        {tab === 'letters' && (
          <button className="btn btn-primary btn-sm" onClick={() => setShowCompose(!showCompose)}>
            New Message
          </button>
        )}
      </div>

      {showCompose && (
        <div className="section-panel" style={{ marginBottom: 20 }}>
          {sendError && <div className="form-error">{sendError}</div>}
          <div className="form-group">
            <label className="form-label">To</label>
            <input className="form-input" placeholder="Username" value={toName} onChange={e => setToName(e.target.value)} />
          </div>
          <div className="form-group">
            <label className="form-label">Message</label>
            <textarea className="form-textarea" placeholder="Write your message..." value={content} onChange={e => setContent(e.target.value)} />
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-primary btn-sm" onClick={handleSend}>Send</button>
            <button className="btn btn-ghost btn-sm" onClick={() => setShowCompose(false)}>Cancel</button>
          </div>
        </div>
      )}

      <div className="tab-group">
        <button className={`tab-btn ${tab === 'letters' ? 'active' : ''}`} onClick={() => setTab('letters')}>
          Direct Messages
        </button>
        <button className={`tab-btn ${tab === 'notices' ? 'active' : ''}`} onClick={() => setTab('notices')}>
          Notifications
        </button>
      </div>

      {loading ? <div className="loading">Loading...</div> : tab === 'letters' ? (
        letters.length === 0 ? (
          <div className="empty-state"><div className="empty-state-text">No messages yet.</div></div>
        ) : (
          letters.map((item: any) => (
            <div key={item.conversation.id} className="message-item" onClick={() => navigate(`/community/app/messages/${item.conversation.conversationId}`)}>
              <img src={item.target.headerUrl} alt={item.target.username} className="message-avatar" />
              <div className="message-body">
                <div className="message-title">{item.target.username}</div>
                <div className="message-preview">{item.conversation.content}</div>
              </div>
              <div className="message-meta">
                <div>{new Date(item.conversation.createTime).toLocaleDateString()}</div>
                {item.unreadCount > 0 && <span className="message-badge">{item.unreadCount}</span>}
              </div>
            </div>
          ))
        )
      ) : (
        <div>
          {notices && ['comment', 'like', 'follow'].map(topic => {
            const info = notices[topic];
            if (!info || !info.message) return null;
            return (
              <div key={topic} className="notice-card" onClick={() => navigate(`/community/app/notices/${topic}`)} style={{ cursor: 'pointer' }}>
                <div className={`notice-icon notice-icon-${topic}`}>
                  {topic === 'comment' ? '💬' : topic === 'like' ? '❤️' : '👤'}
                </div>
                <div className="notice-body">
                  <div className="notice-text">
                    <strong>{info.user?.username}</strong>
                    {topic === 'comment' ? ' commented on your post' : topic === 'like' ? ' liked your content' : ' started following you'}
                  </div>
                  <div className="notice-time">
                    {info.count} total · {info.unread} unread
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
