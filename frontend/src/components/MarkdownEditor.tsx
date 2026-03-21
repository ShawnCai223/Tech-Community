import { useEffect, useRef, useState } from 'react';
import { uploadFile } from '../api/upload';

interface MarkdownEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  rows?: number;
  minHeight?: number;
}

export default function MarkdownEditor({ value, onChange, placeholder, rows, minHeight }: MarkdownEditorProps) {
  const editorRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const isInternalChange = useRef(false);
  const [uploading, setUploading] = useState(false);
  const [uploadMode, setUploadMode] = useState<'image' | 'video'>('image');
  const [error, setError] = useState('');

  useEffect(() => {
    // Skip sync when the change originated from user editing inside the editor,
    // so we don't reset innerHTML and destroy cursor/selection/formatting state.
    if (isInternalChange.current) {
      isInternalChange.current = false;
      return;
    }
    const editor = editorRef.current;
    if (!editor) {
      return;
    }

    if (editor.innerHTML !== value) {
      editor.innerHTML = value;
    }
  }, [value]);

  const focusEditor = () => {
    editorRef.current?.focus();
  };

  const exec = (command: string, valueArg?: string) => {
    focusEditor();
    document.execCommand(command, false, valueArg);
    syncValue();
  };

  const syncValue = () => {
    const editor = editorRef.current;
    if (!editor) {
      return;
    }
    const nextValue = normalizeEditorHtml(editor.innerHTML);
    if (nextValue !== value) {
      isInternalChange.current = true;
      onChange(nextValue);
    }
  };

  const openUploadPicker = (mode: 'image' | 'video') => {
    setUploadMode(mode);
    setError('');
    requestAnimationFrame(() => fileInputRef.current?.click());
  };

  const handleFileUpload = async (file: File) => {
    setUploading(true);
    setError('');
    try {
      const result = await uploadFile(file);
      const altText = escapeHtml(file.name || result.type);
      const mediaHtml = result.type === 'video'
        ? `<video src="${result.url}" controls class="markdown-video"></video><p><br></p>`
        : `<img src="${result.url}" alt="${altText}" class="markdown-image" /><p><br></p>`;
      exec('insertHTML', mediaHtml);
    } catch (err: any) {
      setError(err.response?.data?.message || 'File upload failed.');
    } finally {
      setUploading(false);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) handleFileUpload(file);
    e.target.value = '';
  };

  const handlePaste = (e: React.ClipboardEvent) => {
    const items = e.clipboardData?.items;
    if (!items) return;
    for (const item of items) {
      if (item.type.startsWith('image/') || item.type.startsWith('video/')) {
        e.preventDefault();
        const file = item.getAsFile();
        if (file) handleFileUpload(file);
        return;
      }
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    const file = e.dataTransfer?.files?.[0];
    if (file && (file.type.startsWith('image/') || file.type.startsWith('video/'))) {
      handleFileUpload(file);
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
  };

  const insertLink = () => {
    focusEditor();
    const url = window.prompt('请输入网站地址', 'https://');
    if (!url) {
      return;
    }
    exec('createLink', url);
  };

  const insertCodeBlock = () => {
    exec('insertHTML', '<pre><code>code</code></pre><p><br></p>');
  };

  return (
    <div className="markdown-editor">
      <div className="markdown-editor-toolbar">
        <button type="button" className="toolbar-btn" title="Bold" onClick={() => exec('bold')}>B</button>
        <button type="button" className="toolbar-btn" title="Italic" onClick={() => exec('italic')} style={{ fontStyle: 'italic' }}>I</button>
        <button type="button" className="toolbar-btn" title="Code block" onClick={insertCodeBlock}>&lt;/&gt;</button>
        <button type="button" className="toolbar-btn" title="Link" onClick={insertLink}>Link</button>
        <button
          type="button"
          className="toolbar-btn"
          title="Upload image"
          onClick={() => openUploadPicker('image')}
          disabled={uploading}
        >
          {uploading && uploadMode === 'image' ? 'Uploading...' : 'Image'}
        </button>
        <button
          type="button"
          className="toolbar-btn"
          title="Upload video"
          onClick={() => openUploadPicker('video')}
          disabled={uploading}
        >
          {uploading && uploadMode === 'video' ? 'Uploading...' : 'Video'}
        </button>
        <span className="toolbar-hint">Select text to format it. Paste or drag images and videos to upload.</span>
      </div>
      <div
        ref={editorRef}
        className="form-textarea rich-editor-content markdown-body"
        style={minHeight ? { minHeight } : rows ? { minHeight: `${Math.max(rows, 3) * 24}px` } : undefined}
        data-placeholder={placeholder}
        contentEditable
        suppressContentEditableWarning
        onInput={syncValue}
        onPaste={handlePaste}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onBlur={syncValue}
        role="textbox"
        aria-multiline="true"
        spellCheck
      />
      {error ? <div className="form-error" style={{ marginTop: 8 }}>{error}</div> : null}
      <input
        ref={fileInputRef}
        type="file"
        accept={uploadMode === 'image'
          ? 'image/png,image/jpeg,image/gif,image/webp'
          : 'video/mp4,video/webm,video/quicktime'}
        style={{ display: 'none' }}
        onChange={handleFileSelect}
      />
    </div>
  );
}

function normalizeEditorHtml(html: string) {
  return html
    .replace(/<(div|p)><br><\/(div|p)>/gi, '<p><br></p>')
    .replace(/<div>/gi, '<p>')
    .replace(/<\/div>/gi, '</p>')
    .replace(/&nbsp;/gi, ' ')
    .trim();
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
