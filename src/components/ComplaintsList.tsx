import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

interface Complaint {
  id: number;
  grievanceNumber: string;
  victimName: string;
  victimContact: string;
  complaintType: string;
  incidentDate: string;
  incidentLocation: string;
  status: string;
  createdAt: string;
  formSubmitted: boolean;
  emergency: boolean;
}

interface ComplaintDetail extends Complaint {
  formDataJson: string;
  userMessage: string;
  aiResponse: string;
  incidentSummary: string;
}

const ComplaintsList: React.FC = () => {
  const [complaints, setComplaints] = useState<Complaint[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedComplaint, setSelectedComplaint] = useState<ComplaintDetail | null>(null);
  const { isAuthenticated } = useAuth();

  useEffect(() => {
    if (isAuthenticated) {
      fetchComplaints();
    }
  }, [isAuthenticated]);

  const fetchComplaints = async () => {
    try {
      setLoading(true);
      const response = await api.get('/grievance/list');
      setComplaints(response.data);
      setError(null);
    } catch (err: any) {
      setError('Failed to load complaints: ' + (err.message || 'Unknown error'));
      console.error('Error fetching complaints:', err);
    } finally {
      setLoading(false);
    }
  };

  const viewComplaintDetails = async (id: number) => {
    try {
      const response = await api.get(`/grievance/${id}`);
      setSelectedComplaint(response.data);
    } catch (err: any) {
      setError('Failed to load complaint details');
      console.error('Error fetching complaint details:', err);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString();
  };

  const closeDetails = () => {
    setSelectedComplaint(null);
  };

  if (!isAuthenticated) {
    return <div>Please log in to view complaints</div>;
  }

  if (loading) {
    return <div className="loading">Loading complaints...</div>;
  }

  if (error) {
    return <div className="error">{error}</div>;
  }

  return (
    <div className="complaints-list-container">
      <h2>My Complaints</h2>
      
      {selectedComplaint ? (
        <div className="complaint-details">
          <button onClick={closeDetails} className="back-button">← Back to List</button>
          <h3>Complaint Details</h3>
          <div className="detail-section">
            <h4>Grievance Number</h4>
            <div className="detail-text">{selectedComplaint.grievanceNumber}</div>
          </div>
          
          <div className="detail-section">
            <h4>Status</h4>
            <div className={`detail-text status ${selectedComplaint.status.toLowerCase()}`}>
              {selectedComplaint.status}
              {selectedComplaint.emergency && <span className="emergency-badge">EMERGENCY</span>}
            </div>
          </div>
          
          <div className="detail-section">
            <h4>Category</h4>
            <div className="detail-text">{selectedComplaint.category}</div>
          </div>
          
          <div className="detail-section">
            <h4>Priority</h4>
            <div className="detail-text">{selectedComplaint.priority}</div>
          </div>
          
          {selectedComplaint.victimName && (
            <div className="detail-section">
              <h4>Victim Name</h4>
              <div className="detail-text">{selectedComplaint.victimName}</div>
            </div>
          )}
          
          {selectedComplaint.victimContact && (
            <div className="detail-section">
              <h4>Contact</h4>
              <div className="detail-text">{selectedComplaint.victimContact}</div>
            </div>
          )}
          
          {selectedComplaint.incidentDate && (
            <div className="detail-section">
              <h4>Incident Date</h4>
              <div className="detail-text">{selectedComplaint.incidentDate}</div>
            </div>
          )}
          
          {selectedComplaint.incidentLocation && (
            <div className="detail-section">
              <h4>Incident Location</h4>
              <div className="detail-text">{selectedComplaint.incidentLocation}</div>
            </div>
          )}
          
          {selectedComplaint.incidentSummary && (
            <div className="detail-section">
              <h4>Incident Summary</h4>
              <div className="detail-text">{selectedComplaint.incidentSummary}</div>
            </div>
          )}
          
          {selectedComplaint.formSubmitted && selectedComplaint.formDataJson && (
            <div className="detail-section">
              <h4>Complete Form Data</h4>
              <div className="detail-json">
                <pre>{JSON.stringify(JSON.parse(selectedComplaint.formDataJson), null, 2)}</pre>
              </div>
            </div>
          )}
          
          <div className="detail-section">
            <h4>Conversation History</h4>
            <div className="detail-text conversation-history">
              <pre>{selectedComplaint.userMessage}</pre>
            </div>
          </div>
        </div>
      ) : (
        <>
          {complaints.length === 0 ? (
            <div className="no-complaints">No complaints found.</div>
          ) : (
            <div className="complaints-list">
              <table>
                <thead>
                  <tr>
                    <th>Grievance Number</th>
                    <th>Victim Name</th>
                    <th>Type</th>
                    <th>Date</th>
                    <th>Status</th>
                    <th>Form Submitted</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {complaints.map((complaint) => (
                    <tr 
                      key={complaint.id} 
                      className={`
                        ${complaint.formSubmitted ? 'form-submitted' : 'form-pending'}
                        ${complaint.emergency ? 'emergency' : ''}
                      `}
                    >
                      <td>{complaint.grievanceNumber}</td>
                      <td>{complaint.victimName || 'Unknown'}</td>
                      <td>
                        {complaint.complaintType || complaint.category}
                        {complaint.emergency && <span className="emergency-badge">EMERGENCY</span>}
                      </td>
                      <td>{formatDate(complaint.createdAt)}</td>
                      <td className={`status ${complaint.status.toLowerCase()}`}>{complaint.status}</td>
                      <td>
                        {complaint.formSubmitted ? (
                          <span className="form-status submitted">✅ Complete</span>
                        ) : (
                          <span className="form-status pending">⏳ In Progress</span>
                        )}
                      </td>
                      <td>
                        <button 
                          onClick={() => viewComplaintDetails(complaint.id)}
                          className="view-button"
                        >
                          View
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          
          <button onClick={fetchComplaints} className="refresh-button">
            Refresh
          </button>
        </>
      )}
    </div>
  );
};

export default ComplaintsList;