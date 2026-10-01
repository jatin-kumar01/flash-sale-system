'use client'

import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { useEffect, useRef, useState } from 'react'
import { Bell, ChevronDown, Menu, Search, ShoppingBag, X } from 'lucide-react'
import { getStoreName } from '../../lib/auth'
import { useCart } from './CartProvider'
import api from '../../lib/api'

export default function UserHeader() {
  const pathname = usePathname()
  const router = useRouter()
  const { count } = useCart()

  const [cartCount, setCartCount] = useState(0)
  const [userInitials, setUserInitials] = useState('CU')
  const [mobileOpen, setMobileOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [storeName, setStoreName] = useState('Swiftly')

  // Notifications State
  const [notificationsList, setNotificationsList] = useState([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [notificationsLoading, setNotificationsLoading] = useState(false)

  // Search State
  const [searchOpen, setSearchOpen] = useState(false)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [searchLoading, setSearchLoading] = useState(false)
  const [searchError, setSearchError] = useState('')

  const notificationRef = useRef(null)
  const searchRef = useRef(null)
  const searchInputRef = useRef(null)

  // Hydration-safe cart count sync after component mounts in browser
  useEffect(() => {
    setCartCount(count)
  }, [count])

  const fetchNotifications = async () => {
    try {
      setNotificationsLoading(true)
      const userStr = typeof window !== 'undefined' ? sessionStorage.getItem('user') : null
      if (!userStr) {
        setNotificationsList([])
        setUnreadCount(0)
        return
      }
      const user = JSON.parse(userStr)
      const userId = user.id || user.userId
      if (!userId) {
        setNotificationsList([])
        setUnreadCount(0)
        return
      }

      let list = []
      const seenKeys = new Set()

      // 1. Fetch backend notification log list if available
      try {
        const res = await api.get('/api/notifications/my-notifications-list')
        const logs = res.data?.data ?? res.data ?? []
        if (Array.isArray(logs)) {
          logs.forEach((logItem) => {
            const key = `log-${logItem.id || logItem.orderReference + '-' + logItem.eventType}`
            if (!seenKeys.has(key)) {
              seenKeys.add(key)
              let type = logItem.eventType || 'ORDER_CONFIRMED'
              let title = logItem.subject || 'Notification'
              let text = logItem.content ? logItem.content.replace(/<[^>]*>?/gm, '') : ''
              let link = logItem.orderReference ? `/orders/${logItem.orderReference}` : ''

              if (type.includes('PAYMENT_COMPLETED_SUCCESS') || type.includes('PAYMENT_SUCCESS')) {
                type = 'PAYMENT_SUCCESS'
                title = 'Payment successful'
                text = `Your payment for ${logItem.orderReference || 'your order'} was successful.`
              } else if (type.includes('ORDER_CREATED') || type.includes('ORDER_CONFIRMED')) {
                type = 'ORDER_CONFIRMED'
                title = 'Order confirmed'
                text = `Your order ${logItem.orderReference} has been confirmed.`
              } else if (type.includes('ORDER_CANCELLED')) {
                type = 'ORDER_CANCELLED'
                title = 'Order cancelled'
                text = `Your order ${logItem.orderReference} was cancelled.`
              } else if (type.includes('ORDER_EXPIRED')) {
                type = 'ORDER_EXPIRED'
                title = 'Order expired'
                text = `Your payment window for ${logItem.orderReference} expired.`
              }

              list.push({
                id: key,
                backendId: logItem.id,
                type,
                title,
                text,
                link,
                createdAt: logItem.createdAt || new Date().toISOString(),
                isRead: Boolean(logItem.isRead),
              })
            }
          })
        }
      } catch (e) {
        // Ignore backend log fetch error
      }

      // 2. Fetch customer's real orders
      try {
        const ordersRes = await api.get('/api/orders')
        const ordersData = ordersRes.data?.data ?? ordersRes.data ?? []
        const ordersArr = ordersData.content || (Array.isArray(ordersData) ? ordersData : [])

        ordersArr.forEach((ord) => {
          const ordRef = ord.orderReference || `ORD-${ord.id}`
          const ordStatus = String(ord.status || '').toUpperCase()

          if (ordStatus === 'PAID' || ordStatus === 'COMPLETED') {
            const key = `order-paid-${ordRef}`
            if (!seenKeys.has(key)) {
              seenKeys.add(key)
              list.push({
                id: key,
                type: 'PAYMENT_SUCCESS',
                title: 'Payment successful',
                text: `Your payment for ${ordRef} was successful.`,
                link: `/orders/${ordRef}`,
                createdAt: ord.updatedAt || ord.createdAt || new Date().toISOString(),
                isRead: false,
              })
            }
          } else if (ordStatus === 'PENDING' || ordStatus === 'CREATED') {
            const key = `order-confirmed-${ordRef}`
            if (!seenKeys.has(key)) {
              seenKeys.add(key)
              list.push({
                id: key,
                type: 'ORDER_CONFIRMED',
                title: 'Order confirmed',
                text: `Your order ${ordRef} has been confirmed.`,
                link: `/orders/${ordRef}`,
                createdAt: ord.createdAt || new Date().toISOString(),
                isRead: false,
              })
            }
          } else if (ordStatus === 'CANCELLED') {
            const key = `order-cancelled-${ordRef}`
            if (!seenKeys.has(key)) {
              seenKeys.add(key)
              list.push({
                id: key,
                type: 'ORDER_CANCELLED',
                title: 'Order cancelled',
                text: `Your order ${ordRef} was cancelled.`,
                link: `/orders/${ordRef}`,
                createdAt: ord.updatedAt || ord.createdAt || new Date().toISOString(),
                isRead: false,
              })
            }
          } else if (ordStatus === 'EXPIRED') {
            const key = `order-expired-${ordRef}`
            if (!seenKeys.has(key)) {
              seenKeys.add(key)
              list.push({
                id: key,
                type: 'ORDER_EXPIRED',
                title: 'Order expired',
                text: `Your payment window for ${ordRef} expired.`,
                link: `/orders/${ordRef}`,
                createdAt: ord.updatedAt || ord.createdAt || new Date().toISOString(),
                isRead: false,
              })
            }
          }
        })
      } catch (e) {
        // Ignore order fetch errors
      }

      // 3. Check for active flash sale products
      try {
        const prodRes = await api.get('/api/products?page=0&size=50')
        const pageData = prodRes.data?.data ?? prodRes.data ?? {}
        const prods = pageData.content || (Array.isArray(pageData) ? pageData : [])
        const activeFlash = prods.filter(
          (p) => String(p.status || '').toUpperCase() === 'ACTIVE' && (p.flashSalePrice != null || p.salePrice != null)
        )
        if (activeFlash.length > 0) {
          const key = `flash-sale-live`
          if (!seenKeys.has(key)) {
            seenKeys.add(key)
            list.push({
              id: key,
              type: 'FLASH_SALE',
              title: 'Flash sale is live',
              text: `${activeFlash.length} product${activeFlash.length > 1 ? 's are' : ' is'} currently on flash sale!`,
              link: '/flash-sale',
              createdAt: new Date().toISOString(),
              isRead: false,
            })
          }
        }
      } catch (e) {
        // Ignore product fetch errors
      }

      // 4. Check cart for real low stock
      try {
        const cartStr = localStorage.getItem('cart')
        if (cartStr) {
          const cartItems = JSON.parse(cartStr)
          if (Array.isArray(cartItems) && cartItems.length > 0) {
            for (const cItem of cartItems) {
              const pId = cItem.product?.id || cItem.productId || cItem.id
              const pName = cItem.product?.title || cItem.product?.name || cItem.name || 'Item'
              if (pId) {
                try {
                  const invRes = await api.get(`/api/inventory/${pId}`)
                  const invData = invRes.data?.data ?? invRes.data
                  const avail = invData?.availableStock != null ? Number(invData.availableStock) : null
                  if (avail !== null && avail > 0 && avail <= 5) {
                    const key = `low-stock-${pId}`
                    if (!seenKeys.has(key)) {
                      seenKeys.add(key)
                      list.push({
                        id: key,
                        type: 'LOW_STOCK',
                        title: 'Stock running low',
                        text: `Stock is running low for ${pName} in your cart (Only ${avail} left).`,
                        link: '/cart',
                        createdAt: new Date().toISOString(),
                        isRead: false,
                      })
                    }
                  }
                } catch (e) {
                  // ignore inventory error
                }
              }
            }
          }
        }
      } catch (e) {
        // Ignore cart error
      }

      // Merge read state from localStorage
      const readKeysStr = localStorage.getItem('readNotificationKeys')
      const readKeys = readKeysStr ? JSON.parse(readKeysStr) : []
      const readSet = new Set(readKeys)

      const merged = list.map((item) => ({
        ...item,
        isRead: item.isRead || readSet.has(item.id),
      }))

      setNotificationsList(merged)
      setUnreadCount(merged.filter((n) => !n.isRead).length)
    } catch (err) {
      console.error('Failed to fetch notifications:', err)
    } finally {
      setNotificationsLoading(false)
    }
  }

  const handleMarkAllRead = async () => {
    try {
      await api.put('/api/notifications/mark-all-read')
    } catch (e) {
      // Ignore API failure
    }
    const allIds = notificationsList.map((n) => n.id)
    localStorage.setItem('readNotificationKeys', JSON.stringify(allIds))
    setNotificationsList((prev) => prev.map((n) => ({ ...n, isRead: true })))
    setUnreadCount(0)
  }

  const handleNotificationClick = async (item) => {
    if (item.backendId) {
      try {
        await api.put(`/api/notifications/${item.backendId}/read`)
      } catch (e) {
        // Ignore API failure
      }
    }
    const readKeysStr = localStorage.getItem('readNotificationKeys')
    const currentReadKeys = readKeysStr ? JSON.parse(readKeysStr) : []
    if (!currentReadKeys.includes(item.id)) {
      currentReadKeys.push(item.id)
      localStorage.setItem('readNotificationKeys', JSON.stringify(currentReadKeys))
    }
    setNotificationsList((prev) =>
      prev.map((n) => (n.id === item.id ? { ...n, isRead: true } : n))
    )
    setUnreadCount((prev) => Math.max(0, prev - 1))
    setNotificationsOpen(false)
    if (item.link) {
      router.push(item.link)
    }
  }

  useEffect(() => {
    function handleClickOutside(event) {
      if (notificationRef.current && !notificationRef.current.contains(event.target)) {
        setNotificationsOpen(false)
      }
      if (searchRef.current && !searchRef.current.contains(event.target)) {
        setSearchOpen(false)
      }
    }

    function handleKeyDownGlobal(event) {
      if (event.key === 'Escape') {
        setNotificationsOpen(false)
        setSearchOpen(false)
      }
    }

    document.addEventListener('mousedown', handleClickOutside)
    document.addEventListener('keydown', handleKeyDownGlobal)

    fetchNotifications()
    const interval = setInterval(() => {
      fetchNotifications()
    }, 45000)

    const loadHeaderData = () => {
      setStoreName(getStoreName())
      try {
        const stored = sessionStorage.getItem('user')
        if (stored) {
          const u = JSON.parse(stored)
          const fn = u.firstName || ''
          const ln = u.lastName || ''
          const init = `${fn[0] || ''}${ln[0] || ''}`.toUpperCase()
          if (init) setUserInitials(init)
        }
      } catch (e) {
        console.warn('Failed to load user header info:', e)
      }
      fetchNotifications()
    }

    loadHeaderData()
    window.addEventListener('swiftlySettingsUpdated', loadHeaderData)
    window.addEventListener('swiftlyCartUpdated', loadHeaderData)
    window.addEventListener('storage', loadHeaderData)

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleKeyDownGlobal)
      clearInterval(interval)
      window.removeEventListener('swiftlySettingsUpdated', loadHeaderData)
      window.removeEventListener('swiftlyCartUpdated', loadHeaderData)
      window.removeEventListener('storage', loadHeaderData)
    }
  }, [])

  // Debounced Search API fetch
  useEffect(() => {
    if (!searchOpen || !searchQuery.trim()) {
      setSearchResults([])
      setSearchLoading(false)
      setSearchError('')
      return
    }

    setSearchLoading(true)
    setSearchError('')

    const timer = setTimeout(async () => {
      try {
        const res = await api.get('/api/products?page=0&size=100')
        const pageData = res.data?.data ?? res.data
        const content = pageData?.content ?? (Array.isArray(pageData) ? pageData : [])

        // Filter active products
        const activeProducts = content.filter(
          (p) => String(p.status || '').toUpperCase() === 'ACTIVE'
        )

        const term = searchQuery.trim().toLowerCase()
        const matched = activeProducts.filter((p) => {
          const title = (p.title || p.name || '').toLowerCase()
          const desc = (p.description || '').toLowerCase()
          return title.includes(term) || desc.includes(term)
        })

        const mapped = await Promise.all(
          matched.slice(0, 10).map(async (prod) => {
            let available = prod.initialStock ?? 0
            try {
              const invRes = await api.get(`/api/inventory/${prod.id}`)
              const invData = invRes.data?.data ?? invRes.data
              if (invData && invData.availableStock != null) {
                available = Number(invData.availableStock)
              }
            } catch (e) {
              // fallback to initialStock
            }

            const original = Number(prod.originalPrice || 0)
            const sale = Number(prod.flashSalePrice ?? prod.salePrice ?? original)
            const isOutOfStock = available <= 0

            let stockText = 'In stock'
            if (isOutOfStock) {
              stockText = 'Out of stock'
            } else if (available >= 1 && available <= 5) {
              stockText = `Only ${available} left`
            }

            return {
              id: prod.id,
              name: prod.title || prod.name || `Product #${prod.id}`,
              image: prod.imageUrl || prod.image || '/placeholder.jpg',
              price: `₹${sale.toLocaleString('en-IN')}`,
              originalPrice: original > sale ? `₹${original.toLocaleString('en-IN')}` : '',
              stockText,
              isOutOfStock,
            }
          })
        )

        setSearchResults(mapped)
      } catch (err) {
        console.error('Search error:', err)
        setSearchError('Unable to search products. Please try again.')
      } finally {
        setSearchLoading(false)
      }
    }, 350)

    return () => clearTimeout(timer)
  }, [searchQuery, searchOpen])

  function closeMobile() {
    setMobileOpen(false)
  }

  const toggleSearch = () => {
    setSearchOpen((open) => {
      const next = !open
      if (next) {
        setTimeout(() => {
          searchInputRef.current?.focus()
        }, 50)
      } else {
        setSearchQuery('')
        setSearchResults([])
      }
      return next
    })
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && searchQuery.trim()) {
      e.preventDefault()
      const q = searchQuery.trim()
      setSearchOpen(false)
      setSearchQuery('')
      router.push(`/products?search=${encodeURIComponent(q)}`)
    } else if (e.key === 'Escape') {
      setSearchOpen(false)
      setSearchQuery('')
    }
  }

  const handleLogout = (e) => {
    if (e) e.preventDefault()
    sessionStorage.removeItem('accessToken')
    sessionStorage.removeItem('refreshToken')
    sessionStorage.removeItem('user')
    setProfileOpen(false)
    router.push('/login')
  }

  return (
    <header className="user-header">
      <Link className="brand" href="/" onClick={closeMobile}>
        ● {storeName}
      </Link>
      <button
        className="mobile-menu"
        onClick={() => setMobileOpen((open) => !open)}
        aria-label={mobileOpen ? 'Close navigation' : 'Open navigation'}
      >
        {mobileOpen ? <X size={19} /> : <Menu size={19} />}
      </button>
      <nav className={mobileOpen ? 'user-nav open' : 'user-nav'}>
        <Link href="/" className={pathname === '/' ? 'active' : ''} onClick={closeMobile}>
          Home
        </Link>
        <Link
          href="/dashboard"
          className={pathname === '/dashboard' ? 'active' : ''}
          onClick={closeMobile}
        >
          Dashboard
        </Link>
        <Link
          href="/flash-sale"
          className={pathname?.startsWith('/flash-sale') ? 'active' : ''}
          onClick={closeMobile}
        >
          Flash Sale
        </Link>
        <Link
          href="/products"
          className={pathname?.startsWith('/products') ? 'active' : ''}
          onClick={closeMobile}
        >
          Products
        </Link>
        <Link
          href="/orders"
          className={pathname?.startsWith('/orders') ? 'active' : ''}
          onClick={closeMobile}
        >
          My Orders
        </Link>
        <Link
          href="/profile"
          className={pathname === '/profile' ? 'active' : ''}
          onClick={closeMobile}
        >
          Profile
        </Link>
        <Link href="/cart" className={pathname === '/cart' ? 'active' : ''} onClick={closeMobile}>
          Cart
        </Link>
      </nav>
      <div className="header-actions">
        <div className="search-popover-wrap" ref={searchRef} style={{ position: 'relative' }}>
          <button
            className="header-icon"
            onClick={toggleSearch}
            aria-label="Search"
          >
            <Search size={17} />
          </button>
          {searchOpen && (
            <div
              className="search-dropdown-wrapper"
              style={{
                position: 'absolute',
                top: 'calc(100% + 8px)',
                right: 0,
                width: '380px',
                maxWidth: 'calc(100vw - 24px)',
                zIndex: 9999,
                display: 'flex',
                flexDirection: 'column',
                gap: '6px',
                boxSizing: 'border-box',
              }}
            >
              <div
                className="search-input-wrap"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  background: '#ffffff',
                  border: '1px solid #ddd',
                  borderRadius: '10px',
                  padding: '4px 10px',
                  boxShadow: '0 4px 16px rgba(0, 0, 0, 0.1)',
                  boxSizing: 'border-box',
                  width: '100%',
                }}
              >
                <Search size={15} className="search-input-icon" style={{ color: '#888', marginRight: '6px', flexShrink: 0 }} />
                <input
                  ref={searchInputRef}
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  onKeyDown={handleKeyDown}
                  className="search-input"
                  placeholder="Search products..."
                  aria-label="Search products"
                  style={{
                    flex: 1,
                    border: 'none',
                    outline: 'none',
                    fontSize: '13px',
                    padding: '6px 0',
                    background: 'transparent',
                    color: '#171717',
                    minWidth: 0,
                    boxSizing: 'border-box',
                  }}
                />
                <button
                  className="search-close-btn"
                  onClick={() => {
                    setSearchOpen(false)
                    setSearchQuery('')
                  }}
                  aria-label="Close search"
                  style={{
                    border: 'none',
                    background: 'transparent',
                    color: '#777',
                    cursor: 'pointer',
                    padding: '4px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    borderRadius: '50%',
                    flexShrink: 0,
                  }}
                >
                  <X size={15} />
                </button>
              </div>

              {searchQuery.trim() !== '' && (
                <div
                  className="search-results-dropdown"
                  style={{
                    width: '100%',
                    maxHeight: '400px',
                    overflowY: 'auto',
                    overflowX: 'hidden',
                    background: '#ffffff',
                    border: '1px solid #e5e5e5',
                    borderRadius: '10px',
                    padding: '6px',
                    boxShadow: '0 12px 35px rgba(0,0,0,0.15)',
                    boxSizing: 'border-box',
                  }}
                >
                  {searchLoading ? (
                    <div className="search-status-item" style={{ padding: '16px', textAlign: 'center', color: '#666', fontSize: '13px', fontWeight: 500 }}>
                      Searching...
                    </div>
                  ) : searchError ? (
                    <div className="search-status-item search-error" style={{ padding: '16px', textAlign: 'center', color: '#ef4444', fontSize: '13px', fontWeight: 500 }}>
                      {searchError}
                    </div>
                  ) : searchResults.length > 0 ? (
                    <div className="search-results-list" style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                      {searchResults.map((item) => (
                        <Link
                          key={item.id}
                          href={`/products/${item.id}`}
                          className="search-result-item"
                          onClick={() => {
                            setSearchOpen(false)
                            setSearchQuery('')
                          }}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '10px',
                            padding: '10px 12px',
                            minHeight: '68px',
                            maxHeight: '68px',
                            boxSizing: 'border-box',
                            position: 'relative',
                            width: '100%',
                            overflow: 'hidden',
                            textDecoration: 'none',
                            color: '#171717',
                            borderBottom: '1px solid #f5f5f5',
                          }}
                        >
                          <img
                            src={item.image}
                            alt={item.name}
                            className="search-result-image"
                            style={{
                              width: '48px',
                              height: '48px',
                              minWidth: '48px',
                              minHeight: '48px',
                              maxWidth: '48px',
                              maxHeight: '48px',
                              objectFit: 'cover',
                              display: 'block',
                              flex: '0 0 48px',
                              flexShrink: 0,
                              borderRadius: '6px',
                              background: '#eee',
                            }}
                            onError={(e) => {
                              e.currentTarget.style.opacity = '0'
                            }}
                          />
                          <div
                            className="search-result-info"
                            style={{
                              minWidth: 0,
                              flex: 1,
                              overflow: 'hidden',
                              display: 'flex',
                              flexDirection: 'column',
                              gap: '2px',
                            }}
                          >
                            <span
                              className="search-result-title"
                              style={{
                                fontFamily: "'Space Grotesk', sans-serif",
                                fontWeight: 600,
                                fontSize: '13px',
                                color: '#171717',
                                whiteSpace: 'nowrap',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                display: 'block',
                              }}
                            >
                              {item.name}
                            </span>
                            <div className="search-result-meta" style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', marginTop: '2px' }}>
                              <span className="search-result-price" style={{ fontWeight: 700, color: '#ff5b49' }}>
                                {item.price}
                              </span>
                              {item.originalPrice && (
                                <del className="search-result-original" style={{ fontSize: '11px', color: '#aaa' }}>
                                  {item.originalPrice}
                                </del>
                              )}
                            </div>
                          </div>
                          <span
                            className={`search-result-stock ${
                              item.isOutOfStock ? 'out-of-stock' : ''
                            }`}
                            style={{
                              fontSize: '10px',
                              fontWeight: 600,
                              color: item.isOutOfStock ? '#c74536' : '#1c9a4b',
                              background: item.isOutOfStock ? '#fff0ed' : '#e7f7ec',
                              padding: '2px 6px',
                              borderRadius: '4px',
                              whiteSpace: 'nowrap',
                              flexShrink: 0,
                            }}
                          >
                            {item.stockText}
                          </span>
                        </Link>
                      ))}
                    </div>
                  ) : (
                    <div className="search-status-item" style={{ padding: '16px', textAlign: 'center', color: '#666', fontSize: '13px', fontWeight: 500 }}>
                      No products found
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>

        <div className="popover-wrap notification-wrap" ref={notificationRef} style={{ position: 'relative' }}>
          <button
            className="header-icon"
            onClick={() => {
              setNotificationsOpen((open) => {
                const next = !open
                if (next) fetchNotifications()
                return next
              })
            }}
            aria-label="Notifications"
          >
            <Bell size={17} />
            {unreadCount > 0 ? (
              <span className="notification-badge" style={{
                position: 'absolute',
                top: '2px',
                right: '2px',
                background: '#ff5b49',
                color: '#ffffff',
                fontSize: '10px',
                fontWeight: 700,
                borderRadius: '10px',
                padding: '1px 5px',
                lineHeight: 1.2
              }}>
                {unreadCount}
              </span>
            ) : null}
          </button>
          {notificationsOpen && (
            <div
              className="popover notification-popover"
              style={{
                position: 'absolute',
                top: 'calc(100% + 10px)',
                right: 0,
                width: '340px',
                maxWidth: 'calc(100vw - 24px)',
                maxHeight: '420px',
                overflowY: 'auto',
                zIndex: 9999,
                background: '#ffffff',
                border: '1px solid #dfddd8',
                borderRadius: '12px',
                padding: '16px',
                boxShadow: '0 12px 35px rgba(0, 0, 0, 0.12)',
                boxSizing: 'border-box',
              }}
            >
              <div
                className="notification-header"
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  paddingBottom: '10px',
                  borderBottom: '1px solid #eee',
                  marginBottom: '12px',
                }}
              >
                <b style={{ fontSize: '14px', fontFamily: "'Space Grotesk', sans-serif", color: '#171717' }}>
                  Notifications
                </b>
                {unreadCount > 0 && (
                  <button
                    onClick={handleMarkAllRead}
                    className="mark-all-read-btn"
                    style={{
                      border: 'none',
                      background: 'transparent',
                      color: '#ff5b49',
                      fontSize: '11px',
                      fontWeight: 600,
                      cursor: 'pointer',
                      padding: 0,
                    }}
                  >
                    Mark all as read
                  </button>
                )}
              </div>

              {notificationsLoading && notificationsList.length === 0 ? (
                <div style={{ padding: '24px 0', textAlign: 'center', color: '#888', fontSize: '13px', fontWeight: 500 }}>
                  Loading notifications...
                </div>
              ) : notificationsList.length === 0 ? (
                <div style={{ padding: '24px 0', textAlign: 'center', color: '#777', fontSize: '13px', fontWeight: 500 }}>
                  You're all caught up.
                </div>
              ) : (
                <div className="notification-list" style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {notificationsList.map((item) => (
                    <div
                      key={item.id}
                      onClick={() => handleNotificationClick(item)}
                      className={`notification-item ${item.isRead ? 'read' : 'unread'}`}
                      style={{
                        padding: '10px 12px',
                        borderRadius: '8px',
                        background: item.isRead ? '#ffffff' : '#fff8f7',
                        border: item.isRead ? '1px solid #f0eee9' : '1px solid #ffd8d3',
                        cursor: 'pointer',
                        transition: 'background 0.15s ease',
                        display: 'flex',
                        gap: '10px',
                        alignItems: 'flex-start',
                      }}
                    >
                      {!item.isRead && (
                        <span
                          className="unread-dot"
                          style={{
                            width: '7px',
                            height: '7px',
                            borderRadius: '50%',
                            background: '#ff5b49',
                            marginTop: '5px',
                            flexShrink: 0,
                          }}
                        />
                      )}
                      <div style={{ flex: 1, minWidth: 0, marginTop: item.isRead ? 0 : '-1px' }}>
                        <div
                          style={{
                            fontFamily: "'Space Grotesk', sans-serif",
                            fontWeight: item.isRead ? 600 : 700,
                            fontSize: '13px',
                            color: '#171717',
                            marginBottom: '3px',
                          }}
                        >
                          {item.title}
                        </div>
                        <div
                          style={{
                            fontSize: '12px',
                            color: item.isRead ? '#666' : '#333',
                            lineHeight: '1.4',
                          }}
                        >
                          {item.text}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
        <Link className="header-icon cart-link" href="/cart" aria-label="Cart">
          <ShoppingBag size={17} />
          <span>{cartCount}</span>
        </Link>
        <div className="popover-wrap">
          <button
            className="profile-trigger"
            onClick={() => setProfileOpen((open) => !open)}
            aria-expanded={profileOpen}
          >
            <span className="avatar">{userInitials}</span>
            <ChevronDown size={14} />
          </button>
          {profileOpen && (
            <div className="popover profile-popover">
              <Link href="/dashboard">Dashboard</Link>
              <Link href="/orders">My Orders</Link>
              <Link href="/profile">Profile</Link>
              <a href="#logout" onClick={handleLogout}>Logout</a>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
