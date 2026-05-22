import axios from 'axios';

const API = axios.create({
  baseURL: process.env.REACT_APP_API_URL 
    ? `${process.env.REACT_APP_API_URL}/api`
    : '/api',
});

API.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

API.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.clear();
      window.location.href = '/login';
    }
    return Promise.reject(err);
  }
);
export const verifyPayment = (data) => API.post('/payments/verify-payment', data);

export const register = (data) => API.post('/auth/register', data);
export const login = (data) => API.post('/auth/login', data);
export const forgotPassword = (email) => API.post('/auth/forgot-password', { email });
export const resetPassword = (data) => API.post('/auth/reset-password', data);
export const generateTrip = (data) => API.post('/trips/generate', data);
export const getTrips = () => API.get('/trips');
export const getTripById = (id) => API.get(`/trips/${id}`);
export const deleteTrip = (id) => API.delete(`/trips/${id}`);
export const getPackages = () => API.get('/payments/packages');
export const createOrder = (packageName) => API.post('/payments/create-order', { packageName });
export const getBillingHistory = () => API.get('/payments/billing-history');

export default API;
