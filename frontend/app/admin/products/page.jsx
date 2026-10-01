'use client'

import { useEffect, useMemo, useState } from 'react'
import Link from 'next/link'
import { Eye, Pencil, Plus, Trash2 } from 'lucide-react'
import api from '@/lib/api'
import {
  PageHeading,
  StatusPill,
  Toolbar,
  Modal,
  ProductThumb,
} from '@/components/admin/AdminUI'

function calculateSaleStatus(product) {
  const statusUpper = String(product.status || '').toUpperCase()
  if (statusUpper !== 'ACTIVE') {
    return 'Inactive'
  }

  const now = Date.now()
  const start = product.startTime ? new Date(product.startTime).getTime() : 0
  const end = product.endTime ? new Date(product.endTime).getTime() : 0

  if (start && now < start) {
    return 'Scheduled'
  }
  if (start && end && now >= start && now <= end) {
    return 'Live'
  }
  if (end && now > end) {
    return 'Ended'
  }
  return 'Scheduled'
}

function mapProduct(product) {
  return {
    id: product.id,
    name: product.title || product.name || `Product ${product.id}`,
    title: product.title || product.name || `Product ${product.id}`,
    description: product.description || '',
    originalPrice: Number(product.originalPrice || 0),
    salePrice: Number(product.flashSalePrice ?? product.salePrice ?? 0),
    stock: Number(product.initialStock ?? product.stock ?? 0),
    image: product.imageUrl || product.image || '/placeholder.jpg',
    imageUrl: product.imageUrl || product.image || '/placeholder.jpg',
    category: 'Flash Sale',
    sale: calculateSaleStatus(product),
    saleActive: Boolean(product.saleActive),
    status: product.status || 'ACTIVE',
    startTime: product.startTime,
    endTime: product.endTime,
    createdAt: product.createdAt,
    updatedAt: product.updatedAt,
  }
}

