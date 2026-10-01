'use client'

import Link from 'next/link'
import UserHeader from '../components/user/UserHeader'
import ProductCard from '../components/user/ProductCard'
import { useEffect, useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'
import { formatINR, useCart } from '../components/user/CartProvider'
import { useCurrency } from '../lib/currency'
import api from '../lib/api'
import { isAuthenticated, isAdmin } from '../lib/auth'

export function PageFrame({ eyebrow, title, children }) {
  return (
    <>
      <UserHeader />
      <main className="user-page support-page">
        <span className="eyebrow">{eyebrow}</span>
        <h1>{title}</h1>
        {children}
      </main>
    </>
  )
}

export { default as OrdersPage } from './orders/page'

export function OrderPage({ reference }) {
  const router = useRouter()
  const { lastOrder } = useCart()
  const { formatPrice } = useCurrency()

  const [order, setOrder] = useState(null)
  const [product, setProduct] = useState(null)
  const [loading, setLoading] = useState(true)
  const [cancelled, setCancelled] = useState(false)
  const [showCancel, setShowCancel] = useState(false)

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
    }
  }, [router])

  useEffect(() => {
    async function loadOrder() {
      if (!reference) return
      try {
        setLoading(true)

        if (lastOrder && lastOrder.reference === reference) {
          setOrder(lastOrder)
          if (lastOrder.productId) {
            try {
              const pRes = await api.get(`/api/products/${lastOrder.productId}`)
              const p = pRes.data?.data ?? pRes.data
              setProduct(p)
            } catch (e) {
              console.warn('Product lookup failed for lastOrder:', e.message)
            }
          }
          setLoading(false)
          return
        }

        const res = await api.get(`/api/orders/${reference}`)
        const ordData = res.data?.data ?? res.data

        if (!ordData || !ordData.orderReference) {
          setOrder(null)
          return
        }

        let prodDetails = null
        if (ordData.productId) {
          try {
            const pRes = await api.get(`/api/products/${ordData.productId}`)
            prodDetails = pRes.data?.data ?? pRes.data
          } catch (e) {
            console.warn(`Product lookup failed for order product ${ordData.productId}:`, e.message)
          }
        }

        const formattedDate = ordData.createdAt
          ? new Date(ordData.createdAt).toLocaleDateString('en-US', {
              month: 'short',
              day: 'numeric',
              year: 'numeric',
            })
          : 'N/A'

        const unit = Number(ordData.unitPrice || 0)
        const tot = Number(ordData.totalAmount || unit * (ordData.quantity || 1))

        setOrder({
          reference: ordData.orderReference,
          productId: ordData.productId,
          product: prodDetails?.title || prodDetails?.name || `Product #${ordData.productId}`,
          quantity: ordData.quantity,
          unitPriceNum: unit,
          totalAmountNum: tot,
          unitPrice: `₹${unit.toLocaleString('en-IN')}`,
          amount: `₹${tot.toLocaleString('en-IN')}`,
          status: ordData.status || 'PAID',
          paymentStatus: ordData.status === 'CANCELLED' ? 'CANCELLED' : 'PAID',
          date: formattedDate,
          paymentMethod: 'UPI',
          transactionId: `TXN-${ordData.id || 1000}`,
          deliveryStatus: 'Order confirmed',
          canCancel: ordData.status === 'PENDING_PAYMENT' || ordData.status === 'PENDING',
        })
        setProduct(prodDetails)
      } catch (err) {
        console.error(`Failed to fetch order ${reference}:`, err)
        setOrder(null)
      } finally {
        setLoading(false)
      }
    }

    loadOrder()
  }, [reference, lastOrder])

  if (loading) {
    return (
      <PageFrame eyebrow="ORDER SUPPORT" title="Loading order...">
        <div className="support-panel" style={{ padding: '40px', textAlign: 'center' }}>
          <p style={{ color: '#666' }}>Fetching order details...</p>
        </div>
      </PageFrame>
    )
  }

  if (!order) {
    return (
      <PageFrame eyebrow="ORDER SUPPORT" title="Order Not Found">
        <div className="support-panel order-detail">
          <p>
            We couldn&apos;t find an order matching <strong>{reference}</strong>.
          </p>
          <Link className="button button-dark" href="/orders">
            Back to My Orders
          </Link>
        </div>
      </PageFrame>
    )
  }

  const status = cancelled ? 'CANCELLED' : order.status
  const steps = ['Order Placed', 'Payment Confirmed', 'Processing', 'Completed']
  const currentStep =
    status === 'PAID' || status === 'COMPLETED'
      ? 4
      : status === 'PENDING' || status === 'PENDING_PAYMENT'
      ? 2
      : 0

  const handleBuyAgain = async () => {
    if (!order.productId) return
    try {
      const res = await api.get(`/api/products/${order.productId}`)
      const p = res.data?.data ?? res.data
      if (p && String(p.status || '').toUpperCase() === 'ACTIVE') {
        router.push(`/checkout/${order.productId}`)
      } else {
        alert('Product is currently unavailable.')
      }
    } catch (e) {
      alert('Product is currently unavailable.')
    }
  }

  return (
    <PageFrame
      eyebrow="ORDER DETAILS"
      title={
        status === 'CANCELLED'
          ? 'Order Cancelled'
          : status === 'EXPIRED'
          ? 'Order Expired'
          : 'Order Confirmed'
      }
    >
      <div className="order-detail-layout">
        <div className="support-panel order-detail">
          <Link className="back-link" href="/orders">
            ← Back to My Orders
          </Link>
          <div className="detail-heading">
            <div>
              <span className="order-reference">{order.reference}</span>
              <p>Placed on {order.date}</p>
            </div>
            <span className={`status status-${status.toLowerCase()}`}>{status}</span>
          </div>
          <Link href={`/products/${order.productId}`} className="detail-product">
            <div className="detail-image">
              <img
                src={product?.imageUrl || product?.image || '/placeholder.jpg'}
                alt={order.product}
                onError={(event) => {
                  event.currentTarget.style.opacity = '0'
                }}
              />
            </div>
            <div>
              <h2>{order.product}</h2>
              <p>
                Quantity {order.quantity} · Unit price {formatPrice(order.unitPriceNum ?? order.unitPrice)}
              </p>
              <strong className="detail-price">{formatPrice(order.totalAmountNum ?? order.amount)}</strong>
            </div>
          </Link>
          <div className="detail-grid">
            <div>
              <span>Payment method</span>
              <b>{order.paymentMethod}</b>
            </div>
            <div>
              <span>Payment status</span>
              <b>{order.paymentStatus}</b>
            </div>
            <div>
              <span>Delivery status</span>
              <b>{order.deliveryStatus}</b>
            </div>
            <div>
              <span>Transaction ID</span>
              <b>{order.transactionId}</b>
            </div>
          </div>
          {status === 'PENDING' && order.canCancel ? (
            <button
              className="button button-light"
              type="button"
              onClick={() => setShowCancel(true)}
            >
              Cancel order
            </button>
          ) : status === 'PAID' || status === 'COMPLETED' ? (
            <div className="detail-actions">
              <span className="success-copy">Order confirmed</span>
              <button className="button button-dark" type="button" onClick={handleBuyAgain}>
                Buy again
              </button>
            </div>
          ) : status === 'EXPIRED' ? (
            <Link className="button button-dark" href="/flash-sale">
              Shop flash sale
            </Link>
          ) : status === 'CANCELLED' ? (
            <Link className="button button-dark" href="/">
              Continue shopping
            </Link>
          ) : null}
        </div>
        <div className="support-panel payment-card">
          <h2>Payment Information</h2>
          <span className="payment-check">✓</span>
          <b>
            Payment{' '}
            {status === 'PAID' || status === 'COMPLETED' ? 'Successful' : 'Pending'}
          </b>
          <p>Method: {order.paymentMethod}</p>
          <p>Transaction ID: {order.transactionId}</p>
          <strong>{formatPrice(order.totalAmountNum ?? order.amount)}</strong>
        </div>
        <div className="support-panel timeline-card">
          <h2>Order status</h2>
          {status === 'CANCELLED' ? (
            <p className="danger-copy">This order was cancelled.</p>
          ) : (
            steps.map((step, index) => (
              <div
                className={`timeline-row ${index < currentStep ? 'complete' : ''}`}
                key={step}
              >
                <span>{index < currentStep ? '✓' : index + 1}</span>
                <b>{step}</b>
              </div>
            ))
          )}
        </div>
      </div>
      {showCancel && (
        <div className="modal-backdrop" role="presentation">
          <div
            className="cancel-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="cancel-title"
          >
            <h2 id="cancel-title">Cancel this order?</h2>
            <p>Your pending order will be marked as cancelled.</p>
            <div>
              <button
                className="button button-light"
                type="button"
                onClick={() => setShowCancel(false)}
              >
                Keep order
              </button>
              <button
                className="button button-dark"
                type="button"
                onClick={async () => {
                  try {
                    await api.post(`/api/orders/${reference}/cancel`)
                  } catch (e) {
                    console.warn('Backend cancel failed:', e.message)
                  }
                  setCancelled(true)
                  setShowCancel(false)
                }}
              >
                Cancel order
              </button>
            </div>
          </div>
        </div>
      )}
    </PageFrame>
  )
}

