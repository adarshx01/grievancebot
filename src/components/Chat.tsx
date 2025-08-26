// filepath: /home/adarsh/Desktop/GrievanceRedressal/frontend/src/components/Chat.tsx
import React, { useState, useEffect, useRef, useCallback } from 'react';
import { chatAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';

interface Message {
  type: 'user' | 'ai';
  content: string;
  timestamp: Date;
}

interface FormData {
  submitForm: boolean;
  mandatoryInfoQueried: boolean;
  // Other form fields
}

interface ChatResponse {
  sessionId: number;
  userMessage: string;
  aiResponse: string;
  category: string;
  grievanceId?: number;
  grievanceNumber?: string;
  submitForm?: boolean;
  mandatoryInfoQueried?: boolean;
  formData?: string;
}

const Chat: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [sessionId, setSessionId] = useState<number | undefined>();
  const [error, setError] = useState<string>('');
  const [welcomeMessageAdded, setWelcomeMessageAdded] = useState(false);
  const [formSubmitted, setFormSubmitted] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const { isAuthenticated, user } = useAuth();

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages, scrollToBottom]);

  // Add welcome message only once when user is authenticated
  useEffect(() => {
    if (isAuthenticated && user && !welcomeMessageAdded) {
      const welcomeMessage: Message = {
        type: 'ai',
        content: `Hello ${user.firstName}! I'm here to help you file a complaint. Please describe the issue you're facing.`,
        timestamp: new Date(),
      };
      setMessages([welcomeMessage]);
      setWelcomeMessageAdded(true);
    } else if (!isAuthenticated) {
      // Reset when user logs out
      setMessages([]);
      setWelcomeMessageAdded(false);
      setSessionId(undefined);
      setError('');
      setFormSubmitted(false);
    }
  }, [isAuthenticated, user, welcomeMessageAdded]);

  const sendMessage = useCallback(async () => {
    if (!input.trim()) return;

    const userMessage: Message = {
      type: 'user',
      content: input,
      timestamp: new Date(),
    };

    setMessages(prev => [...prev, userMessage]);
    setLoading(true);
    setError('');
    const currentInput = input;
    setInput('');

    try {
      console.log('Sending message:', currentInput);
      console.log('Session ID:', sessionId);
      
      const response = await chatAPI.sendMessage({
        message: currentInput,
        sessionId,
      });

      console.log('Chat response:', response.data);
      const data: ChatResponse = response.data;
      
      // Update session ID if it's a new session
      if (!sessionId && data.sessionId) {
        console.log('Setting new session ID:', data.sessionId);
        setSessionId(data.sessionId);
      }

      const aiMessage: Message = {
        type: 'ai',
        content: data.aiResponse,
        timestamp: new Date(),
      };

      setMessages(prev => [...prev, aiMessage]);

      // Check for form submission flags
      if (data.submitForm && data.mandatoryInfoQueried && data.formData) {
        try {
          // Parse the form data to display a confirmation
          const formData = JSON.parse(data.formData);
          console.log('Form submitted:', formData);
          
          // Show confirmation message
          const confirmationMessage: Message = {
            type: 'ai',
            content: `✅ Your complaint has been successfully submitted! Reference ID: ${data.grievanceNumber}`,
            timestamp: new Date(),
          };
          setMessages(prev => [...prev, confirmationMessage]);
          setFormSubmitted(true);
        } catch (e) {
          console.error('Error parsing form data:', e);
        }
      }
      
      // Show grievance info if created
      else if (data.grievanceId) {
        const grievanceMessage: Message = {
          type: 'ai',
          content: `📋 Your grievance has been logged with ID: ${data.grievanceNumber}. Category: ${data.category}`,
          timestamp: new Date(),
        };
        setMessages(prev => [...prev, grievanceMessage]);
      }

    } catch (error: any) {
      console.error('Error sending message:', error);
      
      let errorMsg = 'Sorry, I encountered an error. Please try again.';
      
      if (error.response?.status === 401) {
        errorMsg = 'Your session has expired. Please refresh the page and log in again.';
      } else if (error.response?.status === 500) {
        errorMsg = 'Server error occurred. Please try again later.';
      } else if (error.code === 'NETWORK_ERROR') {
        errorMsg = 'Network error. Please check your connection.';
      }
      
      setError(errorMsg);
      
      const errorMessage: Message = {
        type: 'ai',
        content: errorMsg,
        timestamp: new Date(),
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setLoading(false);
    }
  }, [input, sessionId]);

  const handleKeyPress = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  }, [sendMessage]);

  const clearError = useCallback(() => {
    setError('');
  }, []);

  const startNewChat = useCallback(() => {
    setMessages([]);
    setSessionId(undefined);
    setFormSubmitted(false);
    
    const welcomeMessage: Message = {
      type: 'ai',
      content: `Hello ${user?.firstName}! I'm here to help you file a new complaint. Please describe the issue you're facing.`,
      timestamp: new Date(),
    };
    setMessages([welcomeMessage]);
  }, [user]);

  return (
    <div className="chat-container">
      <div className="chat-header">
        <h2>Cyber Crime Complaint Registration</h2>
        <div className="chat-info">
          {sessionId && <span className="session-id">Session: {sessionId}</span>}
          {user && <span className="user-name">User: {user.username}</span>}
          {formSubmitted && (
            <button onClick={startNewChat} className="new-chat-button">
              Start New Complaint
            </button>
          )}
        </div>
      </div>
      
      {error && (
        <div className="error-banner">
          {error}
          <button onClick={clearError} className="error-close">×</button>
        </div>
      )}
      
      <div className="chat-messages">
        {messages.map((message, index) => (
          <div
            key={index}
            className={`message ${message.type === 'user' ? 'user-message' : 'ai-message'}`}
          >
            <div className="message-content">{message.content}</div>
            <div className="message-time">
              {message.timestamp.toLocaleTimeString()}
            </div>
          </div>
        ))}
        
        {loading && (
          <div className="message ai-message">
            <div className="message-content typing">Typing...</div>
          </div>
        )}
        
        <div ref={messagesEndRef} />
      </div>

      <div className="chat-input">
        <textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyPress={handleKeyPress}
          placeholder={formSubmitted ? "Complaint submitted. Start a new chat for another complaint." : "Type your message here..."}
          rows={3}
          disabled={loading || formSubmitted}
        />
        <button 
          onClick={sendMessage} 
          disabled={loading || !input.trim() || formSubmitted}
        >
          Send
        </button>
      </div>
    </div>
  );
};

export default Chat;