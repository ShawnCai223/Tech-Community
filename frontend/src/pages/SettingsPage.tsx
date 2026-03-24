import { useRef, useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import client from '../api/client';
import { updateMyPassword } from '../api/users';

export default function SettingsPage() {
  const { user } = useAuth();
  const [uploading, setUploading] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);
  const [avatarMessage, setAvatarMessage] = useState('');
  const [avatarError, setAvatarError] = useState('');
  const [passwordMessage, setPasswordMessage] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [originalPassword, setOriginalPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const fileRef = useRef<HTMLInputElement>(null);

  const handleUpload = async () => {
    const file = fileRef.current?.files?.[0];
    if (!file) return;

    setUploading(true);
    setAvatarError('');
    setAvatarMessage('');

    const formData = new FormData();
    formData.append('headerImage', file);

    try {
      const res = await client.post('/users/me/avatar', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      if (res.data.code === 0) {
        setAvatarMessage('Avatar updated successfully! Refresh to see changes.');
      } else {
        setAvatarError(res.data.message);
      }
    } catch (err: any) {
      setAvatarError(err.response?.data?.message || 'Upload failed.');
    } finally {
      setUploading(false);
    }
  };

  const handlePasswordSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSavingPassword(true);
    setPasswordError('');
    setPasswordMessage('');

    try {
      const res = await updateMyPassword({
        originalPassword,
        newPassword,
        confirmPassword,
      });

      if (res.code === 0) {
        setPasswordMessage('Password updated successfully.');
        setOriginalPassword('');
        setNewPassword('');
        setConfirmPassword('');
      } else {
        setPasswordError(res.message);
      }
    } catch (err: any) {
      setPasswordError(err.response?.data?.message || 'Failed to update password.');
    } finally {
      setSavingPassword(false);
    }
  };

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>Account Settings</h2>

      <div className="settings-grid">
        <div className="section-panel">
          <h3 style={{ marginBottom: 16, fontSize: 18 }}>Profile Avatar</h3>

          {avatarError && <div className="form-error">{avatarError}</div>}
          {avatarMessage && <div className="form-success">{avatarMessage}</div>}

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

        <div className="section-panel">
          <h3 style={{ marginBottom: 16, fontSize: 18 }}>Change Password</h3>

          {passwordError && <div className="form-error">{passwordError}</div>}
          {passwordMessage && <div className="form-success">{passwordMessage}</div>}

          <form className="auth-form" onSubmit={handlePasswordSubmit}>
            <div className="form-group">
              <label className="form-label" htmlFor="originalPassword">
                Current Password
              </label>
              <input
                id="originalPassword"
                type="password"
                className="form-input"
                placeholder="Enter your current password"
                value={originalPassword}
                onChange={(event) => setOriginalPassword(event.target.value)}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="newPassword">
                New Password
              </label>
              <input
                id="newPassword"
                type="password"
                className="form-input"
                placeholder="Enter your new password"
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="confirmPassword">
                Confirm New Password
              </label>
              <input
                id="confirmPassword"
                type="password"
                className="form-input"
                placeholder="Re-enter your new password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
              />
            </div>

            <button className="btn btn-primary btn-sm" type="submit" disabled={savingPassword}>
              {savingPassword ? 'Saving...' : 'Update Password'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
