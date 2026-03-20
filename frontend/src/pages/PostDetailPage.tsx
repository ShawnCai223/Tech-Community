import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getPostDetail } from '../api/posts';
import { toggleLike } from '../api/likes';
import { useAuth } from '../contexts/AuthContext';

export default function PostDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user: currentUser } = useAuth();
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    getPostDetail(Number(id))
      .then(setData)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [id]);

  const handleLike = async () => {
    if (!data || !currentUser) return;
    try {
      const res = await toggleLike(1, data.post.id, data.post.userId, data.post.id);
      setData({ ...data, likeCount: res.likeCount, likeStatus: res.likeStatus });
    } catch {}
  };

  if (loading) return <div className="loading">Loading...</div>;
  if (!data) return <div className="empty-state"><div className="empty-state-text">Post not found.</div></div>;

  const { post, user, likeCount, likeStatus, comments } = data;

  return (
    <div>
      <Link to="/community/app" className="page-backlink">&larr; Back to posts</Link>

      <div className="detail-header">
        <h1 className="detail-title">{post.title}</h1>
        <div className="detail-meta">
          <Link to={`/community/app/profile/${user.id}`}>
            <img src={user.headerUrl} alt={`${user.username}'s avatar`} className="detail-author-avatar" />
          </Link>
          <div>
            <Link to={`/community/app/profile/${user.id}`} className="detail-author-name">{user.username}</Link>
            <span style={{ margin: '0 8px', color: '#94a3b8' }}>&middot;</span>
            <span>{new Date(post.createTime).toLocaleString()}</span>
          </div>
          <div className="post-stats" style={{ marginLeft: 'auto' }}>
            {currentUser ? (
              <button className={`btn btn-sm ${likeStatus === 1 ? 'btn-accent' : 'btn-ghost'}`} onClick={handleLike}>
                {likeStatus === 1 ? '❤️' : '🤍'} {likeCount}
              </button>
            ) : (
              <span className="stat-pill">{likeCount} likes</span>
            )}
          </div>
        </div>
      </div>

      <div className="detail-content">{post.content}</div>

      <div className="detail-section-title">Comments ({data.commentCount})</div>

      {comments && comments.length === 0 && (
        <div className="empty-state"><div className="empty-state-text">No comments yet.</div></div>
      )}

      {comments && comments.map((c: any) => (
        <div key={c.comment.id} className="comment-card">
          <div className="comment-header">
            <img src={c.user.headerUrl} alt={`${c.user.username}'s avatar`} className="comment-avatar" />
            <span className="comment-author">{c.user.username}</span>
            <span className="comment-time">{new Date(c.comment.createTime).toLocaleString()}</span>
            <span className="comment-likes">{c.likeCount} likes</span>
          </div>
          <div className="comment-body">{c.comment.content}</div>

          {c.replies && c.replies.length > 0 && (
            <div className="reply-thread">
              {c.replies.map((r: any) => (
                <div key={r.reply.id} className="reply-item">
                  <strong>{r.user.username}</strong>
                  {r.target && <span> replied to <strong>{r.target.username}</strong></span>}
                  : {r.reply.content}
                </div>
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
