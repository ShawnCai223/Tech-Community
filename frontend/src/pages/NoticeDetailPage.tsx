import { useState, useEffect } from 'react';
import { useParams, Link, useSearchParams } from 'react-router-dom';
import { getNoticeDetail } from '../api/messages';
import { useNotifications } from '../contexts/NotificationContext';

export default function NoticeDetailPage() {
  const { topic } = useParams<{ topic: string }>();
  const [searchParams] = useSearchParams();
  const { refreshSummary } = useNotifications();
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
      .finally(() => {
        refreshSummary();
        setLoading(false);
      });
  }, [topic, page, entityType]);

  const topicLabel = topic === 'comment'
    ? parsedEntityType === 2 ? 'Replies' : 'Comments'
    : topic === 'like'
      ? 'Likes'
      : 'Follows';

  if (loading) return <div className="loading">Loading...</div>;

  return (
    <div>
      <Link to="/community/app/messages" className="page-backlink">&larr; Back to messages</Link>
      <h2 style={{ marginBottom: 20 }}>{topicLabel} Notifications</h2>

      {notices.length === 0 ? (
        <div className="empty-state"><div className="empty-state-text">No notifications.</div></div>
      ) : (
        notices.map((item: any, i: number) => (
          <div key={i} className="notice-card">
            <div className={`notice-icon notice-icon-${topic}`}>
              {topic === 'comment' ? '💬' : topic === 'like' ? '❤️' : '👤'}
            </div>
            <div className="notice-body">
              <div className="notice-text">
                <strong>{item.user?.username}</strong>
                {topic === 'comment'
                  ? parsedEntityType === 2 ? ' replied in ' : ' commented on '
                  : topic === 'like'
                    ? ' liked '
                    : ' followed you'}
                {item.postId && (
                  <Link to={`/community/app/post/${item.postId}${item.commentId ? `#thread-${item.commentId}` : ''}`}>
                    view post
                  </Link>
                )}
                {topic === 'follow' && item.user?.id && (
                  <Link to={`/community/app/profile/${item.user.id}`}>view profile</Link>
                )}
              </div>
              <div className="notice-time">{new Date(item.notice.createTime).toLocaleString()}</div>
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
