import React, { useEffect, useState } from 'react';
import { apiCall } from '../api';

export function OrderStatusTracker({ orderReference, onCompleted, onClose }) {
  const [status, setStatus] = useState('PENDING');
  const [attempts, setAttempts] = useState(0);

  useEffect(() => {
    if (!orderReference || status === 'PAID' || status === 'CANCELLED') return;

    const interval = setInterval(async () => {
      try {
        setAttempts((prev) => prev + 1);
        //const res = await apiCall(`/api/orders/reference/${orderReference}`);
        const res = await apiCall(`/api/orders/${orderReference}`);
        if (res.data && res.data.status) {
          setStatus(res.data.status);
          if (res.data.status === 'PAID') {
            clearInterval(interval);
            if (onCompleted) onCompleted();
          }
        }
      } catch (err) {
        console.error('Error polling order state:', err);
      }
    }, 1500);

    return () => clearInterval(interval);
  }, [orderReference, status]);

  return (
    <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
      <div className="bg-slate-800 border border-slate-700 rounded-xl p-6 max-w-sm w-full text-center">
        <h3 className="text-lg font-bold mb-2">Order Verification</h3>
        <p className="text-xs text-slate-400 font-mono mb-4">{orderReference}</p>

        {status === 'PENDING' && (
          <div className="py-4">
            <div className="w-10 h-10 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
            <p className="text-sm font-semibold text-blue-400">Processing Payment Saga...</p>
            <p className="text-xs text-slate-500 mt-1">Waiting for Kafka event confirmation (attempt {attempts})</p>
          </div>
        )}

        {status === 'PAID' && (
          <div className="py-4">
            <div className="w-12 h-12 bg-green-500/20 text-green-400 rounded-full flex items-center justify-center text-2xl mx-auto mb-3">
              ✓
            </div>
            <p className="text-base font-bold text-green-400">Order Paid Successfully!</p>
            <p className="text-xs text-slate-400 mt-1">Stock permanently deducted. Confirmation email dispatched.</p>
          </div>
        )}

        <button
          onClick={onClose}
          className="mt-4 w-full bg-slate-700 hover:bg-slate-600 text-white text-sm py-2 rounded-lg font-medium transition"
        >
          Close
        </button>
      </div>
    </div>
  );
}
