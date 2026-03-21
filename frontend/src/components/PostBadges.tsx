type PostFlagValue = number | string | null | undefined;

interface PostBadgesProps {
  type?: PostFlagValue;
  status?: PostFlagValue;
}

function isPinned(type: PostFlagValue) {
  return Number(type) === 1;
}

function isFeatured(status: PostFlagValue) {
  return Number(status) === 1;
}

export default function PostBadges({ type, status }: PostBadgesProps) {
  if (!isPinned(type) && !isFeatured(status)) {
    return null;
  }

  return (
    <>
      {isPinned(type) && <span className="badge badge-pinned">Pinned</span>}
      {isFeatured(status) && <span className="badge badge-featured">Featured</span>}
    </>
  );
}