export function ProfilePage() {
  const router = useRouter()
  const { currency, setCurrency } = useCurrency()

  const [authUser, setAuthUser] = useState(null)
  const [draft, setDraft] = useState({ firstName: '', lastName: '', phone: '', address: '' })
  const [editing, setEditing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState('')

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
    }
  }, [router])

  useEffect(() => {
    let isMounted = true

    const fetchProfile = async () => {
      try {
        const stored = sessionStorage.getItem('user')
        if (!stored) {
          if (isMounted) router.push('/login')
          return
        }

        // Fetch latest real profile data from PostgreSQL backend authority
        const response = await api.get('/api/auth/me')
        const data = response.data?.data ?? response.data

        if (data && isMounted) {
          const userId = data.id || data.userId
          const updatedSession = {
            id: userId,
            userId: userId,
            email: data.email || '',
            firstName: data.firstName || '',
            lastName: data.lastName || '',
            phone: data.phone || '',
            address: data.address || '',
            roles: data.roles || ['ROLE_USER'],
            enabled: data.enabled !== false,
          }

          setAuthUser(updatedSession)
          sessionStorage.setItem('user', JSON.stringify(updatedSession))
        }
      } catch (err) {
        console.warn('Backend profile fetch failed, using session data:', err.message)
        try {
          const stored = sessionStorage.getItem('user')
          if (stored && isMounted) {
            const parsed = JSON.parse(stored)
            const userId = parsed.userId || parsed.id
            const detailsKey = `user_details_${userId}`
            const storedDetails = JSON.parse(sessionStorage.getItem(detailsKey) || localStorage.getItem(detailsKey) || '{}')

            setAuthUser({
              id: userId,
              email: parsed.email || '',
              firstName: parsed.firstName || '',
              lastName: parsed.lastName || '',
              roles: parsed.roles || ['ROLE_USER'],
              phone: parsed.phone || storedDetails.phone || '',
              address: parsed.address || storedDetails.address || '',
              enabled: parsed.enabled !== false,
            })
          } else if (isMounted) {
            router.push('/login')
          }
        } catch (e) {
          if (isMounted) router.push('/login')
        }
      }
    }

    fetchProfile()

    return () => {
      isMounted = false
    }
  }, [router])

  const userFirstName = authUser?.firstName || ''
  const userLastName = authUser?.lastName || ''
  const initials = `${userFirstName[0] || ''}${userLastName[0] || ''}`.toUpperCase() || 'CU'

  function startEditing() {
    setDraft({
      firstName: authUser?.firstName || '',
      lastName: authUser?.lastName || '',
      phone: authUser?.phone || '',
      address: authUser?.address || '',
    })
    setEditing(true)
    setSaved(false)
    setError('')
  }

  function cancelEditing() {
    setEditing(false)
    setError('')
  }

  async function saveProfile() {
    if (!authUser || saving) return
    setError('')
    setSaving(true)

    try {
      const payload = {
        firstName: draft.firstName.trim(),
        lastName: draft.lastName.trim(),
        phone: draft.phone.trim(),
        address: draft.address.trim(),
      }

      // Persist profile changes directly to PostgreSQL backend
      const response = await api.put('/api/auth/profile', payload)
      const updatedData = response.data?.data ?? response.data

      const userId = updatedData.id || updatedData.userId || authUser.id
      const updatedSession = {
        ...authUser,
        id: userId,
        userId: userId,
        firstName: updatedData.firstName || draft.firstName.trim(),
        lastName: updatedData.lastName || draft.lastName.trim(),
        phone: updatedData.phone !== undefined ? updatedData.phone : draft.phone.trim(),
        address: updatedData.address !== undefined ? updatedData.address : draft.address.trim(),
      }

      setAuthUser(updatedSession)
      sessionStorage.setItem('user', JSON.stringify(updatedSession))

      setEditing(false)
      setSaved(true)
      setTimeout(() => setSaved(false), 2600)
    } catch (err) {
      console.error('Failed to save profile to backend:', err)
      const msg = err.response?.data?.message || 'Failed to update profile on backend. Please try again.'
      setError(msg)
    } finally {
      setSaving(false)
    }
  }

  const handleLogout = async (e) => {
    e.preventDefault()
    try {
      const refreshToken = sessionStorage.getItem('refreshToken')
      if (refreshToken) {
        await api.post('/api/auth/logout', { refreshToken })
      }
    } catch (err) {
      console.warn('Logout API call failed:', err.message)
    } finally {
      sessionStorage.removeItem('accessToken')
      sessionStorage.removeItem('refreshToken')
      sessionStorage.removeItem('user')
      router.push('/login')
    }
  }

  if (!authUser) {
    return (
      <PageFrame eyebrow="YOUR ACCOUNT" title="My Profile">
        <p className="profile-subtitle">Loading account details...</p>
      </PageFrame>
    )
  }

  return (
    <PageFrame eyebrow="YOUR ACCOUNT" title="My Profile">
      <p className="profile-subtitle">Manage your account information and preferences.</p>
      <div className="profile-layout">
        <section className="profile-main">
          {/* Profile Hero Card */}
          <div className="support-panel profile-hero">
            <div className="profile-avatar avatar-tone-0">{initials}</div>
            <div>
              <span className="eyebrow">SWIFTLY MEMBER</span>
              <h2>
                {authUser.firstName} {authUser.lastName}
              </h2>
              <p>{authUser.email}</p>
            </div>
            <span className="status status-paid">
              {authUser.enabled ? 'Active' : 'Disabled'}
            </span>
          </div>

          {/* Personal Information Section */}
          <section className="support-panel profile-section">
            <div className="profile-section-heading">
              <div>
                <span className="eyebrow">YOUR DETAILS</span>
                <h2>Personal Information</h2>
              </div>
              {editing ? (
                <div className="profile-actions">
                  <button className="button button-light" type="button" onClick={cancelEditing} disabled={saving}>
                    Cancel
                  </button>
                  <button className="button button-dark" type="button" onClick={saveProfile} disabled={saving}>
                    {saving ? 'Saving...' : 'Save Changes'}
                  </button>
                </div>
              ) : (
                <button className="button button-light" type="button" onClick={startEditing}>
                  Edit Profile
                </button>
              )}
            </div>
            <div className="profile-fields">
              <label className="profile-field">
                First Name
                <input
                  value={editing ? draft.firstName : authUser.firstName}
                  readOnly={!editing}
                  onChange={(e) => setDraft({ ...draft, firstName: e.target.value })}
                />
              </label>

              <label className="profile-field">
                Last Name
                <input
                  value={editing ? draft.lastName : authUser.lastName}
                  readOnly={!editing}
                  onChange={(e) => setDraft({ ...draft, lastName: e.target.value })}
                />
              </label>

              <label className="profile-field">
                Email
                <input
                  value={authUser.email}
                  readOnly
                  disabled
                  style={{ backgroundColor: 'var(--color-bg-subtle, #f5f5f7)', cursor: 'not-allowed', color: '#666' }}
                />
              </label>

              <label className="profile-field">
                Phone Number
                <input
                  value={editing ? draft.phone : authUser.phone}
                  readOnly={!editing}
                  placeholder={editing ? 'Enter phone number' : 'Not provided'}
                  onChange={(e) => setDraft({ ...draft, phone: e.target.value })}
                />
              </label>

              <label className="profile-field">
                Address
                <input
                  value={editing ? draft.address : authUser.address}
                  readOnly={!editing}
                  placeholder={editing ? 'Enter delivery address' : 'Not provided'}
                  onChange={(e) => setDraft({ ...draft, address: e.target.value })}
                />
              </label>
            </div>
            {saved && <p className="success-copy">Profile updated successfully.</p>}
            {error && <p className="error-copy" style={{ color: '#d93025', marginTop: '10px' }}>{error}</p>}
          </section>
        </section>

        {/* Sidebar Sections */}
        <aside className="profile-side">
          {/* Account Information */}
          <section className="support-panel profile-section">
            <div className="profile-section-heading">
              <div>
                <span className="eyebrow">ACCOUNT</span>
                <h2>Account Information</h2>
              </div>
            </div>
            <div className="account-list">
              <div>
                <span>Email</span>
                <b>{authUser.email}</b>
              </div>
              <div>
                <span>Account status</span>
                <b>{authUser.enabled ? 'Active' : 'Disabled'}</b>
              </div>
              <div>
                <span>User ID</span>
                <b>{authUser.id}</b>
              </div>
            </div>
          </section>

          {/* Preferences */}
          <section className="support-panel profile-section">
            <div className="profile-section-heading">
              <div>
                <span className="eyebrow">YOUR CHOICES</span>
                <h2>Preferences</h2>
              </div>
            </div>
            <label className="select-field">
              Currency
              <select value={currency} onChange={(event) => setCurrency(event.target.value)}>
                <option value="INR">INR (₹)</option>
                <option value="USD">USD ($)</option>
                <option value="EUR">EUR (€)</option>
                <option value="GBP">GBP (£)</option>
              </select>
            </label>
          </section>

          {/* Shortcuts */}
          <section className="support-panel profile-shortcuts">
            <Link href="/orders">
              <span>My Orders</span>
              <b>→</b>
            </Link>
            <Link href="/cart">
              <span>My Cart</span>
              <b>→</b>
            </Link>
            <a href="#logout" onClick={handleLogout}>
              <span>Logout</span>
              <b>↗</b>
            </a>
          </section>
        </aside>
      </div>
    </PageFrame>
  )
}

