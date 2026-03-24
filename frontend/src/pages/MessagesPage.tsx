import { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { getLetters, getNoticeDetail, sendLetter, markNoticeRead } from '../api/messages';
import { useNotifications } from '../contexts/NotificationContext';

type MessageView = 'letters' | 'like' | 'comment' | 'reply' | 'follow';

const VALID_VIEWS: MessageView[] = ['letters', 'like', 'comment', 'reply', 'follow'];

export default function MessagesPage() {
  const FALLBACK_REFRESH_MS = 5000;
  const [searchParams, setSearchParams] = useSearchParams();
  const initialView = searchParams.get('view');
  const [view, setView] = useState<MessageView>(
    VALID_VIEWS.includes(initialView as MessageView) ? (initialView as MessageView) : 'letters',
  );
  const [letters, setLetters] = useState<any[]>([]);
  const [notices, setNotices] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCompose, setShowCompose] = useState(false);
  const [toName, setToName] = useState('');
  const [content, setContent] = useState('');
  const [sendError, setSendError] = useState('');
  const { summary, refreshSummary } = useNotifications();
  const navigate = useNavigate();

  const setCurrentView = (nextView: MessageView) => {
    setView(nextView);
    const nextParams = new URLSearchParams(searchParams);
    if (nextView === 'letters') {
      nextParams.delete('view');
    } else {
      nextParams.set('view', nextView);
    }
    setSearchParams(nextParams, { replace: true });
  };

  const getNoticeQuery = () => {
    if (view === 'comment') {
      return { topic: 'comment', entityType: 1 };
    }
    if (view === 'reply') {
      return { topic: 'comment', entityType: 2 };
    }
    if (view === 'like') {
      return { topic: 'like' };
    }
    return { topic: 'follow' };
  };

  const getNoticeTarget = (item: any) => {
    if (view === 'follow') {
      return `/community/app/profile/${item.user?.id}`;
    }
    if (!item.postId) {
      return '/community/app/messages';
    }
    const threadId = item.commentId ?? (item.entityType === 2 ? item.entityId : null);
    return `/community/app/post/${item.postId}${threadId ? `#thread-${threadId}` : ''}`;
  };

  const load = useCallback((silent = false) => {
    if (!silent) {
      setLoading(true);
    }
    if (view === 'letters') {
      getLetters()
        .then((data) => setLetters(data.content))
        .catch(() => {})
        .finally(() => {
          if (!silent) {
            setLoading(false);
          }
        });
    } else {
      const { topic, entityType } = getNoticeQuery();
      getNoticeDetail(topic, 0, 20, entityType)
        .then((data) => setNotices(data.content))
        .catch(() => {})
        .finally(() => {
          if (!silent) {
            setLoading(false);
          }
        });
    }
  }, [view]);

  const handleNoticeClick = async (item: any, index: number) => {
    // Mark as read locally first for instant UI feedback
    if (item.notice.status === 0) {
      const updated = [...notices];
      updated[index] = { ...item, notice: { ...item.notice, status: 1 } };
      setNotices(updated);
      try {
        await markNoticeRead(item.notice.id);
        refreshSummary();
      } catch { /* ignore */ }
    }
    navigate(getNoticeTarget(item));
  };

  useEffect(() => {
    const queryView = searchParams.get('view');
    const nextView = VALID_VIEWS.includes(queryView as MessageView)
      ? (queryView as MessageView)
      : 'letters';
    if (nextView !== view) {
      setView(nextView);
    }
  }, [searchParams, view]);

  useEffect(() => {
    load();
  }, [view]);

  useEffect(() => {
    const intervalId = window.setInterval(() => {
      load(true);
      refreshSummary();
    }, FALLBACK_REFRESH_MS);

    const handleRealtime = (event: Event) => {
      const payload = (event as CustomEvent).detail;
      if (payload?.type === 'letter.created' && view === 'letters') {
        load(true);
      } else if (payload?.type === 'notice.created' && view !== 'letters') {
        load(true);
      }
    };

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        load(true);
        refreshSummary();
      }
    };

    window.addEventListener('community:ws-event', handleRealtime as EventListener);
    document.addEventListener('visibilitychange', handleVisibilityChange);
    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener('community:ws-event', handleRealtime as EventListener);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, [view]);

  const handleSend = async () => {
    setSendError('');
    try {
      const res = await sendLetter(toName, content);
      if (res.code === 0) {
        setShowCompose(false);
        setToName('');
        setContent('');
        load(true);
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
        {view === 'letters' && (
          <button className="btn btn-primary btn-sm" onClick={() => setShowCompose(!showCompose)}>
            New Message
          </button>
        )}
      </div>

      <div className="message-tabs">
        <button className={`message-tab${view === 'letters' ? ' active' : ''}`} onClick={() => setCurrentView('letters')} type="button">
          Direct Messages
          {summary.directMessageUnreadCount > 0 && <span className="unread-dot" />}
        </button>
        <button className={`message-tab${view === 'like' ? ' active' : ''}`} onClick={() => setCurrentView('like')} type="button">
          Likes
          {summary.likeUnreadCount > 0 && <span className="unread-dot" />}
        </button>
        <button className={`message-tab${view === 'comment' ? ' active' : ''}`} onClick={() => setCurrentView('comment')} type="button">
          Comments
          {summary.commentUnreadCount > 0 && <span className="unread-dot" />}
        </button>
        <button className={`message-tab${view === 'reply' ? ' active' : ''}`} onClick={() => setCurrentView('reply')} type="button">
          Replies
          {summary.replyUnreadCount > 0 && <span className="unread-dot" />}
        </button>
        <button className={`message-tab${view === 'follow' ? ' active' : ''}`} onClick={() => setCurrentView('follow')} type="button">
          Followers
          {summary.followUnreadCount > 0 && <span className="unread-dot" />}
        </button>
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

      {loading ? <div className="loading">Loading...</div> : view === 'letters' ? (
        letters.length === 0 ? (
          <div className="empty-state"><div className="empty-state-text">No messages yet.</div></div>
        ) : (
          letters.map((item: any) => (
            <div key={item.conversation.id} className="message-item" onClick={() => navigate(`/community/app/messages/${item.conversation.conversationId}`)}>
              <img src={item.target.headerUrl} alt={item.target.username} className="message-avatar" />
              <div className="message-body">
                <div className="message-title">
                  {item.target.username}
                  {item.unreadCount > 0 && <span className="unread-dot" />}
                </div>
                <div className="message-preview">{item.conversation.content}</div>
              </div>
              <div className="message-meta">
                <div>{new Date(item.conversation.createTime).toLocaleDateString()}</div>
              </div>
            </div>
          ))
        )
      ) : (
        <div>
          {notices.length === 0 ? (
            <div className="empty-state"><div className="empty-state-text">No notifications.</div></div>
          ) : (
            notices.map((item: any, index: number) => (
              <div
                key={item.notice.id ?? index}
                className="message-item"
                onClick={() => handleNoticeClick(item, index)}
              >
                <div className="message-body">
                  <div className="message-title">
                    {item.user?.username}
                    {item.notice.status === 0 && <span className="unread-dot" />}
                  </div>
                  <div className="message-preview">
                    {view === 'like'
                      ? 'liked your content'
                      : view === 'comment'
                        ? 'commented on your post'
                        : view === 'reply'
                          ? 'replied to your comment'
                          : 'started following you'}
                  </div>
                </div>
                <div className="message-meta">
                  <div>{new Date(item.notice.createTime).toLocaleDateString()}</div>
                </div>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}
