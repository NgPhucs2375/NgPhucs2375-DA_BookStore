# BOOKOM Real-Time Chat System - Development Roadmap

**Status**: Planning Phase | **Target**: Q3 2026 | **Effort**: 3-4 Sprints

---

## 📋 Executive Summary

Implement a real-time messaging system between buyers and sellers to enhance user experience, support post-purchase communication, and build community engagement.

---

## 🎯 Business Objectives

| Objective | Impact | Priority |
|-----------|--------|----------|
| **Reduce Customer Support Load** | Sellers can answer product Q&A in real-time | High |
| **Increase Conversion Rate** | Buyers can inquire before purchase | High |
| **Build Trust & Engagement** | Direct communication channel | Medium |
| **Retain Customers** | Post-purchase support improves satisfaction | High |
| **Differentiate from Competitors** | Premium feature vs. static Q&A | Medium |

---

## 🏗️ Technical Architecture

### 1. **Real-Time Communication Layer**

#### Protocol Options:
- **WebSocket** (Recommended) - ✅ Native browser support, low latency
- **Server-Sent Events (SSE)** - One-way, simpler but less interactive
- **Socket.io** - Wrapper around WebSocket with fallbacks

#### Implementation: Spring WebSocket + STOMP

```
┌─────────────────────────────────────────────────────┐
│                    BOOKOM Platform                  │
├─────────────────────────────────────────────────────┤
│  Frontend Layer                                     │
│  ├─ Buyer Chat Widget (React/Vue component)        │
│  ├─ Seller Chat Dashboard                          │
│  └─ WebSocket Client Handler                       │
├─────────────────────────────────────────────────────┤
│  Backend Layer                                      │
│  ├─ Spring Boot WebSocket Config                   │
│  ├─ STOMP Message Broker (RabbitMQ/ActiveMQ)       │
│  ├─ Chat Service Logic                             │
│  └─ Message Persistence Service                    │
├─────────────────────────────────────────────────────┤
│  Data Layer                                         │
│  ├─ Conversation (JPA Entity)                       │
│  ├─ Message (JPA Entity)                            │
│  ├─ ChatNotification (Redis Cache)                 │
│  └─ ReadReceipt (Optional)                          │
└─────────────────────────────────────────────────────┘
```

### 2. **Database Schema**

#### Table: Conversation
```sql
CREATE TABLE conversations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    book_id BIGINT,  -- optional, linked to product discussion
    subject VARCHAR(255),
    last_message_id BIGINT,
    last_message_at TIMESTAMP,
    buyer_unread_count INT DEFAULT 0,
    seller_unread_count INT DEFAULT 0,
    status ENUM('ACTIVE', 'ARCHIVED', 'BLOCKED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY unique_conversation (buyer_id, seller_id)
);
```

#### Table: Message
```sql
CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    message_type ENUM('TEXT', 'IMAGE', 'FILE', 'PRODUCT_INQUIRY') DEFAULT 'TEXT',
    attachment_url VARCHAR(500),  -- for images/files
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    status ENUM('PENDING', 'SENT', 'FAILED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_conversation_created (conversation_id, created_at DESC),
    INDEX idx_sender (sender_id)
);
```

#### Table: TypingIndicator (Redis, TTL: 3s)
```redis
KEY: chat:typing:{conversation_id}:{user_id}
VALUE: {user_id, username, avatar_url}
TTL: 3 seconds (auto-refresh per keystroke)
```

#### Table: OnlineStatus (Redis, TTL: Session)
```redis
KEY: chat:online:{user_id}
VALUE: {user_id, last_seen_at}
TTL: Session duration
```

---

## 📱 Frontend Features by Sprint

### **Sprint 1: Core Chat UI & Real-Time Message**
**Duration**: 2 weeks | **Deliverables**: MVP

#### Components to Build:
1. **Chat List View**
   - List all conversations with seller/buyer
   - Show unread count badge
   - Last message preview + timestamp
   - Search conversations
   - Mark as read/unread
   - Archive conversation option

2. **Chat Message Window**
   - Message list with infinite scroll (load older messages)
   - Message input box with rich text support
   - Send button (with loading state)
   - Timestamps for each message
   - Sender avatar & name
   - Online/offline status indicator

3. **WebSocket Handler**
   - Connect on page load
   - Handle incoming messages in real-time
   - Reconnect logic with exponential backoff
   - Auto-sync unread badges

