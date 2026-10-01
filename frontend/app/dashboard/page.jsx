'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import {
  ArrowUpRight,
  Check,
  Clock3,
  CreditCard,
  Package,
  ShoppingBag,
  Sparkles,
} from 'lucide-react'
import { createElement, useEffect, useMemo, useState } from 'react'
import UserHeader from '@/components/user/UserHeader'
import ProductCard from '@/components/user/ProductCard'
import { useCart } from '@/components/user/CartProvider'
import { getStoredUser, isAuthenticated, isAdmin } from '@/lib/auth'
import { useCurrency } from '@/lib/currency'
import api from '@/lib/api'

function mapProduct(prod) {
  const original = Number(prod.originalPrice || 0)
  const sale = Number(prod.flashSalePrice ?? prod.salePrice ?? original)
  const discountPct =
    original > sale ? Math.round(((original - sale) / original) * 100) : 0

  return {
    id: prod.id,
    name: prod.title || prod.name || `Product #${prod.id}`,
    image: prod.imageUrl || prod.image || '/placeholder.jpg',
    price: `₹${sale.toLocaleString('en-IN')}`,
    original: original > sale ? `₹${original.toLocaleString('en-IN')}` : '',
    discount:
      discountPct > 0
        ? `${discountPct}% OFF`
        : prod.saleActive
        ? 'Live Sale'
        : 'In Stock',
    stock:
      prod.initialStock != null
        ? `${prod.initialStock} units available`
        : prod.saleActive
        ? 'Limited stock'
        : 'In Stock',
    saleActive: Boolean(prod.saleActive),
    rawProduct: prod,
  }
}

function isFlashSaleEligible(prod) {
  const statusOk = String(prod.status || '').toUpperCase() === 'ACTIVE'
  const saleActiveOk = Boolean(prod.saleActive)

  if (!statusOk || !saleActiveOk) return false

  const now = new Date()

  if (prod.startTime) {
    const start = new Date(prod.startTime)
    if (!isNaN(start.getTime()) && start > now) return false
  }

  if (prod.endTime) {
    const end = new Date(prod.endTime)
    if (!isNaN(end.getTime()) && end < now) return false
  }

  return true
}

function formatOrderStatus(st) {
  if (!st) return 'Unknown'
  const u = String(st).toUpperCase()
  if (u === 'PENDING_PAYMENT' || u === 'PENDING') return 'Pending'
  if (u === 'PAID' || u === 'COMPLETED') return 'Paid'
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
    })
  } catch (e) {
    return isoString
  }
}

function Countdown({ targetEndTime }) {
  const [seconds, setSeconds] = useState(() => {
    if (targetEndTime) {
      const end = new Date(targetEndTime).getTime()
      const now = new Date().getTime()
      if (!isNaN(end) && end > now) {
        return Math.floor((end - now) / 1000)
      }
    }
    return 2 * 3600 + 14 * 60 + 36
  })

  useEffect(() => {
    const timer = setInterval(
      () => setSeconds((value) => Math.max(0, value - 1)),
      1000
    )
    return () => clearInterval(timer)
  }, [])

  if (!seconds) return <strong>Flash Sale Ended</strong>

  const parts = [
    Math.floor(seconds / 3600),
    Math.floor((seconds % 3600) / 60),
    seconds % 60,
  ]

  return (
    <div className="countdown">
      {parts.map((part, index) => (
        <span key={index}>
          <b>{String(part).padStart(2, '0')}</b>
          <small>{['Hours', 'Minutes', 'Seconds'][index]}</small>
        </span>
      ))}
    </div>
  )
}

function Status({ value }) {
  return <span className={`status status-${value.toLowerCase()}`}>{value}</span>
}

