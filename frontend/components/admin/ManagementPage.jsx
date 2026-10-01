'use client'

import { useEffect, useMemo, useState } from 'react'
import { Eye, Pencil, Plus, RotateCcw, Trash2, X } from 'lucide-react'
import api from '@/lib/api'
import { Modal, PageHeading, ProductThumb, StatCards, StatusPill, Toolbar } from './AdminUI'

export function InventoryPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [modal, setModal] = useState(null)
  const [quantity, setQuantity] = useState(10)
  const [replenishing, setReplenishing] = useState(false)
  const [modalError, setModalError] = useState('')

  const loadInventory = async () => {
    try {
      setLoading(true)
      setError('')

      const productsRes = await api.get('/api/products?page=0&size=100&sort=createdAt,desc')
      const pageData = productsRes.data?.data ?? productsRes.data
      const products = pageData?.content ?? (Array.isArray(pageData) ? pageData : [])

      const inventoryList = await Promise.all(
        products.map(async (prod) => {
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
            console.warn(`Could not fetch inventory for product ${prod.id}:`, invErr.message)
          }

          return {
            id: prod.id,
            productId: prod.id,
            name: prod.title || prod.name || `Product #${prod.id}`,
            image: prod.imageUrl || prod.image || '/images/aeropods.jpg',
            imageUrl: prod.imageUrl || prod.image || '/images/aeropods.jpg',
            available,
            locked,
            total,
          }
        })
      )

      setRows(inventoryList)
    } catch (err) {
      console.error('Failed to load inventory:', err)
      const msg =
        err.response?.data?.message ||
        err.response?.data?.errorDetails?.[0]?.message ||
        err.message ||
        'Failed to load inventory data.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadInventory()
  }, [])

  const totalInventory = useMemo(
    () => rows.reduce((sum, r) => sum + (r.total || 0), 0),
    [rows]
  )
  const availableStock = useMemo(
    () => rows.reduce((sum, r) => sum + (r.available || 0), 0),
    [rows]
  )
  const lockedStock = useMemo(
    () => rows.reduce((sum, r) => sum + (r.locked || 0), 0),
    [rows]
  )
  const lowStockCount = useMemo(
    () => rows.filter((r) => r.available < 20).length,
    [rows]
  )

  const visible = useMemo(() => {
    if (!search.trim()) return rows
    const searchLower = search.trim().toLowerCase()
    return rows.filter((row) => row.name.toLowerCase().includes(searchLower))
  }, [rows, search])

  const openReplenishModal = (row) => {
    setModal(row)
    setQuantity(10)
    setModalError('')
  }

  const handleReplenish = async () => {
    if (!modal) return
    const qty = Number(quantity)
    if (!qty || isNaN(qty) || qty < 1) {
      setModalError('Quantity must be at least 1.')
      return
    }

    try {
      setReplenishing(true)
      setModalError('')

      await api.post(`/api/inventory/replenish?productId=${modal.id}&quantity=${qty}`)

      setModal(null)
      await loadInventory()
    } catch (err) {
      console.error('Replenish failed:', err)
      const msg =
        err.response?.data?.message ||
        err.response?.data?.errorDetails?.[0]?.message ||
        err.response?.data?.errors?.[0]?.message ||
        err.message ||
        'Failed to replenish stock.'
      setModalError(msg)
    } finally {
      setReplenishing(false)
    }
  }

  return (
    <>
      <PageHeading
        eyebrow="Stock control"
        title="Inventory"
        description="Monitor available, locked, and replenished stock."
      />
      <StatCards
        items={[
          {
            label: 'Total inventory',
            value: totalInventory.toLocaleString(),
            note: 'units',
            detail: 'across catalog',
          },
          {
            label: 'Available stock',
            value: availableStock.toLocaleString(),
            note: 'ready to sell',
            detail: 'active stock',
          },
          {
            label: 'Locked stock',
            value: lockedStock.toLocaleString(),
            note: 'reserved',
            detail: 'active checkouts',
          },
          {
            label: 'Low stock',
            value: lowStockCount,
            note: 'Needs attention',
            detail: 'below threshold',
            warning: true,
          },
        ]}
      />
      <Toolbar
        search={search}
        setSearch={setSearch}
        placeholder="Search inventory..."
      />
      <section className="panel">
        <div className="table-scroll">
          {loading ? (
            <div className="empty-state">
              <p>Loading inventory...</p>
            </div>
          ) : error ? (
            <div className="empty-state">
              <p>{error}</p>
              <button className="button primary" onClick={loadInventory}>
                Retry
              </button>
            </div>
          ) : (
            <>
              <table>
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Available stock</th>
                    <th>Locked stock</th>
                    <th>Total stock</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {visible.map((row) => (
                    <tr key={row.id}>
                      <td>
                        <div className="table-product">
                          <ProductThumb product={row} />
                          <strong>{row.name}</strong>
                        </div>
                      </td>
                      <td>{row.available}</td>
                      <td>{row.locked}</td>
                      <td>{row.total ?? (row.available + row.locked)}</td>
                      <td>
                        <StatusPill>
                          {row.available < 10
                            ? 'Critical'
                            : row.available < 20
                            ? 'Low'
                            : 'Healthy'}
                        </StatusPill>
                      </td>
                      <td>
                        <div className="row-actions">
                          <button onClick={() => openReplenishModal(row)}>
                            <RotateCcw size={15} /> Replenish
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {visible.length === 0 && (
                <p className="empty-state">No inventory available.</p>
              )}
            </>
          )}
        </div>
      </section>

      {modal && (
        <Modal
          title={`Replenish ${modal.name}`}
          onClose={() => {
            if (!replenishing) {
              setModal(null)
              setModalError('')
            }
          }}
          onConfirm={handleReplenish}
          confirmLabel={replenishing ? 'Replenishing...' : 'Replenish stock'}
        >
          {modalError && (
            <p style={{ color: '#ef4444', marginBottom: '12px', fontSize: '14px' }}>
              {modalError}
            </p>
          )}
          <p className="modal-copy">
            Current available stock: <strong>{modal.available}</strong>
          </p>
          <label className="modal-field">
            Add quantity
            <input
              type="number"
              value={quantity}
              onChange={(e) => setQuantity(e.target.value)}
              min="1"
              disabled={replenishing}
            />
          </label>
        </Modal>
      )}
    </>
  )
}

export function OrdersPage() {
  const [rows, setRows] = useState([])
  const [productsMap, setProductsMap] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('All')
  const [modal, setModal] = useState(null)
  const [cancellingRef, setCancellingRef] = useState(null)

  const loadOrders = async () => {
    try {
      setLoading(true)
      setError('')

      const ordersRes = await api.get('/api/orders?page=0&size=100&sort=createdAt,desc')
      const pageData = ordersRes.data?.data ?? ordersRes.data
      const orderList = pageData?.content ?? (Array.isArray(pageData) ? pageData : [])

      let prodMap = {}
      try {
        const productsRes = await api.get('/api/products?page=0&size=100')
        const prodPage = productsRes.data?.data ?? productsRes.data
        const prodList = prodPage?.content ?? (Array.isArray(prodPage) ? prodPage : [])
        prodList.forEach((p) => {
          if (p.id) {
            prodMap[p.id] = p.title || p.name
          }
        })
      } catch (pErr) {
        console.warn('Could not fetch product catalog for names:', pErr.message)
      }

      setProductsMap(prodMap)
      setRows(orderList)
    } catch (err) {
      console.error('Failed to load orders:', err)
      const msg =
        err.response?.data?.message ||
        err.response?.data?.errorDetails?.[0]?.message ||
        err.message ||
        'Failed to load orders.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadOrders()
  }, [])

  const handleCancelOrder = async (orderRef, userId) => {
    if (!window.confirm(`Are you sure you want to cancel order ${orderRef}?`)) {
      return
    }
    try {
      setCancellingRef(orderRef)
      await api.post(`/api/orders/${orderRef}/cancel`, null, {
        headers: { 'X-User-Id': userId || 1 },
      })
      await loadOrders()
      if (modal && (modal.orderReference === orderRef || modal.id === orderRef)) {
        setModal(null)
      }
    } catch (err) {
      console.error('Failed to cancel order:', err)
      const msg =
        err.response?.data?.message ||
        err.response?.data?.errorDetails?.[0]?.message ||
        err.message ||
        'Failed to cancel order.'
      alert(msg)
    } finally {
      setCancellingRef(null)
    }
  }

  const formatStatus = (st) => {
    if (!st) return 'Unknown'
    const u = st.toUpperCase()
    if (u === 'PENDING_PAYMENT') return 'Pending'
    if (u === 'PAID') return 'Paid'
    if (u === 'CANCELLED') return 'Cancelled'
    if (u === 'EXPIRED') return 'Expired'
    return st
  }

  const formatDate = (isoString) => {
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

  const totalOrders = rows.length
  const pendingOrders = useMemo(
    () => rows.filter((r) => (r.status || '').toUpperCase() === 'PENDING_PAYMENT').length,
    [rows]
  )
  const paidOrders = useMemo(
    () => rows.filter((r) => (r.status || '').toUpperCase() === 'PAID').length,
    [rows]
  )
  const cancelledOrders = useMemo(
    () => rows.filter((r) => ['CANCELLED', 'EXPIRED'].includes((r.status || '').toUpperCase())).length,
    [rows]
  )

  const pendingPct = totalOrders > 0 ? Math.round((pendingOrders / totalOrders) * 100) : 0
  const paidPct = totalOrders > 0 ? Math.round((paidOrders / totalOrders) * 100) : 0
  const cancelledPct = totalOrders > 0 ? Math.round((cancelledOrders / totalOrders) * 100) : 0

  const visible = useMemo(() => {
    return rows.filter((row) => {
      const orderRef = (row.orderReference || row.id || '').toString().toLowerCase()
      const customer = `User #${row.userId}`.toLowerCase()
      const prodName = (productsMap[row.productId] || `Product #${row.productId}`).toLowerCase()
      const sTerm = search.trim().toLowerCase()

      const matchesSearch =
        !sTerm ||
        orderRef.includes(sTerm) ||
        customer.includes(sTerm) ||
        prodName.includes(sTerm) ||
        String(row.userId).includes(sTerm) ||
        String(row.productId).includes(sTerm)

      let matchesFilter = true
      if (filter !== 'All') {
        const normStatus = (row.status || '').toUpperCase()
        if (filter === 'Paid') matchesFilter = normStatus === 'PAID'
        else if (filter === 'Pending') matchesFilter = normStatus === 'PENDING_PAYMENT'
        else if (filter === 'Cancelled') matchesFilter = normStatus === 'CANCELLED'
        else if (filter === 'Expired') matchesFilter = normStatus === 'EXPIRED'
        else matchesFilter = normStatus === filter.toUpperCase()
      }

      return matchesSearch && matchesFilter
    })
  }, [rows, search, filter, productsMap])

  return (
    <>
      <PageHeading
        eyebrow="Commerce"
        title="Orders"
        description="Track customer orders and fulfillment status."
      />
      <StatCards
        items={[
          {
            label: 'Total orders',
            value: totalOrders.toLocaleString(),
            note: 'total',
            detail: 'across system',
          },
          {
            label: 'Pending',
            value: pendingOrders.toLocaleString(),
            note: `${pendingPct}%`,
            detail: 'awaiting payment',
          },
          {
            label: 'Paid',
            value: paidOrders.toLocaleString(),
            note: `${paidPct}%`,
            detail: 'successful orders',
          },
          {
            label: 'Cancelled',
            value: cancelledOrders.toLocaleString(),
            note: `${cancelledPct}%`,
            detail: 'cancelled / expired',
          },
        ]}
      />
      <Toolbar
        search={search}
        setSearch={setSearch}
        placeholder="Search order reference, user ID, or product..."
        filter={filter}
        setFilter={setFilter}
        filterOptions={['Paid', 'Pending', 'Cancelled', 'Expired']}
      />
      <section className="panel">
        <div className="table-scroll">
          {loading ? (
            <div className="empty-state">
              <p>Loading orders...</p>
            </div>
          ) : error ? (
            <div className="empty-state">
              <p>{error}</p>
              <button className="button primary" onClick={loadOrders}>
                Retry
              </button>
            </div>
          ) : (
            <>
              <table>
                <thead>
                  <tr>
                    <th>Order reference</th>
                    <th>Customer</th>
                    <th>Product</th>
                    <th>Qty</th>
                    <th>Amount</th>
                    <th>Status</th>
                    <th>Date</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {visible.map((row) => {
                    const isPending = (row.status || '').toUpperCase() === 'PENDING_PAYMENT'
                    const isCancellingThis = cancellingRef === row.orderReference

                    return (
                      <tr key={row.orderReference || row.id}>
                        <td className="order-id">{row.orderReference || `ORD-${row.id}`}</td>
                        <td>
                          User #{row.userId}
                          <small>User ID: {row.userId}</small>
                        </td>
                        <td>
                          {productsMap[row.productId] || `Product #${row.productId}`}
                          <small>Product ID: {row.productId}</small>
                        </td>
                        <td>{row.quantity}</td>
                        <td>₹{Number(row.totalAmount || 0).toLocaleString()}</td>
                        <td>
                          <StatusPill>{formatStatus(row.status)}</StatusPill>
                        </td>
                        <td>{formatDate(row.createdAt)}</td>
                        <td>
                          <div className="row-actions">
                            <button
                              className="icon-button"
                              onClick={() => setModal(row)}
                              aria-label="View order"
                              title="View Order Details"
                            >
                              <Eye size={15} />
                            </button>
                            {isPending && (
                              <button
                                className="icon-button"
                                style={{ color: '#ef4444' }}
                                onClick={() => handleCancelOrder(row.orderReference, row.userId)}
                                disabled={isCancellingThis}
                                aria-label="Cancel order"
                                title="Cancel Order"
                              >
                                <X size={15} />
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
              {visible.length === 0 && (
                <p className="empty-state">No orders found.</p>
              )}
            </>
          )}
        </div>
      </section>

      {modal && (
        <Modal title={modal.orderReference || `ORD-${modal.id}`} onClose={() => setModal(null)}>
          <div className="detail-list">
            <p>
              <span>Customer</span>
              <strong>User #{modal.userId}</strong>
            </p>
            <p>
              <span>Product</span>
              <strong>{productsMap[modal.productId] || `Product #${modal.productId}`} (ID: {modal.productId})</strong>
            </p>
            <p>
              <span>Quantity</span>
              <strong>{modal.quantity}</strong>
            </p>
            <p>
              <span>Unit Price</span>
              <strong>₹{Number(modal.unitPrice || 0).toLocaleString()}</strong>
            </p>
            <p>
              <span>Total Amount</span>
              <strong>₹{Number(modal.totalAmount || 0).toLocaleString()}</strong>
            </p>
            <p>
              <span>Status</span>
              <StatusPill>{formatStatus(modal.status)}</StatusPill>
            </p>
            <p>
              <span>Created At</span>
              <strong>{formatDate(modal.createdAt)}</strong>
            </p>
            {modal.paymentDeadline && (
              <p>
                <span>Payment Deadline</span>
                <strong>{formatDate(modal.paymentDeadline)}</strong>
              </p>
            )}
          </div>
          {(modal.status || '').toUpperCase() === 'PENDING_PAYMENT' && (
            <div style={{ marginTop: '16px', display: 'flex', justifyContent: 'flex-end' }}>
              <button
                className="button danger"
                onClick={() => handleCancelOrder(modal.orderReference, modal.userId)}
                disabled={cancellingRef === modal.orderReference}
              >
                {cancellingRef === modal.orderReference ? 'Cancelling...' : 'Cancel Order'}
              </button>
            </div>
          )}
        </Modal>
      )}
    </>
  )
}

export function PaymentsPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('All')
  const [modal, setModal] = useState(null)

  const loadPayments = async () => {
    try {
      setLoading(true)
      setError('')

      const res = await api.get('/api/payments?page=0&size=100&sort=createdAt,desc')
      const pageData = res.data?.data ?? res.data
      const paymentList = pageData?.content ?? (Array.isArray(pageData) ? pageData : [])

      setRows(paymentList)
    } catch (err) {
      console.error('Failed to load payments:', err)
      const msg =
        err.response?.data?.message ||
        err.response?.data?.errorDetails?.[0]?.message ||
        err.message ||
        'Failed to load payments.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadPayments()
  }, [])

  const formatStatus = (st) => {
    if (!st) return 'Unknown'
    const u = st.toUpperCase()
    if (u === 'SUCCESS') return 'Paid'
    if (u === 'PENDING') return 'Pending'
    if (u === 'FAILED') return 'Failed'
    if (u === 'REFUNDED') return 'Refunded'
    return st
  }

  const formatDate = (isoString) => {
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

  const totalPayments = rows.length
  const successfulCount = useMemo(
    () => rows.filter((r) => (r.status || '').toUpperCase() === 'SUCCESS').length,
    [rows]
  )
  const pendingCount = useMemo(
    () => rows.filter((r) => (r.status || '').toUpperCase() === 'PENDING').length,
    [rows]
  )
  const totalRevenue = useMemo(
    () =>
      rows
        .filter((r) => (r.status || '').toUpperCase() === 'SUCCESS')
        .reduce((sum, r) => sum + Number(r.amount || 0), 0),
    [rows]
  )

  const successPct = totalPayments > 0 ? Math.round((successfulCount / totalPayments) * 100) : 0
  const pendingPct = totalPayments > 0 ? Math.round((pendingCount / totalPayments) * 100) : 0

  const visible = useMemo(() => {
    return rows.filter((row) => {
      const txnId = (row.transactionId || row.id || '').toString().toLowerCase()
      const orderRef = (row.orderReference || '').toLowerCase()
      const customer = `User #${row.userId}`.toLowerCase()
      const method = (row.paymentMethod || '').toLowerCase()
      const sTerm = search.trim().toLowerCase()

      const matchesSearch =
        !sTerm ||
        txnId.includes(sTerm) ||
        orderRef.includes(sTerm) ||
        customer.includes(sTerm) ||
        method.includes(sTerm) ||
        String(row.userId).includes(sTerm)

      let matchesFilter = true
      if (filter !== 'All') {
        const normStatus = (row.status || '').toUpperCase()
        if (filter === 'Paid' || filter === 'Success') matchesFilter = normStatus === 'SUCCESS'
        else if (filter === 'Pending') matchesFilter = normStatus === 'PENDING'
        else if (filter === 'Failed') matchesFilter = normStatus === 'FAILED'
        else if (filter === 'Refunded') matchesFilter = normStatus === 'REFUNDED'
        else matchesFilter = normStatus === filter.toUpperCase()
      }

      return matchesSearch && matchesFilter
    })
  }, [rows, search, filter])

  return (
    <>
      <PageHeading
        eyebrow="Finance"
        title="Payments"
        description="Review transaction health and total revenue."
      />
      <StatCards
        items={[
          {
            label: 'Total payments',
            value: totalPayments.toLocaleString(),
            note: 'total',
            detail: 'all transactions',
          },
          {
            label: 'Successful',
            value: successfulCount.toLocaleString(),
            note: `${successPct}%`,
            detail: 'settled payments',
          },
          {
            label: 'Pending',
            value: pendingCount.toLocaleString(),
            note: `${pendingPct}%`,
            detail: 'awaiting confirmation',
          },
          {
            label: 'Total revenue',
            value: `₹${totalRevenue.toLocaleString()}`,
            note: 'settled',
            detail: 'settled revenue',
          },
        ]}
      />
      <Toolbar
        search={search}
        setSearch={setSearch}
        placeholder="Search transaction or order..."
        filter={filter}
        setFilter={setFilter}
        filterOptions={['Paid', 'Pending', 'Failed']}
      />
      <section className="panel">
        <div className="table-scroll">
          {loading ? (
            <div className="empty-state">
              <p>Loading payments...</p>
            </div>
          ) : error ? (
            <div className="empty-state">
              <p>{error}</p>
              <button className="button primary" onClick={loadPayments}>
                Retry
              </button>
            </div>
          ) : (
            <>
              <table>
                <thead>
                  <tr>
                    <th>Transaction ID</th>
                    <th>Order reference</th>
                    <th>Customer</th>
                    <th>Amount</th>
                    <th>Method</th>
                    <th>Status</th>
                    <th>Date</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {visible.map((row) => (
                    <tr key={row.transactionId || row.id}>
                      <td className="order-id">{row.transactionId || `TXN-${row.id}`}</td>
                      <td>{row.orderReference}</td>
                      <td>
                        User #{row.userId}
                        <small>User ID: {row.userId}</small>
                      </td>
                      <td>₹{Number(row.amount || 0).toLocaleString()}</td>
                      <td>{row.paymentMethod}</td>
                      <td>
                        <StatusPill>{formatStatus(row.status)}</StatusPill>
                      </td>
                      <td>{formatDate(row.createdAt)}</td>
                      <td>
                        <button
                          className="icon-button"
                          onClick={() => setModal(row)}
                          aria-label="View payment"
                          title="View Payment Details"
                        >
                          <Eye size={15} />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {visible.length === 0 && (
                <p className="empty-state">No payment records found.</p>
              )}
            </>
          )}
        </div>
      </section>

      {modal && (
        <Modal title={modal.transactionId || `TXN-${modal.id}`} onClose={() => setModal(null)}>
          <div className="detail-list">
            <p>
              <span>Transaction ID</span>
              <strong>{modal.transactionId || modal.id}</strong>
            </p>
            <p>
              <span>Order Reference</span>
              <strong>{modal.orderReference}</strong>
            </p>
            <p>
              <span>Customer</span>
              <strong>User #{modal.userId}</strong>
            </p>
            <p>
              <span>Payment Method</span>
              <strong>{modal.paymentMethod}</strong>
            </p>
            <p>
              <span>Amount</span>
              <strong>₹{Number(modal.amount || 0).toLocaleString()}</strong>
            </p>
            <p>
              <span>Status</span>
              <StatusPill>{formatStatus(modal.status)}</StatusPill>
            </p>
            <p>
              <span>Created At</span>
              <strong>{formatDate(modal.createdAt)}</strong>
            </p>
            {modal.failureReason && (
              <p>
                <span>Failure Reason</span>
                <strong style={{ color: '#ef4444' }}>{modal.failureReason}</strong>
              </p>
            )}
          </div>
        </Modal>
      )}
    </>
  )
}

export function UsersPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('All')
  const [modal, setModal] = useState(null)
  const [deleting, setDeleting] = useState(false)
  const [modalError, setModalError] = useState('')

  const loadUsers = async () => {
    try {
      setLoading(true)
      setError('')

      const res = await api.get('/api/auth/users')
      const pageData = res.data?.data ?? res.data
      const userList = Array.isArray(pageData) ? pageData : pageData?.content ?? []

      setRows(userList)
    } catch (err) {
      console.error('Failed to load users:', err)
      const msg =
        err.response?.data?.message ||
        err.response?.data?.errorDetails?.[0]?.message ||
        err.message ||
        'Unable to load users.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadUsers()
  }, [])

  const formatDate = (isoString) => {
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

  // Filter out any administrator accounts (only display ROLE_USER customers)
  const customerRows = useMemo(() => {
    return rows.filter((r) => {
      const roles = r.roles
      const roleArray = Array.isArray(roles) ? roles : Array.from(roles || [])
      return !roleArray.includes('ROLE_ADMIN')
    })
  }, [rows])

  const totalUsers = customerRows.length
  const activeUsers = useMemo(
    () => customerRows.filter((r) => r.enabled !== false).length,
    [customerRows]
  )
  const activePct = totalUsers > 0 ? Math.round((activeUsers / totalUsers) * 100) : 0

  const recentUsers = useMemo(() => {
    const now = new Date()
    return customerRows.filter((r) => {
      if (!r.createdAt) return false
      try {
        const d = new Date(r.createdAt)
        return (
          d.getMonth() === now.getMonth() &&
          d.getFullYear() === now.getFullYear()
        )
      } catch (e) {
        return false
      }
    }).length
  }, [customerRows])

  const visible = useMemo(() => {
    return customerRows.filter((row) => {
      const userName = (
        row.name ||
        `${row.firstName || ''} ${row.lastName || ''}`
      ).toLowerCase()
      const email = (row.email || '').toLowerCase()
      const sTerm = search.trim().toLowerCase()

      const matchesSearch =
        !sTerm || userName.includes(sTerm) || email.includes(sTerm)

      let matchesFilter = true
      if (filter !== 'All') {
        const isEnabled = row.enabled !== false
        if (filter === 'Active') matchesFilter = isEnabled
        else if (filter === 'Disabled') matchesFilter = !isEnabled
        else matchesFilter = true
      }

      return matchesSearch && matchesFilter
    })
  }, [customerRows, search, filter])

  const handleDeleteUser = async (user) => {
    if (!user || deleting) return
    try {
      setDeleting(true)
      setModalError('')

      await api.delete(`/api/auth/users/${user.id}`)

      setModal(null)
      await loadUsers()
    } catch (err) {
      console.error('Failed to delete user account:', err)
      const msg =
        err.response?.data?.message ||
        err.response?.data?.errorDetails?.[0]?.message ||
        err.message ||
        'Failed to delete user account.'
      setModalError(msg)
    } finally {
      setDeleting(false)
    }
  }

  return (
    <>
      <PageHeading
        eyebrow="Customers"
        title="Users"
        description={`${totalUsers} customer accounts in Swiftly.`}
      />
      <StatCards
        items={[
          {
            label: 'Total users',
            value: totalUsers.toLocaleString(),
            note: 'total',
            detail: 'customer accounts',
          },
          {
            label: 'Active users',
            value: activeUsers.toLocaleString(),
            note: `${activePct}%`,
            detail: 'currently enabled',
          },
          {
            label: 'Recent users',
            value: recentUsers.toLocaleString(),
            note: 'this month',
            detail: 'new accounts',
          },
        ]}
      />
      <Toolbar
        search={search}
        setSearch={setSearch}
        placeholder="Search name or email..."
        filter={filter}
        setFilter={setFilter}
        filterOptions={['Active', 'Disabled']}
      />
      <section className="panel">
        <div className="table-scroll">
          {loading ? (
            <div className="empty-state">
              <p>Loading users...</p>
            </div>
          ) : error ? (
            <div className="empty-state">
              <p>{error}</p>
              <button className="button primary" onClick={loadUsers}>
                Retry
              </button>
            </div>
          ) : (
            <>
              <table>
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Status</th>
                    <th>Joined</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {visible.map((row) => {
                    const displayName =
                      row.name ||
                      `${row.firstName || ''} ${row.lastName || ''}`.trim() ||
                      row.email

                    const isEnabled = row.enabled !== false
                    const joinedDate = formatDate(row.createdAt)

                    return (
                      <tr key={row.id}>
                        <td>
                          <strong>{displayName}</strong>
                        </td>
                        <td>{row.email}</td>
                        <td>Customer</td>
                        <td>
                          <StatusPill>{isEnabled ? 'Active' : 'Disabled'}</StatusPill>
                        </td>
                        <td>{joinedDate}</td>
                        <td>
                          <div className="row-actions">
                            <button
                              onClick={() => {
                                setModal({ type: 'view', user: row })
                                setModalError('')
                              }}
                              aria-label="View user"
                              title="View User Details"
                            >
                              <Eye size={15} />
                            </button>
                            <button
                              onClick={() => {
                                setModal({ type: 'delete', user: row })
                                setModalError('')
                              }}
                              aria-label="Delete user"
                              title="Delete User Account"
                              style={{ color: '#ef4444' }}
                            >
                              <Trash2 size={15} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
              {visible.length === 0 && (
                <p className="empty-state">No customer accounts found.</p>
              )}
            </>
          )}
        </div>
      </section>

      {modal?.type === 'view' && (
        <Modal
          title={
            modal.user.name ||
            `${modal.user.firstName || ''} ${modal.user.lastName || ''}`.trim() ||
            modal.user.email
          }
          onClose={() => setModal(null)}
        >
          <div className="detail-list">
            <p>
              <span>User ID</span>
              <strong>#{modal.user.id}</strong>
            </p>
            <p>
              <span>Email</span>
              <strong>{modal.user.email}</strong>
            </p>
            <p>
              <span>Role</span>
              <strong>Customer</strong>
            </p>
            <p>
              <span>Status</span>
              <StatusPill>{modal.user.enabled !== false ? 'Active' : 'Disabled'}</StatusPill>
            </p>
            <p>
              <span>Joined</span>
              <strong>{formatDate(modal.user.createdAt)}</strong>
            </p>
          </div>
        </Modal>
      )}

      {modal?.type === 'delete' && (
        <Modal
          title="Delete this user account?"
          onClose={() => {
            if (!deleting) setModal(null)
          }}
          onConfirm={() => handleDeleteUser(modal.user)}
          confirmLabel={deleting ? 'Deleting...' : 'Delete account'}
          danger
        >
          {modalError && (
            <p style={{ color: '#ef4444', marginBottom: '12px', fontSize: '14px' }}>
              {modalError}
            </p>
          )}
          <p className="modal-copy">
            Are you sure you want to delete user account <strong>{modal.user.name || modal.user.email}</strong>? This action will permanently remove the user from the database and prevent future logins.
          </p>
        </Modal>
      )}
    </>
  )
}

export const inventoryRows = [
  { id: 'p1', name: 'AirPods', available: 18, locked: 4, image: '/images/aeropods.jpg' },
  { id: 'p2', name: 'iPhone 17', available: 100, locked: 12, image: '/images/iphone.jpg' },
  { id: 'p3', name: 'Lumen Lamp', available: 42, locked: 5, image: '/images/lumen-lamp.jpg' },
  { id: 'p4', name: 'Nova Mirrorless Camera', available: 9, locked: 2, image: '/images/nova-camera.jpg' },
  { id: 'p6', name: 'Pulse Smartwatch', available: 12, locked: 8, image: '/images/pulse-smartwatch.jpg' },
]