export function CartPage() {
  const router = useRouter()
  const { items, detailedItems, count, subtotal, discount, total, updateQuantity, removeItem, loading } = useCart()
  const { formatPrice } = useCurrency()

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
    }
  }, [router])

  if (loading && (!detailedItems || detailedItems.length === 0)) {
    return (
      <PageFrame eyebrow="YOUR BAG" title="Cart">
        <div className="support-panel" style={{ padding: '60px 20px', textAlign: 'center' }}>
          <p style={{ color: '#666', fontSize: '16px', fontWeight: 500 }}>
            Loading cart...
          </p>
        </div>
      </PageFrame>
    )
  }

  if (!items || items.length === 0) {
    return (
      <PageFrame eyebrow="YOUR BAG" title="Cart">
        <div className="empty-cart support-panel">
          <h2>Your Cart is Empty</h2>
          <p>Add products to your cart and they will appear here.</p>
          <div className="cart-actions">
            <Link className="button button-dark" href="/products">
              Start Shopping →
            </Link>
            <Link className="button button-light" href="/flash-sale">
              Shop Flash Sale →
            </Link>
          </div>
        </div>
      </PageFrame>
    )
  }

  const hasUnavailableItems = (detailedItems || []).some((d) => d.isUnavailable || !d.product)

  return (
    <PageFrame eyebrow="YOUR BAG" title="Cart">
      <p className="cart-subtitle">
        {count} {count === 1 ? 'item' : 'items'} in cart
      </p>
      <div className="cart-layout">
        <section className="cart-items support-panel">
          <h2>Items in Cart</h2>
          {(detailedItems || []).map((item) => {
            const productId = item.productId
            const product = item.product

            if (!product) {
              return (
                <article className="cart-item" key={productId} style={{ borderLeft: '3px solid #ef4444' }}>
                  <div className="cart-item-main">
                    <h3 style={{ color: '#ef4444' }}>This product is no longer available.</h3>
                    <p style={{ color: '#888', fontSize: '13px' }}>Item ID: #{productId}</p>
                  </div>
                  <button
                    className="remove-item"
                    type="button"
                    onClick={() => removeItem(productId)}
                  >
                    Remove
                  </button>
                </article>
              )
            }

            const { quantity, availableStock, isInactive, isOutOfStock, isUnavailable, sellingPrice, originalPrice } = item
            const pName = product.title || product.name
            const pImage = product.imageUrl || product.image

            return (
              <article className="cart-item" key={productId}>
                <Link href={`/products/${productId}`} className="cart-image">
                  <img
                    src={pImage}
                    alt={pName}
                    onError={(e) => {
                      e.currentTarget.style.opacity = '0'
                    }}
                  />
                </Link>
                <div className="cart-item-main">
                  <Link href={`/products/${productId}`}>
                    <h3>{pName}</h3>
                  </Link>

                  {isInactive ? (
                    <span className="stock-label" style={{ color: '#ef4444', fontWeight: 600 }}>Currently unavailable</span>
                  ) : isOutOfStock ? (
                    <span className="stock-label" style={{ color: '#ef4444', fontWeight: 600 }}>Out of stock</span>
                  ) : availableStock > 0 && availableStock <= 5 ? (
                    <span className="stock-label" style={{ color: '#f59e0b', fontWeight: 600 }}>Only {availableStock} left</span>
                  ) : (
                    <span className="stock-label">In stock</span>
                  )}

                  <span className="cart-unit-price">
                    {formatPrice(sellingPrice)} {originalPrice > sellingPrice && <del>{formatPrice(originalPrice)}</del>}
                  </span>
                  <div className="quantity-control">
                    <button
                      type="button"
                      disabled={quantity <= 1 || isUnavailable}
                      aria-label={`Decrease ${pName} quantity`}
                      onClick={() => updateQuantity(productId, quantity - 1, availableStock)}
                    >
                      −
                    </button>
                    <span>{quantity}</span>
                    <button
                      type="button"
                      disabled={quantity >= availableStock || isUnavailable}
                      aria-label={`Increase ${pName} quantity`}
                      onClick={() => updateQuantity(productId, quantity + 1, availableStock)}
                    >
                      +
                    </button>
                  </div>
                </div>
                <strong>
                  {formatPrice(sellingPrice * quantity)}
                </strong>
                <button
                  className="remove-item"
                  type="button"
                  onClick={() => removeItem(productId)}
                >
                  Remove
                </button>
              </article>
            )
          })}
        </section>

        <aside className="cart-summary support-panel">
          <h2>Price Summary</h2>
          <div>
            <span>Subtotal</span>
            <b>{formatPrice(subtotal)}</b>
          </div>
          <div>
            <span>Discount</span>
            <b className="discount-text">-{formatPrice(discount)}</b>
          </div>
          <div>
            <span>Flash Sale Savings</span>
            <b className="discount-text">-{formatPrice(discount)}</b>
          </div>
          <div className="summary-total">
            <span>Total</span>
            <strong>{formatPrice(total)}</strong>
          </div>

          {hasUnavailableItems && (
            <p className="danger-copy" style={{ marginTop: '12px', fontSize: '12px' }}>
              Some items in your cart are currently unavailable or out of stock. Please remove them to proceed.
            </p>
          )}

          {hasUnavailableItems ? (
            <button
              className="button button-dark full-button"
              type="button"
              disabled
              style={{ opacity: 0.6, cursor: 'not-allowed' }}
            >
              Checkout
            </button>
          ) : (
            <Link
              className="button button-dark full-button"
              href={detailedItems && detailedItems.length === 1 ? `/checkout/${detailedItems[0].productId}` : '/checkout/cart'}
            >
              Checkout
            </Link>
          )}

          <Link className="button button-light full-button" href="/products">
            Continue Shopping
          </Link>
        </aside>
      </div>
    </PageFrame>
  )
}