export default function ProductsPage() {
  const [items, setItems] = useState([])
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('All')
  const [sort, setSort] = useState('default')

  const [modal, setModal] = useState(null)
  const [loading, setLoading] = useState(true)
  const [deleting, setDeleting] = useState(false)
  const [error, setError] = useState('')

  // Fetch products from API Gateway
  const loadProducts = async () => {
    try {
      setLoading(true)
      setError('')

      const response = await api.get(`/api/products?page=0&size=100&sort=createdAt,desc&_t=${Date.now()}`)
      console.log('Products API response:', response.data)

      const pageData = response.data?.data ?? response.data
      const content = pageData?.content ?? (Array.isArray(pageData) ? pageData : [])

      const productsWithInventory = await Promise.all(
        content.map(async (prod) => {
          let available = prod.initialStock ?? 0
          try {
            const invRes = await api.get(`/api/inventory/${prod.id}?_t=${Date.now()}`)
            const invData = invRes.data?.data ?? invRes.data
            if (invData && invData.availableStock != null) {
              available = Number(invData.availableStock)
            }
          } catch (invErr) {
            console.warn(`Could not fetch inventory for product ${prod.id}:`, invErr.message)
          }

          const mapped = mapProduct(prod)
          return {
            ...mapped,
            stock: available, // Real current available stock from Inventory Service
          }
        })
      )

      setItems(productsWithInventory)
    } catch (err) {
      console.error('Failed to load products:', err)
      const message =
        err.response?.data?.message ||
        err.response?.data?.errorDetails?.[0]?.message ||
        err.message ||
        'Failed to load products from server.'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadProducts()
    const handleFocus = () => loadProducts()
    window.addEventListener('focus', handleFocus)
    return () => window.removeEventListener('focus', handleFocus)
  }, [])

  // View product details from backend API
  const handleView = async (product) => {
    try {
      const response = await api.get(`/api/products/${product.id}`)
      const singleData = response.data?.data ?? response.data

      let available = singleData.initialStock ?? 0
      try {
        const invRes = await api.get(`/api/inventory/${product.id}`)
        const invData = invRes.data?.data ?? invRes.data
        if (invData && invData.availableStock != null) {
          available = Number(invData.availableStock)
        }
      } catch (invErr) {
        console.warn(`Could not fetch inventory for product ${product.id}:`, invErr.message)
      }

      const mapped = mapProduct(singleData)
      mapped.stock = available

      setModal({
        type: 'view',
        product: mapped,
      })
    } catch (err) {
      console.error('Failed to fetch product details:', err)
      setModal({
        type: 'view',
        product,
      })
    }
  }

  // Delete product from backend API
  const handleDelete = async (product) => {
    if (deleting) return
    try {
      setDeleting(true)
      await api.delete(`/api/products/${product.id}`)
      setModal(null)
      await loadProducts()
    } catch (err) {
      console.error('Failed to delete product:', err)
      const message =
        err.response?.data?.message ||
        err.response?.data?.errorDetails?.[0]?.message ||
        err.message ||
        'Failed to delete product.'
      alert(message)
    } finally {
      setDeleting(false)
    }
  }

  // Search, filter, and sort
  const visible = useMemo(() => {
    let list = [...items]
    const searchText = search.trim().toLowerCase()

    if (searchText) {
      list = list.filter((p) =>
        p.name.toLowerCase().includes(searchText)
      )
    }

    if (filter !== 'All') {
      const filterUpper = filter.toUpperCase()
      list = list.filter(
        (p) =>
          p.status.toUpperCase() === filterUpper ||
          p.sale.toUpperCase() === filterUpper
      )
    }

    if (sort === 'name') {
      list.sort((a, b) => a.name.localeCompare(b.name))
    } else if (sort === 'price') {
      list.sort((a, b) => b.salePrice - a.salePrice)
    } else if (sort === 'stock') {
      list.sort((a, b) => a.stock - b.stock)
    }

    return list
  }, [items, search, filter, sort])

  return (
    <>
      <PageHeading
        eyebrow="Catalog"
        title="Products"
        description={`${items.length} products in your catalog`}
        action={
          <Link href="/admin/products/new" className="button primary">
            <Plus size={16} />
            Add product
          </Link>
        }
      />

      <Toolbar
        search={search}
        setSearch={setSearch}
        placeholder="Search products..."
        filter={filter}
        setFilter={setFilter}
        filterOptions={[
          'ACTIVE',
          'INACTIVE',
          'DRAFT',
          'SOLD_OUT',
          'Live',
          'Scheduled',
        ]}
        sort={sort}
        setSort={setSort}
      />

      <section className="panel">
        <div className="table-scroll">
          {loading ? (
            <div className="empty-state">
              <p>Loading products...</p>
            </div>
          ) : error ? (
            <div className="empty-state">
              <p>{error}</p>
              <button className="button primary" onClick={loadProducts}>
                Retry
              </button>
            </div>
          ) : (
            <>
              <table className="products-table">
                <thead>
                  <tr>
                    <th>Product</th>
                    <th>Original price</th>
                    <th>Sale price</th>
                    <th>Stock</th>
                    <th>Sale status</th>
                    <th>Product status</th>
                    <th>Actions</th>
                  </tr>
                </thead>

                <tbody>
                  {visible.map((product) => (
                    <tr key={product.id}>
                      <td>
                        <div className="table-product">
                          <ProductThumb product={product} />
                          <span>
                            <strong>{product.name}</strong>
                            <small>{product.description || product.category}</small>
                          </span>
                        </div>
                      </td>

                      <td>
                        ₹{product.originalPrice.toLocaleString('en-IN')}
                      </td>

                      <td>
                        <strong>
                          ₹{product.salePrice.toLocaleString('en-IN')}
                        </strong>
                      </td>

                      <td>{product.stock}</td>

                      <td>
                        <StatusPill>{product.sale}</StatusPill>
                      </td>

                      <td>
                        <StatusPill>{product.status}</StatusPill>
                      </td>

                      <td>
                        <div className="row-actions">
                          {/* VIEW BUTTON */}
                          <button
                            onClick={() => handleView(product)}
                            aria-label="View product"
                          >
                            <Eye size={15} />
                          </button>

                          {/* EDIT BUTTON - Navigates to /admin/products/{id}/edit */}
                          <Link
                            href={`/admin/products/${product.id}/edit`}
                            aria-label="Edit product"
                            className="button icon-only"
                          >
                            <Pencil size={15} />
                          </Link>

                          {/* DELETE BUTTON */}
                          <button
                            onClick={() =>
                              setModal({
                                type: 'delete',
                                product,
                              })
                            }
                            aria-label="Delete product"
                          >
                            <Trash2 size={15} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {visible.length === 0 && (
                <p className="empty-state">No products match your search.</p>
              )}
            </>
          )}
        </div>
      </section>

      {/* VIEW PRODUCT MODAL */}
      {modal?.type === 'view' && (
        <Modal
          title={modal.product.name}
          onClose={() => setModal(null)}
        >
          <div className="detail-view">
            <ProductThumb product={modal.product} size="hero" />
            <div>
              <p>
                Status: <strong>{modal.product.status}</strong> · Sale:{' '}
                <strong>{modal.product.sale}</strong>
              </p>
              <h3>
                ₹{modal.product.salePrice.toLocaleString('en-IN')}{' '}
                <del>₹{modal.product.originalPrice.toLocaleString('en-IN')}</del>
              </h3>
              <p>
                <strong>{modal.product.stock}</strong> units available stock.
              </p>
              {modal.product.description && (
                <p style={{ marginTop: '12px', color: 'var(--color-muted, #666)' }}>
                  {modal.product.description}
                </p>
              )}
            </div>
          </div>
        </Modal>
      )}

      {/* DELETE PRODUCT CONFIRMATION MODAL */}
      {modal?.type === 'delete' && (
        <Modal
          title="Delete this product?"
          onClose={() => {
            if (!deleting) setModal(null)
          }}
          onConfirm={() => handleDelete(modal.product)}
          confirmLabel={deleting ? 'Deleting...' : 'Delete product'}
          danger
        >
          <p className="modal-copy">
            This will permanently remove <strong>{modal.product.name}</strong> from the backend catalog.
          </p>
        </Modal>
      )}
    </>
  )
}