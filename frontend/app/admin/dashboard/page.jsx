'use client'

import { useEffect, useMemo, useState } from 'react'
import Link from 'next/link'
import {
  ArrowUpRight,
  MoreHorizontal,
  Package,
  Zap,
  ShoppingBag,
  CreditCard,
  Layers,
  AlertTriangle,
  Activity,
  TrendingUp,
  PieChart,
} from 'lucide-react'
import api from '@/lib/api'
import { getAdminProfile, getStoreName } from '@/lib/auth'
import {
  Panel,
  PageHeading,
  ProductThumb,
  StatCards,
  StatusPill,
} from '@/components/admin/AdminUI'

function calculateSaleStatus(product) {
  const statusUpper = String(product.status || '').toUpperCase()
  if (statusUpper !== 'ACTIVE') {
    return 'Inactive'
  }

  const now = Date.now()
  const start = product.startTime ? new Date(product.startTime).getTime() : 0
  const end = product.endTime ? new Date(product.endTime).getTime() : 0

  if (start && now < start) {
    return 'Scheduled'
  }
  if (start && end && now >= start && now <= end) {
    return 'Live'
  }
  if (end && now > end) {
    return 'Ended'
  }
  return 'Scheduled'
}

function mapProduct(product) {
  return {
    id: product.id,
    name: product.title || product.name || `Product ${product.id}`,
    title: product.title || product.name || `Product ${product.id}`,
    description: product.description || '',
    originalPrice: Number(product.originalPrice || 0),
    salePrice: Number(product.flashSalePrice ?? product.salePrice ?? 0),
    stock: Number(product.initialStock ?? product.stock ?? 0),
    image: product.imageUrl || product.image || '/placeholder.jpg',
    imageUrl: product.imageUrl || product.image || '/placeholder.jpg',
    category: 'Flash Sale',
    sale: calculateSaleStatus(product),
    saleActive: Boolean(product.saleActive),
    status: product.status || 'ACTIVE',
    startTime: product.startTime,
    endTime: product.endTime,
    createdAt: product.createdAt,
    updatedAt: product.updatedAt,
  }
}

function formatOrderStatus(st) {
  if (!st) return 'Unknown'
  const u = st.toUpperCase()
  if (u === 'PENDING_PAYMENT') return 'Pending'
  if (u === 'PAID') return 'Paid'
  if (u === 'CANCELLED') return 'Cancelled'
  if (u === 'EXPIRED') return 'Expired'
  return st
}