#### UI Mockup Structure (Tailwind CSS):
```html
<div class="flex h-screen bg-white">
  <!-- Chat List Sidebar -->
  <aside class="w-80 border-r border-brand-accent bg-brand-cream/20">
    <div class="p-4 border-b sticky top-0 bg-white">
      <h2 class="font-bold text-lg">Tin Nhắn</h2>
      <input type="search" placeholder="Tìm kiếm..." class="w-full mt-2 px-3 py-2 border rounded-lg">
    </div>
    <ul id="conversations-list" class="overflow-y-auto">
      <!-- Dynamically rendered conversations -->
    </ul>
  </aside>

  <!-- Chat Window -->
  <main class="flex-1 flex flex-col">
    <header class="p-4 border-b bg-white flex justify-between items-center">
      <div>
        <h3 id="chat-header-name" class="font-bold"></h3>
        <span id="online-status" class="text-xs text-gray-500"></span>
      </div>
      <button class="text-gray-500 hover:text-brand-dark">⋮</button>
    </header>
    
    <div id="messages-container" class="flex-1 overflow-y-auto p-4 bg-brand-cream/10">
      <!-- Messages rendered here -->
    </div>

    <!-- Typing Indicator -->
    <div id="typing-indicator" class="px-4 py-2 text-xs text-gray-400 hidden">
      <span id="typer-name"></span> đang nhập...
    </div>

    <!-- Message Input -->
    <footer class="p-4 border-t bg-white">
      <div class="flex gap-2">
        <input id="message-input" type="text" placeholder="Aa" class="flex-1 border rounded-lg px-3 py-2">
        <button id="send-btn" class="bg-brand-orange text-white px-4 py-2 rounded-lg hover:bg-brand-biscuit">Gửi</button>
      </div>
    </footer>
  </main>
</div>
```

#### Code Example: WebSocket Setup (JavaScript)
```javascript
class ChatWebSocketService {
  constructor(userId, conversationId) {
    this.userId = userId;
    this.conversationId = conversationId;
    this.ws = null;
    this.messageQueue = [];
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
  }

  connect() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    this.ws = new WebSocket(`${protocol}//${window.location.host}/ws/chat/${this.conversationId}`);
    
    this.ws.onopen = () => {
      console.log('✅ Chat connected');
      this.reconnectAttempts = 0;
      this.flushQueue();
    };
    
    this.ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      this.handleMessage(message);
    };
    
    this.ws.onerror = (error) => console.error('❌ WebSocket error:', error);
    this.ws.onclose = () => this.attemptReconnect();
  }

  sendMessage(content) {
    const message = {
      conversationId: this.conversationId,
      senderId: this.userId,
      content: content,
      timestamp: new Date().toISOString()
    };

    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      this.messageQueue.push(message);
    }
  }

  handleMessage(message) {
    // Render message in UI
    const messageEl = this.createMessageElement(message);
    document.getElementById('messages-container').appendChild(messageEl);
    // Mark as read
    this.markAsRead(message.id);
  }

  attemptReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      const delay = Math.pow(2, this.reconnectAttempts) * 1000;
      setTimeout(() => {
        this.reconnectAttempts++;
        this.connect();
      }, delay);
    }
  }

  flushQueue() {
    while (this.messageQueue.length > 0) {
      const msg = this.messageQueue.shift();
      this.ws.send(JSON.stringify(msg));
    }
  }
}
```

---

### **Sprint 2: Advanced Features**
**Duration**: 2 weeks | **Deliverables**: Enhanced UX

1. **Typing Indicator**
   - Show "Người dùng đang nhập..." when seller types
   - Clear indicator when user stops typing (3s timeout)
   - Redis-backed presence data

2. **Message Reactions** (Optional)
   - Emoji reactions on messages
   - Count reactions per emoji

3. **Read Receipts**
   - Single tick (sent)
   - Double tick (delivered)
   - Double tick blue (read)
   - Show "Đã xem lúc..." on hover

4. **File/Image Sharing**
   - Image preview in chat
   - File download link
   - Virus scan integration (ClamAV recommended)

#### Code Example: Typing Indicator Handler
```javascript
let typingTimeout;
const messageInput = document.getElementById('message-input');

messageInput.addEventListener('keydown', () => {
  clearTimeout(typingTimeout);
  
  // Send typing notification
  ChatService.notifyTyping(conversationId);
  
  // Clear typing status after 3 seconds of inactivity
  typingTimeout = setTimeout(() => {
    ChatService.clearTyping(conversationId);
  }, 3000);
});

// Listen for typing events from WebSocket
ChatService.on('typing', (data) => {
  document.getElementById('typing-indicator').classList.remove('hidden');
  document.getElementById('typer-name').textContent = data.senderName;
  
  // Auto-hide after 3 seconds if no update
  setTimeout(() => {
    document.getElementById('typing-indicator').classList.add('hidden');
  }, 3000);
});
```

---

### **Sprint 3: Analytics & Moderation**
**Duration**: 2 weeks | **Deliverables**: Admin Tools

1. **Chat Analytics Dashboard**
   - Average response time by seller
   - Most active chat hours
   - Resolution rate
   - Customer satisfaction score

2. **Message Moderation**
   - Flag inappropriate content
   - Auto-block spam/links
   - Report conversation
   - Manual review queue

3. **Notifications**
   - Browser push notifications
   - Email notification for @mentions
   - SMS for urgent issues (optional)

---

## 🔌 Backend API Endpoints

### **Chat Endpoints** (JWT Protected)

```
# Get all conversations for user
GET /api/chats/conversations
Response: { conversations: [...], unreadCount: 5 }

