import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getNoticeDetail } from '../api/messages';

export default function NoticeDetailPage() {
  const { topic } = useParams<{ topic: string }>();
  const [notices, setNotices] = useState<any[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!topic) return;
    setLoading(true);
    getNoticeDetail(topic, page, 10)
      .then((data) => { setNotices(data.content); setTotalPages(data.totalPages); })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [topic, page]);

  const topicLabel = topic === 'comment' ? 'Comments' : topic === 'like' ? 'Likes' : 'Follows';

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
                {topic === 'comment' ? ' commented on ' : topic === 'like' ? ' liked ' : ' followed you'}
                {item.postId && <Link to={`/community/app/post/${item.postId}`}>view post</Link>}
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