function formatDate(isoString) {
  if (!isoString) return 'N/A'
  try {
    const d = new Date(isoString)
    if (isNaN(d.getTime())) return isoString
    return d.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch (e) {
    return isoString
  }
}

function formatTimeAgo(isoString) {
  if (!isoString) return 'Recently'
  try {
    const date = new Date(isoString)
    if (isNaN(date.getTime())) return 'Recently'
    const now = new Date()
    const diffMs = now - date
    if (diffMs < 0) return 'Just now'
    const diffMins = Math.floor(diffMs / 60000)
    if (diffMins < 1) return 'Just now'
    if (diffMins < 60) return `${diffMins} min ago`
    const diffHours = Math.floor(diffMins / 60)
    if (diffHours < 24) return `${diffHours} hr ago`
    const diffDays = Math.floor(diffHours / 24)
    return `${diffDays} d ago`
  } catch (e) {
    return 'Recently'
  }
}

export default function DashboardPage() {
  const [products, setProducts] = useState([])
  const [inventory, setInventory] = useState([])
  const [orders, setOrders] = useState([])
  const [payments, setPayments] = useState([])
  const [productsMap, setProductsMap] = useState({})

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [salesTab, setSalesTab] = useState('Today')

  const [adminName, setAdminName] = useState('Jatin')
  const [storeName, setStoreName] = useState('Swiftly')

  useEffect(() => {
    const loadInfo = () => {
      const adminProf = getAdminProfile()
      setAdminName(adminProf.displayName || 'Jatin')
      setStoreName(getStoreName())
    }
    loadInfo()
    window.addEventListener('swiftlyAdminProfileUpdated', loadInfo)
    window.addEventListener('swiftlySettingsUpdated', loadInfo)
    window.addEventListener('storage', loadInfo)
    return () => {
      window.removeEventListener('swiftlyAdminProfileUpdated', loadInfo)
      window.removeEventListener('swiftlySettingsUpdated', loadInfo)
      window.removeEventListener('storage', loadInfo)
    }
  }, [])

  const loadDashboard = async () => {
    try {
      setLoading(true)
      setError('')

      const [productsRes, ordersRes, paymentsRes] = await Promise.allSettled([
        api.get('/api/products?page=0&size=100&sort=createdAt,desc'),
        api.get('/api/orders?page=0&size=100&sort=createdAt,desc'),
        api.get('/api/payments?page=0&size=100&sort=createdAt,desc'),
      ])

      let prodList = []
      if (productsRes.status === 'fulfilled') {
        const pageData = productsRes.value.data?.data ?? productsRes.value.data
        prodList = pageData?.content ?? (Array.isArray(pageData) ? pageData : [])
      } else {
        console.warn('Dashboard failed to fetch products:', productsRes.reason?.message)
      }

      let ordList = []
      if (ordersRes.status === 'fulfilled') {
        const pageData = ordersRes.value.data?.data ?? ordersRes.value.data
        ordList = pageData?.content ?? (Array.isArray(pageData) ? pageData : [])
      } else {
        console.warn('Dashboard failed to fetch orders:', ordersRes.reason?.message)
      }

      let payList = []
      if (paymentsRes.status === 'fulfilled') {
        const pageData = paymentsRes.value.data?.data ?? paymentsRes.value.data
        payList = pageData?.content ?? (Array.isArray(pageData) ? pageData : [])
      } else {
        console.warn('Dashboard failed to fetch payments:', paymentsRes.reason?.message)
      }

      const pMap = {}
      prodList.forEach((p) => {
        if (p.id) {
          pMap[p.id] = p.title || p.name
        }
      })
      setProductsMap(pMap)

      const inventoryList = await Promise.all(
        prodList.map(async (prod) => {
          let available = prod.initialStock || 0
          let locked = 0
          let total = prod.initialStock || 0

          try {
            const invRes = await api.get(`/api/inventory/${prod.id}`)
            const invData = invRes.data?.data ?? invRes.data
            if (invData) {
              available = invData.availableStock ?? 0
              locked = invData.lockedStock ?? 0
              total = invData.totalStock ?? (available + locked)
            }
          } catch (invErr) {
            console.warn(`Dashboard inventory fetch failed for product ${prod.id}:`, invErr.message)
          }

          return {
            id: prod.id,
            productId: prod.id,
            name: prod.title || prod.name || `Product #${prod.id}`,
            image: prod.imageUrl || prod.image || '/placeholder.jpg',
            imageUrl: prod.imageUrl || prod.image || '/placeholder.jpg',
            available,
            locked,
            total,
            originalPrice: Number(prod.originalPrice || 0),
            salePrice: Number(prod.flashSalePrice ?? prod.salePrice ?? 0),
            stock: available,
            sale: prod.saleActive ? 'Live' : 'Scheduled',
            saleActive: Boolean(prod.saleActive),
            createdAt: prod.createdAt,
          }
        })
      )

      setProducts(prodList.map(mapProduct))
      setInventory(inventoryList)
      setOrders(ordList)
      setPayments(payList)
    } catch (err) {
      console.error('Failed to load dashboard data:', err)
      setError('Unable to load dashboard data.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadDashboard()
  }, [])

  // Top Statistics Calculations
  const totalProducts = products.length
  const activeFlashSales = products.filter((p) => p.saleActive).length
  const totalOrders = orders.length

  const successfulPayments = useMemo(
    () => payments.filter((p) => (p.status || '').toUpperCase() === 'SUCCESS'),
    [payments]
  )
  const successfulPaymentsCount = successfulPayments.length
  const successPaymentPct =
    payments.length > 0
      ? Math.round((successfulPaymentsCount / payments.length) * 100)
      : 0

  const totalInventory = useMemo(
    () => inventory.reduce((sum, r) => sum + (r.total || 0), 0),
    [inventory]
  )

  const lowStockCount = useMemo(
    () => inventory.filter((r) => r.available < 20).length,
    [inventory]
  )

  const stats = [
    {
      label: (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
          <Package size={14} /> Total products
        </span>
      ),
      value: totalProducts.toLocaleString(),
      note: 'catalog',
      detail: 'total items',
    },
    {
      label: (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
          <Zap size={14} /> Active flash sales
        </span>
      ),
      value: activeFlashSales.toLocaleString(),
      note: 'live',
      detail: 'active campaigns',
    },
    {
      label: (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
          <ShoppingBag size={14} /> Total orders
        </span>
      ),
      value: totalOrders.toLocaleString(),
      note: 'total',
      detail: 'across system',
    },
    {
      label: (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
          <CreditCard size={14} /> Successful payments
        </span>
      ),
      value: successfulPaymentsCount.toLocaleString(),
      note: `${successPaymentPct}%`,
      detail: 'settled orders',
    },
    {
      label: (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
          <Layers size={14} /> Total inventory
        </span>
      ),
      value: totalInventory.toLocaleString(),
      note: 'units',
      detail: 'across all products',
    },
    {
      label: (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
          <AlertTriangle size={14} /> Low stock products
        </span>
      ),
      value: lowStockCount.toLocaleString(),
      note: lowStockCount > 0 ? 'Needs attention' : 'Healthy',
      detail: 'below threshold',
      warning: lowStockCount > 0,
    },
  ]

  // Order Breakdown (Donut Chart)
  const orderBreakdown = useMemo(() => {
    const paidCount = orders.filter(
      (o) => (o.status || '').toUpperCase() === 'PAID'
    ).length
    const pendingCount = orders.filter(
      (o) => (o.status || '').toUpperCase() === 'PENDING_PAYMENT'
    ).length
    const cancelledCount = orders.filter(
      (o) => (o.status || '').toUpperCase() === 'CANCELLED'
    ).length
    const expiredCount = orders.filter(
      (o) => (o.status || '').toUpperCase() === 'EXPIRED'
    ).length

    const paidPct =
      totalOrders > 0 ? Math.round((paidCount / totalOrders) * 100) : 0
    const pendingPct =
      totalOrders > 0 ? Math.round((pendingCount / totalOrders) * 100) : 0
    const cancelledPct =
      totalOrders > 0 ? Math.round((cancelledCount / totalOrders) * 100) : 0
    const expiredPct =
      totalOrders > 0 ? Math.round((expiredCount / totalOrders) * 100) : 0

    const p1 = paidPct
    const p2 = p1 + pendingPct
    const p3 = p2 + cancelledPct

    const donutStyle =
      totalOrders > 0
        ? {
            background: `conic-gradient(var(--coral) 0% ${p1}%, #f6c1ba ${p1}% ${p2}%, #dcd8d1 ${p2}% ${p3}%, #9e9991 ${p3}% 100%)`,
          }
        : { background: '#dcd8d1' }

    return {
      paidCount,
      pendingCount,
      cancelledCount,
      expiredCount,
      paidPct,
      pendingPct,
      cancelledPct,
      expiredPct,
      donutStyle,
    }
  }, [orders, totalOrders])

  // Sales Chart Calculation
  const salesChartData = useMemo(() => {
    let labels = ['8 AM', '10 AM', '12 PM', '2 PM', '4 PM', '6 PM']
    let amounts = [0, 0, 0, 0, 0, 0]

    const now = new Date()

    if (salesTab === 'Today') {
      labels = ['8 AM', '10 AM', '12 PM', '2 PM', '4 PM', '6 PM']
      const hours = [8, 10, 12, 14, 16, 18]
      amounts = [0, 0, 0, 0, 0, 0]

      successfulPayments.forEach((p) => {
        if (!p.createdAt) return
        const d = new Date(p.createdAt)
        if (d.toDateString() === now.toDateString()) {
          const h = d.getHours()
          let closestIdx = 0
          let minDiff = Math.abs(h - hours[0])
          for (let i = 1; i < hours.length; i++) {
            const diff = Math.abs(h - hours[i])
            if (diff < minDiff) {
              minDiff = diff
              closestIdx = i
            }
          }
          amounts[closestIdx] += Number(p.amount || 0)
        }
      })
    } else if (salesTab === '7 days') {
      labels = []
      amounts = [0, 0, 0, 0, 0, 0, 0]
      for (let i = 6; i >= 0; i--) {
        const d = new Date()
        d.setDate(d.getDate() - i)
        labels.push(d.toLocaleDateString('en-US', { weekday: 'short' }))
      }

      successfulPayments.forEach((p) => {
        if (!p.createdAt) return
        const d = new Date(p.createdAt)
        const diffTime = now.getTime() - d.getTime()
        const diffDays = Math.floor(diffTime / (1000 * 3600 * 24))
        if (diffDays >= 0 && diffDays < 7) {
          amounts[6 - diffDays] += Number(p.amount || 0)
        }
      })
    } else if (salesTab === '30 days') {
      labels = ['Wk 1', 'Wk 2', 'Wk 3', 'Wk 4', 'Wk 5', 'Recent']
      amounts = [0, 0, 0, 0, 0, 0]

      successfulPayments.forEach((p) => {
        if (!p.createdAt) return
        const d = new Date(p.createdAt)
        const diffTime = now.getTime() - d.getTime()
        const diffDays = Math.floor(diffTime / (1000 * 3600 * 24))
        if (diffDays >= 0 && diffDays < 30) {
          const bucket = Math.min(5, Math.floor(diffDays / 5))
          amounts[5 - bucket] += Number(p.amount || 0)
        }
      })
    }

    const xCoords =
      amounts.length === 7
        ? [0, 116, 233, 350, 466, 583, 700]
        : [0, 140, 280, 420, 560, 700]

    const maxVal = Math.max(...amounts, 1)
    const yCoords = amounts.map(
      (v) => 160 - Math.round((v / maxVal) * 120)
    )

    const stroke = yCoords
      .map((y, i) => `${i === 0 ? 'M' : 'L'}${xCoords[i]} ${y}`)
      .join(' ')
    const fill = `${stroke} L 700 190 L 0 190 Z`

    return { labels, stroke, fill }
  }, [salesTab, successfulPayments])

  // Flash Sale Products
  const flashSaleProducts = useMemo(
    () => inventory.filter((p) => p.saleActive).slice(0, 3),
    [inventory]
  )

  // Inventory Alerts
  const inventoryAlerts = useMemo(
    () => inventory.filter((p) => p.available < 20),
    [inventory]
  )

  // Recent Activity
  const recentActivities = useMemo(() => {
    const list = []

    successfulPayments.forEach((p) => {
      list.push({
        id: `pay-${p.id}`,
        title: 'Payment received',
        detail: `Order ${p.orderReference || 'TXN-' + p.transactionId}`,
        timestamp: p.createdAt,
        icon: '✓',
      })
    })

    orders.forEach((o) => {
      list.push({
        id: `ord-${o.id}`,
        title: 'New order created',
        detail: `Order ${o.orderReference || 'ORD-' + o.id}`,
        timestamp: o.createdAt,
        icon: '✓',
      })
    })

    inventoryAlerts.forEach((i) => {
      list.push({
        id: `inv-${i.id}`,
        title: 'Low inventory',
        detail: `${i.name} (${i.available} left)`,
        timestamp: null,
        icon: '!',
      })
    })

    products
      .filter((p) => p.saleActive)
      .forEach((p) => {
        list.push({
          id: `fs-${p.id}`,
          title: 'Flash sale activated',
          detail: p.name,
          timestamp: p.createdAt,
          icon: '✓',
        })
      })

    list.sort((a, b) => {
      if (!a.timestamp) return 1
      if (!b.timestamp) return -1
      return new Date(b.timestamp) - new Date(a.timestamp)
    })

    return list.slice(0, 4).map((act) => ({
      ...act,
      timeAgo: formatTimeAgo(act.timestamp),
    }))
  }, [successfulPayments, orders, inventoryAlerts, products])

  // Recent Orders
  const recentOrders = useMemo(() => orders.slice(0, 5), [orders])

  const todayDateString = useMemo(() => {
    return new Date().toLocaleDateString('en-US', {
      month: 'long',
      day: 'numeric',
      year: 'numeric',
    })
  }, [])

  if (loading) {
    return (
      <div className="empty-state" style={{ padding: '60px 20px' }}>
        <p style={{ fontSize: '16px', fontWeight: 600 }}>Loading dashboard...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="empty-state" style={{ padding: '60px 20px' }}>
        <p style={{ fontSize: '16px', color: '#ef4444', marginBottom: '16px' }}>
          {error}
        </p>
        <button className="button primary" onClick={loadDashboard}>
          Retry
        </button>
      </div>
    )
  }

  return (
    <>
      <PageHeading
        eyebrow="Live flash sale"
        title={
          <>
            Good afternoon, <em>{adminName}.</em>
          </>
        }
        description={`Here's what's happening with your ${storeName} store today.`}
        action={<span className="date-chip">{todayDateString}</span>}
      />

      <StatCards items={stats} />

      <div className="overview-grid">
        <Panel
          eyebrow="Performance"
          title={
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
              <TrendingUp size={16} /> Sales overview
            </span>
          }
          action={
            <div className="tabs">
              <button
                className={salesTab === 'Today' ? 'selected' : ''}
                onClick={() => setSalesTab('Today')}
              >
                Today
              </button>
              <button
                className={salesTab === '7 days' ? 'selected' : ''}
                onClick={() => setSalesTab('7 days')}
              >
                7 days
              </button>
              <button
                className={salesTab === '30 days' ? 'selected' : ''}
                onClick={() => setSalesTab('30 days')}
              >
                30 days
              </button>
            </div>
          }
        >
          <div className="sales-chart">
            <div className="chart-grid" />
            <svg
              viewBox="0 0 700 190"
              preserveAspectRatio="none"
              aria-label="Sales trending"
            >
              <path d={salesChartData.fill} fill="rgba(250,90,78,.14)" />
              <path
                d={salesChartData.stroke}
                fill="none"
                stroke="#fa5a4e"
                strokeWidth="3"
                strokeLinecap="round"
              />
            </svg>
            <div className="chart-labels">
              {salesChartData.labels.map((lbl, idx) => (
                <span key={`chart-lbl-${idx}`}>{lbl}</span>
              ))}
            </div>
          </div>
        </Panel>

        <Panel
          eyebrow="At a glance"
          title={
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
              <PieChart size={16} /> Order overview
            </span>
          }
          action={
            <Link
              className="icon-button"
              href="/admin/orders"
              aria-label="View all orders"
            >
              <MoreHorizontal size={17} />
            </Link>
          }
        >
          <div className="donut-row">
            <div className="donut" style={orderBreakdown.donutStyle}>
              <strong>{totalOrders.toLocaleString()}</strong>
              <span>total orders</span>
            </div>
            <div className="legend">
              {[
                ['Paid', `${orderBreakdown.paidPct}%`, 'paid'],
                ['Pending', `${orderBreakdown.pendingPct}%`, 'pending'],
                ['Cancelled', `${orderBreakdown.cancelledPct}%`, 'cancelled'],
                ['Expired', `${orderBreakdown.expiredPct}%`, 'expired'],
              ].map(([name, value, kind], index) => (
                <div key={`legend-${name}-${index}`}>
                  <i className={kind} />
                  <span>{name}</span>
                  <b>{value}</b>
                </div>
              ))}
            </div>
          </div>
        </Panel>
      </div>

      <Panel
        eyebrow="Live now"
        title={
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
            <Zap size={16} /> Flash sale products
          </span>
        }
        action={
          <Link href="/admin/products" className="text-link">
            View all products <ArrowUpRight size={14} />
          </Link>
        }
      >
        <div className="dashboard-product-grid">
          {flashSaleProducts.length === 0 ? (
            <p
              className="empty-state"
              style={{ gridColumn: '1 / -1', padding: '20px' }}
            >
              No active flash sale products right now.
            </p>
          ) : (
            flashSaleProducts.map((product, index) => (
              <Link
                href="/admin/products"
                className="dashboard-product"
                key={`flash-prod-${product.id || index}`}
              >
                <ProductThumb product={product} size="large" />
                <div>
                  <strong>{product.name}</strong>
                  <span>
                    ₹{product.salePrice.toLocaleString('en-IN')}{' '}
                    <del>₹{product.originalPrice.toLocaleString('en-IN')}</del>
                  </span>
                  <small>
                    {product.stock} left · <StatusPill>{product.sale}</StatusPill>
                  </small>
                </div>
              </Link>
            ))
          )}
        </div>
      </Panel>

      <div className="two-column">
        <Panel
          eyebrow="Needs attention"
          title={
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
              <AlertTriangle size={16} /> Inventory alerts
            </span>
          }
          action={
            <Link href="/admin/inventory" className="text-link">
              View inventory <ArrowUpRight size={14} />
            </Link>
          }
        >
          <div className="alert-list">
            {inventoryAlerts.length === 0 ? (
              <p className="empty-state" style={{ padding: '20px' }}>
                All inventory levels are healthy.
              </p>
            ) : (
              inventoryAlerts.map((product, index) => (
                <div className="alert-row" key={`inv-alert-${product.id || index}`}>
                  <ProductThumb product={product} />
                  <div>
                    <strong>{product.name}</strong>
                    <small>Only {product.available} units remaining</small>
                  </div>
                  <StatusPill>
                    {product.available < 10 ? 'Critical' : 'Low'}
                  </StatusPill>
                </div>
              ))
            )}
          </div>
        </Panel>

        <Panel
          eyebrow="What's happening"
          title={
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
              <Activity size={16} /> Recent activity
            </span>
          }
        >
          <div className="activity-list">
            {recentActivities.length === 0 ? (
              <p className="empty-state" style={{ padding: '20px' }}>
                No recent activity recorded.
              </p>
            ) : (
              recentActivities.map((activity, index) => (
                <div className="activity-row" key={`activity-${activity.id || index}`}>
                  <i>{activity.icon}</i>
                  <div>
                    <strong>{activity.title}</strong>
                    <small>{activity.detail}</small>
                  </div>
                  <time>{activity.timeAgo}</time>
                </div>
              ))
            )}
          </div>
        </Panel>
      </div>

      <Panel
        eyebrow="Latest activity"
        title={
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
            <ShoppingBag size={16} /> Recent orders
          </span>
        }
        action={
          <Link href="/admin/orders" className="text-link">
            View all orders <ArrowUpRight size={14} />
          </Link>
        }
      >
        <div className="table-scroll">
          {recentOrders.length === 0 ? (
            <p className="empty-state">No recent orders found.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Order reference</th>
                  <th>Customer</th>
                  <th>Product</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {recentOrders.map((order, index) => (
                  <tr key={`order-${order.orderReference || order.id || index}`}>
                    <td className="order-id">
                      {order.orderReference || `ORD-${order.id}`}
                    </td>
                    <td>User #{order.userId}</td>
                    <td>
                      {productsMap[order.productId] ||
                        `Product #${order.productId}`}
                    </td>
                    <td>
                      ₹{Number(order.totalAmount || 0).toLocaleString('en-IN')}
                    </td>
                    <td>
                      <StatusPill>{formatOrderStatus(order.status)}</StatusPill>
                    </td>
                    <td>{formatDate(order.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </Panel>

      <section className="quick-actions">
        <div>
          <span className="eyebrow coral">Shortcuts</span>
          <h2>Quick actions</h2>
        </div>
        <div className="quick-grid">
          <Link href="/admin/products/new">
            <b>＋</b>
            <span>Add product</span>
            <small>Create a new listing</small>
          </Link>
          <Link href="/admin/inventory">
            <b>▥</b>
            <span>Manage inventory</span>
            <small>Review stock levels</small>
          </Link>
          <Link href="/admin/orders">
            <b>≡</b>
            <span>View orders</span>
            <small>Track customer orders</small>
          </Link>
          <Link href="/admin/payments">
            <b>◉</b>
            <span>View payments</span>
            <small>Review transactions</small>
          </Link>
        </div>
      </section>
    </>
  )
}