export function ProductPage({ id }) {
  const router = useRouter()
  const [product, setProduct] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [added, setAdded] = useState(false)
  const { addItem } = useCart()

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
    }
  }, [router])

  useEffect(() => {
    async function loadProduct() {
      if (!id) return
      try {
        setLoading(true)
        setError('')

        const res = await api.get(`/api/products/${id}`)
        const prodData = res.data?.data ?? res.data

        if (!prodData || !prodData.id) {
          throw new Error('Product not found.')
        }

        let available = prodData.initialStock ?? 0
        try {
          const invRes = await api.get(`/api/inventory/${id}`)
          const invData = invRes.data?.data ?? invRes.data
          if (invData && invData.availableStock != null) {
            available = Number(invData.availableStock)
          }
        } catch (invErr) {
          console.warn(`Inventory fetch failed for product ${id}:`, invErr.message)
        }

        const original = Number(prodData.originalPrice || 0)
        const sale = Number(prodData.flashSalePrice ?? prodData.salePrice ?? original)
        const discountPct =
          original > sale
            ? Math.round(((original - sale) / original) * 100)
            : 0

        const isInactive = String(prodData.status || '').toUpperCase() !== 'ACTIVE'
        const isOutOfStock = available <= 0

        let stockText = 'In stock'
        if (isOutOfStock) {
          stockText = 'Out of stock'
        } else if (available >= 1 && available <= 5) {
          stockText = `Only ${available} left`
        }

        setProduct({
          id: prodData.id,
          name: prodData.title || prodData.name || `Product #${prodData.id}`,
          image: prodData.imageUrl || prodData.image || '/placeholder.jpg',
          price: `₹${sale.toLocaleString('en-IN')}`,
          original: original > sale ? `₹${original.toLocaleString('en-IN')}` : '',
          discount: discountPct > 0 ? `${discountPct}% OFF` : 'In Stock',
          stock: stockText,
          availableStock: available,
          isOutOfStock,
          isInactive,
          status: prodData.status,
          rawProduct: prodData,
        })
      } catch (err) {
        console.error('Failed to fetch product details:', err)
        setError('Product not found or unavailable.')
      } finally {
        setLoading(false)
      }
    }

    loadProduct()
  }, [id])

  if (loading) {
    return (
      <PageFrame eyebrow="PRODUCT DETAIL" title="Loading product...">
        <div className="support-panel" style={{ padding: '40px', textAlign: 'center' }}>
          <p style={{ color: '#666' }}>Loading product details...</p>
        </div>
      </PageFrame>
    )
  }

  if (error || !product) {
    return (
      <PageFrame eyebrow="PRODUCT DETAIL" title="Product not found">
        <div className="support-panel" style={{ padding: '40px' }}>
          <p style={{ color: '#ef4444', marginBottom: '16px' }}>
            We couldn&apos;t find that product.
          </p>
          <Link className="button button-dark" href="/products">
            Back to products
          </Link>
        </div>
      </PageFrame>
    )
  }

  if (product.isInactive) {
    return (
      <PageFrame eyebrow="PRODUCT DETAIL" title={product.name}>
        <div className="product-detail">
          <ProductCard product={product} />
          <div className="support-panel" style={{ padding: '40px' }}>
            <h2 style={{ color: '#ef4444', marginBottom: '12px' }}>
              Product is currently unavailable.
            </h2>
            <p style={{ color: '#666', marginBottom: '20px' }}>
              This item is currently inactive and unavailable for purchase.
            </p>
            <div className="product-detail-actions">
              <button
                className="button button-light"
                type="button"
                disabled
                style={{ opacity: 0.5, cursor: 'not-allowed' }}
              >
                Add to Cart
              </button>
              <button
                className="button button-dark"
                type="button"
                disabled
                style={{ opacity: 0.5, cursor: 'not-allowed' }}
              >
                Buy Now
              </button>
            </div>
          </div>
        </div>
      </PageFrame>
    )
  }

  return (
    <PageFrame eyebrow="PRODUCT DETAIL" title={product.name}>
      <div className="product-detail">
        <ProductCard product={product} />
        <div className="support-panel">
          <h2>Made for the moments that matter.</h2>
          <p>Limited-time Swiftly pricing, ready to ship while stock lasts.</p>
          <div className="product-detail-actions">
            {product.isOutOfStock ? (
              <>
                <button
                  className="button button-light"
                  type="button"
                  disabled
                  style={{ opacity: 0.5, cursor: 'not-allowed' }}
                >
                  Add to Cart
                </button>
                <button
                  className="button button-dark"
                  type="button"
                  disabled
                  style={{ opacity: 0.5, cursor: 'not-allowed' }}
                >
                  Out of stock
                </button>
              </>
            ) : (
              <>
                <button
                  className="button button-light"
                  type="button"
                  onClick={() => {
                    addItem(product.id)
                    setAdded(true)
                  }}
                >
                  Add to Cart
                </button>
                <Link className="button button-dark" href={`/checkout/${product.id}`}>
                  Buy Now
                </Link>
              </>
            )}
          </div>
          {added && <p className="success-copy">Added to cart.</p>}
        </div>
      </div>
    </PageFrame>
  )
}