export default function DashboardPage() {
  const router = useRouter()
  const { formatPrice } = useCurrency()

  const [authChecking, setAuthChecking] = useState(true)
  const [userName, setUserName] = useState('')
  const [orders, setOrders] = useState([])
  const [products, setProducts] = useState([])
  const [productsMap, setProductsMap] = useState({})

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const cartCtx = useCart()

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

      const u = getStoredUser()
      if (u) {
        const name = `${u.firstName || ''} ${u.lastName || ''}`.trim() || u.name || u.email || ''
        setUserName(name ? name.split(' ')[0] : '')
      } else {
        setUserName('')
      }
    }
  }, [router])

  useEffect(() => {
    async function loadDashboardData() {
      const token = sessionStorage.getItem('accessToken')
      if (!token) return

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
          console.warn('Dashboard failed to fetch user orders:', ordersRes.reason?.message)
        }

        let prodList = []
        if (productsRes.status === 'fulfilled') {
          const pageData = productsRes.value.data?.data ?? productsRes.value.data
          prodList = pageData?.content ?? (Array.isArray(pageData) ? pageData : [])
        } else {
          console.warn('Dashboard failed to fetch products:', productsRes.reason?.message)
        }

        const pMap = {}
        prodList.forEach((p) => {
          if (p.id) {
            pMap[p.id] = p
          }
        })

        setProductsMap(pMap)
        setProducts(prodList)
        setOrders(ordList)
      } catch (err) {
        console.error('Error loading dashboard data:', err)
        setError('Unable to load your dashboard. Please try again.')
      } finally {
        setLoading(false)
      }
    }

    loadDashboardData()
  }, [])

  // Order statistics calculations
  const totalOrdersCount = orders.length

  const pendingOrdersCount = useMemo(() => {
    return orders.filter((o) => {
      const st = String(o.status || '').toUpperCase()
      return st === 'PENDING_PAYMENT' || st === 'PENDING'
    }).length
  }, [orders])

  const completedOrdersCount = useMemo(() => {
    return orders.filter((o) => {
      const st = String(o.status || '').toUpperCase()
      return st === 'PAID' || st === 'COMPLETED'
    }).length
  }, [orders])

  const totalSpentAmount = useMemo(() => {
    return orders
      .filter((o) => {
        const st = String(o.status || '').toUpperCase()
        return st === 'PAID' || st === 'COMPLETED'
      })
      .reduce((sum, o) => sum + Number(o.totalAmount || 0), 0)
  }, [orders])

  const summary = [
    {
      label: 'Total orders',
      value: totalOrdersCount.toLocaleString(),
      detail: 'placed by you',
    },
    {
      label: 'Pending orders',
      value: pendingOrdersCount.toLocaleString(),
      detail: 'in progress',
    },
    {
      label: 'Completed orders',
      value: completedOrdersCount.toLocaleString(),
      detail: 'successfully paid',
    },
    {
      label: 'Total spent',
      value: formatPrice(totalSpentAmount),
      detail: 'across completed orders',
    },
  ]

  // Product categories
  const flashSaleProducts = useMemo(() => {
    return products.filter(isFlashSaleEligible).map(mapProduct)
  }, [products])

  const recommendedProducts = useMemo(() => {
    const flashIds = new Set(flashSaleProducts.map((p) => p.id))
    const activeProducts = products
      .filter((p) => String(p.status || '').toUpperCase() === 'ACTIVE')
      .map(mapProduct)

    const nonFlash = activeProducts.filter((p) => !flashIds.has(p.id))
    return nonFlash.length > 0 ? nonFlash.slice(0, 4) : activeProducts.slice(0, 4)
  }, [products, flashSaleProducts])

  // Recent & Latest Orders
  const recentOrders = useMemo(() => {
    const sorted = [...orders].sort((a, b) => {
      if (!a.createdAt) return 1
      if (!b.createdAt) return -1
      return new Date(b.createdAt) - new Date(a.createdAt)
    })
    return sorted.slice(0, 4)
  }, [orders])

  const latestOrder = useMemo(() => {
    if (recentOrders.length === 0) return null
    return recentOrders[0]
  }, [recentOrders])

  const latestPaidOrder = useMemo(() => {
    const paidList = orders.filter((o) => {
      const st = String(o.status || '').toUpperCase()
      return st === 'PAID' || st === 'COMPLETED'
    })
    if (paidList.length > 0) return paidList[0]
    return latestOrder
  }, [orders, latestOrder])

  // Target end time for closest flash sale
  const closestEndTime = useMemo(() => {
    const eligibleWithEnd = products.filter(
      (p) => isFlashSaleEligible(p) && p.endTime
    )
    if (eligibleWithEnd.length === 0) return null
    return eligibleWithEnd[0].endTime
  }, [products])

  // Cart summary
  const cartCount = cartCtx?.count || 0
  const cartTotalFormatted = useMemo(() => {
    if (!cartCtx) return formatPrice(0)
    if (typeof cartCtx.total === 'number') {
      return formatPrice(cartCtx.total)
    }
    return formatPrice(0)
  }, [cartCtx, formatPrice])

  if (authChecking) return null

  if (loading) {
    return (
      <>
        <UserHeader />
        <main className="user-page">
          <div
            className="empty-state"
            style={{ padding: '80px 20px', textAlign: 'center' }}
          >
            <p style={{ fontSize: '18px', fontWeight: 600, color: '#555' }}>
              Loading your dashboard...
            </p>
          </div>
        </main>
      </>
    )
  }

  if (error && orders.length === 0 && products.length === 0) {
    return (
      <>
        <UserHeader />
        <main className="user-page">
          <div
            className="empty-state"
            style={{ padding: '80px 20px', textAlign: 'center' }}
          >
            <p style={{ fontSize: '16px', color: '#ef4444', marginBottom: '16px' }}>
              {error}
            </p>
            <button
              className="button button-dark"
              onClick={() => window.location.reload()}
            >
              Retry
            </button>
          </div>
        </main>
      </>
    )
  }

  return (
    <>
      <UserHeader />
      <main className="user-page">
        {/* Welcome greeting */}
        <section className="welcome-row">
          <div>
            <span className="eyebrow">CUSTOMER DASHBOARD</span>
            <h1>Good afternoon, {userName || 'there'}.</h1>
            <p>
              Welcome back to Swiftly. Here&apos;s what&apos;s happening with your
              orders.
            </p>
          </div>
          <Link className="button button-dark" href="/flash-sale">
            Shop flash sale <ArrowUpRight size={16} />
          </Link>
        </section>

        {/* Summary grid */}
        <section className="summary-grid">
          {summary.map((item, index) => (
            <article className="summary-card" key={item.label}>
              <div className="summary-icon">
                {createElement(
                  [ShoppingBag, Clock3, Check, CreditCard][index],
                  { size: 17 }
                )}
              </div>
              <span>{item.label}</span>
              <strong>{item.value}</strong>
              <small>{item.detail}</small>
            </article>
          ))}
        </section>

        {/* Live flash sale banner */}
        <section className="sale-banner">
          <div>
            <span className="eyebrow coral">LIVE FLASH SALE</span>
            <h2>Great things go quickly.</h2>
            <p>Limited-time prices on the products everyone is talking about.</p>
          </div>
          <Countdown targetEndTime={closestEndTime} />
        </section>

        {/* Flash sale products grid */}
        <section className="section-block">
          <div className="section-heading">
            <div>
              <span className="eyebrow">CURATED FOR YOU</span>
              <h2>Today&apos;s best drops</h2>
            </div>
            <Link href="/flash-sale">
              View all <ArrowUpRight size={14} />
            </Link>
          </div>
          <div className="product-grid">
            {flashSaleProducts.length > 0 ? (
              flashSaleProducts
                .slice(0, 4)
                .map((product) => (
                  <ProductCard product={product} key={product.id} />
                ))
            ) : (
              <div
                style={{
                  gridColumn: '1 / -1',
                  padding: '30px',
                  textAlign: 'center',
                  color: '#888',
                }}
              >
                No active flash sales right now. Check back soon.
              </div>
            )}
          </div>
        </section>

        {/* Recent orders & Payment confirmation */}
        <section className="dashboard-columns">
          {/* Recent orders */}
          <div className="panel">
            <div className="section-heading">
              <div>
                <span className="eyebrow">YOUR ACTIVITY</span>
                <h2>Recent orders</h2>
              </div>
              <Link href="/orders">
                View all <ArrowUpRight size={14} />
              </Link>
            </div>
            <div className="orders-list">
              {recentOrders.length === 0 ? (
                <div
                  style={{
                    padding: '30px',
                    textAlign: 'center',
                    color: '#888',
                  }}
                >
                  No orders yet.
                </div>
              ) : (
                recentOrders.map((order) => {
                  const refStr = order.orderReference || `ORD-${order.id}`
                  const prod = productsMap[order.productId]
                  const prodTitle = prod?.title || prod?.name || `Product #${order.productId}`
                  const formattedAmt = formatPrice(Number(order.totalAmount || 0))

                  return (
                    <div className="order-row" key={refStr}>
                      <div className="order-icon">
                        <Package size={16} />
                      </div>
                      <div>
                        <b>{prodTitle}</b>
                        <small>
                          {refStr} · {formatDate(order.createdAt)}
                        </small>
                      </div>
                      <div className="order-amount">
                        <b>{formattedAmt}</b>
                        <Status value={formatOrderStatus(order.status)} />
                      </div>
                      <Link
                        href={`/orders/${encodeURIComponent(refStr)}`}
                        aria-label={`View ${refStr}`}
                      >
                        <ArrowUpRight size={16} />
                      </Link>
                    </div>
                  )
                })
              )}
            </div>
          </div>

          {/* Payment confirmation card */}
          <div className="payment-card">
            <span className="payment-check">
              <Check size={17} />
            </span>
            <span className="eyebrow">PAYMENT CONFIRMATION</span>
            {latestPaidOrder ? (
              <>
                <h2>Payment successful</h2>
                <p>Your order is confirmed and on its way.</p>
                <dl>
                  <div>
                    <dt>Order</dt>
                    <dd>{latestPaidOrder.orderReference || `ORD-${latestPaidOrder.id}`}</dd>
                  </div>
                  <div>
                    <dt>Amount</dt>
                    <dd>{formatPrice(Number(latestPaidOrder.totalAmount || 0))}</dd>
                  </div>
                  <div>
                    <dt>Status</dt>
                    <dd>{formatOrderStatus(latestPaidOrder.status)}</dd>
                  </div>
                  <div>
                    <dt>Date</dt>
                    <dd>{formatDate(latestPaidOrder.createdAt)}</dd>
                  </div>
                </dl>
                <Link
                  className="button button-dark full-button"
                  href={`/orders/${encodeURIComponent(latestPaidOrder.orderReference || latestPaidOrder.id)}`}
                >
                  View order
                </Link>
              </>
            ) : (
              <>
                <h2>No payment history</h2>
                <p>Complete your first order to view payment details here.</p>
                <Link className="button button-dark full-button" href="/flash-sale">
                  Shop flash sale
                </Link>
              </>
            )}
          </div>
        </section>

        {/* Order tracking section */}
        <section className="track-panel">
          <div>
            <span className="eyebrow">ORDER TRACKING</span>
            <h2>Your latest order</h2>
            {latestOrder ? (
              <p>
                {latestOrder.orderReference || `ORD-${latestOrder.id}`} ·{' '}
                {productsMap[latestOrder.productId]?.title ||
                  productsMap[latestOrder.productId]?.name ||
                  `Product #${latestOrder.productId}`}
              </p>
            ) : (
              <p>No active orders to track.</p>
            )}
          </div>
          <div className="tracking-steps">
            {['Order Placed', 'Payment Confirmed', 'Processing', 'Completed'].map(
              (step, index) => {
                let isComplete = false
                if (latestOrder) {
                  const st = String(latestOrder.status || '').toUpperCase()
                  if (st === 'PAID' || st === 'COMPLETED') {
                    isComplete = index < 3 || st === 'COMPLETED'
                  } else if (st === 'PENDING_PAYMENT' || st === 'PENDING') {
                    isComplete = index === 0
                  }
                }

                return (
                  <div
                    className={
                      isComplete ? 'tracking-step complete' : 'tracking-step'
                    }
                    key={step}
                  >
                    <span>
                      {isComplete ? <Check size={13} /> : index + 1}
                    </span>
                    <small>{step}</small>
                  </div>
                )
              }
            )}
          </div>
        </section>

        {/* Recommended for you */}
        <section className="section-block recommendations">
          <div className="section-heading">
            <div>
              <span className="eyebrow">JUST FOR YOU</span>
              <h2>Recommended for you</h2>
            </div>
            <Link href="/">
              Continue shopping <ArrowUpRight size={14} />
            </Link>
          </div>
          <div className="product-grid recommendation-grid">
            {recommendedProducts.length > 0 ? (
              recommendedProducts
                .slice(0, 4)
                .map((product) => (
                  <ProductCard product={product} compact key={product.id} />
                ))
            ) : (
              <div
                style={{
                  gridColumn: '1 / -1',
                  padding: '30px',
                  textAlign: 'center',
                  color: '#888',
                }}
              >
                No products available right now.
              </div>
            )}
          </div>
        </section>

        {/* Cart summary strip */}
        <aside className="cart-strip">
          <div className="cart-strip-icon">
            <ShoppingBag size={18} />
          </div>
          <div>
            <b>Your cart is waiting</b>
            <span>
              {cartCount} {cartCount === 1 ? 'item' : 'items'} ready for checkout
            </span>
          </div>
          <strong>{cartTotalFormatted}</strong>
          <Link className="button button-dark" href="/cart">
            View cart
          </Link>
        </aside>
      </main>
    </>
  )
}
