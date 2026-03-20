import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getPostDetail } from '../api/posts';

export default function PostDetailPage() {
  const { id } = useParams<{ id: string }>();
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

  if (loading) return <div className="loading">Loading...</div>;
  if (!data) return <div className="loading">Post not found.</div>;

  const { post, user, likeCount, comments } = data;

  return (
    <div>
      <Link to="/app" className="page-backlink">&larr; Back to posts</Link>

      <div className="detail-header">
        <h1 className="detail-title">{post.title}</h1>
        <div className="detail-meta">
          <img
            src={user.headerUrl}
            alt={`${user.username}'s avatar`}
            className="detail-author-avatar"
          />
          <div>
            <span className="detail-author-name">{user.username}</span>
            <span style={{ margin: '0 8px', color: '#94a3b8' }}>&middot;</span>
            <span>{new Date(post.createTime).toLocaleString()}</span>
          </div>
          <div className="post-stats" style={{ marginLeft: 'auto' }}>
            <span className="stat-pill">{likeCount} likes</span>
          </div>
        </div>
      </div>

      <div className="detail-content">{post.content}</div>

      <div className="detail-section-title">Comments ({data.commentCount})</div>

      {comments && comments.map((c: any) => (
        <div key={c.comment.id} className="comment-card">
          <div className="comment-header">
            <img
              src={c.user.headerUrl}
              alt={`${c.user.username}'s avatar`}
              className="comment-avatar"
            />
            <span className="comment-author">{c.user.username}</span>
            <span className="comment-time">
              {new Date(c.comment.createTime).toLocaleString()}
            </span>
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