export function CheckoutPage({ id }) {
  const router = useRouter()
  const { detailedItems, subtotal: cartSubtotal, total: cartTotal, clearCart, addItem, setLastOrder } = useCart()
  const { formatPrice } = useCurrency()

  const [product, setProduct] = useState(null)
  const [loading, setLoading] = useState(true)
  const [fetchError, setFetchError] = useState('')
  const [isInactive, setIsInactive] = useState(false)
  const [availableStock, setAvailableStock] = useState(0)

  const [quantity, setQuantity] = useState(1)
  const [method, setMethod] = useState('UPI')
  const [processing, setProcessing] = useState(false)
  const [formErrors, setFormErrors] = useState('')
  const [form, setForm] = useState({
    name: '',
    email: '',
    phone: '',
    address: '',
  })

  useEffect(() => {
    try {
      const stored = sessionStorage.getItem('user')
      if (stored) {
        const p = JSON.parse(stored)
        const userId = p.userId || p.id
        const details = JSON.parse(sessionStorage.getItem(`user_details_${userId}`) || localStorage.getItem(`user_details_${userId}`) || '{}')
        setForm({
          name: `${p.firstName || ''} ${p.lastName || ''}`.trim() || 'Customer User',
          email: p.email || 'customer@example.com',
          phone: details.phone || p.phone || '',
          address: details.address || p.address || '',
        })
      }
    } catch (e) {
      console.warn('Failed to prefill checkout customer form:', e)
    }
  }, [])

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
    }

    async function loadProductData() {
      if (!id) return

      if (id === 'cart') {
        if (!detailedItems || detailedItems.length === 0) {
          setFetchError('Your cart is empty.')
          setLoading(false)
          return
        }
        setProduct({
          id: 'cart',
          title: `Cart Checkout (${detailedItems.length} items)`,
          name: `Cart Checkout (${detailedItems.length} items)`,
          image: detailedItems[0]?.product?.image || '/placeholder.jpg',
          imageUrl: detailedItems[0]?.product?.image || '/placeholder.jpg',
          originalPrice: cartSubtotal,
          flashSalePrice: cartSubtotal,
        })
        setAvailableStock(999)
        setLoading(false)
        return
      }

      try {
        setLoading(true)
        setFetchError('')
        setIsInactive(false)

        const res = await api.get(`/api/products/${id}`)
        const prodData = res.data?.data ?? res.data

        if (!prodData || !prodData.id) {
          setFetchError('Product not found')
          return
        }

        if (String(prodData.status || '').toUpperCase() !== 'ACTIVE') {
          setIsInactive(true)
          setProduct(prodData)
          return
        }

        let stock = prodData.initialStock ?? 0
        try {
          const invRes = await api.get(`/api/inventory/${id}`)
          const invData = invRes.data?.data ?? invRes.data
          if (invData && invData.availableStock != null) {
            stock = Number(invData.availableStock)
          }
        } catch (invErr) {
          console.warn(`Inventory fetch failed for product ${id}:`, invErr.message)
        }

        setAvailableStock(stock)
        setProduct(prodData)
      } catch (err) {
        console.error('Failed to load product for checkout:', err)
        if (err.response?.status === 404) {
          setFetchError('Product not found')
        } else {
          setFetchError('Unable to load product. Please try again.')
        }
      } finally {
        setLoading(false)
      }
    }

    loadProductData()
  }, [id, router, detailedItems, cartSubtotal])

  if (loading) {
    return (
      <PageFrame eyebrow="SECURE CHECKOUT" title="Loading checkout...">
        <div className="support-panel" style={{ padding: '40px', textAlign: 'center' }}>
          <p style={{ color: '#666' }}>Loading product details for checkout...</p>
        </div>
      </PageFrame>
    )
  }

  if (fetchError || !product) {
    return (
      <PageFrame eyebrow="SECURE CHECKOUT" title="Product not found">
        <div className="support-panel" style={{ padding: '40px' }}>
          <p style={{ color: '#ef4444', marginBottom: '16px' }}>
            {fetchError || "We couldn't find that product."}
          </p>
          <Link className="button button-dark" href="/products">
            Back to products
          </Link>
        </div>
      </PageFrame>
    )
  }

  if (isInactive) {
    return (
      <PageFrame eyebrow="SECURE CHECKOUT" title="Product Unavailable">
        <div className="support-panel" style={{ padding: '40px' }}>
          <h2 style={{ color: '#ef4444', marginBottom: '12px' }}>
            Product is currently unavailable.
          </h2>
          <p style={{ color: '#666', marginBottom: '20px' }}>
            This product is currently inactive and cannot be purchased.
          </p>
          <Link className="button button-dark" href="/products">
            Back to products
          </Link>
        </div>
      </PageFrame>
    )
  }

  const isOutOfStock = availableStock <= 0
  const originalPrice = Number(product.originalPrice || 0)
  const flashSalePrice = Number(product.flashSalePrice ?? product.salePrice ?? originalPrice)
  const price = flashSalePrice > 0 ? flashSalePrice : originalPrice
  const total = id === 'cart' ? cartTotal : price * quantity

  const maxAllowedQuantity = Math.min(5, Math.max(1, availableStock))

  const submitOrder = async () => {
    if (Object.values(form).some((value) => !String(value || '').trim())) {
      setFormErrors('Please complete all customer information fields.')
      return
    }

    if (!method) {
      setFormErrors('Please select a payment method.')
      return
    }

    try {
      setFormErrors('')
      setProcessing(true)

      let lastRef = ''
      let lastTxn = ''
      let totalAmountPaid = 0

      if (id === 'cart') {
        const checkoutItems = (detailedItems || []).filter((d) => !d.isUnavailable)
        if (checkoutItems.length === 0) {
          throw new Error('No valid items available for checkout in your cart.')
        }

        for (const item of checkoutItems) {
          const idempotencyKey = `ORD-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`
          const orderPayload = {
            productId: Number(item.productId),
            quantity: Number(item.quantity),
            idempotencyKey,
          }

          const orderRes = await api.post('/api/orders', orderPayload)
          const orderData = orderRes.data?.data ?? orderRes.data

          if (!orderData || !orderData.orderReference) {
            throw new Error(`Order creation failed for product #${item.productId}.`)
          }

          const orderReference = orderData.orderReference
          const payableAmount = Number(orderData.totalAmount ?? item.itemSubtotal)

          const payIdempotencyKey = `PAY-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`
          const paymentPayload = {
            orderReference,
            amount: payableAmount,
            paymentMethod: method,
            idempotencyKey: payIdempotencyKey,
          }

          const payRes = await api.post('/api/payments', paymentPayload)
          const payData = payRes.data?.data ?? payRes.data

          lastRef = orderReference
          lastTxn = payData?.transactionId || `TXN-${Math.floor(100000 + Math.random() * 900000)}`
          totalAmountPaid += payableAmount
        }

        const createdOrder = {
          reference: lastRef,
          product: `Cart (${checkoutItems.length} items)`,
          quantity: checkoutItems.reduce((s, i) => s + i.quantity, 0),
          unitPrice: formatPrice(totalAmountPaid),
          amount: formatPrice(totalAmountPaid),
          totalAmountNum: totalAmountPaid,
          status: 'PAID',
          orderStatus: 'COMPLETED',
          paymentStatus: 'PAID',
          date: new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }),
          paymentMethod: method,
          transactionId: lastTxn,
          deliveryStatus: 'Order confirmed',
          canCancel: false,
        }

        clearCart()
        setLastOrder(createdOrder)
        router.push(`/order-success?reference=${encodeURIComponent(lastRef)}`)
      } else {
        if (isOutOfStock) {
          setFormErrors('Product is out of stock.')
          return
        }

        if (quantity > availableStock) {
          setFormErrors(`Only ${availableStock} units available in stock.`)
          return
        }

        const idempotencyKey = `ORD-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`
        const orderPayload = {
          productId: Number(product.id),
          quantity: Number(quantity),
          idempotencyKey,
        }

        const orderRes = await api.post('/api/orders', orderPayload)
        const orderData = orderRes.data?.data ?? orderRes.data

        if (!orderData || !orderData.orderReference) {
          throw new Error('Order creation did not return a valid order reference.')
        }

        const orderReference = orderData.orderReference
        const payableAmount = Number(orderData.totalAmount ?? total)

        const payIdempotencyKey = `PAY-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`
        const paymentPayload = {
          orderReference,
          amount: payableAmount,
          paymentMethod: method,
          idempotencyKey: payIdempotencyKey,
        }

        const payRes = await api.post('/api/payments', paymentPayload)
        const payData = payRes.data?.data ?? payRes.data

        const transactionId = payData?.transactionId || `TXN-${Math.floor(100000 + Math.random() * 900000)}`

        const createdOrder = {
          reference: orderReference,
          product: product.title || product.name || `Product #${product.id}`,
          productId: product.id,
          quantity,
          unitPrice: formatPrice(price),
          amount: formatPrice(payableAmount),
          totalAmountNum: payableAmount,
          status: 'PAID',
          orderStatus: 'COMPLETED',
          paymentStatus: 'PAID',
          date: new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }),
          paymentMethod: method,
          transactionId,
          deliveryStatus: 'Order confirmed',
          canCancel: false,
        }

        addItem(product.id, quantity)
        setLastOrder(createdOrder)
        router.push(`/order-success?reference=${encodeURIComponent(orderReference)}`)
      }
    } catch (err) {
      console.error('Checkout processing error:', err)
      const backendMsg = err.response?.data?.message || err.message || 'Payment processing failed. Please try again.'
      setFormErrors(backendMsg)
    } finally {
      setProcessing(false)
    }
  }

  return (
    <PageFrame eyebrow="SECURE CHECKOUT" title="Checkout">
      <div className="checkout-layout">
        <section className="checkout-main">
          <div className="support-panel checkout-card">
            <h2>Customer Information</h2>
            <div className="checkout-fields">
              <label>
                Full Name
                <input
                  value={form.name}
                  onChange={(event) => setForm({ ...form, name: event.target.value })}
                />
              </label>
              <label>
                Email
                <input
                  type="email"
                  value={form.email}
                  onChange={(event) => setForm({ ...form, email: event.target.value })}
                />
              </label>
              <label>
                Phone Number
                <input
                  value={form.phone}
                  onChange={(event) => setForm({ ...form, phone: event.target.value })}
                />
              </label>
              <label>
                Address
                <textarea
                  value={form.address}
                  onChange={(event) => setForm({ ...form, address: event.target.value })}
                />
              </label>
            </div>
          </div>

          <div className="support-panel checkout-card">
            <h2>Payment Method</h2>
            <div className="payment-options">
              {['UPI', 'Card', 'Cash on Delivery'].map((option) => (
                <label
                  className={`payment-option ${method === option ? 'selected' : ''}`}
                  key={option}
                >
                  <input
                    type="radio"
                    name="payment"
                    value={option}
                    checked={method === option}
                    onChange={() => setMethod(option)}
                  />
                  <span>
                    <b>{option}</b>
                    <small>
                      {option === 'UPI'
                        ? 'Pay securely with your UPI app'
                        : option === 'Card'
                        ? 'Visa, Mastercard, RuPay'
                        : 'Pay when your order arrives'}
                    </small>
                  </span>
                </label>
              ))}
            </div>
          </div>
        </section>

        <aside className="support-panel checkout-summary">
          <h2>Order Summary</h2>
          <div className="checkout-product">
            <img
              src={product.imageUrl || product.image || '/placeholder.jpg'}
              alt={product.title || product.name}
              onError={(e) => {
                e.currentTarget.style.opacity = '0'
              }}
            />
            <div>
              <b>{product.title || product.name}</b>
              {id !== 'cart' && (
                <span>
                  Available Stock: {isOutOfStock ? '0 (Out of stock)' : availableStock}
                </span>
              )}
            </div>
          </div>

          {id !== 'cart' && (
            <div className="quantity-control checkout-quantity">
              <button
                type="button"
                disabled={quantity <= 1 || isOutOfStock}
                onClick={() => setQuantity((value) => Math.max(1, value - 1))}
              >
                −
              </button>
              <span>{quantity}</span>
              <button
                type="button"
                disabled={quantity >= maxAllowedQuantity || isOutOfStock}
                onClick={() => setQuantity((value) => Math.min(maxAllowedQuantity, value + 1))}
              >
                +
              </button>
            </div>
          )}

          <div className="checkout-lines">
            {id !== 'cart' && originalPrice > price && (
              <div>
                <span>Product Price</span>
                <b>{formatPrice(originalPrice * quantity)}</b>
              </div>
            )}
            {id !== 'cart' && originalPrice > price && (
              <div>
                <span>Discount</span>
                <b className="discount-text">
                  -{formatPrice((originalPrice - price) * quantity)}
                </b>
              </div>
            )}
            {id !== 'cart' && (
              <div>
                <span>Flash Sale Price</span>
                <b>{formatPrice(price * quantity)}</b>
              </div>
            )}
            <div className="summary-total">
              <span>Total</span>
              <strong>{formatPrice(total)}</strong>
            </div>
          </div>

          {formErrors && <p className="danger-copy">{formErrors}</p>}

          <button
            className="button button-dark full-button"
            type="button"
            disabled={processing || (id !== 'cart' && isOutOfStock)}
            style={id !== 'cart' && isOutOfStock ? { opacity: 0.6, cursor: 'not-allowed' } : {}}
            onClick={submitOrder}
          >
            {processing
              ? 'Processing...'
              : id !== 'cart' && isOutOfStock
              ? 'Out of stock'
              : method === 'Cash on Delivery'
              ? 'Place Order'
              : 'Pay Now'}
          </button>
        </aside>
      </div>
    </PageFrame>
  )
}

