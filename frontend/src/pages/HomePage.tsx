import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getPosts } from '../api/posts';

export default function HomePage() {
  const [posts, setPosts] = useState<any[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [orderMode, setOrderMode] = useState(0);
  const [loading, setLoading] = useState(true);

  const loadPosts = async () => {
    setLoading(true);
    try {
      const data = await getPosts(page, 10, orderMode);
      setPosts(data.content);
      setTotalPages(data.totalPages);
    } catch {
      // Handle error silently
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPosts();
  }, [page, orderMode]);

  return (
    <div>
      <div className="feed-toolbar">
        <button
          className={`btn btn-sm ${orderMode === 0 ? 'btn-secondary active' : 'btn-secondary'}`}
          onClick={() => { setOrderMode(0); setPage(0); }}
        >
          Latest
        </button>
        <button
          className={`btn btn-sm ${orderMode === 1 ? 'btn-secondary active' : 'btn-secondary'}`}
          onClick={() => { setOrderMode(1); setPage(0); }}
        >
          Hot
        </button>
      </div>

      {loading ? (
        <div className="loading">Loading posts...</div>
      ) : (
        <div>
          {posts.map((item) => (
            <div key={item.post.id} className="post-card">
              <img
                src={item.user.headerUrl}
                alt={`${item.user.username}'s avatar`}
                className="post-avatar"
              />
              <div className="post-card-body">
                <div className="post-meta">
                  <span className="post-author">{item.user.username}</span>
                  <span className="post-dot" />
                  <span>{new Date(item.post.createTime).toLocaleDateString()}</span>
                </div>
                <div className="post-card-title">
                  <Link to={`/app/post/${item.post.id}`}>{item.post.title}</Link>
                </div>
                <div className="post-stats">
                  <span className="stat-pill">{item.likeCount} likes</span>
                  <span className="stat-pill">{item.post.commentCount} comments</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="pagination">
          <button
            className="btn btn-sm btn-secondary"
            disabled={page === 0}
            onClick={() => setPage(page - 1)}
          >
            Previous
          </button>
          <span className="pagination-info">
            Page {page + 1} / {totalPages}
          </span>
          <button
            className="btn btn-sm btn-secondary"
            disabled={page >= totalPages - 1}
            onClick={() => setPage(page + 1)}
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}
