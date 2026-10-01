export const productImages = {
  AirPods: '/images/aeropods.jpg',
  'iPhone 17': '/images/iphone.jpg',
  'Lumen Lamp': '/images/lumen-lamp.jpg',
  'Nova Mirrorless Camera': '/images/nova-camera.jpg',
  'Orbit Mechanical': '/images/orbit-mechanical.jpg',
  'Pulse Smartwatch': '/images/pulse-smartwatch.jpg',
  'Velocity Runner': '/images/velocity-runner.jpg',
}

export const products = [
  { id: 'p1', name: 'AirPods', category: 'Audio', originalPrice: 189, salePrice: 129, stock: 18, status: 'Active', sale: 'Live', image: productImages.AirPods },
  { id: 'p2', name: 'iPhone 17', category: 'Electronics', originalPrice: 999, salePrice: 899, stock: 100, status: 'Active', sale: 'Scheduled', image: productImages['iPhone 17'] },
  { id: 'p3', name: 'Lumen Lamp', category: 'Home', originalPrice: 120, salePrice: 84, stock: 42, status: 'Active', sale: 'Live', image: productImages['Lumen Lamp'] },
  { id: 'p4', name: 'Nova Mirrorless Camera', category: 'Photography', originalPrice: 899, salePrice: 699, stock: 9, status: 'Active', sale: 'Live', image: productImages['Nova Mirrorless Camera'] },
  { id: 'p5', name: 'Orbit Mechanical', category: 'Accessories', originalPrice: 160, salePrice: 120, stock: 27, status: 'Draft', sale: 'None', image: productImages['Orbit Mechanical'] },
  { id: 'p6', name: 'Pulse Smartwatch', category: 'Wearables', originalPrice: 249, salePrice: 199, stock: 12, status: 'Active', sale: 'Live', image: productImages['Pulse Smartwatch'] },
  { id: 'p7', name: 'Velocity Runner', category: 'Lifestyle', originalPrice: 120, salePrice: 84, stock: 24, status: 'Active', sale: 'Scheduled', image: productImages['Velocity Runner'] },
]

export const orders = [
  { id: 'ORD-FLASH-1001', customer: 'Jatin Kumar', email: 'jatin@example.com', product: 'AirPods', quantity: 1, amount: 129, status: 'Paid', date: 'Today' },
  { id: 'ORD-FLASH-1002', customer: 'Rahul Sharma', email: 'rahul@example.com', product: 'Velocity Runner', quantity: 1, amount: 84, status: 'Pending', date: 'Today' },
  { id: 'ORD-FLASH-1003', customer: 'Priya Singh', email: 'priya@example.com', product: 'Nova Mirrorless Camera', quantity: 1, amount: 699, status: 'Paid', date: 'Yesterday' },
  { id: 'ORD-FLASH-1004', customer: 'Maya Patel', email: 'maya@example.com', product: 'Pulse Smartwatch', quantity: 2, amount: 398, status: 'Cancelled', date: 'Sep 17' },
]

export const payments = orders.map((order, index) => ({ id: `TXN-${7821 + index}`, order: order.id, customer: order.customer, amount: order.amount, method: index % 2 ? 'Card' : 'UPI', status: order.status === 'Cancelled' ? 'Failed' : order.status, date: order.date }))

export const users = [
  { id: 'u1', name: 'Jatin Kumar', email: 'jatin@example.com', role: 'Customer', status: 'Active', joined: 'Sep 03, 2026' },
  { id: 'u2', name: 'Rahul Sharma', email: 'rahul@example.com', role: 'Customer', status: 'Active', joined: 'Aug 21, 2026' },
  { id: 'u3', name: 'Priya Singh', email: 'priya@example.com', role: 'Customer', status: 'Disabled', joined: 'Jul 14, 2026' },
  { id: 'u4', name: 'Maya Patel', email: 'maya@example.com', role: 'Customer', status: 'Active', joined: 'Jun 28, 2026' },
]

export const notifications = ['8 products are low in stock.', 'Flash sale is live.', 'Payment received for ORD-FLASH-1001.']
export const activities = ['Payment received', 'New order created', 'Low inventory', 'Flash sale activated']

export const cloneData = (value) => JSON.parse(JSON.stringify(value))

export const apiMap = {
  products: 'GET/POST/PUT/PATCH/DELETE /api/products/{id}',
  inventory: 'GET /api/inventory/{productId}; POST /api/inventory/replenish?productId={productId}&quantity={quantity}',
  orders: 'GET /api/orders/{orderReference}; POST /api/orders/{orderReference}/cancel',
  payments: 'POST /api/payments (admin list endpoint not confirmed)',
  users: 'No confirmed admin users endpoint',
}

// UI-only mock data. Future requests must use the API Gateway at http://localhost:8080.
const unused = null
void unused
