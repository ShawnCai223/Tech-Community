import { Outlet, Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';

export default function Layout() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/app/login');
  };

  return (
    <div>
      <header className="navbar">
        <div className="app-container">
          <div className="navbar-inner">
            <Link to="/app" className="navbar-brand">
              <span className="brand-mark">TC</span>
              <span className="brand-text">
                <strong>Tech Community</strong>
                <small>Discuss &amp; Share</small>
              </span>
            </Link>
            <div className="navbar-spacer" />
            <nav className="nav-links">
              {isAuthenticated ? (
                <>
                  <div className="nav-user">
                    <img
                      src={user?.headerUrl}
                      alt={`${user?.username}'s avatar`}
                      className="nav-user-avatar"
                    />
                    <span className="nav-username">{user?.username}</span>
                  </div>
                  <button className="btn btn-ghost btn-sm" onClick={handleLogout}>
                    Logout
                  </button>
                </>
              ) : (
                <>
                  <Link to="/app/login" className="nav-link">Login</Link>
                  <Link to="/app/register" className="nav-link">Register</Link>
                </>
              )}
            </nav>
          </div>
        </div>
      </header>
      <main style={{ padding: '20px 0', paddingBottom: '80px' }}>
        <div className="app-container">
          <div className="main-panel">
            <Outlet />
          </div>
        </div>
      </main>
    </div>
  );
}
