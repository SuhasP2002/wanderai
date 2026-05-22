import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Bar } from 'react-chartjs-2';
import { Chart as ChartJS, CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend } from 'chart.js';
import { getTrips, deleteTrip } from '../services/api';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend);

export default function Dashboard() {
  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(true);
  const { user } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    getTrips()
      .then(({ data }) => setTrips(data))
      .catch(() => toast.error('Failed to load trips'))
      .finally(() => setLoading(false));
  }, []);

  const handleDelete = async (e, id) => {
    e.preventDefault();
    e.stopPropagation();
    if (!window.confirm('Delete this trip?')) return;
    try {
      await deleteTrip(id);
      setTrips(trips.filter(t => t.id !== id));
      toast.success('Trip deleted');
    } catch { toast.error('Failed to delete trip'); }
  };

  const chartData = {
    labels: trips.slice(0, 6).map(t => t.destination),
    datasets: [{
      label: 'Budget (Currency)',
      data: trips.slice(0, 6).map(t => t.budget),
      backgroundColor: '#6366f1aa',
      borderColor: '#6366f1',
      borderWidth: 2,
      borderRadius: 6,
    }]
  };

  if (loading) return <div className="loading-screen"><div className="spinner" /></div>;

  return (
    <div>
      <div className="page-header">
        <h1>Welcome back, {user?.name?.split(' ')[0]} 👋</h1>
        <p>Here are all your AI-generated travel plans</p>
      </div>

      {/* Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 20, marginBottom: 28 }}>
        {[
          { label: 'Total Trips', value: trips.length, icon: '🗺️' },
          { label: 'AI Credits Left', value: user?.creditsBalance, icon: '⚡' },
          { label: 'Destinations', value: new Set(trips.map(t => t.destination)).size, icon: '📍' },
        ].map(s => (
          <div className="card" key={s.label} style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 32, marginBottom: 8 }}>{s.icon}</div>
            <div style={{ fontSize: 32, fontWeight: 800, color: 'var(--primary)' }}>{s.value}</div>
            <div style={{ color: 'var(--text-muted)', fontSize: 14 }}>{s.label}</div>
          </div>
        ))}
      </div>

      {/* Chart */}
      {trips.length > 0 && (
        <div className="card" style={{ marginBottom: 28 }}>
          <div className="card-header"><h2>Budget Overview</h2></div>
          <Bar data={chartData} options={{ responsive: true, plugins: { legend: { display: false } } }} height={80} />
        </div>
      )}

      {/* Trips grid */}
      <div className="card-header">
        <h2>My Trips</h2>
        <button className="btn btn-primary" onClick={() => navigate('/plan')}>+ Plan New Trip</button>
      </div>

      {trips.length === 0 ? (
        <div className="empty-state">
          <div style={{ fontSize: 48, marginBottom: 12 }}>✈️</div>
          <h3>No trips yet</h3>
          <p>Plan your first AI-powered trip!</p>
          <button className="btn btn-primary" style={{ marginTop: 16 }} onClick={() => navigate('/plan')}>
            Plan a Trip
          </button>
        </div>
      ) : (
        <div className="trips-grid" style={{ marginTop: 16 }}>
          {trips.map(trip => (
            <Link to={`/trips/${trip.id}`} className="trip-card" key={trip.id}>
              <div className="trip-card-header">
                <div className="trip-destination">📍 {trip.destination}</div>
                <span className={`trip-status status-${trip.status}`}>{trip.status}</span>
              </div>
              <div className="trip-meta">
                <span>⏱ {trip.durationDays} days</span>
                <span>💰 {trip.budget} {trip.currency}</span>
              </div>
              <div style={{ marginTop: 12, display: 'flex', justifyContent: 'flex-end' }}>
                <button className="btn btn-danger" style={{ fontSize: 12, padding: '4px 12px' }}
                  onClick={(e) => handleDelete(e, trip.id)}>Delete</button>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
}
