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
  const [uploadMode, setUploadMode] = useState<'image' | 'video'>('image');

  const updateSelection = (
    builder: (selectedText: string) => {
      text: string;
      selectionStartOffset?: number;
      selectionEndOffset?: number;
    }
  ) => {
    const textarea = textareaRef.current;
    if (!textarea) {
      const next = builder('');
      onChange(value + next.text);
      return;
    }

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const selectedText = value.substring(start, end);
    const next = builder(selectedText);
    const newValue = value.substring(0, start) + next.text + value.substring(end);
    onChange(newValue);

    requestAnimationFrame(() => {
      const selectionStartOffset = next.selectionStartOffset ?? next.text.length;
      const selectionEndOffset = next.selectionEndOffset ?? selectionStartOffset;
      textarea.selectionStart = start + selectionStartOffset;
      textarea.selectionEnd = start + selectionEndOffset;
      textarea.focus();
    });
  };

  const wrapSelection = (prefix: string, suffix: string, fallbackText: string) => {
    updateSelection((selectedText) => {
      const content = selectedText || fallbackText;
      const text = `${prefix}${content}${suffix}`;
      const selectionStartOffset = prefix.length;
      const selectionEndOffset = prefix.length + content.length;
      return { text, selectionStartOffset, selectionEndOffset };
    });
  };

  const insertCodeBlock = () => {
    updateSelection((selectedText) => {
      const content = selectedText || 'code';
      const text = `\`\`\`\n${content}\n\`\`\``;
      return {
        text,
        selectionStartOffset: 4,
        selectionEndOffset: 4 + content.length,
      };
    });
  };

  const openUploadPicker = (mode: 'image' | 'video') => {
    setUploadMode(mode);
    requestAnimationFrame(() => fileInputRef.current?.click());
  };

  const handleFileUpload = async (file: File) => {
    setUploading(true);
    try {
      const result = await uploadFile(file);
      updateSelection(() => ({
        text: `![${file.name}](${result.url})\n`,
      }));
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

  return (
    <div className="markdown-editor">
      <div className="markdown-editor-toolbar">
        <button type="button" className="toolbar-btn" title="Bold" onClick={() => wrapSelection('**', '**', 'bold text')}>B</button>
        <button type="button" className="toolbar-btn" title="Italic" onClick={() => wrapSelection('*', '*', 'italic text')} style={{ fontStyle: 'italic' }}>I</button>
        <button type="button" className="toolbar-btn" title="Code block" onClick={insertCodeBlock}>&lt;/&gt;</button>
        <button type="button" className="toolbar-btn" title="Link" onClick={() => wrapSelection('[', '](https://example.com)', 'link text')}>Link</button>
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
        accept={uploadMode === 'image'
          ? 'image/png,image/jpeg,image/gif,image/webp'
          : 'video/mp4,video/webm,video/quicktime'}
        style={{ display: 'none' }}
        onChange={handleFileSelect}
      />
    </div>
  );
}
