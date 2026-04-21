import { useState, useEffect, useRef, useCallback } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { chatAPI } from '../api/api';
import { useAuth } from '../context/AuthContext';
import { IconChat, IconPackage } from '../components/Icons';

function formatTime(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  const now = new Date();
  const isToday = d.toDateString() === now.toDateString();
  if (isToday) return d.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
  return d.toLocaleDateString('ru-RU', { day: 'numeric', month: 'short' }) + ' ' +
    d.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
}

function UserAvatar({ name, isAdmin, size = 36 }) {
  const initial = name ? name.charAt(0).toUpperCase() : '?';
  return (
    <div
      className={`chat-user-avatar ${isAdmin ? 'admin' : ''}`}
      style={{ width: size, height: size, fontSize: size * 0.38 }}
      title={name}
    >
      {initial}
    </div>
  );
}

export default function ChatPage() {
  const { user } = useAuth();
  const [searchParams] = useSearchParams();
  const [rooms, setRooms] = useState([]);
  const [activeRoomId, setActiveRoomId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState('');
  const [showSidebar, setShowSidebar] = useState(true);
  const messagesEndRef = useRef(null);
  const messagesContainerRef = useRef(null);
  const fileInputRef = useRef(null);
  const pollRef = useRef(null);
  const lastMsgIdRef = useRef(null);
  const inputRef = useRef(null);

  const loadRooms = useCallback(async () => {
    try {
      const res = await chatAPI.getMyChats();
      setRooms(res.data || []);
    } catch {} finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { loadRooms(); }, [loadRooms]);

  // автоматическое открытие чата по adId из URL
  useEffect(() => {
    const adId = searchParams.get('adId');
    if (adId) {
      chatAPI.startChat(parseInt(adId)).then(res => {
        setActiveRoomId(res.data.id);
        setShowSidebar(false);
        loadRooms();
      }).catch(() => {});
    }
  }, [searchParams, loadRooms]);

  const loadMessages = useCallback(async (roomId) => {
    try {
      const res = await chatAPI.getMessages(roomId);
      const msgs = res.data || [];
      setMessages(prev => {
        // обновляем только при реальном изменении
        const newLastId = msgs.length > 0 ? msgs[msgs.length - 1].id : null;
        const prevLastId = prev.length > 0 ? prev[prev.length - 1].id : null;
        if (newLastId === prevLastId && msgs.length === prev.length) return prev;
        return msgs;
      });
    } catch {}
  }, []);

  useEffect(() => {
    if (!activeRoomId) return;
    loadMessages(activeRoomId);
    chatAPI.markAsRead(activeRoomId).catch(() => {});

    pollRef.current = setInterval(() => {
      loadMessages(activeRoomId);
      loadRooms();
    }, 4000);

    return () => clearInterval(pollRef.current);
  }, [activeRoomId, loadMessages, loadRooms]);

  // скролл только при новых сообщениях, чтобы не дёргать при polling
  useEffect(() => {
    if (messages.length === 0) return;
    const currentLastId = messages[messages.length - 1].id;
    if (currentLastId !== lastMsgIdRef.current) {
      lastMsgIdRef.current = currentLastId;
      // стабильный скролл через rAF
      requestAnimationFrame(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
      });
    }
  }, [messages]);

  const handleSend = async (e) => {
    e.preventDefault();
    if (!newMessage.trim() && !fileInputRef.current?.files[0]) return;

    setSending(true);
    setSendError('');
    try {
      const file = fileInputRef.current?.files[0] || null;
      await chatAPI.sendMessage(activeRoomId, newMessage.trim() || null, file);
      setNewMessage('');
      if (fileInputRef.current) fileInputRef.current.value = '';
      await loadMessages(activeRoomId);
      await loadRooms();
      // возврат фокуса в поле ввода
      inputRef.current?.focus();
    } catch (err) {
      setSendError('Не удалось отправить. Попробуйте ещё раз.');
    } finally {
      setSending(false);
    }
  };

  const handleFileClick = () => fileInputRef.current?.click();

  const handleFileSend = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setSending(true);
    setSendError('');
    try {
      await chatAPI.sendMessage(activeRoomId, null, file);
      if (fileInputRef.current) fileInputRef.current.value = '';
      await loadMessages(activeRoomId);
      await loadRooms();
    } catch {
      setSendError('Не удалось отправить файл.');
    } finally {
      setSending(false);
    }
  };

  const selectRoom = (roomId) => {
    setActiveRoomId(roomId);
    setShowSidebar(false);
    lastMsgIdRef.current = null;
    chatAPI.markAsRead(roomId).catch(() => {});
  };

  const handleBack = () => {
    setShowSidebar(true);
    setActiveRoomId(null);
  };

  const activeRoom = rooms.find(r => r.id === activeRoomId);
  const isOtherAdmin = activeRoom?.otherUserName === 'Администратор';

  if (loading) {
    return <div className="loading"><div className="spinner"></div><p className="loading-text">Загружаем чаты...</p></div>;
  }

  return (
    <div className="chat-page">
      {/* Sidebar */}
      <div className={`chat-sidebar ${!showSidebar ? 'chat-sidebar--hidden' : ''}`}>
        <div className="chat-sidebar-header">
          <h2><IconChat size={20} style={{verticalAlign:'middle',marginRight:6}} /> Сообщения</h2>
        </div>
        <div className="chat-rooms-list">
          {rooms.length === 0 ? (
            <div className="chat-empty-sidebar">
              <div className="empty-state-icon"><IconChat size={40} /></div>
              <p>Пока нет сообщений</p>
              <p className="chat-empty-hint">Напишите продавцу на странице объявления</p>
            </div>
          ) : (
            rooms.map(room => {
              const roomIsAdmin = room.otherUserName === 'Администратор';
              return (
                <div
                  key={room.id}
                  className={`chat-room-item ${room.id === activeRoomId ? 'active' : ''}`}
                  onClick={() => selectRoom(room.id)}
                >
                  <div className="chat-room-avatar">
                    {room.adImageUrl ? (
                      <img src={`/api${room.adImageUrl}`} alt="" />
                    ) : (
                      <IconPackage size={20} />
                    )}
                  </div>
                  <div className="chat-room-info">
                    <div className="chat-room-top">
                      <span className="chat-room-name">
                        {room.otherUserName}
                        {roomIsAdmin && <span className="nado-team-badge">Nado</span>}
                      </span>
                      <span className="chat-room-time">{formatTime(room.lastMessageAt)}</span>
                    </div>
                    <div className="chat-room-ad-title">{room.adTitle}</div>
                    <div className="chat-room-bottom">
                      <span className="chat-room-last-msg">{room.lastMessage || 'Нет сообщений'}</span>
                      {room.unreadCount > 0 && (
                        <span className="chat-room-badge">{room.unreadCount}</span>
                      )}
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* Chat window */}
      <div className={`chat-window ${showSidebar ? 'chat-window--hidden' : ''}`}>
        {activeRoom ? (
          <>
            <div className="chat-window-header">
              <button className="chat-back-btn" type="button" onClick={handleBack} aria-label="Назад">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
              </button>
              <div className="chat-window-user">
                <UserAvatar name={activeRoom.otherUserName} isAdmin={isOtherAdmin} size={32} />
                <div>
                  <strong>
                    {activeRoom.otherUserName}
                    {isOtherAdmin && <span className="nado-team-badge">Команда Nado</span>}
                  </strong>
                  <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                    <Link to={`/profile/${activeRoom.otherUserId}`} className="chat-room-profile-link">
                      Профиль
                    </Link>
                    <Link to={`/ads/${activeRoom.adId}`} className="chat-window-ad-link">
                      {activeRoom.adTitle}
                    </Link>
                  </div>
                </div>
              </div>
            </div>

            <div className="chat-messages" ref={messagesContainerRef}>
              {messages.length === 0 && (
                <div className="chat-empty-messages">
                  <p>Начните диалог — напишите первое сообщение</p>
                </div>
              )}
              {messages.map(msg => (
                <div key={msg.id} className={`chat-msg ${msg.mine ? 'mine' : 'theirs'}`}>
                  {!msg.mine && (
                    <UserAvatar name={activeRoom.otherUserName} isAdmin={isOtherAdmin} size={28} />
                  )}
                  <div className="chat-msg-bubble">
                    {msg.attachmentUrl && (
                      <div className="chat-msg-attachment">
                        {msg.attachmentType?.startsWith('image/') ? (
                          <img src={`/api${msg.attachmentUrl}`} alt="Фото"
                            onClick={() => window.open(`/api${msg.attachmentUrl}`, '_blank')} />
                        ) : msg.attachmentType?.startsWith('video/') ? (
                          <video controls src={`/api${msg.attachmentUrl}`} />
                        ) : (
                          <a href={`/api${msg.attachmentUrl}`} target="_blank" rel="noopener noreferrer">📎 Файл</a>
                        )}
                      </div>
                    )}
                    {msg.content && <div className="chat-msg-text">{msg.content}</div>}
                    <div className="chat-msg-time">
                      {formatTime(msg.createdAt)}
                      {msg.mine && <span className="chat-msg-status">{msg.read ? ' ✓✓' : ' ✓'}</span>}
                    </div>
                  </div>
                </div>
              ))}
              <div ref={messagesEndRef} />
            </div>

            {sendError && (
              <div className="chat-send-error">
                {sendError}
                <button type="button" onClick={() => setSendError('')}>✕</button>
              </div>
            )}

            <form className="chat-input" onSubmit={handleSend}>
              <input
                type="file"
                ref={fileInputRef}
                style={{ display: 'none' }}
                accept="image/*,video/*,.pdf,.doc,.docx"
                onChange={handleFileSend}
              />
              <button type="button" className="chat-attach-btn" onClick={handleFileClick}
                title="Прикрепить файл" disabled={sending}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M21.44 11.05l-9.19 9.19a6 6 0 01-8.49-8.49l9.19-9.19a4 4 0 015.66 5.66l-9.2 9.19a2 2 0 01-2.83-2.83l8.49-8.48"/>
                </svg>
              </button>
              <input
                ref={inputRef}
                type="text"
                placeholder="Напишите сообщение..."
                value={newMessage}
                onChange={e => setNewMessage(e.target.value)}
                disabled={sending}
                autoComplete="off"
              />
              <button type="submit" className="chat-send-btn"
                disabled={sending || (!newMessage.trim())}>
                {sending ? (
                  <div className="spinner-sm"></div>
                ) : (
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/>
                  </svg>
                )}
              </button>
            </form>
          </>
        ) : (
          <div className="chat-no-selection">
            <div className="empty-state-icon"><IconChat size={48} /></div>
            <h3>Выберите диалог</h3>
            <p>или напишите продавцу на странице объявления</p>
          </div>
        )}
      </div>
    </div>
  );
}
