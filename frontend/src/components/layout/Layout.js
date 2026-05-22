import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const navItems = [
  { label: 'Dashboard', icon: '🗺️', path: '/dashboard' },
  { label: 'Plan a Trip', icon: '✨', path: '/plan' },
  { label: 'Billing', icon: '💳', path: '/billing' },
];

export default function Layout({ children }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-logo">
          <h2>🌍 WanderAI</h2>
          <span>AI Travel Planner</span>
        </div>
        <nav className="sidebar-nav">
          {navItems.map((item) => (
            <button
              key={item.path}
              className={`nav-item ${location.pathname === item.path ? 'active' : ''}`}
              onClick={() => navigate(item.path)}
            >
              <span>{item.icon}</span>
              {item.label}
            </button>
          ))}
          <button className="nav-item" onClick={logout} style={{ marginTop: 'auto', color: '#ef4444' }}>
            <span>🚪</span> Logout
          </button>
        </nav>
        <div className="sidebar-credits">
          <div className="credits-badge">
            <div className="count">{user?.creditsBalance ?? 0}</div>
            <div className="label">AI Credits Left</div>
          </div>
          <button className="btn btn-primary btn-full" style={{ marginTop: 10 }} onClick={() => navigate('/billing')}>
            Buy Credits
          </button>
        </div>
      </aside>
      <main className="main-content">{children}</main>
    </div>
  );
}
