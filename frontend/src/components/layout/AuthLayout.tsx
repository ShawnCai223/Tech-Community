import { Outlet, Link } from 'react-router-dom';

export default function AuthLayout() {
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
            <nav className="nav-links">
              <Link to="/community/app/login" className="nav-link">Login</Link>
              <Link to="/community/app/register" className="btn btn-primary btn-sm" style={{ textDecoration: 'none' }}>Register</Link>
            </nav>
          </div>
        </div>
      </header>
      <main className="auth-layout-main">
        <div className="app-container">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
