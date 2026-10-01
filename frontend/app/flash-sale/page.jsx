'use client'

import { useEffect, useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'
import UserHeader from '@/components/user/UserHeader'
import ProductCard from '@/components/user/ProductCard'
import api from '@/lib/api'
import { isAuthenticated, isAdmin } from '@/lib/auth'

function isFlashSaleActive(prod) {
  const statusOk = String(prod.status || '').toUpperCase() === 'ACTIVE'
  const saleActiveOk = Boolean(prod.saleActive)

  if (!statusOk || !saleActiveOk) return false

  const now = new Date()

  if (prod.startTime) {
    const start = new Date(prod.startTime)
    if (isNaN(start.getTime()) || start > now) return false
  }

  if (prod.endTime) {
    const end = new Date(prod.endTime)
    if (isNaN(end.getTime()) || end < now) return false
  }

  return true
}

function formatStockLabel(availableStock) {
  if (availableStock <= 0) {
    return 'Out of stock'
  }
  if (availableStock >= 1 && availableStock <= 5) {
    return `Only ${availableStock} left`
  }
  return 'In stock'
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
    return 0
  })

  useEffect(() => {
    if (!seconds) return
    const timer = setInterval(
      () => setSeconds((value) => Math.max(0, value - 1)),
      1000
    )
    return () => clearInterval(timer)
  }, [seconds])

  if (!seconds) return null

  const parts = [
    Math.floor(seconds / 3600),
    Math.floor((seconds % 3600) / 60),
    seconds % 60,
  ]

  return (
    <div className="countdown" style={{ marginBottom: '24px' }}>
      {parts.map((part, index) => (
        <span key={index}>
          <b>{String(part).padStart(2, '0')}</b>
          <small>{['Hours', 'Minutes', 'Seconds'][index]}</small>
        </span>
      ))}
    </div>
  )
}

export default function FlashSalePage() {
  const router = useRouter()
  const [authChecking, setAuthChecking] = useState(true)
  const [products, setProducts] = useState([])
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
    async function loadFlashSaleProducts() {
      try {
        setLoading(true)
        setError('')

        const res = await api.get('/api/products?page=0&size=100')
        const pageData = res.data?.data ?? res.data
        const content = pageData?.content ?? (Array.isArray(pageData) ? pageData : [])

        // Filter active flash sale products
        const activeList = content.filter(isFlashSaleActive)

        // Fetch real-time available stock from inventory API for each active product
        const mappedProducts = await Promise.all(
          activeList.map(async (prod) => {
            let available = prod.initialStock ?? 0

            try {
              const invRes = await api.get(`/api/inventory/${prod.id}`)
              const invData = invRes.data?.data ?? invRes.data
              if (invData && invData.availableStock != null) {
                available = Number(invData.availableStock)
              }
            } catch (invErr) {
              console.warn(
                `Inventory fetch failed for product ${prod.id}, using initial stock:`,
                invErr.message
              )
            }

            const original = Number(prod.originalPrice || 0)
            const sale = Number(prod.flashSalePrice ?? prod.salePrice ?? original)
            const discountPct =
              original > sale
                ? Math.round(((original - sale) / original) * 100)
                : 0

            const stockText = formatStockLabel(available)
            const isOutOfStock = available <= 0

            return {
              id: prod.id,
              name: prod.title || prod.name || `Product #${prod.id}`,
              image: prod.imageUrl || prod.image || '/placeholder.jpg',
              price: `₹${sale.toLocaleString('en-IN')}`,
              original:
                original > sale ? `₹${original.toLocaleString('en-IN')}` : '',
              discount: discountPct > 0 ? `${discountPct}% OFF` : 'Live Sale',
              stock: stockText,
              availableStock: available,
              isOutOfStock,
              endTime: prod.endTime,
              rawProduct: prod,
            }
          })
        )

        setProducts(mappedProducts)
      } catch (err) {
        console.error('Failed to load flash sale products:', err)
        setError('Unable to load flash sales. Please try again.')
      } finally {
        setLoading(false)
      }
    }

    loadFlashSaleProducts()
  }, [])

  // Find nearest ending flash sale timestamp for page countdown
  const nearestEndTime = useMemo(() => {
    const withEndTime = products
      .filter((p) => p.endTime)
      .map((p) => new Date(p.endTime).getTime())
      .filter((t) => !isNaN(t) && t > Date.now())

    if (withEndTime.length === 0) return null
    return Math.min(...withEndTime)
  }, [products])

  if (authChecking) return null

  return (
    <>
      <UserHeader />
      <main className="user-page support-page">
        <span className="eyebrow">LIMITED TIME ONLY</span>
        <h1>Live flash sale</h1>

        {nearestEndTime && <Countdown targetEndTime={nearestEndTime} />}

        {loading ? (
          <div
            className="empty-state"
            style={{ padding: '60px 20px', textAlign: 'center', color: '#666' }}
          >
            <p style={{ fontSize: '16px', fontWeight: 500 }}>
              Loading flash sales...
            </p>
          </div>
        ) : error ? (
          <div
            className="empty-state"
            style={{ padding: '60px 20px', textAlign: 'center', color: '#ef4444' }}
          >
            <p style={{ fontSize: '16px', marginBottom: '16px' }}>{error}</p>
            <button
              className="button button-dark"
              onClick={() => window.location.reload()}
            >
              Retry
            </button>
          </div>
        ) : products.length > 0 ? (
          <div className="product-grid">
            {products.map((product) => (
              <ProductCard product={product} key={product.id} />
            ))}
          </div>
        ) : (
          <div
            className="empty-state"
            style={{ padding: '60px 20px', textAlign: 'center', color: '#666' }}
          >
            <p style={{ fontSize: '16px' }}>
              No active flash sales right now. Check back soon.
            </p>
          </div>
        )}
      </main>
    </>
  )
}
