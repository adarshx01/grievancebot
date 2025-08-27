// filepath: /home/adarsh/Desktop/GrievanceRedressal/frontend/src/components/Chat.tsx
import React, { useState, useEffect, useRef, useCallback } from 'react';
import { chatAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';

interface Message {
  type: 'user' | 'ai';
  content: string;
  timestamp: Date;
}

interface ChatResponse {
  sessionId: string; // Change from number to string for UUID
  userMessage: string;
  aiResponse: string;
  category: string;
  grievanceId?: string; // Change from number to string for UUID
  grievanceNumber?: string;
  submitForm?: boolean;
  mandatoryInfoQueried?: boolean;
  formData?: string;
}

const Chat: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [sessionId, setSessionId] = useState<string | undefined>(); // Change to string
  const [error, setError] = useState<string>('');
  const [welcomeMessageAdded, setWelcomeMessageAdded] = useState(false);
  const [formSubmitted, setFormSubmitted] = useState(false);
  const [showEmergencyModal, setShowEmergencyModal] = useState(false);
  const [emergencyDetails, setEmergencyDetails] = useState('');
  const [emergencySubmitting, setEmergencySubmitting] = useState(false);
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

  const openEmergencyModal = useCallback(() => {
    setShowEmergencyModal(true);
  }, []);

  const closeEmergencyModal = useCallback(() => {
    setShowEmergencyModal(false);
    setEmergencyDetails('');
  }, []);

  const submitEmergency = useCallback(async () => {
    if (!emergencyDetails.trim()) return;
    
    setLoading(true);
    setShowEmergencyModal(false);
    
    try {
      const response = await chatAPI.submitEmergency({
        details: emergencyDetails
      });
      
      const data = response.data;
      
      // Add messages to the chat
      const userMessage: Message = {
        type: 'user',
        content: `🚨 EMERGENCY: ${emergencyDetails}`,
        timestamp: new Date(),
      };
      
      const systemMessage: Message = {
        type: 'ai',
        content: `⚠️ EMERGENCY SUBMITTED!\nYour emergency has been reported with reference ID: ${data.grievanceNumber}.\nAuthorities will be contacted immediately.`,
        timestamp: new Date(),
      };
      
      setMessages(prev => [...prev, userMessage, systemMessage]);
      setEmergencyDetails('');
      
    } catch (error: any) {
      console.error('Error submitting emergency:', error);
      
      let errorMsg = 'Failed to submit emergency. Please try again or call emergency services directly.';
      
      if (error.response?.status === 401) {
        errorMsg = 'Your session has expired. Please refresh the page and log in again.';
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
  }, [emergencyDetails]);

  const handleEmergencySubmit = async () => {
    if (!emergencyDetails.trim()) return;
    
    try {
      setEmergencySubmitting(true);
      console.log('🚨 Submitting emergency:', emergencyDetails);
      
      const response = await chatAPI.submitEmergency({ details: emergencyDetails });
      console.log('✅ Emergency submitted successfully:', response.data);
      
      if (response.data.success) {
        // Add confirmation message to chat
        const confirmationMessage: Message = {
          content: `🚨 EMERGENCY SUBMITTED SUCCESSFULLY\n\nGrievance Number: ${response.data.grievanceNumber}\nGrievance ID: ${response.data.grievanceId}\n\nYour emergency has been recorded and authorities will be contacted immediately. Please keep this reference number for future communication.`,
          isUser: false,
          timestamp: new Date(),
          isEmergency: true
        };
        
        setMessages(prev => [...prev, confirmationMessage]);
        setEmergencyDetails('');
        setShowEmergencyModal(false);
        
        // Show success alert
        alert(`Emergency submitted successfully!\nGrievance Number: ${response.data.grievanceNumber}`);
      } else {
        throw new Error(response.data.message || 'Emergency submission failed');
      }
      
    } catch (error: any) {
      console.error('❌ Error submitting emergency:', error);
      
      let errorMessage = 'Failed to submit emergency. Please try again.';
      if (error.response?.data?.message) {
        errorMessage = error.response.data.message;
      } else if (error.message) {
        errorMessage = error.message;
      }
      
      setError(errorMessage);
      alert(`Emergency submission failed: ${errorMessage}`);
    } finally {
      setEmergencySubmitting(false);
    }
  };

  return (
    <div className="chat-container">
      <div className="chat-header">
        <h2>Cyber Crime Complaint Registration</h2>
        <div className="chat-info">
          {sessionId && <span className="session-id">Session: {sessionId}</span>}
          {user && <span className="user-name">User: {user.username}</span>}
          <div className="header-buttons">
            {formSubmitted && (
              <button onClick={startNewChat} className="new-chat-button">
                Start New Complaint
              </button>
            )}
            <button 
              onClick={openEmergencyModal} 
              className="emergency-button"
              disabled={loading}
            >
              🚨 Emergency
            </button>
          </div>
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

      {/* Emergency Modal */}
      {showEmergencyModal && (
        <div className="modal-overlay">
          <div className="modal-content emergency-modal">
            <h3>🚨 Submit Emergency Report</h3>
            <p>Use this only for urgent situations requiring immediate attention.</p>
            <textarea
              value={emergencyDetails}
              onChange={(e) => setEmergencyDetails(e.target.value)}
              placeholder="Briefly describe the emergency situation..."
              rows={4}
            />
            <div className="modal-buttons">
              <button onClick={closeEmergencyModal} className="cancel-button">
                Cancel
              </button>
              <button 
                onClick={submitEmergency} 
                className="submit-emergency-button"
                disabled={!emergencyDetails.trim()}
              >
                Submit Emergency
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Chat;