import { useState, useRef } from 'react';
import './index.css';

function App() {
  const [file, setFile] = useState(null);
  const [mediaUrl, setMediaUrl] = useState('');
  const [mediaType, setMediaType] = useState(''); // 'video', 'audio', 'pdf'
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');
  const [status, setStatus] = useState('');
  const [summary, setSummary] = useState('');
  const [loading, setLoading] = useState(false);
  const mediaRef = useRef(null);

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files.length > 0) {
      const selected = e.target.files[0];
      setFile(selected);
      const url = URL.createObjectURL(selected);
      setMediaUrl(url);
      
      if (selected.type.startsWith('video/')) setMediaType('video');
      else if (selected.type.startsWith('audio/')) setMediaType('audio');
      else if (selected.type === 'application/pdf') setMediaType('pdf');
      else setMediaType('');
      
      setStatus('');
      setSummary('');
    }
  };

  const uploadFile = async () => {
    if (!file) return;
    setLoading(true);
    setStatus('Uploading and indexing...');
    
    const formData = new FormData();
    formData.append('file', file);
    
    let endpoint = 'http://localhost:8081/api/upload';
    if (mediaType === 'video' || mediaType === 'audio') {
      endpoint = 'http://localhost:8081/api/upload/media';
    }

    try {
      const res = await fetch(endpoint, {
        method: 'POST',
        body: formData,
      });
      const data = await res.json();
      if (res.ok) {
        setStatus(`Upload Success: ${data.message}`);
        fetchSummary(file.name);
      } else {
        setStatus(`Upload Failed: ${data.message || 'Error'}`);
      }
    } catch (e) {
      setStatus(`Error: ${e.message}`);
    }
    setLoading(false);
  };

  const fetchSummary = async (filename) => {
    try {
      const res = await fetch(`http://localhost:8081/api/summary?filename=${encodeURIComponent(filename)}`, {
        method: 'POST'
      });
      const data = await res.json();
      setSummary(data.summary);
    } catch (e) {
      console.error('Failed to parse summary', e);
    }
  };

  const sendMessage = async (e) => {
    e.preventDefault();
    if (!input.trim()) return;
    
    const userMsg = input;
    setInput('');
    setMessages(prev => [...prev, { role: 'user', content: userMsg }]);
    
    try {
      const res = await fetch('http://localhost:8081/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question: userMsg })
      });
      const data = await res.json();
      setMessages(prev => [...prev, { role: 'ai', content: data.answer }]);
    } catch (e) {
      setMessages(prev => [...prev, { role: 'ai', content: 'Connection error' }]);
    }
  };

  // Turn "[00:00:15,000 to ..." into clickable jumps
  const renderMessageContent = (content) => {
    // Regex to match timestamps like [00:01:23,456 to 00:02:00,000] or [00:01:23 to ...]
    const timeRegex = /\[(\d{2}:\d{2}:\d{2}(?:,\d{3})?).*?\]/g;
    
    let parts = [];
    let lastIndex = 0;
    
    let match;
    while ((match = timeRegex.exec(content)) !== null) {
      if (match.index > lastIndex) {
        parts.push(<span key={lastIndex}>{content.substring(lastIndex, match.index)}</span>);
      }
      
      const timeStr = match[1];
      // Convert HH:MM:SS,ms to seconds
      const timeParts = timeStr.replace(',', ':').split(':');
      let seconds = 0;
      if (timeParts.length >= 3) {
        seconds = parseInt(timeParts[0]) * 3600 + parseInt(timeParts[1]) * 60 + parseInt(timeParts[2]);
        if (timeParts[3]) seconds += parseInt(timeParts[3]) / 1000;
      }
      
      parts.push(
        <a key={match.index} 
           className="timestamp-link"
           onClick={() => jumpToTime(seconds)}>
             {match[0]}
        </a>
      );
      lastIndex = timeRegex.lastIndex;
    }
    
    if (lastIndex < content.length) {
      parts.push(<span key={lastIndex}>{content.substring(lastIndex)}</span>);
    }
    
    return parts.length > 0 ? parts : content;
  };

  const jumpToTime = (seconds) => {
    if (mediaRef.current) {
      mediaRef.current.currentTime = seconds;
      mediaRef.current.play();
    }
  };

  return (
    <div className="container">
      <div className="header">
        <h1>OmniStream AI</h1>
      </div>
      
      <div className="main-layout">
        {/* Left Side */}
        <div className="left-panel glass-panel">
          <div className="card">
            <h2>1. Upload Content</h2>
            <div className="file-input-wrapper">
              <input type="file" className="file-input" onChange={handleFileChange} />
            </div>
            <button className="btn" onClick={uploadFile} disabled={!file || loading}>
              {loading ? 'Processing...' : 'Upload & Index'}
            </button>
            {status && <div className={`status-text ${status.includes('Success') ? 'status-success' : 'status-error'}`}>{status}</div>}
          </div>

          {(mediaType === 'video' || mediaType === 'audio') && (
            <div className="card" style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
              <h2>2. Media Player</h2>
              <div className="media-container">
                {mediaType === 'video' ? (
                  <video ref={mediaRef} src={mediaUrl} controls />
                ) : (
                  <audio ref={mediaRef} src={mediaUrl} controls />
                )}
              </div>
            </div>
          )}
          
          {mediaType === 'pdf' && (
             <div className="card" style={{ flex: 1 }}>
               <h2>2. Document Viewer</h2>
               <div style={{color: 'var(--text-secondary)'}}>PDF loaded locally. Ask questions on the right.</div>
             </div>
          )}

          {summary && (
            <div className="card">
              <h2>Actionable Summary</h2>
              <div className="summary-box">{summary}</div>
            </div>
          )}
        </div>

        {/* Right Side */}
        <div className="right-panel glass-panel" style={{ padding: 0 }}>
          <div className="chat-window">
             <div className="messages" style={{borderBottom: '1px solid var(--glass-border)' }}>
               {messages.length === 0 && (
                 <div style={{color: 'var(--text-secondary)', textAlign: 'center', marginTop: 'auto', marginBottom: 'auto'}}>
                   Ask me anything about the uploaded file.<br/>
                   If it's an audio/video, I will provide timestamps!
                 </div>
               )}
               {messages.map((msg, idx) => (
                 <div key={idx} className={`message ${msg.role}`}>
                   <strong>{msg.role === 'user' ? 'You' : 'AI Agent'}</strong><br/>
                   <div style={{marginTop: '0.5rem'}}>{renderMessageContent(msg.content)}</div>
                 </div>
               ))}
             </div>
             
             <form className="chat-input-area" onSubmit={sendMessage}>
               <input 
                 className="text-input" 
                 value={input} 
                 onChange={e => setInput(e.target.value)} 
                 placeholder="Type your question here to retrieve information..." 
               />
               <button type="submit" className="btn" style={{width: 'auto', margin: 0}}>Send Response</button>
             </form>
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;
