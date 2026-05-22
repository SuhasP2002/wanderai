import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { generateTrip } from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function PlanTrip() {
  const [form, setForm] = useState({
    destination: '', durationDays: 5, budget: '', currency: 'INR',
    travelStyle: 'balanced', interests: '', accommodationType: 'hotel', specialRequests: '',
  });
  const [loading, setLoading] = useState(false);
  const { user, updateCredits } = useAuth();
  const navigate = useNavigate();

  const set = (key, val) => setForm(f => ({ ...f, [key]: val }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (user?.creditsBalance < 1) {
      toast.error('No credits left! Please buy more credits.');
      navigate('/billing');
      return;
    }
    setLoading(true);
    try {
      const { data } = await generateTrip(form);
      updateCredits(user.creditsBalance - 1);
      toast.success('Itinerary generated! 🎉');
      navigate(`/trips/${data.id}`);
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to generate itinerary');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <h1>✨ Plan a New Trip</h1>
        <p>Tell our AI your preferences and get a personalized itinerary instantly</p>
      </div>

      {user?.creditsBalance < 1 && (
        <div className="alert alert-error">
          You have no credits left. <a href="/billing" style={{ color: 'inherit', fontWeight: 700 }}>Buy more credits →</a>
        </div>
      )}

      <div className="card plan-form">
        {loading && (
          <div className="generating-banner">
            <div className="spinner" style={{ borderTopColor: 'white' }} />
            <div>
              <div style={{ fontWeight: 700, fontSize: 16 }}>AI is building your itinerary...</div>
              <div style={{ fontSize: 13, opacity: 0.9 }}>This usually takes 10-20 seconds</div>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <p className="form-section-title">🗺️ Destination & Duration</p>
          <div className="form-row">
            <div className="form-group">
              <label>Destination *</label>
              <input placeholder="e.g. Bali, Tokyo, Paris" value={form.destination}
                onChange={e => set('destination', e.target.value)} required />
            </div>
            <div className="form-group">
              <label>Duration (days) *</label>
              <input type="number" min={1} max={30} value={form.durationDays}
                onChange={e => set('durationDays', parseInt(e.target.value))} required />
            </div>
          </div>

          <p className="form-section-title">💰 Budget</p>
          <div className="form-row">
            <div className="form-group">
              <label>Total Budget *</label>
              <input type="number" min={1} placeholder="e.g. 50000" value={form.budget}
                onChange={e => set('budget', e.target.value)} required />
            </div>
            <div className="form-group">
              <label>Currency</label>
              <select value={form.currency} onChange={e => set('currency', e.target.value)}>
                <option value="INR">INR (₹)</option>
                <option value="USD">USD ($)</option>
                <option value="EUR">EUR (€)</option>
                <option value="GBP">GBP (£)</option>
              </select>
            </div>
          </div>

          <p className="form-section-title">🎨 Preferences</p>
          <div className="form-row">
            <div className="form-group">
              <label>Travel Style</label>
              <select value={form.travelStyle} onChange={e => set('travelStyle', e.target.value)}>
                <option value="budget">Budget / Backpacker</option>
                <option value="balanced">Balanced</option>
                <option value="luxury">Luxury</option>
                <option value="family">Family-friendly</option>
                <option value="adventure">Adventure</option>
              </select>
            </div>
            <div className="form-group">
              <label>Accommodation</label>
              <select value={form.accommodationType} onChange={e => set('accommodationType', e.target.value)}>
                <option value="hostel">Hostel</option>
                <option value="hotel">Hotel</option>
                <option value="airbnb">Airbnb</option>
                <option value="resort">Resort</option>
              </select>
            </div>
          </div>

          <div className="form-group">
            <label>Interests</label>
            <input placeholder="e.g. food, museums, beaches, hiking, nightlife" value={form.interests}
              onChange={e => set('interests', e.target.value)} />
          </div>

          <div className="form-group">
            <label>Special Requests</label>
            <textarea placeholder="Any dietary restrictions, accessibility needs, must-see places..." value={form.specialRequests}
              onChange={e => set('specialRequests', e.target.value)} />
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <button className="btn btn-primary" type="submit" disabled={loading || user?.creditsBalance < 1}>
              {loading ? 'Generating...' : '✨ Generate Itinerary (1 credit)'}
            </button>
            <span style={{ color: 'var(--text-muted)', fontSize: 14 }}>
              You have {user?.creditsBalance} credit{user?.creditsBalance !== 1 ? 's' : ''} left
            </span>
          </div>
        </form>
      </div>
    </div>
  );
}
