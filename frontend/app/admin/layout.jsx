// 'use client'
//
// import './admin.css'
// import Link from 'next/link'
// import { usePathname, useRouter } from 'next/navigation'
// import { useState } from 'react'
// import { Bell, ChevronDown, ExternalLink, LayoutDashboard, LogOut, Menu, Package, Settings, ShoppingBag, Users, Wallet, X } from 'lucide-react'
//
// const navItems = [
//   ['Dashboard', '/admin/dashboard', LayoutDashboard],
//   ['Products', '/admin/products', Package],
//   ['Inventory', '/admin/inventory', ShoppingBag],
//   ['Orders', '/admin/orders', ShoppingBag],
//   ['Payments', '/admin/payments', Wallet],
//   ['Users', '/admin/users', Users],
//   ['Settings', '/admin/settings', Settings],
// ]
//
// export default function AdminLayout({ children }) {
//   const pathname = usePathname()
//   const router = useRouter()
//   const [mobileOpen, setMobileOpen] = useState(false)
//   const [notificationsOpen, setNotificationsOpen] = useState(false)
//   const [profileOpen, setProfileOpen] = useState(false)
//   const current = navItems.find(([, href]) => pathname.startsWith(href))?.[0] || 'Dashboard'
//   return <div className="admin-shell">
//     <aside className={`admin-sidebar ${mobileOpen ? 'is-open' : ''}`}>
//       <div className="admin-brand"><span>S</span><strong>Swiftly</strong><small>ADMIN</small><button className="mobile-close" onClick={() => setMobileOpen(false)} aria-label="Close navigation"><X size={18} /></button></div>
//       <div className="side-label">Workspace</div>
//       <nav className="admin-nav">{navItems.map(([label, href, Icon]) => <Link key={href} href={href} className={pathname.startsWith(href) ? 'active' : ''} onClick={() => setMobileOpen(false)}><Icon size={17} /><span>{label}</span>{label === 'Orders' && <b>12</b>}</Link>)}</nav>
//       <div className="admin-sidebar-bottom"><Link href="/" onClick={() => setMobileOpen(false)}><ExternalLink size={16} />View store</Link><button onClick={() => { setProfileOpen(false); router.push('/') }}><LogOut size={16} />Logout</button><p>Swiftly admin<br /><span>UI preview mode</span></p></div>
//     </aside>
//     {mobileOpen && <button className="sidebar-overlay" onClick={() => setMobileOpen(false)} aria-label="Close navigation overlay" />}
//     <main className="admin-main"><header className="admin-header"><button className="menu-button" onClick={() => setMobileOpen(true)} aria-label="Open navigation"><Menu size={20} /></button><div className="breadcrumbs"><span>Workspace</span><i>/</i><strong>{current}</strong></div><div className="header-actions"><div className="notification-wrap"><button className="header-icon" onClick={() => setNotificationsOpen(!notificationsOpen)} aria-label="Notifications"><Bell size={17} /><b /></button>{notificationsOpen && <div className="popover"><strong>Notifications</strong><p>8 products are low in stock.</p><p>Flash sale is live.</p></div>}</div><div className="profile-wrap"><button className="profile-button" onClick={() => setProfileOpen(!profileOpen)}><span className="avatar">AK</span><span><strong>Alex Kim</strong><small>Administrator</small></span><ChevronDown size={14} /></button>{profileOpen && <div className="popover profile-popover"><Link href="/admin/settings">Profile settings</Link><button onClick={() => router.push('/')}>Sign out</button></div>}</div></div></header><div className="admin-content">{children}</div></main>
//   </div>
// }



// 2nd time

'use client'

import './admin.css'
import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { useEffect, useRef, useState } from 'react'
import {
  Bell,
  ChevronDown,
  ExternalLink,
  LayoutDashboard,
  LogOut,
  Menu,
  Package,
  Settings,
  ShoppingBag,
  Users,
  Wallet,
  X
} from 'lucide-react'
import { getStoredUser, getStoreName, getAdminProfile, isAuthenticated, isAdmin as checkIsAdmin } from '../../lib/auth'
import api from '../../lib/api'

