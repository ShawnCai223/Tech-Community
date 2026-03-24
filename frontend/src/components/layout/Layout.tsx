import { Outlet, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { useState } from 'react';
import { useNotifications } from '../../contexts/NotificationContext';

export default function Layout() {
  const { user, isAuthenticated, logout } = useAuth();
  const { summary } = useNotifications();
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');

  const handleLogout = async () => {
    await logout();
    navigate('/community/app/login');
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/community/app/search?keyword=${encodeURIComponent(searchQuery.trim())}`);
      setSearchQuery('');
    }
  };

  return (
    <div>
      <header className="navbar">
        <div className="app-container">
          <div className="navbar-inner">
            <Link to="/community/app" className="navbar-brand">
              <span className="brand-mark">TC</span>
              <span className="brand-text">
                <strong>Tech Community</strong>
                <small>Discuss &amp; Share</small>
              </span>
            </Link>
            <div className="navbar-spacer" />
            <form onSubmit={handleSearch} style={{ display: 'flex', gap: 6 }}>
              <input
                className="form-input navbar-search"
                style={{ padding: '8px 14px', borderRadius: 999, fontSize: 13 }}
                placeholder="Search..."
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
              />
            </form>
            <nav className="nav-links">
              {isAuthenticated ? (
                <>
                  <Link to="/community/app/messages" className="nav-link nav-link-with-badge">
                    <span>Messages</span>
                    {summary.totalUnreadCount > 0 && (
                      <span className="nav-unread-badge">{summary.totalUnreadCount}</span>
                    )}
                  </Link>
                  <Link to={`/community/app/profile/${user?.id}`} className="nav-user">
                    <img src={user?.headerUrl} alt={`${user?.username}'s avatar`} className="nav-user-avatar" />
                    <span className="nav-username">{user?.username}</span>
                  </Link>
                  <button className="btn btn-ghost btn-sm" onClick={handleLogout}>Logout</button>
                </>
              ) : (
                <>
                  <Link to="/community/app/login" className="nav-link">Login</Link>
                  <Link to="/community/app/register" className="btn btn-primary btn-sm" style={{ textDecoration: 'none' }}>Register</Link>
                </>
              )}
            </nav>
          </div>
        </div>
      </header>
      <main className="app-main">
        <div className="app-container">
          <div className="main-panel">
            <Outlet />
          </div>
        </div>
      </main>
    </div>
  );
}
