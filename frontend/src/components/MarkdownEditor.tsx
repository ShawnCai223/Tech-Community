import { useRef, useState } from 'react';
import { uploadFile } from '../api/upload';

interface MarkdownEditorProps {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  rows?: number;
  minHeight?: number;
}

export default function MarkdownEditor({ value, onChange, placeholder, rows, minHeight }: MarkdownEditorProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [uploading, setUploading] = useState(false);

  const insertAtCursor = (text: string) => {
    const textarea = textareaRef.current;
    if (!textarea) {
      onChange(value + text);
      return;
    }
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const newValue = value.substring(0, start) + text + value.substring(end);
    onChange(newValue);
    // Restore cursor position after the inserted text
    requestAnimationFrame(() => {
      textarea.selectionStart = textarea.selectionEnd = start + text.length;
      textarea.focus();
    });
  };

  const handleFileUpload = async (file: File) => {
    setUploading(true);
    try {
      const result = await uploadFile(file);
      if (result.type === 'image') {
        insertAtCursor(`![${file.name}](${result.url})\n`);
      } else {
        insertAtCursor(`<video src="${result.url}" controls></video>\n`);
      }
    } catch {
      alert('File upload failed.');
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
      if (item.type.startsWith('image/')) {
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

  return (
    <div className="markdown-editor">
      <div className="markdown-editor-toolbar">
        <button type="button" className="toolbar-btn" title="Bold" onClick={() => insertAtCursor('**bold**')}>B</button>
        <button type="button" className="toolbar-btn" title="Italic" onClick={() => insertAtCursor('*italic*')} style={{ fontStyle: 'italic' }}>I</button>
        <button type="button" className="toolbar-btn" title="Code block" onClick={() => insertAtCursor('\n```\ncode\n```\n')}>&lt;/&gt;</button>
        <button type="button" className="toolbar-btn" title="Link" onClick={() => insertAtCursor('[text](url)')}>Link</button>
        <button
          type="button"
          className="toolbar-btn"
          title="Upload image or video"
          onClick={() => fileInputRef.current?.click()}
          disabled={uploading}
        >
          {uploading ? 'Uploading...' : 'Upload'}
        </button>
        <span className="toolbar-hint">Markdown supported. Paste or drag to upload images.</span>
      </div>
      <textarea
        ref={textareaRef}
        className="form-textarea"
        style={minHeight ? { minHeight } : undefined}
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onPaste={handlePaste}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        rows={rows}
      />
      <input
        ref={fileInputRef}
        type="file"
        accept="image/png,image/jpeg,image/gif,image/webp,video/mp4,video/webm,video/quicktime"
        style={{ display: 'none' }}
        onChange={handleFileSelect}
      />
    </div>
  );
}
