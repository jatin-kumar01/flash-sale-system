'use client'

import './admin.css'
import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { useState } from 'react'
import { Bell, ChevronDown, ExternalLink, LayoutDashboard, LogOut, Menu, Package, Settings, ShoppingBag, Users, Wallet, X } from 'lucide-react'

const navItems = [
  ['Dashboard', '/admin/dashboard', LayoutDashboard],
  ['Products', '/admin/products', Package],
  ['Inventory', '/admin/inventory', ShoppingBag],
  ['Orders', '/admin/orders', ShoppingBag],
  ['Payments', '/admin/payments', Wallet],
  ['Users', '/admin/users', Users],
  ['Settings', '/admin/settings', Settings],
]

export default function AdminLayout({ children }) {
  const pathname = usePathname()
  const router = useRouter()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const current = navItems.find(([, href]) => pathname.startsWith(href))?.[0] || 'Dashboard'
  return <div className="admin-shell">
    <aside className={`admin-sidebar ${mobileOpen ? 'is-open' : ''}`}>
      <div className="admin-brand"><span>S</span><strong>Swiftly</strong><small>ADMIN</small><button className="mobile-close" onClick={() => setMobileOpen(false)} aria-label="Close navigation"><X size={18} /></button></div>
      <div className="side-label">Workspace</div>
      <nav className="admin-nav">{navItems.map(([label, href, Icon]) => <Link key={href} href={href} className={pathname.startsWith(href) ? 'active' : ''} onClick={() => setMobileOpen(false)}><Icon size={17} /><span>{label}</span>{label === 'Orders' && <b>12</b>}</Link>)}</nav>
      <div className="admin-sidebar-bottom"><Link href="/" onClick={() => setMobileOpen(false)}><ExternalLink size={16} />View store</Link><button onClick={() => { setProfileOpen(false); router.push('/') }}><LogOut size={16} />Logout</button><p>Swiftly admin<br /><span>UI preview mode</span></p></div>
    </aside>
    {mobileOpen && <button className="sidebar-overlay" onClick={() => setMobileOpen(false)} aria-label="Close navigation overlay" />}
    <main className="admin-main"><header className="admin-header"><button className="menu-button" onClick={() => setMobileOpen(true)} aria-label="Open navigation"><Menu size={20} /></button><div className="breadcrumbs"><span>Workspace</span><i>/</i><strong>{current}</strong></div><div className="header-actions"><div className="notification-wrap"><button className="header-icon" onClick={() => setNotificationsOpen(!notificationsOpen)} aria-label="Notifications"><Bell size={17} /><b /></button>{notificationsOpen && <div className="popover"><strong>Notifications</strong><p>8 products are low in stock.</p><p>Flash sale is live.</p></div>}</div><div className="profile-wrap"><button className="profile-button" onClick={() => setProfileOpen(!profileOpen)}><span className="avatar">AK</span><span><strong>Alex Kim</strong><small>Administrator</small></span><ChevronDown size={14} /></button>{profileOpen && <div className="popover profile-popover"><Link href="/admin/settings">Profile settings</Link><button onClick={() => router.push('/')}>Sign out</button></div>}</div></div></header><div className="admin-content">{children}</div></main>
  </div>
}
