import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

// Create axios instance with default config
const api = axios.create({
  baseURL: API_BASE_URL,
  withCredentials: true, // Important for session-based auth
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000, // Increased timeout
});

// Add response interceptor for better error handling
api.interceptors.response.use(
  (response) => {
    console.log(`✅ ${response.config.method?.toUpperCase()} ${response.config.url} - ${response.status}`);
    return response;
  },
  (error) => {
    if (error.code === 'ERR_NETWORK') {
      console.error('❌ Network Error: Likely CORS/preflight blocked or backend not running');
      console.error('Please check if backend is running on http://localhost:8080');
      console.error('Check Spring Security CORS configuration and that preflight OPTIONS requests are allowed');
    } else if (error.response) {
      console.error(`❌ API Error: ${error.response.status} - ${error.response.data}`);
    } else if (error.request) {
      console.error('❌ Request Error: No response received', error.request);
    } else {
      console.error('❌ Error:', error.message);
    }
    
    return Promise.reject(error);
  }
);

// Test connectivity function
export const testConnection = async () => {
  try {
    const response = await axios.get('http://localhost:8080/health', { timeout: 5000 });
    return { success: true, message: response.data };
  } catch (error) {
    return { success: false, error };
  }
};

// Auth API
export const authAPI = {
  register: (userData: {
    username: string;
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    phoneNumber: string;
  }) => api.post('/auth/register', userData),
  
  login: (credentials: { username: string; password: string }) =>
    api.post('/auth/login', credentials),
  
  logout: () => api.post('/auth/logout'),
  
  getProfile: () => api.get('/auth/profile'),
  
  getStatus: () => api.get('/auth/status'),
};

// Chat API
export const chatAPI = {
  sendMessage: (message: { message: string; sessionId?: string }) => // Change to string
    api.post('/chat/message', message),
  
  getSessions: () => api.get('/chat/sessions'),
  
  test: () => api.get('/chat/test'),
  
  submitEmergency: (data: { details: string }) => {
    console.log('🚨 Submitting emergency with data:', data);
    return api.post('/chat/emergency', data);
  },
};

// Complaints API functions
export const fetchComplaints = async (): Promise<any[]> => {
  try {
    console.log('🔄 Fetching complaints...');
    const response = await api.get('/grievance/list');
    console.log('✅ Complaints fetched successfully:', response.data);
    return response.data;
  } catch (error: any) {
    console.error('❌ Error fetching complaints:', error);
    
    if (error.response?.status === 401) {
      console.error('Authentication failed - redirecting to login');
      throw new Error('Authentication failed. Please login again.');
    } else if (error.response?.status === 403) {
      throw new Error('Access denied.');
    } else if (error.response?.status === 500) {
      throw new Error('Server error. Please try again later.');
    }
    
    throw new Error(error.response?.data?.message || 'Failed to fetch complaints');
  }
};

export const getComplaintDetails = async (id: number): Promise<any> => {
  try {
    console.log(`🔄 Fetching complaint details for ID: ${id}`);
    const response = await api.get(`/grievance/${id}`);
    console.log('✅ Complaint details fetched successfully:', response.data);
    return response.data;
  } catch (error: any) {
    console.error('❌ Error fetching complaint details:', error);
    throw new Error(error.response?.data?.message || 'Failed to fetch complaint details');
  }
};

export default api;