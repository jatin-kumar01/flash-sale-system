// import React, { useState, useEffect } from 'react';
// import { apiCall, getToken, setToken, clearToken } from './api';
// import { ProductCard } from './components/ProductCard';
// import { OrderStatusTracker } from './components/OrderStatusTracker';
//
// export default function App() {
//   const [token, setAuthToken] = useState(getToken());
//   const [stock, setStock] = useState(5);
//   const [loading, setLoading] = useState(false);
//   const [activeOrder, setActiveOrder] = useState(null);
//   const [message, setMessage] = useState('');
//
//   // Sample static product metadata for display
//   const product = {
//     id: 1,
//     name: 'Flash Edition Smartphone Pro',
//     description: 'Limited edition high-concurrency drop. Atomic stock allocation.',
//     price: 499.99
//   };
//
//   // Fetch initial stock from inventory-service
//   const loadStock = async () => {
//     try {
//       const res = await apiCall(`/api/inventory/${product.id}`);
//       if (res.data && typeof res.data.availableStock === 'number') {
//         setStock(res.data.availableStock);
//       }
//     } catch {
//       // Fallback display if inventory empty
//     }
//   };
//
//   useEffect(() => {
//     loadStock();
//   }, []);
//
//   // Quick Mock Login
//   const handleQuickLogin = async () => {
//     try {
//       const res = await apiCall('/api/auth/login', 'POST', {
//         email: 'customer@flashsale.com',
//         password: 'Password123!'
//       });
//       if (res.data && res.data.accessToken) {
//         setToken(res.data.accessToken);
//         setAuthToken(res.data.accessToken);
//         setMessage('Logged in successfully!');
//       }
//     } catch (err) {
//       setMessage('Login failed: ' + err.message);
//     }
//   };
//
//   const handleLogout = () => {
//     clearToken();
//     setAuthToken(null);
//     setMessage('Logged out');
//   };
//
//   // Step 1: Order Reservation
//   const handleBuy = async () => {
//     if (!token) {
//       handleQuickLogin();
//       return;
//     }
//
//     setLoading(true);
//     setMessage('');
//
//     try {
//       // 1. Reserve stock via order-service
//       const idempotencyKey = `IDEM-ORDER-${Date.now()}`;
//       const orderRes = await apiCall('/api/orders', 'POST', {
//         productId: product.id,
//         quantity: 1,
//         idempotencyKey
//       });
//
//       const orderRef = orderRes.data.orderReference;
//       setMessage(`Stock reserved! Order: ${orderRef}`);
//       loadStock();
//
//       // 2. Automatically dispatch simulated payment
//       const payRes = await apiCall('/api/payments', 'POST', {
//         orderReference: orderRef,
//         amount: product.price,
//         paymentMethod: 'CREDIT_CARD',
//         idempotencyKey: `IDEM-PAY-${Date.now()}`
//       });
//
//       if (payRes.data && payRes.data.status === 'SUCCESS') {
//         setActiveOrder(orderRef); // Opens the Kafka saga status poller
//       }
//     } catch (err) {
//       setMessage(`Checkout failed: ${err.message}`);
//       loadStock();
//     } finally {
//       setLoading(false);
//     }
//   };
//
//   return (
//     <div className="min-h-screen bg-slate-900 text-slate-100 flex flex-col">
//       {/* Top Bar */}
//       <header className="border-b border-slate-800 bg-slate-900/50 backdrop-blur px-6 py-4 flex justify-between items-center">
//         <h1 className="text-xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-red-500 to-orange-400">
//           ⚡ FlashSale Engine
//         </h1>
//         <div>
//           {token ? (
//             <div className="flex items-center space-x-3">
//               <span className="text-xs text-slate-400">Authenticated</span>
//               <button
//                 onClick={handleLogout}
//                 className="text-xs bg-slate-800 hover:bg-slate-700 px-3 py-1.5 rounded-md border border-slate-700"
//               >
//                 Logout
//               </button>
//             </div>
//           ) : (
//             <button
//               onClick={handleQuickLogin}
//               className="text-xs bg-blue-600 hover:bg-blue-500 text-white font-semibold px-4 py-2 rounded-md transition"
//             >
//               Demo Login
//             </button>
//           )}
//         </div>
//       </header>
//
//       {/* Main Content */}
//       <main className="flex-1 flex flex-col items-center justify-center p-6">
//         {message && (
//           <div className="mb-6 px-4 py-2 bg-slate-800 border border-slate-700 text-xs rounded-lg text-yellow-400 font-mono">
//             {message}
//           </div>
//         )}
//
//         <ProductCard
//           product={product}
//           stock={stock}
//           token={token}
//           loading={loading}
//           onBuyClick={handleBuy}
//         />
//       </main>
//
//       {/* Async Order Status Popup */}
//       {activeOrder && (
//         <OrderStatusTracker
//           orderReference={activeOrder}
//           onCompleted={loadStock}
//           onClose={() => setActiveOrder(null)}
//         />
//       )}
//     </div>
//   );
// }



