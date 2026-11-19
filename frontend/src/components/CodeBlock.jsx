import React, { useState } from 'react';
import { Copy, Check } from 'lucide-react';
import Prism from 'prismjs';

// Load common Prism language definitions
import 'prismjs/components/prism-javascript';
import 'prismjs/components/prism-typescript';
import 'prismjs/components/prism-jsx';
import 'prismjs/components/prism-tsx';
import 'prismjs/components/prism-java';
import 'prismjs/components/prism-python';
import 'prismjs/components/prism-json';
import 'prismjs/components/prism-bash';
import 'prismjs/components/prism-sql';
import 'prismjs/components/prism-css';
import 'prismjs/components/prism-markdown';

export default function CodeBlock({ node, inline, className, children, ...props }) {
  const [copied, setCopied] = useState(false);

  const match = /language-(\w+)/.exec(className || '');
  const language = match ? match[1].toLowerCase() : '';
  const rawCode = String(children).replace(/\n$/, '');

  // If it's inline code without newlines
  if (inline || (!match && !rawCode.includes('\n'))) {
    return (
      <code className="inline-code" {...props}>
        {children}
      </code>
    );
  }

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(rawCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Copy failed:', err);
    }
  };

  // Syntax highlight with Prism
  let highlightedHtml = '';
  try {
    const grammar = Prism.languages[language] || Prism.languages.javascript || null;
    if (grammar) {
      highlightedHtml = Prism.highlight(rawCode, grammar, language);
    }
  } catch (e) {
    console.warn('Prism highlight error:', e);
  }

  return (
    <div className="code-block-wrapper">
      <div className="code-header">
        <span className="code-language">{language || 'code'}</span>
        <button
          type="button"
          className={`copy-code-btn ${copied ? 'copied' : ''}`}
          onClick={handleCopy}
          title="Copy code to clipboard"
        >
          {copied ? (
            <>
              <Check size={14} className="copy-icon" />
              <span>Copied!</span>
            </>
          ) : (
            <>
              <Copy size={14} className="copy-icon" />
              <span>Copy code</span>
            </>
          )}
        </button>
      </div>

      <pre className="code-pre">
        {highlightedHtml ? (
          <code
            className={`code-content language-${language}`}
            dangerouslySetInnerHTML={{ __html: highlightedHtml }}
          />
        ) : (
          <code className="code-content">{rawCode}</code>
        )}
      </pre>
    </div>
  );
}
