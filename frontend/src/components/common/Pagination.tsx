import { useState } from 'react';

interface Props {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export default function Pagination({ page, totalPages, onPageChange }: Props) {
  const [jumpInput, setJumpInput] = useState('');

  if (totalPages <= 1) return null;

  const pages: number[] = [];
  const start = Math.max(0, page - 2);
  const end = Math.min(totalPages - 1, page + 2);
  for (let i = start; i <= end; i++) {
    pages.push(i);
  }

  const handleJump = () => {
    const target = parseInt(jumpInput, 10);
    if (!isNaN(target) && target >= 1 && target <= totalPages) {
      onPageChange(target - 1);
      setJumpInput('');
    }
  };

  return (
    <div className="pagination">
      <button
        className="page-btn"
        disabled={page === 0}
        onClick={() => onPageChange(page - 1)}
      >
        &lsaquo;
      </button>

      {start > 0 && (
        <>
          <button className="page-btn" onClick={() => onPageChange(0)}>1</button>
          {start > 1 && <span className="page-ellipsis">&hellip;</span>}
        </>
      )}

      {pages.map((p) => (
        <button
          key={p}
          className={`page-btn ${p === page ? 'active' : ''}`}
          onClick={() => onPageChange(p)}
        >
          {p + 1}
        </button>
      ))}

      {end < totalPages - 1 && (
        <>
          {end < totalPages - 2 && <span className="page-ellipsis">&hellip;</span>}
          <button className="page-btn" onClick={() => onPageChange(totalPages - 1)}>{totalPages}</button>
        </>
      )}

      <button
        className="page-btn"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
      >
        &rsaquo;
      </button>

      <span className="page-jump">
        <input
          className="page-jump-input"
          type="text"
          placeholder="Go"
          value={jumpInput}
          onChange={(e) => setJumpInput(e.target.value.replace(/\D/g, ''))}
          onKeyDown={(e) => e.key === 'Enter' && handleJump()}
        />
        <button className="page-jump-btn" onClick={handleJump}>Go</button>
      </span>
    </div>
  );
}
