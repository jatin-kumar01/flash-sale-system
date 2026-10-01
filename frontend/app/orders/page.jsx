'use client'

import { useEffect, useMemo, useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import UserHeader from '@/components/user/UserHeader'
import api from '@/lib/api'
import { useCurrency } from '@/lib/currency'
import { isAuthenticated, isAdmin } from '@/lib/auth'

function formatDate(isoString) {
  if (!isoString) return 'N/A'
  try {
    const d = new Date(isoString)
    if (isNaN(d.getTime())) return isoString
    return d.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
    })
  } catch (e) {
    return isoString
  }
}

function formatOrderStatus(st) {
  if (!st) return { text: 'Unknown', statusClass: 'unknown' }
  const u = String(st).toUpperCase()
  if (u === 'PENDING_PAYMENT' || u === 'PENDING') {
    return { text: 'Pending', statusClass: 'pending' }
  }
  if (u === 'PAID' || u === 'COMPLETED') {
    return { text: 'Paid', statusClass: 'paid' }
  }
  if (u === 'CANCELLED') {
    return { text: 'Cancelled', statusClass: 'cancelled' }
  }
  if (u === 'EXPIRED') {
    return { text: 'Expired', statusClass: 'expired' }
  }
  return { text: st, statusClass: u.toLowerCase() }
}

