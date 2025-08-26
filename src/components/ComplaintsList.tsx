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
    <div className="complaints-container">
      <h2>Submitted Complaints</h2>
      
      {selectedComplaint ? (
        <div className="complaint-details">
          <button onClick={closeDetails} className="back-button">← Back to list</button>
          <h3>Complaint #{selectedComplaint.grievanceNumber}</h3>
          
          <div className="detail-section">
            <h4>Basic Information</h4>
            <div className="detail-row">
              <div className="detail-label">Victim Name:</div>
              <div className="detail-value">{selectedComplaint.victimName || 'Not provided'}</div>
            </div>
            <div className="detail-row">
              <div className="detail-label">Contact:</div>
              <div className="detail-value">{selectedComplaint.victimContact || 'Not provided'}</div>
            </div>
            <div className="detail-row">
              <div className="detail-label">Complaint Type:</div>
              <div className="detail-value">{selectedComplaint.complaintType}</div>
            </div>
            <div className="detail-row">
              <div className="detail-label">Incident Date:</div>
              <div className="detail-value">{selectedComplaint.incidentDate || 'Not specified'}</div>
            </div>
            <div className="detail-row">
              <div className="detail-label">Location:</div>
              <div className="detail-value">{selectedComplaint.incidentLocation || 'Not specified'}</div>
            </div>
            <div className="detail-row">
              <div className="detail-label">Status:</div>
              <div className="detail-value status">{selectedComplaint.status}</div>
            </div>
            <div className="detail-row">
              <div className="detail-label">Submitted:</div>
              <div className="detail-value">{formatDate(selectedComplaint.createdAt)}</div>
            </div>
          </div>
          
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
                    <th>ID</th>
                    <th>Name</th>
                    <th>Type</th>
                    <th>Date</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {complaints.map((complaint) => (
                    <tr key={complaint.id} className={complaint.formSubmitted ? 'form-submitted' : ''}>
                      <td>{complaint.grievanceNumber}</td>
                      <td>{complaint.victimName || 'Unknown'}</td>
                      <td>{complaint.complaintType || complaint.status}</td>
                      <td>{formatDate(complaint.createdAt)}</td>
                      <td className={`status ${complaint.status.toLowerCase()}`}>{complaint.status}</td>
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