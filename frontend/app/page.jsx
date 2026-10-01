'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import {
  ArrowUpRight,
  Zap,
  ShoppingBag,
  ShieldCheck,
  Sparkles,
} from 'lucide-react'
import UserHeader from '../components/user/UserHeader'
import ProductCard from '../components/user/ProductCard'
import { getStoreName, isAuthenticated, isAdmin } from '../lib/auth'
import api from '../lib/api'

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

export default function HomePage() {
  const router = useRouter()
  const [authChecking, setAuthChecking] = useState(true)
  const [storeName, setStoreName] = useState('Swiftly')
  const [flashProducts, setFlashProducts] = useState([])
  const [featuredProducts, setFeaturedProducts] = useState([])
  const [loading, setLoading] = useState(true)
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
      setAuthChecking(false)
    }
  }, [router])

  useEffect(() => {
    const loadStoreName = () => setStoreName(getStoreName())
    loadStoreName()
    window.addEventListener('swiftlySettingsUpdated', loadStoreName)
    window.addEventListener('storage', loadStoreName)

    return () => {
      window.removeEventListener('swiftlySettingsUpdated', loadStoreName)
      window.removeEventListener('storage', loadStoreName)
    }
  }, [])

  useEffect(() => {
    async function loadProducts() {
      try {
        setLoading(true)
        setError('')

        const res = await api.get('/api/products?page=0&size=100')
        const rawData = res.data?.data ?? res.data
        const content = rawData?.content ?? (Array.isArray(rawData) ? rawData : [])

        // Active flash sale products matching eligibility rules
        const activeFlash = content.filter(isFlashSaleEligible).map(mapProduct)

        // All active products for featured section
        const activeFeatured = content
          .filter((p) => String(p.status || '').toUpperCase() === 'ACTIVE')
          .map(mapProduct)

        setFlashProducts(activeFlash)
        setFeaturedProducts(activeFeatured)
      } catch (err) {
        console.error('Failed to load products for home page:', err)
        setError('Unable to load products. Please try again.')
      } finally {
        setLoading(false)
      }
    }

    loadProducts()
  }, [])

  if (authChecking) return null

  return (
    <>
      <UserHeader />
      <main className="user-page">
        {/* 1. HERO SECTION */}
        <section
          className="welcome-row hero-banner"
          style={{
            background: '#ffffff',
            borderRadius: '15px',
            padding: '36px 32px',
            border: '1px solid #e1ded8',
            marginBottom: '32px',
          }}
        >
          <div>
            <span className="eyebrow coral">EXCLUSIVE DEALS & FLASH SALES</span>
            <h1
              style={{
                font: "700 44px 'Space Grotesk'",
                letterSpacing: '-2px',
                margin: '12px 0 10px',
              }}
            >
              Shop smarter. Buy faster.
            </h1>
            <p
              style={{
                color: '#66635d',
                fontSize: '15px',
                maxWidth: '580px',
                lineHeight: '1.5',
              }}
            >
              Discover great products, limited-time flash sales, and exclusive
              deals on {storeName}.
            </p>
            <div style={{ display: 'flex', gap: '12px', marginTop: '24px' }}>
              <Link
                className="button button-dark"
                href="/products"
                style={{ padding: '12px 22px', fontSize: '13px' }}
              >
                Shop Now <ArrowUpRight size={16} />
              </Link>
              <Link
                className="button button-light"
                href="/flash-sale"
                style={{ padding: '12px 22px', fontSize: '13px' }}
              >
                Explore Flash Sale
              </Link>
            </div>
          </div>
          <div
            className="hero-icon-card"
            style={{
              background: '#fff0ed',
              border: '1px solid #ffd4cb',
              borderRadius: '16px',
              padding: '32px 40px',
              textAlign: 'center',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: '12px',
            }}
          >
            <div
              style={{
                width: '56px',
                height: '56px',
                background: '#ff5b49',
                color: '#fff',
                borderRadius: '50%',
                display: 'grid',
                placeItems: 'center',
              }}
            >
              <Zap size={28} />
            </div>
            <strong
              style={{ font: "700 22px 'Space Grotesk'", color: '#171717' }}
            >
              Live Drops
            </strong>
            <span style={{ fontSize: '12px', color: '#777' }}>
              Up to 50% Off Flash Price
            </span>
          </div>
        </section>

        {/* 2. LIVE FLASH SALE SECTION */}
        <section className="section-block">
          <div className="section-heading">
            <div>
              <span className="eyebrow coral">LIMITED-TIME OFFERS</span>
              <h2>Live Flash Sale</h2>
              <p
                style={{
                  color: '#77736d',
                  fontSize: '13px',
                  margin: '4px 0 0',
                }}
              >
                Limited-time deals. Grab them before they're gone.
              </p>
            </div>
            <Link href="/flash-sale">
              View all flash sales <ArrowUpRight size={14} />
            </Link>
          </div>
          <div className="product-grid">
            {loading ? (
              <div
                style={{
                  gridColumn: '1 / -1',
                  padding: '30px',
                  textAlign: 'center',
                  color: '#888',
                }}
              >
                Loading flash sales...
              </div>
            ) : error ? (
              <div
                style={{
                  gridColumn: '1 / -1',
                  padding: '30px',
                  textAlign: 'center',
                  color: '#ef4444',
                }}
              >
                {error}
              </div>
            ) : flashProducts.length > 0 ? (
              flashProducts.map((product) => (
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

        {/* 3. FEATURED PRODUCTS SECTION */}
        <section className="section-block">
          <div className="section-heading">
            <div>
              <span className="eyebrow">HANDPICKED FOR YOU</span>
              <h2>Featured Products</h2>
            </div>
            <Link href="/products">
              View all products <ArrowUpRight size={14} />
            </Link>
          </div>
          <div className="product-grid">
            {loading ? (
              <div
                style={{
                  gridColumn: '1 / -1',
                  padding: '30px',
                  textAlign: 'center',
                  color: '#888',
                }}
              >
                Loading products...
              </div>
            ) : error ? (
              <div
                style={{
                  gridColumn: '1 / -1',
                  padding: '30px',
                  textAlign: 'center',
                  color: '#ef4444',
                }}
              >
                {error}
              </div>
            ) : featuredProducts.length > 0 ? (
              featuredProducts.slice(0, 8).map((product) => (
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
                No products available right now.
              </div>
            )}
          </div>
        </section>

        {/* 4. WHY STORE NAME SECTION */}
        <section className="section-block">
          <div className="section-heading">
            <div>
              <span className="eyebrow">THE EXPERIENCE</span>
              <h2>Why {storeName}?</h2>
            </div>
          </div>
          <div className="summary-grid" style={{ marginTop: '18px' }}>
            {[
              {
                title: 'Fast Checkout',
                text: 'Instant order placement and smooth checkout process.',
                icon: Zap,
              },
              {
                title: 'Secure Payments',
                text: 'Encrypted and verified transactions across all methods.',
                icon: ShieldCheck,
              },
              {
                title: 'Flash Sale Deals',
                text: 'Exclusive limited-time price drops on top products.',
                icon: Sparkles,
              },
              {
                title: 'Real-time Stock',
                text: 'Live inventory tracking and accurate stock reservation.',
                icon: ShoppingBag,
              },
            ].map((item) => {
              const Icon = item.icon
              return (
                <article className="summary-card" key={item.title}>
                  <div className="summary-icon">
                    <Icon size={17} />
                  </div>
                  <strong style={{ font: "700 16px 'Space Grotesk'" }}>
                    {item.title}
                  </strong>
                  <small
                    style={{
                      color: '#75716a',
                      fontSize: '12px',
                      lineHeight: '1.4',
                    }}
                  >
                    {item.text}
                  </small>
                </article>
              )
            })}
          </div>
        </section>

        {/* 5. FINAL CTA SECTION */}
        <section className="sale-banner" style={{ marginTop: '40px' }}>
          <div>
            <span className="eyebrow coral">JOIN THE FLASH SALE</span>
            <h2>Ready to find your next deal?</h2>
            <p>Browse our full catalog and catch limited-time price drops today.</p>
          </div>
          <Link
            className="button button-dark"
            href="/products"
            style={{ background: '#fff', color: '#171717', border: 0 }}
          >
            Start Shopping <ArrowUpRight size={16} />
          </Link>
        </section>

        {/* 6. FOOTER */}
        <footer
          style={{
            marginTop: '60px',
            paddingTop: '32px',
            borderTop: '1px solid #dfddd8',
            display: 'flex',
            justify: 'space-between',
            alignItems: 'center',
            flexWrap: 'wrap',
            gap: '20px',
          }}
        >
          <div>
            <strong
              style={{ font: "700 18px 'Space Grotesk'", color: '#171717' }}
            >
              ● {storeName}
            </strong>
            <p style={{ color: '#888', fontSize: '12px', margin: '4px 0 0' }}>
              Premium flash-sale e-commerce platform.
            </p>
          </div>
          <div
            style={{ display: 'flex', gap: '20px', font: "600 12px 'DM Sans'" }}
          >
            <Link href="/" style={{ color: '#6c6a65', textDecoration: 'none' }}>
              Home
            </Link>
            <Link
              href="/products"
              style={{ color: '#6c6a65', textDecoration: 'none' }}
            >
              Products
            </Link>
            <Link
              href="/flash-sale"
              style={{ color: '#6c6a65', textDecoration: 'none' }}
            >
              Flash Sale
            </Link>
            <Link
              href="/orders"
              style={{ color: '#6c6a65', textDecoration: 'none' }}
            >
              My Orders
            </Link>
            <Link
              href="/profile"
              style={{ color: '#6c6a65', textDecoration: 'none' }}
            >
              Profile
            </Link>
            <Link
              href="/cart"
              style={{ color: '#6c6a65', textDecoration: 'none' }}
            >
              Cart
            </Link>
          </div>
        </footer>
      </main>
    </>
  )
}
