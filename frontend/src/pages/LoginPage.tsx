import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { getCaptcha, login } from '../api/auth';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [captchaCode, setCaptchaCode] = useState('');
  const [captchaOwner, setCaptchaOwner] = useState('');
  const [captchaImage, setCaptchaImage] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [error, setError] = useState('');
  const { login: authLogin } = useAuth();
  const navigate = useNavigate();

  const loadCaptcha = async () => {
    try {
      const data = await getCaptcha();
      setCaptchaOwner(data.captchaOwner);
      setCaptchaImage(data.captchaImage);
      setCaptchaCode('');
    } catch {
      // Captcha service unavailable
    }
  };

  useEffect(() => {
    loadCaptcha();
  }, []);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const res = await login({ username, password, captchaCode, captchaOwner, rememberMe });
      if (res.code === 0) {
        authLogin(res.data.accessToken, res.data.refreshToken, res.data.user);
        navigate('/community/app');
      } else {
        setError(res.message);
        loadCaptcha();
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Login failed.');
      loadCaptcha();
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-aside">
        <span className="section-kicker" style={{ color: 'rgba(246,239,231,0.7)' }}>Welcome Back</span>
        <h1 style={{ marginTop: '12px' }}>Join the conversation.</h1>
        <p style={{ marginTop: '16px' }}>
          Connect with developers, share your knowledge, and explore discussions
          on the topics that matter to you.
        </p>
        <div className="auth-points">
          <span className="auth-point">Share posts and ideas</span>
          <span className="auth-point">Comment and discuss</span>
          <span className="auth-point">Follow your favorite authors</span>
          <span className="auth-point">Get real-time notifications</span>
        </div>
      </div>

      <div className="auth-panel">
        <h2>Sign In</h2>
        <p>Enter your credentials to access your account.</p>

        {error && <div className="form-error" style={{ marginTop: '20px' }}>{error}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label" htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              className="form-input"
              placeholder="Enter your username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              className="form-input"
              placeholder="Enter your password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Verification Code</label>
            <div className="auth-captcha-row">
              <input
                type="text"
                className="form-input"
                placeholder="Enter code"
                value={captchaCode}
                onChange={(e) => setCaptchaCode(e.target.value)}
              />
              <div>
                {captchaImage && (
                  <img
                    src={captchaImage}
                    alt="Verification code"
                    className="captcha-img"
                    onClick={loadCaptcha}
                    title="Click to refresh"
                  />
                )}
              </div>
            </div>
          </div>

          <div className="auth-footer">
            <label className="form-check">
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
              />
              Remember me
            </label>
          </div>

          <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '20px' }}>
            Sign In
          </button>
        </form>

        <div className="auth-alt">
          Don't have an account? <Link to="/community/app/register">Create one</Link>
        </div>
      </div>
    </div>
  );
}
