import { useState, useEffect } from 'react';
import { useParams, Link, useSearchParams, useNavigate } from 'react-router-dom';
import { getNoticeDetail, markNoticeRead } from '../api/messages';
import { useNotifications } from '../contexts/NotificationContext';

export default function NoticeDetailPage() {
  const { topic } = useParams<{ topic: string }>();
  const [searchParams] = useSearchParams();
  const { refreshSummary } = useNotifications();
  const navigate = useNavigate();
  const [notices, setNotices] = useState<any[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const entityType = searchParams.get('entityType');
  const parsedEntityType = entityType ? Number(entityType) : undefined;

  useEffect(() => {
    if (!topic) return;
    setLoading(true);
    getNoticeDetail(topic, page, 10, parsedEntityType)
      .then((data) => { setNotices(data.content); setTotalPages(data.totalPages); })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [topic, page, entityType]);

  const topicLabel = topic === 'comment'
    ? parsedEntityType === 2 ? 'Replies' : 'Comments'
    : topic === 'like'
      ? 'Likes'
      : 'Follows';

  const getTarget = (item: any) => {
    if (topic === 'follow') {
      return `/community/app/profile/${item.user?.id}`;
    }
    if (!item.postId) return null;
    const threadId = item.commentId ?? (item.entityType === 2 ? item.entityId : null);
    return `/community/app/post/${item.postId}${threadId ? `#thread-${threadId}` : ''}`;
  };

  const handleClick = async (item: any, index: number) => {
    if (item.notice.status === 0) {
      const updated = [...notices];
      updated[index] = { ...item, notice: { ...item.notice, status: 1 } };
      setNotices(updated);
      try {
        await markNoticeRead(item.notice.id);
        refreshSummary();
      } catch { /* ignore */ }
    }
    const target = getTarget(item);
    if (target) {
      navigate(target);
    }
  };

  if (loading) return <div className="loading">Loading...</div>;

  return (
    <div>
      <Link to="/community/app/messages" className="page-backlink">&larr; Back to messages</Link>
      <h2 style={{ marginBottom: 20 }}>{topicLabel} Notifications</h2>

      {notices.length === 0 ? (
        <div className="empty-state"><div className="empty-state-text">No notifications.</div></div>
      ) : (
        notices.map((item: any, i: number) => (
          <div
            key={item.notice.id ?? i}
            className="message-item"
            onClick={() => handleClick(item, i)}
          >
            <div className="message-body">
              <div className="message-title">
                {item.notice.status === 0 && <span className="unread-star">*</span>}
                {item.user?.username}
              </div>
              <div className="message-preview">
                {topic === 'comment'
                  ? parsedEntityType === 2 ? 'replied to your comment' : 'commented on your post'
                  : topic === 'like'
                    ? 'liked your content'
                    : 'started following you'}
              </div>
            </div>
            <div className="message-meta">
              <div>{new Date(item.notice.createTime).toLocaleDateString()}</div>
            </div>
          </div>
        ))
      )}

      {totalPages > 1 && (
        <div className="pagination">
          <button className="btn btn-sm btn-ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
          <span className="pagination-info">Page {page + 1} / {totalPages}</span>
          <button className="btn btn-sm btn-ghost" disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>Next</button>
        </div>
      )}
    </div>
  );
}
