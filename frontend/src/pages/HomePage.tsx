import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getPosts, createPost } from '../api/posts';
import { useAuth } from '../contexts/AuthContext';
import Pagination from '../components/common/Pagination';

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
                  {item.post.type === 1 && <span className="badge badge-pinned">Pinned</span>}
                  {item.post.status === 1 && <span className="badge badge-featured">Featured</span>}
                </div>
                <div className="post-stats">
                  <span className="stat-pill">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" style={{ opacity: 0.7 }}>
                      <path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/>
                    </svg>
                    {item.likeCount}
                  </span>
                  <span className="stat-pill stat-pill-blue">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" style={{ opacity: 0.7 }}>
                      <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                    </svg>
                    {item.post.commentCount}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} />
    </div>
  );
}
