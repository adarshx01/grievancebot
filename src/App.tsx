import React, { useState, useCallback, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import Login from './components/Login';
import Register from './components/Register';
import Chat from './components/Chat';
import ComplaintsList from './components/ComplaintsList';
import { testConnection } from './services/api';
import './App.css';

const ConnectionStatus: React.FC = () => {
  const [connectionStatus, setConnectionStatus] = useState<'checking' | 'connected' | 'disconnected'>('checking');
  const [statusMessage, setStatusMessage] = useState('');

  useEffect(() => {
    const checkConnection = async () => {
      const result = await testConnection();
      if (result.success) {
        setConnectionStatus('connected');
        setStatusMessage('Backend connected successfully');
      } else {
        setConnectionStatus('disconnected');
        setStatusMessage('Cannot connect to backend. Please ensure it\'s running on http://localhost:8080');
      }
    };

    checkConnection();
    // Check connection every 30 seconds
    const interval = setInterval(checkConnection, 30000);
    return () => clearInterval(interval);
  }, []);

  if (connectionStatus === 'checking') {
    return <div className="connection-status checking">Checking backend connection...</div>;
  }

  if (connectionStatus === 'disconnected') {
    return (
      <div className="connection-status disconnected">
        ❌ {statusMessage}
        <button onClick={() => window.location.reload()}>Retry</button>
      </div>
    );
  }

  return <div className="connection-status connected">✅ {statusMessage}</div>;
};

const AppContent: React.FC = () => {
  const { user, isAuthenticated, logout, loading } = useAuth();
  const [showRegister, setShowRegister] = useState(false);
  const [activeTab, setActiveTab] = useState<'chat' | 'complaints'>('chat');

  const toggleToRegister = useCallback(() => {
    setShowRegister(true);
  }, []);

  const toggleToLogin = useCallback(() => {
    setShowRegister(false);
  }, []);

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  if (!isAuthenticated) {
    return (
      <div className="auth-container">
        <ConnectionStatus />
        {showRegister ? (
          <div>
            <Register />
            <p>
              Already have an account?{' '}
              <button 
                className="link-button"
                onClick={toggleToLogin}
              >
                Login here
              </button>
            </p>
          </div>
        ) : (
          <div>
            <Login />
            <p>
              Don't have an account?{' '}
              <button 
                className="link-button"
                onClick={toggleToRegister}
              >
                Register here
              </button>
            </p>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>Cyber Crime Complaint System</h1>
        <div className="user-info">
          <span>Welcome, {user?.firstName} {user?.lastName}</span>
          <button onClick={logout} className="logout-button">
            Logout
          </button>
        </div>
      </header>
      
      <ConnectionStatus />
      
      <div className="app-tabs">
        <button 
          className={`tab-button ${activeTab === 'chat' ? 'active' : ''}`}
          onClick={() => setActiveTab('chat')}
        >
          File New Complaint
        </button>
        <button 
          className={`tab-button ${activeTab === 'complaints' ? 'active' : ''}`}
          onClick={() => setActiveTab('complaints')}
        >
          View My Complaints
        </button>
      </div>
      
      <main className="app-main">
        {activeTab === 'chat' ? <Chat /> : <ComplaintsList />}
      </main>
    </div>
  );
};

function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}

export default App;
