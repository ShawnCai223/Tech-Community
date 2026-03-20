import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getPostDetail } from '../api/posts';
import { toggleLike } from '../api/likes';
import { addComment } from '../api/comments';
import { useAuth } from '../contexts/AuthContext';

export default function PostDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user: currentUser, isAuthenticated } = useAuth();
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [commentText, setCommentText] = useState('');
  const [replyingTo, setReplyingTo] = useState<{ commentId: number; username: string; targetId: number } | null>(null);
  const [replyText, setReplyText] = useState('');

  const load = () => {
    if (!id) return;
    setLoading(true);
    getPostDetail(Number(id))
      .then(setData)
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(load, [id]);

  const handleLikePost = async () => {
    if (!data || !currentUser) return;
    try {
      const res = await toggleLike(1, data.post.id, data.post.userId, data.post.id);
      setData({ ...data, likeCount: res.likeCount, likeStatus: res.likeStatus });
    } catch {}
  };

  const handleLikeComment = async (commentId: number, entityUserId: number) => {
    if (!currentUser || !data) return;
    try {
      const res = await toggleLike(2, commentId, entityUserId, data.post.id);
      // Update the comment's like data in state
      const updatedComments = data.comments.map((c: any) => {
        if (c.comment.id === commentId) {
          return { ...c, likeCount: res.likeCount, likeStatus: res.likeStatus };
        }
        // Also check replies
        if (c.replies) {
          const updatedReplies = c.replies.map((r: any) => {
            if (r.reply.id === commentId) {
              return { ...r, likeCount: res.likeCount, likeStatus: res.likeStatus };
            }
            return r;
          });
          return { ...c, replies: updatedReplies };
        }
        return c;
      });
      setData({ ...data, comments: updatedComments });
    } catch {}
  };

  const handleAddComment = async () => {
    if (!commentText.trim() || !data) return;
    try {
      // entityType=1 (post), entityId=postId
      await addComment(data.post.id, 1, data.post.id, commentText);
      setCommentText('');
      load(); // Reload to see the new comment
    } catch {}
  };

  const handleReply = async (parentCommentId: number) => {
    if (!replyText.trim() || !data || !replyingTo) return;
    try {
      // entityType=2 (comment), entityId=parentCommentId, targetId=the user being replied to
      await addComment(data.post.id, 2, parentCommentId, replyText, replyingTo.targetId);
      setReplyText('');
      setReplyingTo(null);
      load();
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
            {isAuthenticated ? (
              <button className={`btn btn-sm ${likeStatus === 1 ? 'btn-accent' : 'btn-outline'}`} onClick={handleLikePost}>
                {likeStatus === 1 ? '❤️' : '🤍'} {likeCount} likes
              </button>
            ) : (
              <span className="stat-pill">{likeCount} likes</span>
            )}
          </div>
        </div>
      </div>

      <div className="detail-content">{post.content}</div>

      {/* Comment Form */}
      {isAuthenticated && (
        <div style={{ marginBottom: 24 }}>
          <div className="form-group">
            <label className="form-label">Add a Comment</label>
            <textarea
              className="form-textarea"
              placeholder="Share your thoughts..."
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              rows={3}
            />
          </div>
          <button className="btn btn-primary btn-sm" onClick={handleAddComment} disabled={!commentText.trim()}>
            Post Comment
          </button>
        </div>
      )}

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
            {isAuthenticated ? (
              <button
                className={`btn btn-sm ${c.likeStatus === 1 ? 'btn-accent' : 'btn-ghost'}`}
                style={{ padding: '4px 10px', fontSize: 12 }}
                onClick={() => handleLikeComment(c.comment.id, c.comment.userId)}
              >
                {c.likeStatus === 1 ? '❤️' : '🤍'} {c.likeCount}
              </button>
            ) : (
              <span className="comment-likes">{c.likeCount} likes</span>
            )}
            {isAuthenticated && (
              <button
                className="btn btn-ghost btn-sm"
                style={{ padding: '4px 10px', fontSize: 12 }}
                onClick={() => setReplyingTo(replyingTo?.commentId === c.comment.id ? null : {
                  commentId: c.comment.id,
                  username: c.user.username,
                  targetId: c.user.id,
                })}
              >
                Reply
              </button>
            )}
          </div>
          <div className="comment-body">{c.comment.content}</div>

          {/* Reply form for this comment */}
          {replyingTo?.commentId === c.comment.id && (
            <div style={{ marginLeft: 38, marginTop: 10 }}>
              <div className="form-group" style={{ marginBottom: 8 }}>
                <textarea
                  className="form-textarea"
                  style={{ minHeight: 60, fontSize: 13 }}
                  placeholder={`Reply to ${replyingTo?.username ?? ''}...`}
                  value={replyText}
                  onChange={(e) => setReplyText(e.target.value)}
                />
              </div>
              <div style={{ display: 'flex', gap: 6 }}>
                <button className="btn btn-primary btn-sm" onClick={() => handleReply(c.comment.id)} disabled={!replyText.trim()}>
                  Reply
                </button>
                <button className="btn btn-ghost btn-sm" onClick={() => { setReplyingTo(null); setReplyText(''); }}>
                  Cancel
                </button>
              </div>
            </div>
          )}

          {/* Existing replies */}
          {c.replies && c.replies.length > 0 && (
            <div className="reply-thread">
              {c.replies.map((r: any) => (
                <div key={r.reply.id} className="reply-item" style={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' }}>
                  <div>
                    <strong>{r.user.username}</strong>
                    {r.target && <span> replied to <strong>{r.target.username}</strong></span>}
                    : {r.reply.content}
                  </div>
                  <div style={{ display: 'flex', gap: 4, flexShrink: 0, marginLeft: 8 }}>
                    {isAuthenticated && (
                      <>
                        <button
                          className={`btn btn-sm ${r.likeStatus === 1 ? 'btn-accent' : 'btn-ghost'}`}
                          style={{ padding: '2px 8px', fontSize: 11 }}
                          onClick={() => handleLikeComment(r.reply.id, r.reply.userId)}
                        >
                          {r.likeStatus === 1 ? '❤️' : '🤍'} {r.likeCount}
                        </button>
                        <button
                          className="btn btn-ghost btn-sm"
                          style={{ padding: '2px 8px', fontSize: 11 }}
                          onClick={() => setReplyingTo(replyingTo && replyingTo.commentId === c.comment.id && replyingTo.targetId === r.user.id ? null : {
                            commentId: c.comment.id,
                            username: r.user.username,
                            targetId: r.user.id,
                          })}
                        >
                          Reply
                        </button>
                      </>
                    )}
                    {!isAuthenticated && r.likeCount > 0 && (
                      <span style={{ fontSize: 11, color: 'var(--accent)' }}>{r.likeCount} likes</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