# Get messages from conversation (with pagination)
GET /api/chats/conversations/{id}/messages?page=0&size=20
Response: { messages: [...], hasMore: true, totalCount: 127 }

# Send message
POST /api/chats/conversations/{id}/messages
Body: { content: "...", attachmentUrl?: "..." }
Response: { messageId, status: 'SENT', timestamp }

# Mark messages as read
PATCH /api/chats/conversations/{id}/read
Response: { readCount: 5 }

# Create/open conversation
POST /api/chats/conversations
Body: { sellerId: 1, bookId?: 5 }
Response: { conversationId, ... }

# Archive conversation
PATCH /api/chats/conversations/{id}/archive
Response: { status: 'ARCHIVED' }

# Block user
PATCH /api/chats/conversations/{id}/block
Response: { status: 'BLOCKED' }

# Upload attachment
POST /api/chats/upload
Body: FormData { file }
Response: { url: "https://..." }
```

### **WebSocket Channels** (STOMP)

```
# Subscribe to user's conversation list updates
SUBSCRIBE /user/queue/conversations

# Subscribe to specific conversation
SUBSCRIBE /topic/conversations/{id}

# Send message (publish)
SEND /app/chat/send
Body: { conversationId, content, ... }

# Typing indicator
SEND /app/chat/typing
Body: { conversationId, userId }

# Mark read
SEND /app/chat/read
Body: { conversationId, messageId }
```

---

## 📊 Success Metrics

| Metric | Target | Timeline |
|--------|--------|----------|
| **Chat Feature Adoption** | 40% of active sellers | 8 weeks post-launch |
| **Average Response Time** | < 2 hours | 12 weeks |
| **Customer Satisfaction** (CSAT) | > 85% | 16 weeks |
| **Chat-to-Sale Conversion** | +15% | 12 weeks |
| **Support Ticket Reduction** | -30% | 16 weeks |

---

## 🚀 Deployment Strategy

### **Phase 1: Beta (Week 1-2)**
- Invite 100 active seller partners
- Internal team testing
- Feedback collection
- Bug fixes

### **Phase 2: Gradual Rollout (Week 3-4)**
- Enable for 20% of sellers
- Monitor performance & errors
- A/B test UI variants

### **Phase 3: Full Launch (Week 5+)**
- Enable for all users
- Marketing campaign
- Training content for sellers
- Support ticket updates

---

## 🛠️ Tech Stack

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| **WebSocket** | Spring Boot Starter WebSocket | Native Spring integration |
| **Message Broker** | RabbitMQ or ActiveMQ | STOMP protocol support |
| **Real-time Cache** | Redis | Typing indicators, online status |
| **Frontend** | Vanilla JS or React | Zero external deps or type safety |
| **Storage** | MySQL/PostgreSQL | Existing DB, good for transactions |
| **File Storage** | AWS S3 or MinIO | Scalable image/file storage |
| **Monitoring** | ELK Stack + Datadog | Chat metrics & debugging |

---

## ⚠️ Risk & Mitigation

| Risk | Impact | Mitigation |
|------|--------|-----------|
| **Server Overload** | Chat unavailable | Auto-scaling + load balancing |
| **Message Loss** | Data integrity | Persistent queue + retry logic |
| **Abuse/Spam** | Poor UX | Content filtering + rate limiting |
| **Privacy** | GDPR violation | End-to-end encryption option, data retention policy |
| **Latency Spikes** | Poor UX | CDN for WebSocket, geo-distributed servers |

---

## 📚 References & Inspiration

- [Spring WebSocket Guide](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [Socket.io Chat Example](https://socket.io/docs/v4/chat-application/)
- [Shopee Chat API](https://shopee.com/faq) (UX inspiration)
- [Tiki Messaging System](https://tiki.vn) (Design patterns)
- [Auth0 Real-time Communication](https://auth0.com/blog/real-time-collaboration-with-websockets/)

---

## 👥 Team Requirements

| Role | Count | Sprint |
|------|-------|--------|
| **Backend Dev** | 2 | Sprint 1-3 |
| **Frontend Dev** | 2 | Sprint 1-3 |
| **DevOps** | 1 | Sprint 1 (setup) |
| **QA** | 1 | Sprint 1-3 |
| **Product Manager** | 1 | Sprint 0-3 |

**Estimated Effort**: 8-10 person-weeks

---

## 📋 Acceptance Criteria

- [ ] Buyer can open chat with seller
- [ ] Messages sent/received in real-time (< 500ms latency)
- [ ] Typing indicator works smoothly
- [ ] Read receipts display correctly
- [ ] File uploads work (images, PDFs)
- [ ] Message history persists & loads correctly
- [ ] Offline messages queue & send when online
- [ ] Notifications work (browser + email)
- [ ] Performance: Server handles 1000+ concurrent connections
- [ ] Mobile responsive design tested on iOS/Android

---

**Document Version**: 1.0 | **Last Updated**: May 13, 2026 | **Next Review**: June 10, 2026
