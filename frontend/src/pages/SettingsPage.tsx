import { useState, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import client from '../api/client';

export default function SettingsPage() {
  const { user } = useAuth();
  const [uploading, setUploading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const fileRef = useRef<HTMLInputElement>(null);

  const handleUpload = async () => {
    const file = fileRef.current?.files?.[0];
    if (!file) return;

    setUploading(true);
    setError('');
    setMessage('');

    const formData = new FormData();
    formData.append('headerImage', file);

    try {
      const res = await client.post('/users/me/avatar', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      if (res.data.code === 0) {
        setMessage('Avatar updated successfully! Refresh to see changes.');
      } else {
        setError(res.data.message);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Upload failed.');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>Account Settings</h2>

      <div className="settings-grid">
        <div className="section-panel">
          <h3 style={{ marginBottom: 16, fontSize: 18 }}>Profile Avatar</h3>

          {error && <div className="form-error">{error}</div>}
          {message && <div className="form-success">{message}</div>}

          <div style={{ display: 'flex', alignItems: 'center', gap: 20, marginBottom: 20 }}>
            <img
              src={user?.headerUrl}
              alt="Current avatar"
              style={{ width: 72, height: 72, borderRadius: '50%', objectFit: 'cover', border: '3px solid rgba(255,255,255,0.9)', boxShadow: '0 4px 12px rgba(61,44,29,0.1)' }}
            />
            <div>
              <div style={{ fontFamily: 'var(--sans)', fontWeight: 700, marginBottom: 4 }}>{user?.username}</div>
              <div style={{ fontFamily: 'var(--sans)', fontSize: 13, color: 'var(--text-secondary)' }}>
                JPG or PNG, max 5MB
              </div>
            </div>
          </div>

          <div className="form-group">
            <input ref={fileRef} type="file" accept=".jpg,.jpeg,.png" className="form-input" />
          </div>

          <button className="btn btn-primary btn-sm" onClick={handleUpload} disabled={uploading}>
            {uploading ? 'Uploading...' : 'Upload Avatar'}
          </button>
        </div>
      </div>
    </div>
  );
}
