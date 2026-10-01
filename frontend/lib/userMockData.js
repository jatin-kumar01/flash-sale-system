export const user = { firstName: 'Jordan', lastName: 'Davis', email: 'jordan.davis@example.com', phone: '+91 98765 43210', address: '42 Swift Street, Bengaluru, Karnataka', initials: 'JD', accountStatus: 'Active', memberSince: 'January 2025', userId: 'USR-SWIFT-2048' }

export const flashSaleProducts = [
  { id: 'airpods', name: 'AirPods', image: '/images/aeropods.png', original: '₹24,999', price: '₹19,999', discount: '20% OFF', stock: 'Only 8 left' },
  { id: 'iphone-17', name: 'iPhone 17', image: '/images/iphone.png', original: '₹89,999', price: '₹69,999', discount: '22% OFF', stock: 'Only 4 left' },
  { id: 'lumen-lamp', name: 'Lumen Lamp', image: '/images/lumen-lamp.png', original: '₹8,999', price: '₹5,999', discount: '33% OFF', stock: 'In stock' },
  { id: 'nova-camera', name: 'Nova Mirrorless Camera', image: '/images/nova-camera.png', original: '₹74,999', price: '₹59,999', discount: '20% OFF', stock: 'Only 3 left' },
  { id: 'orbit-mechanical', name: 'Orbit Mechanical', image: '/images/orbit-mechanical.png', original: '₹12,999', price: '₹8,999', discount: '31% OFF', stock: 'In stock' },
  { id: 'pulse-smartwatch', name: 'Pulse Smartwatch', image: '/images/pulse-smartwatch.png', original: '₹18,999', price: '₹12,999', discount: '32% OFF', stock: 'Only 6 left' },
  { id: 'velocity-runner', name: 'Velocity Runner', image: '/images/velocity-runner.png', original: '₹11,999', price: '₹7,999', discount: '33% OFF', stock: 'In stock' },
]

export const orders = [
  { reference: 'ORD-FLASH-1001', product: 'iPhone 17', productId: 'iphone-17', quantity: 1, unitPrice: '₹69,999', amount: '₹69,999', status: 'PAID', orderStatus: 'COMPLETED', paymentStatus: 'PAID', date: '18 Sep 2026', paymentMethod: 'UPI', transactionId: 'TXN-IPHONE-1001', deliveryStatus: 'Delivered on 20 Sep 2026', canCancel: false },
  { reference: 'ORD-FLASH-1002', product: 'AirPods', productId: 'airpods', quantity: 2, unitPrice: '₹19,999', amount: '₹39,998', status: 'PENDING', orderStatus: 'PROCESSING', paymentStatus: 'PENDING', date: '15 Sep 2026', paymentMethod: 'UPI', transactionId: 'TXN-AIRPODS-1002', deliveryStatus: 'Preparing for dispatch', canCancel: true },
  { reference: 'ORD-FLASH-0987', product: 'Lumen Lamp', productId: 'lumen-lamp', quantity: 1, unitPrice: '₹5,999', amount: '₹5,999', status: 'PAID', orderStatus: 'COMPLETED', paymentStatus: 'PAID', date: '08 Sep 2026', paymentMethod: 'Card', transactionId: 'TXN-LAMP-0987', deliveryStatus: 'Delivered on 12 Sep 2026', canCancel: false },
]

export const payments = [{ order: 'ORD-FLASH-1001', amount: '₹69,999', method: 'UPI', transaction: 'TXN-XXXX', status: 'SUCCESS' }]
export const cart = { items: 2, total: '₹74,998' }
export const notifications = ['Flash sale is live', 'Payment successful', 'Order confirmed', 'Stock running low for an item in your cart']
export const recommendations = flashSaleProducts.slice(2, 7)
export const summary = [{ label: 'Total Orders', value: '12', detail: 'Across all time' }, { label: 'Pending Orders', value: '2', detail: 'Need your attention' }, { label: 'Completed Orders', value: '10', detail: 'Delivered successfully' }, { label: 'Total Spent', value: '₹84,999', detail: 'This month' }]

export function findProduct(id) { return flashSaleProducts.find((product) => product.id === id) }
export function findOrder(reference) { return orders.find((order) => order.reference === reference) }
