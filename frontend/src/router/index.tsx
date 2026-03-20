import { createBrowserRouter } from 'react-router-dom';
import Layout from '../components/layout/Layout';
import AuthLayout from '../components/layout/AuthLayout';
import ProtectedRoute from './ProtectedRoute';
import HomePage from '../pages/HomePage';
import LoginPage from '../pages/LoginPage';
import RegisterPage from '../pages/RegisterPage';
import PostDetailPage from '../pages/PostDetailPage';
import UserProfilePage from '../pages/UserProfilePage';
import MessagesPage from '../pages/MessagesPage';
import MessageDetailPage from '../pages/MessageDetailPage';
import NoticeDetailPage from '../pages/NoticeDetailPage';
import FollowListPage from '../pages/FollowListPage';
import SearchPage from '../pages/SearchPage';
import SettingsPage from '../pages/SettingsPage';

const router = createBrowserRouter([
  {
    path: '/app',
    element: <Layout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'post/:id', element: <PostDetailPage /> },
      { path: 'profile/:id', element: <UserProfilePage /> },
      { path: 'followees/:userId', element: <FollowListPage /> },
      { path: 'followers/:userId', element: <FollowListPage /> },
      { path: 'search', element: <SearchPage /> },
      {
        path: 'messages',
        element: <ProtectedRoute><MessagesPage /></ProtectedRoute>,
      },
      {
        path: 'messages/:conversationId',
        element: <ProtectedRoute><MessageDetailPage /></ProtectedRoute>,
      },
      {
        path: 'notices/:topic',
        element: <ProtectedRoute><NoticeDetailPage /></ProtectedRoute>,
      },
      {
        path: 'settings',
        element: <ProtectedRoute><SettingsPage /></ProtectedRoute>,
      },
    ],
  },
  {
    path: '/app',
    element: <AuthLayout />,
    children: [
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
    ],
  },
]);

export default router;
