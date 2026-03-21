import { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { searchPosts } from '../api/search';
import PostBadges from '../components/PostBadges';

export default function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const keyword = searchParams.get('keyword') || '';
  const [query, setQuery] = useState(keyword);
  const [results, setResults] = useState<any[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!keyword) return;
    setLoading(true);
    searchPosts(keyword, page, 10)
      .then((data) => { setResults(data.content); setTotalPages(data.totalPages); })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [keyword, page]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (query.trim()) {
      setPage(0);
      setSearchParams({ keyword: query.trim() });
    }
  };

  return (
    <div>
      <h2 style={{ marginBottom: 20 }}>Search</h2>

      <form className="search-bar" onSubmit={handleSearch}>
        <input
          className="form-input"
          placeholder="Search posts..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button className="btn btn-primary" type="submit">Search</button>
      </form>

      {loading ? <div className="loading">Searching...</div> : keyword && (
        <>
          <p style={{ marginBottom: 16, color: 'var(--text-secondary)', fontFamily: 'var(--sans)', fontSize: 13 }}>
            {results.length > 0 ? `Results for "${keyword}"` : `No results for "${keyword}"`}
          </p>

          {results.map((item: any) => (
            <div key={item.post.id} className="post-card">
              <img src={item.user.headerUrl} alt={item.user.username} className="post-avatar" />
              <div className="post-card-body">
                <div className="post-meta">
                  <span className="post-author">{item.user.username}</span>
                  <span className="post-dot" />
                  <span>{new Date(item.post.createTime).toLocaleDateString()}</span>
                </div>
                <div className="post-card-title">
                  <div className="post-title-row">
                    <Link
                      to={`/community/app/post/${item.post.id}`}
                      dangerouslySetInnerHTML={{ __html: item.post.title }}
                    />
                    <PostBadges type={item.post.type} status={item.post.status} />
                  </div>
                </div>
                <div className="post-stats">
                  <span className="stat-pill">{item.likeCount} likes</span>
                </div>
              </div>
            </div>
          ))}

          {totalPages > 1 && (
            <div className="pagination">
              <button className="btn btn-sm btn-ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
              <span className="pagination-info">Page {page + 1} / {totalPages}</span>
              <button className="btn btn-sm btn-ghost" disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>Next</button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