export default function OrdersPage() {
  const router = useRouter()
  const { formatPrice } = useCurrency()

  const [authChecking, setAuthChecking] = useState(true)
  const [orders, setOrders] = useState([])
  const [productsMap, setProductsMap] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [query, setQuery] = useState('')
  const [filter, setFilter] = useState('ALL')

  useEffect(() => {
    if (typeof window !== 'undefined') {
      if (!isAuthenticated()) {
        router.push('/login')
        return
      }
      if (isAdmin()) {
        router.push('/admin/dashboard')
        return
      }
      setAuthChecking(false)
    }
  }, [router])

  useEffect(() => {
    async function loadOrdersData() {
      try {
        setLoading(true)
        setError('')

        const [ordersRes, productsRes] = await Promise.allSettled([
          api.get('/api/orders/my-orders?page=0&size=100'),
          api.get('/api/products?page=0&size=100'),
        ])

        let ordList = []
        if (ordersRes.status === 'fulfilled') {
          const pageData = ordersRes.value.data?.data ?? ordersRes.value.data
          ordList = pageData?.content ?? (Array.isArray(pageData) ? pageData : [])
        } else {
          console.warn('Failed to fetch user orders:', ordersRes.reason?.message)
          throw new Error('Unable to load your orders.')
        }

        let prodList = []
        if (productsRes.status === 'fulfilled') {
          const pageData = productsRes.value.data?.data ?? productsRes.value.data
          prodList = pageData?.content ?? (Array.isArray(pageData) ? pageData : [])
        }

        const pMap = {}
        prodList.forEach((p) => {
          if (p.id) {
            pMap[p.id] = p
          }
        })

        // Sort newest orders first
        ordList.sort((a, b) => {
          if (!a.createdAt) return 1
          if (!b.createdAt) return -1
          return new Date(b.createdAt) - new Date(a.createdAt)
        })

        setProductsMap(pMap)
        setOrders(ordList)
      } catch (err) {
        console.error('Error loading my-orders:', err)
        setError('Unable to load your orders. Please try again.')
      } finally {
        setLoading(false)
      }
    }

    loadOrdersData()
  }, [router])

  // Summary counts
  const counts = useMemo(() => {
    const total = orders.length
    const pending = orders.filter((o) => {
      const u = String(o.status || '').toUpperCase()
      return u === 'PENDING_PAYMENT' || u === 'PENDING'
    }).length
    const paid = orders.filter((o) => {
      const u = String(o.status || '').toUpperCase()
      return u === 'PAID' || u === 'COMPLETED'
    }).length
    const cancelled = orders.filter((o) => {
      const u = String(o.status || '').toUpperCase()
      return u === 'CANCELLED'
    }).length

    return { total, pending, paid, cancelled }
  }, [orders])

  // Filtered orders list
  const filteredOrders = useMemo(() => {
    return orders.filter((order) => {
      const refStr = order.orderReference || `ORD-${order.id}`
      const prod = productsMap[order.productId]
      const prodName = prod?.title || prod?.name || `Product #${order.productId}`
      const searchTarget = `${refStr} ${prodName} ${order.productId}`.toLowerCase()

      const matchesQuery = searchTarget.includes(query.trim().toLowerCase())

      let matchesFilter = true
      const st = String(order.status || '').toUpperCase()

      if (filter === 'PENDING') {
        matchesFilter = st === 'PENDING_PAYMENT' || st === 'PENDING'
      } else if (filter === 'PAID') {
        matchesFilter = st === 'PAID' || st === 'COMPLETED'
      } else if (filter === 'CANCELLED') {
        matchesFilter = st === 'CANCELLED'
      } else if (filter === 'EXPIRED') {
        matchesFilter = st === 'EXPIRED'
      }

      return matchesQuery && matchesFilter
    })
  }, [orders, productsMap, query, filter])

  const handleBuyAgain = async (productId) => {
    if (!productId) return
    try {
      const res = await api.get(`/api/products/${productId}`)
      const prodData = res.data?.data ?? res.data
      if (prodData && String(prodData.status || '').toUpperCase() === 'ACTIVE') {
        router.push(`/checkout/${productId}`)
      } else {
        alert('Product is currently unavailable.')
      }
    } catch (err) {
      alert('Product is currently unavailable.')
    }
  }

  if (authChecking) return null

  return (
    <>
      <UserHeader />
      <main className="user-page support-page">
        <span className="eyebrow">YOUR ACTIVITY</span>
        <h1>My Orders</h1>
        <p className="orders-subtitle">Track and manage your Swiftly purchases.</p>

        {loading ? (
          <div className="support-panel" style={{ padding: '60px 20px', textAlign: 'center' }}>
            <p style={{ fontSize: '16px', fontWeight: 500, color: '#666' }}>
              Loading your orders...
            </p>
          </div>
        ) : error ? (
          <div className="support-panel" style={{ padding: '60px 20px', textAlign: 'center' }}>
            <p style={{ fontSize: '16px', color: '#ef4444', marginBottom: '16px' }}>{error}</p>
            <button
              className="button button-dark"
              onClick={() => window.location.reload()}
            >
              Retry
            </button>
          </div>
        ) : (
          <>
            {/* Order summary grid */}
            <div className="order-summary-grid">
              <div>
                <span>Total orders</span>
                <strong>{counts.total}</strong>
              </div>
              <div>
                <span>Pending</span>
                <strong>{counts.pending}</strong>
              </div>
              <div>
                <span>Paid</span>
                <strong>{counts.paid}</strong>
              </div>
              <div>
                <span>Cancelled</span>
                <strong>{counts.cancelled}</strong>
              </div>
            </div>

            {/* Orders toolbar */}
            <div className="orders-toolbar">
              <input
                aria-label="Search orders"
                placeholder="Search orders..."
                value={query}
                onChange={(event) => setQuery(event.target.value)}
              />
              <select
                aria-label="Filter orders"
                value={filter}
                onChange={(event) => setFilter(event.target.value)}
              >
                <option value="ALL">All status</option>
                <option value="PENDING">Pending</option>
                <option value="PAID">Paid</option>
                <option value="CANCELLED">Cancelled</option>
                <option value="EXPIRED">Expired</option>
              </select>
            </div>

            {/* Orders list */}
            <div className="support-panel orders-list">
              {filteredOrders.length ? (
                filteredOrders.map((order) => {
                  const refStr = order.orderReference || `ORD-${order.id}`
                  const prod = productsMap[order.productId]
                  const prodName = prod?.title || prod?.name || `Product #${order.productId}`
                  const prodImage = prod?.imageUrl || prod?.image || '/placeholder.jpg'
                  const formattedAmount = formatPrice(Number(order.totalAmount || 0))
                  const { text: statusText, statusClass } = formatOrderStatus(order.status)

                  return (
                    <article className="order-card" key={refStr}>
                      <Link href={`/products/${order.productId}`} className="order-thumb">
                        <img
                          src={prodImage}
                          alt={prodName}
                          onError={(event) => {
                            event.currentTarget.src = '/placeholder.jpg'
                          }}
                        />
                      </Link>
                      <div className="order-main">
                        <span className="order-reference">{refStr}</span>
                        <Link href={`/products/${order.productId}`}>
                          <b>{prodName}</b>
                        </Link>
                        <span>
                          Qty: {order.quantity} · {formatDate(order.createdAt)}
                        </span>
                      </div>
                      <strong className="order-amount">{formattedAmount}</strong>
                      <span className={`status status-${statusClass}`}>{statusText}</span>
                      <div className="order-actions">
                        <Link
                          className="button button-light"
                          href={`/orders/${encodeURIComponent(refStr)}`}
                        >
                          View order
                        </Link>
                        <button
                          className="button button-dark"
                          type="button"
                          onClick={() => handleBuyAgain(order.productId)}
                        >
                          Buy again
                        </button>
                      </div>
                    </article>
                  )
                })
              ) : (
                <div className="empty-orders">
                  <h2>No orders yet</h2>
                  <p>Start shopping to discover the latest Swiftly flash-sale products.</p>
                  <Link className="button button-dark" href="/products">
                    Start shopping
                  </Link>
                </div>
              )}
            </div>
          </>
        )}
      </main>
    </>
  )
}
