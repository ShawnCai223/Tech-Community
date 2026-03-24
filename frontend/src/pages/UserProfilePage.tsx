import { useState, useEffect } from 'react';
import { useParams, Link, useLocation } from 'react-router-dom';
import { getUserProfile } from '../api/users';
import { follow, unfollow } from '../api/follows';
import { useAuth } from '../contexts/AuthContext';

export default function UserProfilePage() {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const { user: currentUser } = useAuth();
  const [profile, setProfile] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    getUserProfile(Number(id))
      .then(setProfile)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [id]);

  const handleFollow = async () => {
    if (!profile) return;
    try {
      if (profile.hasFollowed) {
        await unfollow(3, profile.id);
      } else {
        await follow(3, profile.id);
      }
      setProfile({ ...profile, hasFollowed: !profile.hasFollowed,
        followerCount: profile.followerCount + (profile.hasFollowed ? -1 : 1) });
    } catch {}
  };

  if (loading) return <div className="loading">Loading...</div>;
  if (!profile) return <div className="empty-state"><div className="empty-state-text">User not found.</div></div>;

  const isMe = currentUser?.id === profile.id;
  const backTo = location.state?.backTo;
  const backLabel = location.state?.backLabel ?? 'Back to messages';

  return (
    <div>
      {backTo && (
        <Link to={backTo} className="page-backlink">&larr; {backLabel}</Link>
      )}

      <div className="profile-hero">
        <img src={profile.headerUrl} alt={`${profile.username}'s avatar`} className="profile-avatar" />
        <div className="profile-info">
          <h1 className="profile-name">{profile.username}</h1>
          <div className="profile-date">
            Joined {profile.createTime ? new Date(profile.createTime).toLocaleDateString() : 'N/A'}
          </div>
          <div className="profile-stats">
            <Link to={`/community/app/followees/${profile.id}`} className="profile-stat">
              <span className="profile-stat-value">{profile.followeeCount}</span>
              <span className="profile-stat-label">Following</span>
            </Link>
            <Link to={`/community/app/followers/${profile.id}`} className="profile-stat">
              <span className="profile-stat-value">{profile.followerCount}</span>
              <span className="profile-stat-label">Followers</span>
            </Link>
            <div className="profile-stat">
              <span className="profile-stat-value">{profile.likeCount}</span>
              <span className="profile-stat-label">Likes</span>
            </div>
          </div>
        </div>
        <div className="profile-actions">
          {isMe ? (
            <Link to="/community/app/settings" className="btn btn-outline btn-sm">Settings</Link>
          ) : currentUser ? (
            <button className={`btn btn-sm ${profile.hasFollowed ? 'btn-ghost' : 'btn-primary'}`} onClick={handleFollow}>
              {profile.hasFollowed ? 'Unfollow' : 'Follow'}
            </button>
          ) : null}
        </div>
      </div>

      <div className="section-panel">
        <div className="section-heading">
          <h3>Recent Activity</h3>
        </div>
        <div className="empty-state">
          <div className="empty-state-text">Posts will appear here.</div>
        </div>
      </div>
    </div>
  );
}