export function OrderSuccessPage() {
  const router = useRouter()
  const { lastOrder } = useCart()
  const { formatPrice } = useCurrency()
  const [refParam, setRefParam] = useState('')

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
      const searchParams = new URLSearchParams(window.location.search)
      setRefParam(searchParams.get('reference') || '')
    }
  }, [router])

  const order = lastOrder || (refParam ? {
    reference: refParam,
    transactionId: 'TXN-CONFIRMED',
    amount: '',
    paymentMethod: 'UPI'
  } : {
    reference: 'ORD-FLASH-1003',
    transactionId: 'TXN-100003',
    amount: 69999,
    paymentMethod: 'UPI'
  })

  const rawAmt = typeof order.amount === 'number' ? order.amount : (parseInt(String(order.amount || '').replace(/[^0-9]/g, ''), 10) || 0)
  const displayAmount = rawAmt > 0 ? formatPrice(rawAmt) : order.amount

  return (
    <PageFrame eyebrow="ORDER CONFIRMATION" title="Order Confirmed">
      <div className="success-panel support-panel">
        <div className="success-icon">✓</div>
        <h2>Payment Successful</h2>
        <p>Your Swiftly order has been confirmed.</p>
        <div className="success-details">
          <div>
            <span>Order Reference</span>
            <b>{order.reference}</b>
          </div>
          <div>
            <span>Transaction ID</span>
            <b>{order.transactionId}</b>
          </div>
          {displayAmount ? (
            <div>
              <span>Amount Paid</span>
              <b>{displayAmount}</b>
            </div>
          ) : null}
          <div>
            <span>Payment Method</span>
            <b>{order.paymentMethod}</b>
          </div>
        </div>
        <div className="success-actions">
          <Link
            className="button button-dark"
            href={`/orders/${encodeURIComponent(order.reference)}`}
          >
            View Order →
          </Link>
          <Link className="button button-light" href="/orders">
            View My Orders →
          </Link>
          <Link className="button button-light" href="/">
            Continue Shopping →
          </Link>
        </div>
      </div>
    </PageFrame>
  )
}

export { default as FlashSalePage } from './flash-sale/page'
