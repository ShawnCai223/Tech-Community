import { useState, useEffect } from 'react';
import { useParams, Link, useLocation } from 'react-router-dom';
import { getFollowees, getFollowers, follow, unfollow } from '../api/follows';
import { useAuth } from '../contexts/AuthContext';

export default function FollowListPage() {
  const { userId } = useParams<{ userId: string }>();
  const location = useLocation();
  const isFollowees = location.pathname.includes('followees');
  const { user: currentUser } = useAuth();
  const [users, setUsers] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!userId) return;
    setLoading(true);
    const fetcher = isFollowees ? getFollowees : getFollowers;
    fetcher(Number(userId), 0, 50)
      .then((data) => setUsers(data || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [userId, isFollowees]);

  const handleToggleFollow = async (targetId: number, hasFollowed: boolean, index: number) => {
    try {
      if (hasFollowed) {
        await unfollow(3, targetId);
      } else {
        await follow(3, targetId);
      }
      const updated = [...users];
      updated[index] = { ...updated[index], hasFollowed: !hasFollowed };
      setUsers(updated);
    } catch {}
  };

  if (loading) return <div className="loading">Loading...</div>;

  return (
    <div>
      <Link to={`/community/app/profile/${userId}`} className="page-backlink">&larr; Back to profile</Link>
      <h2 style={{ marginBottom: 20 }}>{isFollowees ? 'Following' : 'Followers'}</h2>

      {users.length === 0 ? (
        <div className="empty-state"><div className="empty-state-text">No users found.</div></div>
      ) : (
        users.map((item: any, i: number) => (
          <div key={item.user.id} className="user-card">
            <Link to={`/community/app/profile/${item.user.id}`}>
              <img src={item.user.headerUrl} alt={item.user.username} className="user-card-avatar" />
            </Link>
            <div className="user-card-info">
              <Link to={`/community/app/profile/${item.user.id}`} className="user-card-name">{item.user.username}</Link>
              <div className="user-card-date">
                {item.followTime ? new Date(item.followTime).toLocaleDateString() : ''}
              </div>
            </div>
            {currentUser && currentUser.id !== item.user.id && (
              <button
                className={`btn btn-sm ${item.hasFollowed ? 'btn-ghost' : 'btn-outline'}`}
                onClick={() => handleToggleFollow(item.user.id, item.hasFollowed, i)}
              >
                {item.hasFollowed ? 'Unfollow' : 'Follow'}
              </button>
            )}
          </div>
        ))
      )}
    </div>
  );
}
