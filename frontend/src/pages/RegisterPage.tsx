import { useState, type FormEvent } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { register } from '../api/auth';

export default function RegisterPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const res = await register({ username, password, email });
      if (res.code === 0) {
        setSuccess('Registration successful! Please check your email to activate your account.');
        setTimeout(() => navigate('/community/app/login'), 3000);
      } else {
        setError(res.message);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Registration failed.');
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-aside">
        <span className="section-kicker" style={{ color: 'rgba(246,239,231,0.7)' }}>Get Started</span>
        <h1 style={{ marginTop: '12px' }}>Create your account.</h1>
        <p style={{ marginTop: '16px' }}>
          Join a growing community of developers sharing ideas, discussing technology,
          and learning together.
        </p>
        <div className="auth-points">
          <span className="auth-point">Free to join</span>
          <span className="auth-point">Publish your thoughts</span>
          <span className="auth-point">Build your network</span>
          <span className="auth-point">Discover great content</span>
        </div>
      </div>

      <div className="auth-panel">
        <h2>Create Account</h2>
        <p>Fill in your details to get started.</p>

        {error && <div className="form-error" style={{ marginTop: '20px' }}>{error}</div>}
        {success && <div className="form-success" style={{ marginTop: '20px' }}>{success}</div>}

        <form className="auth-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label" htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              className="form-input"
              placeholder="Enter your email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              className="form-input"
              placeholder="Choose a username"
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
              placeholder="Create a password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <button type="submit" className="btn btn-primary" style={{ width: '100%', marginTop: '8px' }}>
            Create Account
          </button>
        </form>

        <div className="auth-alt">
          Already have an account? <Link to="/community/app/login">Sign in</Link>
        </div>
      </div>
    </div>
  );
}