const navItems = [
  ['Dashboard', '/admin/dashboard', LayoutDashboard],
  ['Products', '/admin/products', Package],
  ['Inventory', '/admin/inventory', ShoppingBag],
  ['Orders', '/admin/orders', ShoppingBag],
  ['Payments', '/admin/payments', Wallet],
  ['Users', '/admin/users', Users],
  ['Settings', '/admin/settings', Settings],
]

function timeAgo(dateString) {
  if (!dateString) return ''
  const date = new Date(dateString)
  const now = new Date()
  const seconds = Math.floor((now - date) / 1000)
  if (isNaN(seconds) || seconds < 60) return 'Just now'
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes} minute${minutes > 1 ? 's' : ''} ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} hour${hours > 1 ? 's' : ''} ago`
  const days = Math.floor(hours / 24)
  return `${days} day${days > 1 ? 's' : ''} ago`
}

export default function AdminLayout({ children }) {
  const pathname = usePathname()
  const router = useRouter()

  const [authChecking, setAuthChecking] = useState(true)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const [user, setUser] = useState(null)
  const [adminProfile, setAdminProfile] = useState({ displayName: 'Jatin', role: 'Administrator' })
  const [storeName, setStoreName] = useState('Swiftly')
  const [orderCount, setOrderCount] = useState(0)

  useEffect(() => {
    if (typeof window !== 'undefined') {
      if (!isAuthenticated()) {
        router.push('/login')
        return
      }
      if (!checkIsAdmin()) {
        router.push('/dashboard')
        return
      }
      setAuthChecking(false)
    }
  }, [router])

  // Notifications State
  const [notifications, setNotifications] = useState([])
  const [unreadNotificationCount, setUnreadNotificationCount] = useState(0)
  const [notificationsLoading, setNotificationsLoading] = useState(false)

  const notificationRef = useRef(null)

  const fetchOrderCount = async () => {
    try {
      const res = await api.get('/api/orders?page=0&size=1')
      const pageData = res.data?.data ?? res.data
      const total = pageData?.totalElements != null
        ? Number(pageData.totalElements)
        : (Array.isArray(pageData?.content) ? pageData.content.length : (Array.isArray(pageData) ? pageData.length : 0))
      setOrderCount(total)
    } catch (err) {
      console.warn('Failed to load order count for admin sidebar:', err?.message)
      setOrderCount(0)
    }
  }

  const fetchAdminNotifications = async () => {
    try {
      setNotificationsLoading(true)
      let list = []
      const seenKeys = new Set()

      // 1. Fetch backend notification log list if available
      try {
        const res = await api.get('/api/notifications/admin-notifications-list')
        const logs = res.data?.data ?? res.data ?? []
        if (Array.isArray(logs)) {
          logs.forEach((logItem) => {
            const key = `log-${logItem.id || logItem.orderReference + '-' + logItem.eventType}`
            if (!seenKeys.has(key)) {
              seenKeys.add(key)
              let type = logItem.eventType || 'ORDER_CREATED'
              let title = logItem.subject || 'Admin Notification'
              let text = logItem.content ? logItem.content.replace(/<[^>]*>?/gm, '') : ''
              let link = logItem.orderReference ? '/admin/orders' : '/admin/dashboard'

              if (type.includes('PAYMENT')) {
                type = 'PAYMENT_SUCCESS'
                title = 'Payment successful'
                text = `Order ${logItem.orderReference || 'recent order'} payment processed.`
                link = '/admin/payments'
              } else if (type.includes('ORDER_CREATED') || type.includes('ORDER_CONFIRMED')) {
                type = 'ORDER_CREATED'
                title = 'New order received'
                text = `${logItem.orderReference || 'New order placed'}`
                link = '/admin/orders'
              } else if (type.includes('ORDER_CANCELLED')) {
                type = 'ORDER_CANCELLED'
                title = 'Order cancelled'
                text = `${logItem.orderReference || 'An order'} was cancelled.`
                link = '/admin/orders'
              } else if (type.includes('ORDER_EXPIRED')) {
                type = 'ORDER_EXPIRED'
                title = 'Order expired'
                text = `${logItem.orderReference || 'An order'} payment window expired.`
                link = '/admin/orders'
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

      // 2. Fetch real orders for live admin order/payment notifications
      try {
        const ordersRes = await api.get('/api/orders?page=0&size=50&sort=createdAt,desc')
        const pageData = ordersRes.data?.data ?? ordersRes.data ?? {}
        const ordersArr = pageData.content || (Array.isArray(pageData) ? pageData : [])

        ordersArr.forEach((ord) => {
          const ordRef = ord.orderReference || `ORD-${ord.id}`
          const ordStatus = String(ord.status || '').toUpperCase()
          const amt = ord.totalAmount ? `₹${Number(ord.totalAmount).toLocaleString('en-IN')}` : ''
          const timeStr = ord.createdAt || ord.updatedAt || new Date().toISOString()

          if (ordStatus === 'PAID' || ordStatus === 'COMPLETED') {
            const key = `admin-payment-${ordRef}`
            if (!seenKeys.has(key)) {
              seenKeys.add(key)
              list.push({
                id: key,
                type: 'PAYMENT_SUCCESS',
                title: 'Payment successful',
                text: `Order ${ordRef} ${amt ? '· ' + amt : ''}`,
                link: '/admin/payments',
                createdAt: timeStr,
                isRead: false,
              })
            }
          } else if (ordStatus === 'PENDING' || ordStatus === 'CREATED') {
            const key = `admin-order-${ordRef}`
            if (!seenKeys.has(key)) {
              seenKeys.add(key)
              list.push({
                id: key,
                type: 'ORDER_CREATED',
                title: 'New order received',
                text: `${ordRef} ${amt ? '· ' + amt : ''}`,
                link: '/admin/orders',
                createdAt: timeStr,
                isRead: false,
              })
            }
          } else if (ordStatus === 'CANCELLED') {
            const key = `admin-cancelled-${ordRef}`
            if (!seenKeys.has(key)) {
              seenKeys.add(key)
              list.push({
                id: key,
                type: 'ORDER_CANCELLED',
                title: 'Order cancelled',
                text: `${ordRef} was cancelled.`,
                link: '/admin/orders',
                createdAt: timeStr,
                isRead: false,
              })
            }
          } else if (ordStatus === 'EXPIRED') {
            const key = `admin-expired-${ordRef}`
            if (!seenKeys.has(key)) {
              seenKeys.add(key)
              list.push({
                id: key,
                type: 'ORDER_EXPIRED',
                title: 'Order expired',
                text: `${ordRef} expired because payment was not completed.`,
                link: '/admin/orders',
                createdAt: timeStr,
                isRead: false,
              })
            }
          }
        })
      } catch (e) {
        // Ignore order fetch error
      }

      // 3. Fetch real products & inventory for real low stock & out of stock notifications
      try {
        const prodRes = await api.get('/api/products?page=0&size=50')
        const pageData = prodRes.data?.data ?? prodRes.data ?? {}
        const prods = pageData.content || (Array.isArray(pageData) ? pageData : [])

        for (const prod of prods) {
          const pId = prod.id
          const pName = prod.title || prod.name || `Product #${pId}`
          let avail = prod.initialStock ?? 0
          try {
            const invRes = await api.get(`/api/inventory/${pId}`)
            const invData = invRes.data?.data ?? invRes.data
            if (invData && invData.availableStock != null) {
              avail = Number(invData.availableStock)
            }
          } catch (invErr) {
            // fallback
          }

          if (avail === 0) {
            const key = `admin-out-stock-${pId}`
            if (!seenKeys.has(key)) {
              seenKeys.add(key)
              list.push({
                id: key,
                type: 'OUT_OF_STOCK',
                title: 'Out of stock',
                text: `${pName} is currently out of stock.`,
                link: '/admin/inventory',
                createdAt: new Date().toISOString(),
                isRead: false,
              })
            }
          } else if (avail > 0 && avail <= 5) {
            const key = `admin-low-stock-${pId}`
            if (!seenKeys.has(key)) {
              seenKeys.add(key)
              list.push({
                id: key,
                type: 'LOW_STOCK',
                title: 'Low stock',
                text: `${pName} has only ${avail} unit${avail > 1 ? 's' : ''} remaining.`,
                link: '/admin/inventory',
                createdAt: new Date().toISOString(),
                isRead: false,
              })
            }
          }
        }
      } catch (e) {
        // Ignore inventory fetch error
      }

      // 4. Fetch registered users for NEW_USER notifications
      try {
        const usersRes = await api.get('/api/users?page=0&size=20')
        const pageData = usersRes.data?.data ?? usersRes.data ?? {}
        const usersArr = pageData.content || (Array.isArray(pageData) ? pageData : [])
        usersArr.forEach((u) => {
          const key = `admin-user-${u.id || u.email}`
          if (!seenKeys.has(key)) {
            seenKeys.add(key)
            const name = `${u.firstName || ''} ${u.lastName || ''}`.trim() || u.email || `User #${u.id}`
            list.push({
              id: key,
              type: 'NEW_USER',
              title: 'New customer registered',
              text: `${name} registered a new account.`,
              link: '/admin/users',
              createdAt: u.createdAt || new Date().toISOString(),
              isRead: false,
            })
          }
        })
      } catch (e) {
        // Ignore user fetch error
      }

      // Merge read state from localStorage
      const readKeysStr = typeof window !== 'undefined' ? localStorage.getItem('adminReadNotificationKeys') : null
      const readKeys = readKeysStr ? JSON.parse(readKeysStr) : []
      const readSet = new Set(readKeys)

      const merged = list.map((item) => ({
        ...item,
        isRead: item.isRead || readSet.has(item.id),
      }))

      setNotifications(merged)
      setUnreadNotificationCount(merged.filter((n) => !n.isRead).length)
    } catch (err) {
      console.error('Failed to fetch admin notifications:', err)
    } finally {
      setNotificationsLoading(false)
    }
  }

  const handleMarkAllRead = async () => {
    try {
      await api.put('/api/notifications/admin-mark-all-read')
    } catch (e) {
      // Ignore API error
    }
    const allIds = notifications.map((n) => n.id)
    localStorage.setItem('adminReadNotificationKeys', JSON.stringify(allIds))
    setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })))
    setUnreadNotificationCount(0)
  }

  const handleNotificationClick = async (item) => {
    if (item.backendId) {
      try {
        await api.put(`/api/notifications/${item.backendId}/read`)
      } catch (e) {
        // Ignore API error
      }
    }
    const readKeysStr = localStorage.getItem('adminReadNotificationKeys')
    const currentReadKeys = readKeysStr ? JSON.parse(readKeysStr) : []
    if (!currentReadKeys.includes(item.id)) {
      currentReadKeys.push(item.id)
      localStorage.setItem('adminReadNotificationKeys', JSON.stringify(currentReadKeys))
    }
    setNotifications((prev) =>
      prev.map((n) => (n.id === item.id ? { ...n, isRead: true } : n))
    )
    setUnreadNotificationCount((prev) => Math.max(0, prev - 1))
    setNotificationsOpen(false)
    if (item.link) {
      router.push(item.link)
    }
  }

  // Load admin profile, user auth info, store name, real order count, and notifications
  useEffect(() => {
    const loadInfo = () => {
      const storedUser = getStoredUser()
      setUser(storedUser)
      setAdminProfile(getAdminProfile())
      setStoreName(getStoreName())
    }

    function handleClickOutside(event) {
      if (notificationRef.current && !notificationRef.current.contains(event.target)) {
        setNotificationsOpen(false)
      }
    }

    function handleKeyDownGlobal(event) {
      if (event.key === 'Escape') {
        setNotificationsOpen(false)
      }
    }

    loadInfo()
    fetchOrderCount()
    fetchAdminNotifications()

    document.addEventListener('mousedown', handleClickOutside)
    document.addEventListener('keydown', handleKeyDownGlobal)

    const interval = setInterval(() => {
      fetchOrderCount()
      fetchAdminNotifications()
    }, 45000)

    window.addEventListener('swiftlyAdminProfileUpdated', loadInfo)
    window.addEventListener('swiftlySettingsUpdated', loadInfo)
    window.addEventListener('swiftlyOrdersUpdated', () => {
      fetchOrderCount()
      fetchAdminNotifications()
    })
    window.addEventListener('storage', loadInfo)

    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
      document.removeEventListener('keydown', handleKeyDownGlobal)
      clearInterval(interval)
      window.removeEventListener('swiftlyAdminProfileUpdated', loadInfo)
      window.removeEventListener('swiftlySettingsUpdated', loadInfo)
      window.removeEventListener('swiftlyOrdersUpdated', () => {})
      window.removeEventListener('storage', loadInfo)
    }
  }, [pathname])

  const current =
      navItems.find(([, href]) => pathname.startsWith(href))?.[0] ||
      'Dashboard'

  // Admin Display Name (Independent from customer user profile)
  const displayName = adminProfile?.displayName || 'Jatin'

  // Admin Initials from Display Name
  const nameParts = displayName.trim().split(/\s+/).filter(Boolean)
  const initials = nameParts.length >= 2
    ? `${nameParts[0][0] || ''}${nameParts[1][0] || ''}`.toUpperCase()
    : `${displayName[0] || 'A'}${displayName[1] || 'D'}`.toUpperCase()

  // Check actual role
  const isAdmin =
      Array.isArray(user?.roles) &&
      user.roles.includes('ROLE_ADMIN')

  // Real logout
  const handleLogout = () => {
    sessionStorage.removeItem('accessToken')
    sessionStorage.removeItem('refreshToken')
    sessionStorage.removeItem('user')

    setProfileOpen(false)

    router.push('/login')
  }

  if (authChecking) return null

  return (
      <div className="admin-shell">

        <aside className={`admin-sidebar ${mobileOpen ? 'is-open' : ''}`}>

          <div className="admin-brand">
            <span>S</span>
            <strong>{storeName}</strong>
            <small>ADMIN</small>

            <button
                className="mobile-close"
                onClick={() => setMobileOpen(false)}
                aria-label="Close navigation"
            >
              <X size={18} />
            </button>
          </div>

          <div className="side-label">
            Workspace
          </div>

          <nav className="admin-nav">
            {navItems.map(([label, href, Icon]) => (
                <Link
                    key={href}
                    href={href}
                    className={pathname.startsWith(href) ? 'active' : ''}
                    onClick={() => setMobileOpen(false)}
                >
                  <Icon size={17} />

                  <span>
                {label}
              </span>

                  {label === 'Orders' && orderCount > 0 && (
                      <b>
                        {orderCount}
                      </b>
                  )}
                </Link>
            ))}
          </nav>

          <div className="admin-sidebar-bottom">

            <Link
                href="/"
                onClick={() => setMobileOpen(false)}
            >
              <ExternalLink size={16} />
              View store
            </Link>

            <button onClick={handleLogout}>
              <LogOut size={16} />
              Logout
            </button>

            <p>
              {storeName} admin
              <br />
              <span>
              System Control
            </span>
            </p>

          </div>

        </aside>

        {mobileOpen && (
            <button
                className="sidebar-overlay"
                onClick={() => setMobileOpen(false)}
                aria-label="Close navigation overlay"
            />
        )}

        <main className="admin-main">

          <header className="admin-header">

            <button
                className="menu-button"
                onClick={() => setMobileOpen(true)}
                aria-label="Open navigation"
            >
              <Menu size={20} />
            </button>

            <div className="breadcrumbs">
            <span>
              Workspace
            </span>

              <i>
                /
              </i>

              <strong>
                {current}
              </strong>
            </div>

            <div className="header-actions">

              <div className="notification-wrap" ref={notificationRef} style={{ position: 'relative' }}>

                <button
                    className="header-icon"
                    onClick={() =>
                        setNotificationsOpen((open) => {
                          const next = !open
                          if (next) fetchAdminNotifications()
                          return next
                        })
                    }
                    aria-label="Notifications"
                >
                  <Bell size={17} />
                  {unreadNotificationCount > 0 && (
                    <span className="notification-badge" style={{
                      position: 'absolute',
                      top: '2px',
                      right: '2px',
                      background: '#fa5a4e',
                      color: '#ffffff',
                      fontSize: '10px',
                      fontWeight: 700,
                      borderRadius: '10px',
                      padding: '1px 5px',
                      lineHeight: 1.2
                    }}>
                      {unreadNotificationCount}
                    </span>
                  )}
                </button>

                {notificationsOpen && (
                    <div
                      className="popover notification-popover"
                      style={{
                        position: 'absolute',
                        top: 'calc(100% + 10px)',
                        right: 0,
                        width: '360px',
                        maxWidth: 'calc(100vw - 24px)',
                        maxHeight: '420px',
                        overflowY: 'auto',
                        zIndex: 9999,
                        background: '#ffffff',
                        border: '1px solid #e6e2dc',
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
                          borderBottom: '1px solid #eeeae4',
                          marginBottom: '12px',
                        }}
                      >
                        <strong style={{ fontSize: '14px', color: '#171717', fontFamily: "'Space Grotesk', sans-serif" }}>
                          Notifications
                        </strong>
                        {unreadNotificationCount > 0 && (
                          <button
                            onClick={handleMarkAllRead}
                            style={{
                              border: 'none',
                              background: 'transparent',
                              color: '#fa5a4e',
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

                      {notificationsLoading && notifications.length === 0 ? (
                        <div style={{ padding: '24px 0', textAlign: 'center', color: '#77736e', fontSize: '12px' }}>
                          Loading notifications...
                        </div>
                      ) : notifications.length === 0 ? (
                        <div style={{ padding: '24px 0', textAlign: 'center', color: '#77736e', fontSize: '12px' }}>
                          You're all caught up.
                        </div>
                      ) : (
                        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                          {notifications.map((item) => (
                            <div
                              key={item.id}
                              onClick={() => handleNotificationClick(item)}
                              style={{
                                padding: '10px 12px',
                                borderRadius: '8px',
                                background: item.isRead ? '#ffffff' : '#fff8f7',
                                border: item.isRead ? '1px solid #eeeae4' : '1px solid #ffcfcb',
                                cursor: 'pointer',
                                transition: 'background 0.15s ease',
                                display: 'flex',
                                gap: '10px',
                                alignItems: 'flex-start',
                              }}
                            >
                              {!item.isRead && (
                                <span
                                  style={{
                                    width: '7px',
                                    height: '7px',
                                    borderRadius: '50%',
                                    background: '#fa5a4e',
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
                                    fontSize: '12px',
                                    color: '#171717',
                                    marginBottom: '2px',
                                  }}
                                >
                                  {item.title}
                                </div>
                                <div
                                  style={{
                                    fontSize: '11px',
                                    color: item.isRead ? '#77736e' : '#333',
                                    lineHeight: '1.4',
                                  }}
                                >
                                  {item.text}
                                </div>
                                {item.createdAt && (
                                  <div
                                    style={{
                                      fontSize: '10px',
                                      color: '#aaa39b',
                                      marginTop: '4px',
                                    }}
                                  >
                                    {timeAgo(item.createdAt)}
                                  </div>
                                )}
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                )}

              </div>

              <div className="profile-wrap">

                <button
                    className="profile-button"
                    onClick={() =>
                        setProfileOpen(!profileOpen)
                    }
                >

                  {/* ADMIN DISPLAY INITIALS */}
                  <span className="avatar">
                  {initials}
                </span>

                  <span>

                  {/* ADMIN DISPLAY NAME */}
                    <strong>
                    {displayName}
                  </strong>

                    {/* ADMIN ROLE */}
                    <small>
                    {adminProfile?.role || 'Administrator'}
                  </small>

                </span>

                  <ChevronDown size={14} />

                </button>

                {profileOpen && (
                    <div className="popover profile-popover">

                      <Link href="/admin/settings">
                        Profile settings
                      </Link>

                      <button onClick={handleLogout}>
                        Sign out
                      </button>

                    </div>
                )}

              </div>

            </div>

          </header>

          <div className="admin-content">
            {children}
          </div>

        </main>

      </div>
  )
}