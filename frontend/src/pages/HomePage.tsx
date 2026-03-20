import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getPosts, createPost } from '../api/posts';
import { useAuth } from '../contexts/AuthContext';

export default function HomePage() {
  const [posts, setPosts] = useState<any[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [orderMode, setOrderMode] = useState(0);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const { isAuthenticated } = useAuth();

  const loadPosts = async () => {
    setLoading(true);
    try {
      const data = await getPosts(page, 10, orderMode);
      setPosts(data.content);
      setTotalPages(data.totalPages);
    } catch {} finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadPosts(); }, [page, orderMode]);

  const handleCreate = async () => {
    if (!title.trim()) return;
    try {
      const res = await createPost(title, content);
      if (res.code === 0) {
        setShowCreate(false);
        setTitle('');
        setContent('');
        loadPosts();
      }
    } catch {}
  };

  return (
    <div>
      <div className="section-heading">
        <div>
          <h2>Discussions</h2>
        </div>
        {isAuthenticated && (
          <button className="btn btn-primary btn-sm" onClick={() => setShowCreate(!showCreate)}>
            New Post
          </button>
        )}
      </div>

      {showCreate && (
        <div className="section-panel" style={{ marginBottom: 20 }}>
          <div className="form-group">
            <label className="form-label">Title</label>
            <input className="form-input" placeholder="What's on your mind?" value={title} onChange={e => setTitle(e.target.value)} />
          </div>
          <div className="form-group">
            <label className="form-label">Content</label>
            <textarea className="form-textarea" placeholder="Share your thoughts..." value={content} onChange={e => setContent(e.target.value)} />
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-primary btn-sm" onClick={handleCreate}>Publish</button>
            <button className="btn btn-ghost btn-sm" onClick={() => setShowCreate(false)}>Cancel</button>
          </div>
        </div>
      )}

      <div className="tab-group">
        <button className={`tab-btn ${orderMode === 0 ? 'active' : ''}`} onClick={() => { setOrderMode(0); setPage(0); }}>
          Latest
        </button>
        <button className={`tab-btn ${orderMode === 1 ? 'active' : ''}`} onClick={() => { setOrderMode(1); setPage(0); }}>
          Hottest
        </button>
      </div>

      {loading ? (
        <div className="loading">Loading posts...</div>
      ) : posts.length === 0 ? (
        <div className="empty-state"><div className="empty-state-text">No posts yet. Be the first to share!</div></div>
      ) : (
        <div>
          {posts.map((item) => (
            <div key={item.post.id} className="post-card">
              <Link to={`/community/app/profile/${item.user.id}`}>
                <img src={item.user.headerUrl} alt={`${item.user.username}'s avatar`} className="post-avatar" />
              </Link>
              <div className="post-card-body">
                <div className="post-meta">
                  <Link to={`/community/app/profile/${item.user.id}`} className="post-author">{item.user.username}</Link>
                  <span className="post-dot" />
                  <span>{new Date(item.post.createTime).toLocaleDateString()}</span>
                </div>
                <div className="post-card-title">
                  <Link to={`/community/app/post/${item.post.id}`}>{item.post.title}</Link>
                </div>
                <div className="post-stats">
                  <span className="stat-pill">{item.likeCount} likes</span>
                  <span className="stat-pill stat-pill-blue">{item.post.commentCount} comments</span>
                </div>
              </div>
            </div>
          ))}
        </div>
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