import React, { useState, useEffect } from 'react';
import { apiCall, getToken, setToken, clearToken } from './api';
import { ProductCard } from './components/ProductCard';
import { OrderStatusTracker } from './components/OrderStatusTracker';
import AuthModal from './components/AuthModal';

export default function App() {
  const [token, setAuthToken] = useState(getToken());
  const [stock, setStock] = useState(5);
  const [loading, setLoading] = useState(false);
  const [activeOrder, setActiveOrder] = useState(null);
  const [message, setMessage] = useState('');
  const [showAuthModal, setShowAuthModal] = useState(false);

  // Sample static product metadata for display
  const product = {
    id: 1,
    name: 'Flash Edition Smartphone Pro',
    description: 'Limited edition high-concurrency drop. Atomic stock allocation.',
    price: 499.99
  };

  // Fetch initial stock from inventory-service
  const loadStock = async () => {
    try {
      const res = await apiCall(`/api/inventory/${product.id}`);
      if (res.data && typeof res.data.availableStock === 'number') {
        setStock(res.data.availableStock);
      }
    } catch {
      // Fallback display if inventory empty
    }
  };

  useEffect(() => {
    loadStock();
  }, []);

  const handleLoginSuccess = (authData) => {
    const tokenValue = authData.token || authData.accessToken;
    setToken(tokenValue);
    setAuthToken(tokenValue);
    setShowAuthModal(false);
    setMessage('Logged in successfully!');
    loadStock();
  };

  const handleLogout = () => {
    clearToken();
    setAuthToken(null);
    setMessage('Logged out');
  };

  // Step 1: Order Reservation
  const handleBuy = async () => {
    if (!token) {
      setShowAuthModal(true);
      return;
    }

    setLoading(true);
    setMessage('');

    try {
      // 1. Reserve stock via order-service
      const idempotencyKey = `IDEM-ORDER-${Date.now()}`;
      const orderRes = await apiCall('/api/orders', 'POST', {
        productId: product.id,
        quantity: 1,
        idempotencyKey
      });

      const orderRef = orderRes.data.orderReference;
      setMessage(`Stock reserved! Order: ${orderRef}`);
      loadStock();

      // 2. Automatically dispatch simulated payment
      const payRes = await apiCall('/api/payments', 'POST', {
        orderReference: orderRef,
        amount: product.price,
        paymentMethod: 'CREDIT_CARD',
        idempotencyKey: `IDEM-PAY-${Date.now()}`
      });

      if (payRes.data && payRes.data.status === 'SUCCESS') {
        setActiveOrder(orderRef); // Opens the Kafka saga status poller
      }
    } catch (err) {
      setMessage(`Checkout failed: ${err.message}`);
      loadStock();
    } finally {
      setLoading(false);
    }
  };

  return (
      <div className="min-h-screen bg-slate-900 text-slate-100 flex flex-col">
        {/* Top Bar */}
        <header className="border-b border-slate-800 bg-slate-900/50 backdrop-blur px-6 py-4 flex justify-between items-center">
          <h1 className="text-xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-red-500 to-orange-400">
            ⚡ FlashSale Engine
          </h1>
          <div>
            {token ? (
                <div className="flex items-center space-x-3">
                  <span className="text-xs text-slate-400">Authenticated</span>
                  <button
                      onClick={handleLogout}
                      className="text-xs bg-slate-800 hover:bg-slate-700 px-3 py-1.5 rounded-md border border-slate-700"
                  >
                    Logout
                  </button>
                </div>
            ) : (
                <button
                    onClick={() => setShowAuthModal(true)}
                    className="text-xs bg-indigo-600 hover:bg-indigo-500 text-white font-semibold px-4 py-2 rounded-md transition"
                >
                  Sign In / Register
                </button>
            )}
          </div>
        </header>

        {/* Main Content */}
        <main className="flex-1 flex flex-col items-center justify-center p-6">
          {message && (
              <div className="mb-6 px-4 py-2 bg-slate-800 border border-slate-700 text-xs rounded-lg text-yellow-400 font-mono">
                {message}
              </div>
          )}

          <ProductCard
              product={product}
              stock={stock}
              token={token}
              loading={loading}
              onBuyClick={handleBuy}
          />
        </main>

        {/* Auth Modal Overlay */}
        {showAuthModal && (
            <div className="fixed inset-0 z-50 bg-black/70 flex items-center justify-center p-4">
              <div className="relative w-full max-w-md">
                <button
                    onClick={() => setShowAuthModal(false)}
                    className="absolute top-3 right-3 text-gray-400 hover:text-white text-lg font-bold z-10"
                >
                  ✕
                </button>
                <AuthModal onLoginSuccess={handleLoginSuccess} />
              </div>
            </div>
        )}

        {/* Async Order Status Popup */}
        {activeOrder && (
            <OrderStatusTracker
                orderReference={activeOrder}
                onCompleted={loadStock}
                onClose={() => setActiveOrder(null)}
            />
        )}
      </div>
  );
}