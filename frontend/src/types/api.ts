export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  currentPage: number;
  totalPages: number;
  totalElements: number;
}

export interface User {
  id: number;
  username: string;
  headerUrl: string;
  type: number;
  createTime?: string;
}

export interface UserProfile extends User {
  likeCount: number;
  followeeCount: number;
  followerCount: number;
  hasFollowed: boolean;
}

export interface Post {
  id: number;
  userId: number;
  title: string;
  content: string;
  type: number;
  status: number;
  createTime: string;
  commentCount: number;
  score: number;
}

export interface Comment {
  id: number;
  userId: number;
  entityType: number;
  entityId: number;
  targetId: number;
  content: string;
  status: number;
  createTime: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  user: User;
}

export interface MessageSummary {
  directMessageUnreadCount: number;
  likeUnreadCount: number;
  commentUnreadCount: number;
  replyUnreadCount: number;
  followUnreadCount: number;
  noticeUnreadCount: number;
  totalUnreadCount: number;
}
