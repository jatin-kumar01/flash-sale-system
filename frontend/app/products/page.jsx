'use client'

import { useEffect, useState, Suspense } from 'react'
import { useSearchParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import UserHeader from '@/components/user/UserHeader'
import ProductCard from '@/components/user/ProductCard'
import api from '@/lib/api'
import { isAuthenticated, isAdmin } from '@/lib/auth'

function formatStockLabel(availableStock) {
  if (availableStock <= 0) {
    return 'Out of stock'
  }
  if (availableStock >= 1 && availableStock <= 5) {
    return `Only ${availableStock} left`
  }
  return 'In stock'
}

function ProductsContent() {
  const searchParams = useSearchParams()
  const searchQuery = searchParams.get('search') || searchParams.get('q') || ''
  const [products, setProducts] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadCatalogProducts() {
      try {
        setLoading(true)
        setError('')

        const res = await api.get('/api/products?page=0&size=100')
        const pageData = res.data?.data ?? res.data
        const content = pageData?.content ?? (Array.isArray(pageData) ? pageData : [])

        // Filter ONLY ACTIVE products for customer visibility
        let activeProducts = content.filter(
          (p) => String(p.status || '').toUpperCase() === 'ACTIVE'
        )

        if (searchQuery.trim()) {
          const q = searchQuery.trim().toLowerCase()
          activeProducts = activeProducts.filter((p) => {
            const title = (p.title || p.name || '').toLowerCase()
            const desc = (p.description || '').toLowerCase()
            return title.includes(q) || desc.includes(q)
          })
        }

        // Fetch real-time available stock from Inventory Service for each active product
        const mappedProducts = await Promise.all(
          activeProducts.map(async (prod) => {
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
              discount: discountPct > 0 ? `${discountPct}% OFF` : 'In Stock',
              stock: stockText,
              availableStock: available,
              isOutOfStock,
              rawProduct: prod,
            }
          })
        )

        setProducts(mappedProducts)
      } catch (err) {
        console.error('Failed to load catalog products:', err)
        setError('Unable to load products. Please try again.')
      } finally {
        setLoading(false)
      }
    }

    loadCatalogProducts()
  }, [searchQuery])

  return (
    <main className="user-page support-page">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '12px' }}>
        <div>
          <span className="eyebrow">{searchQuery ? 'SEARCH RESULTS' : 'ALL PRODUCTS'}</span>
          <h1>{searchQuery ? `Results for "${searchQuery}"` : 'Shop all products'}</h1>
        </div>
        {searchQuery && (
          <Link href="/products" className="button button-light" style={{ marginTop: '10px' }}>
            Clear Search
          </Link>
        )}
      </div>

      {loading ? (
        <div
          className="empty-state"
          style={{ padding: '60px 20px', textAlign: 'center', color: '#666' }}
        >
          <p style={{ fontSize: '16px', fontWeight: 500 }}>
            {searchQuery ? 'Searching products...' : 'Loading products...'}
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
          <p style={{ fontSize: '16px', marginBottom: '16px' }}>
            {searchQuery ? `No products found matching "${searchQuery}".` : 'No products available right now.'}
          </p>
          {searchQuery && (
            <Link className="button button-dark" href="/products">
              View All Products
            </Link>
          )}
        </div>
      )}
    </main>
  )
}

export default function ProductsPage() {
  const router = useRouter()
  const [authChecking, setAuthChecking] = useState(true)

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

  if (authChecking) return null

  return (
    <>
      <UserHeader />
      <Suspense
        fallback={
          <main className="user-page support-page">
            <span className="eyebrow">ALL PRODUCTS</span>
            <h1>Shop all products</h1>
            <div
              className="empty-state"
              style={{ padding: '60px 20px', textAlign: 'center', color: '#666' }}
            >
              <p style={{ fontSize: '16px', fontWeight: 500 }}>Loading products...</p>
            </div>
          </main>
        }
      >
        <ProductsContent />
      </Suspense>
    </>
  )
}

