import React, { useState, useEffect } from 'react';

export function ProductCard({ product, stock, onBuyClick, loading, token }) {
  const [timeLeft, setTimeLeft] = useState(120); // 2-minute mock countdown

  useEffect(() => {
    if (timeLeft <= 0) return;
    const timer = setInterval(() => {
      setTimeLeft((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [timeLeft]);

  const formatTime = (secs) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  };

  const isSoldOut = stock <= 0;

  return (
    <div className="max-w-md mx-auto bg-slate-800 border border-slate-700 rounded-xl overflow-hidden shadow-2xl p-6">
      {/* Sale Banner */}
      <div className="flex justify-between items-center mb-4">
        <span className="bg-red-600 text-white text-xs font-bold uppercase px-3 py-1 rounded-full animate-pulse">
          Live Flash Sale
        </span>
        <span className="text-yellow-400 font-mono text-sm font-semibold">
          Ends in: {formatTime(timeLeft)}
        </span>
      </div>

      {/* Product Image Placeholder */}
      <div className="w-full h-48 bg-slate-700 rounded-lg flex items-center justify-center mb-4 text-4xl">
        📱
      </div>

      {/* Product Details */}
      <h2 className="text-xl font-bold text-white mb-1">{product.name || 'Flagship Smartphone Pro'}</h2>
      <p className="text-slate-400 text-sm mb-4">{product.description || 'Exclusive release. Strict limit: 1 per customer.'}</p>

      {/* Pricing */}
      <div className="flex items-baseline space-x-3 mb-4">
        <span className="text-3xl font-extrabold text-green-400">${product.price || '499.99'}</span>
        <span className="text-slate-500 line-through text-lg">$999.99</span>
        <span className="text-red-400 text-xs font-bold">50% OFF</span>
      </div>

      {/* Stock Bar */}
      <div className="mb-6">
        <div className="flex justify-between text-xs font-semibold mb-1">
          <span className="text-slate-300">Remaining Stock:</span>
          <span className={stock > 2 ? "text-green-400" : "text-red-400"}>
            {stock} units left
          </span>
        </div>
        <div className="w-full bg-slate-700 h-2.5 rounded-full overflow-hidden">
          <div 
            className={`h-full transition-all duration-500 ${stock > 2 ? 'bg-green-500' : 'bg-red-500'}`}
            style={{ width: `${Math.min(100, (stock / 10) * 100)}%` }}
          ></div>
        </div>
      </div>

      {/* Purchase Action */}
      <button
        onClick={onBuyClick}
        disabled={isSoldOut || loading || !token}
        className={`w-full py-3 px-4 rounded-lg font-bold text-white transition-all duration-200 ${
          isSoldOut
            ? 'bg-slate-600 cursor-not-allowed text-slate-400'
            : !token
            ? 'bg-blue-600 hover:bg-blue-700'
            : 'bg-gradient-to-r from-red-600 to-orange-500 hover:from-red-500 hover:to-orange-400 shadow-lg shadow-red-900/40'
        }`}
      >
        {isSoldOut ? 'Sold Out' : !token ? 'Login to Buy' : loading ? 'Reserving Stock...' : 'Grab Deal Now!'}
      </button>
    </div>
  );
}
