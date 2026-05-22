import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';
import { getPackages, createOrder, getBillingHistory, verifyPayment } from '../services/api';
export default function Billing() {
  const [packages, setPackages] = useState([]);
  const [billing, setBilling] = useState(null);
  const [loadingPkg, setLoadingPkg] = useState('');
  const { user, updateCredits } = useAuth();

  useEffect(() => {
    getPackages().then(({ data }) => setPackages(data)).catch(() => {});
    getBillingHistory().then(({ data }) => setBilling(data)).catch(() => {});
  }, []);

  const handleBuyCredits = async (pkg) => {
    setLoadingPkg(pkg.name);
    try {
      const { data: order } = await createOrder(pkg.name);

      const options = {
        key: order.keyId,
        amount: order.amount * 100,
        currency: order.currency,
        name: 'WanderAI',
        description: `${pkg.credits} AI Credits - ${pkg.name} Plan`,
        order_id: order.orderId,
        handler: async function (response)
        {
        try {
          const { data } = await verifyPayment({
            razorpayOrderId: order.orderId,
            razorpayPaymentId: response.razorpay_payment_id,
          });
          updateCredits(data.credits);
          toast.success(`${pkg.credits} credits added! 🎉`);
          getBillingHistory().then(({ d }) => setBilling(d));
        } catch {
          toast.error('Payment verification failed');
        }
      },
        prefill: { email: user?.email, name: user?.name },
        theme: { color: '#6366f1' },
        modal: {
          ondismiss: () => toast('Payment cancelled', { icon: 'ℹ️' })
        }
      };

      if (!window.Razorpay) {
        // Dynamically load Razorpay script
        await new Promise((resolve, reject) => {
          const script = document.createElement('script');
          script.src = 'https://checkout.razorpay.com/v1/checkout.js';
          script.onload = resolve;
          script.onerror = reject;
          document.body.appendChild(script);
        });
      }

      const rzp = new window.Razorpay(options);
      rzp.open();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to initiate payment');
    } finally {
      setLoadingPkg('');
    }
  };

  const sortedPackages = [...packages].sort((a, b) => a.price - b.price);

  return (
    <div>
      <div className="page-header">
        <h1>💳 Billing & Credits</h1>
        <p>Buy AI credits to generate travel itineraries</p>
      </div>

      {/* Current balance */}
      <div className="card" style={{ marginBottom: 28, display: 'flex', alignItems: 'center', gap: 20 }}>
        <div style={{ fontSize: 48 }}>⚡</div>
        <div>
          <div style={{ fontSize: 36, fontWeight: 800, color: 'var(--primary)' }}>{billing?.currentCredits ?? user?.creditsBalance ?? 0}</div>
          <div style={{ color: 'var(--text-muted)' }}>Credits remaining · Each credit = 1 AI itinerary</div>
        </div>
      </div>

      {/* Pricing */}
      <h2 style={{ marginBottom: 16, fontWeight: 700 }}>Buy Credits</h2>
      <div className="pricing-grid">
        {sortedPackages.map((pkg, i) => (
          <div className={`pricing-card ${i === 1 ? 'featured' : ''}`} key={pkg.name}>
            {i === 1 && <div style={{ fontSize: 12, fontWeight: 700, marginBottom: 8, opacity: 0.85 }}>⭐ MOST POPULAR</div>}
            <div className="package-name">{pkg.name}</div>
            <div className="price-amount">
              <span className="price-currency">₹</span>{pkg.price}
            </div>
            <div className="credits-count">✨ {pkg.credits} AI Credits</div>
            <div className="pkg-desc">{pkg.description}</div>
            <button
              className={`btn ${i === 1 ? 'btn-secondary' : 'btn-primary'} btn-full`}
              style={i === 1 ? { background: 'white', color: 'var(--primary)' } : {}}
              onClick={() => handleBuyCredits(pkg)}
              disabled={loadingPkg === pkg.name}
            >
              {loadingPkg === pkg.name ? 'Processing...' : 'Buy Now'}
            </button>
          </div>
        ))}
      </div>

      {/* Transaction history */}
      <div className="card">
        <div className="card-header"><h2>Transaction History</h2></div>
        {!billing?.transactions?.length ? (
          <div className="empty-state" style={{ padding: 24 }}>
            <p>No transactions yet. Buy your first credits above!</p>
          </div>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Package</th>
                  <th>Credits</th>
                  <th>Amount</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {billing.transactions.map(t => (
                  <tr key={t.id}>
                    <td>{new Date(t.createdAt).toLocaleDateString()}</td>
                    <td>{t.packageName}</td>
                    <td>+{t.creditsAdded}</td>
                    <td>₹{t.amount}</td>
                    <td><span className={`status-chip chip-${t.status}`}>{t.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
