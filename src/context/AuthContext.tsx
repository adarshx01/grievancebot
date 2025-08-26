import React, { createContext, useContext, useState, useEffect, ReactNode, useCallback } from 'react';
import { authAPI } from '../services/api';

interface User {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  role: string;
}

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (userData: any) => Promise<void>;
  logout: () => Promise<void>;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  // Memoize the checkAuth function to prevent recreating it on every render
  const checkAuth = useCallback(async () => {
    try {
      console.log('Checking authentication status...');
      const response = await authAPI.getProfile();
      console.log('User profile:', response.data);
      setUser(response.data);
      return true;
    } catch (error: any) {
      console.log('Not authenticated:', error.response?.status);
      setUser(null);
      return false;
    }
  }, []); // Empty dependency array since this doesn't depend on any props or state

  // Only run once on mount
  useEffect(() => {
    let isMounted = true;
    
    const checkAuthStatus = async () => {
      try {
        setLoading(true);
        if (isMounted) {
          await checkAuth();
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    checkAuthStatus();

    // Cleanup function
    return () => {
      isMounted = false;
    };
  }, [checkAuth]); // Only depend on the memoized checkAuth function

  const login = useCallback(async (username: string, password: string) => {
    try {
      console.log('Attempting login for:', username);
      const response = await authAPI.login({ username, password });
      console.log('Login response:', response.status);
      
      // Give a moment for the session to be established
      await new Promise(resolve => setTimeout(resolve, 200));
      
      // Now check authentication status
      const authSuccess = await checkAuth();
      if (!authSuccess) {
        throw new Error('Authentication verification failed');
      }
      
    } catch (error: any) {
      console.error('Login failed:', error.response?.data);
      throw new Error(error.response?.data || 'Login failed');
    }
  }, [checkAuth]);

  const register = useCallback(async (userData: any) => {
    try {
      console.log('Attempting registration for:', userData.username);
      await authAPI.register(userData);
      console.log('Registration successful');
    } catch (error: any) {
      console.error('Registration failed:', error.response?.data);
      throw new Error(error.response?.data || 'Registration failed');
    }
  }, []);

  const logout = useCallback(async () => {
    try {
      await authAPI.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      setUser(null);
    }
  }, []);

  // Memoize the context value to prevent unnecessary re-renders
  const value = React.useMemo(() => ({
    user,
    isAuthenticated: !!user,
    login,
    register,
    logout,
    loading,
  }), [user, login, register, logout, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};