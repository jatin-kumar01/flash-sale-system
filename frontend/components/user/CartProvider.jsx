'use client'

import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import api from '../../lib/api'

const CartContext = createContext(null)

export function getCartStorageKey() {
  if (typeof window === 'undefined') return 'swiftly_cart_guest'
  try {
    const userStr = sessionStorage.getItem('user')
    if (userStr) {
      const u = JSON.parse(userStr)
      const userId = u.id || u.userId
      if (userId) return `swiftly_cart_${userId}`
    }
  } catch (e) {}
  return 'swiftly_cart_guest'
}

export function notifyCartUpdated() {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event('swiftlyCartUpdated'))
  }
}

export function normalizeProduct(rawProduct) {
  if (!rawProduct || !rawProduct.id) return null

  const original = Number(rawProduct.originalPrice ?? rawProduct.original ?? 0)
  const flashSale = Number(rawProduct.flashSalePrice ?? rawProduct.salePrice ?? rawProduct.price ?? original)
  const isSaleActive = Boolean(rawProduct.saleActive)

  const sellingPrice = isSaleActive && flashSale > 0 ? flashSale : original
  const originalPrice = original > sellingPrice ? original : sellingPrice

  return {
    id: String(rawProduct.id),
    title: rawProduct.title ?? rawProduct.name ?? `Product #${rawProduct.id}`,
    name: rawProduct.title ?? rawProduct.name ?? `Product #${rawProduct.id}`,
    description: rawProduct.description ?? '',
    originalPrice,
    flashSalePrice: flashSale > 0 ? flashSale : originalPrice,
    sellingPrice,
    imageUrl: rawProduct.imageUrl ?? rawProduct.image ?? '/placeholder.jpg',
    image: rawProduct.imageUrl ?? rawProduct.image ?? '/placeholder.jpg',
    initialStock: Number(rawProduct.initialStock ?? rawProduct.stock ?? 0),
    status: rawProduct.status ? String(rawProduct.status).toUpperCase() : 'ACTIVE',
    saleActive: isSaleActive,
    rawProduct,
  }
}

export function CartProvider({ children }) {
  // 1. Initial state is [] (deterministic for both server and initial client render)
  const [items, setItems] = useState([])
  const [hasHydrated, setHasHydrated] = useState(false)
  const [lastOrder, setLastOrder] = useState(null)

  const [cartDetails, setCartDetails] = useState({
    detailedItems: [],
    subtotal: 0,
    discount: 0,
    total: 0,
    loading: true,
  })

  // 2. Load items from sessionStorage ONLY after mounting in the browser
  useEffect(() => {
    try {
      const key = getCartStorageKey()
      const savedV2 = sessionStorage.getItem(key) || localStorage.getItem(key)
      if (savedV2) {
        const parsed = JSON.parse(savedV2)
        if (Array.isArray(parsed)) {
          const valid = parsed
            .map((it) => ({
              productId: String(it.productId || it.id || ''),
              quantity: Math.max(1, Number(it.quantity) || 1),
            }))
            .filter((it) => it.productId)
          setItems(valid)
          setHasHydrated(true)
          return
        }
      }
    } catch (e) {
      console.warn('Failed to parse cart from storage:', e)
    }
    setHasHydrated(true)
  }, [])

  // 3. Persist items to per-user storage key after hydration
  useEffect(() => {
    if (!hasHydrated) return
    try {
      const cleanItems = items.map((it) => ({
        productId: String(it.productId),
        quantity: Math.max(1, Number(it.quantity) || 1),
      }))
      const key = getCartStorageKey()
      sessionStorage.setItem(key, JSON.stringify(cleanItems))
      localStorage.setItem(key, JSON.stringify(cleanItems))
      notifyCartUpdated()
    } catch (e) {
      console.warn('Failed to save cart to storage:', e)
    }
  }, [items, hasHydrated])

  // 4. Load product & inventory data for items in cart
  useEffect(() => {
    let isMounted = true

    async function loadCartDetails() {
      if (!items || items.length === 0) {
        if (isMounted) {
          setCartDetails({
            detailedItems: [],
            subtotal: 0,
            discount: 0,
            total: 0,
            loading: false,
          })
        }
        return
      }

      setCartDetails((prev) => ({ ...prev, loading: true }))

      try {
        const fetched = await Promise.all(
          items.map(async (item) => {
            const pId = String(item.productId)
            let rawProd = null
            let availableStock = 0

            try {
              const res = await api.get(`/api/products/${pId}`)
              rawProd = res.data?.data ?? res.data
            } catch (e) {
              console.warn(`Cart product lookup failed for #${pId}:`, e.message)
            }

            const normProd = normalizeProduct(rawProd)

            if (normProd) {
              try {
                const invRes = await api.get(`/api/inventory/${pId}`)
                const invData = invRes.data?.data ?? invRes.data
                if (invData && invData.availableStock != null) {
                  availableStock = Number(invData.availableStock)
                } else {
                  availableStock = normProd.initialStock
                }
              } catch (invErr) {
                availableStock = normProd.initialStock
              }
            }

            if (!normProd) {
              return {
                productId: pId,
                quantity: item.quantity,
                product: null,
                availableStock: 0,
                isInactive: true,
                isOutOfStock: true,
                isUnavailable: true,
                sellingPrice: 0,
                originalPrice: 0,
                itemSubtotal: 0,
                itemOriginalTotal: 0,
              }
            }

            const isOutOfStock = availableStock <= 0
            const isInactive = normProd.status !== 'ACTIVE'
            const isUnavailable = isInactive || isOutOfStock

            let effectiveQty = item.quantity
            if (availableStock > 0 && effectiveQty > availableStock) {
              effectiveQty = availableStock
            }

            return {
              productId: pId,
              quantity: effectiveQty,
              product: normProd,
              availableStock,
              isInactive,
              isOutOfStock,
              isUnavailable,
              sellingPrice: normProd.sellingPrice,
              originalPrice: normProd.originalPrice,
              itemSubtotal: normProd.sellingPrice * effectiveQty,
              itemOriginalTotal: normProd.originalPrice * effectiveQty,
            }
          })
        )

        if (!isMounted) return

        let subtotal = 0
        let originalTotal = 0

        fetched.forEach((d) => {
          if (!d.isUnavailable && d.product) {
            subtotal += d.itemSubtotal
            originalTotal += d.itemOriginalTotal
          }
        })

        const discount = Math.max(0, originalTotal - subtotal)

        setCartDetails({
          detailedItems: fetched,
          subtotal,
          discount,
          total: subtotal,
          loading: false,
        })
      } catch (err) {
        console.error('Failed to load cart details:', err)
        if (isMounted) {
          setCartDetails((prev) => ({ ...prev, loading: false }))
        }
      }
    }

    loadCartDetails()

    return () => {
      isMounted = false
    }
  }, [items])

  // Total quantity count for navbar badge
  const count = useMemo(() => {
    return items.reduce((sum, item) => sum + (Number(item.quantity) || 0), 0)
  }, [items])

  const addItem = (productId, quantity = 1) => {
    if (!productId) return
    const pIdStr = String(productId)
    setItems((current) => {
      const existingIndex = current.findIndex((item) => String(item.productId) === pIdStr)
      if (existingIndex >= 0) {
        return current.map((item, idx) =>
          idx === existingIndex
            ? { ...item, quantity: item.quantity + (quantity || 1) }
            : item
        )
      }
      return [...current, { productId: pIdStr, quantity: Math.max(1, quantity || 1) }]
    })
  }

  const updateQuantity = (productId, quantity, maxStock = null) => {
    const pIdStr = String(productId)
    setItems((current) =>
      current.map((item) => {
        if (String(item.productId) !== pIdStr) return item
        let newQty = Math.max(1, quantity)
        if (maxStock != null && maxStock > 0) {
          newQty = Math.min(maxStock, newQty)
        }
        return { ...item, quantity: newQty }
      })
    )
  }

  const removeItem = (productId) => {
    const pIdStr = String(productId)
    setItems((current) => current.filter((item) => String(item.productId) !== pIdStr))
  }

  const clearCart = () => {
    setItems([])
  }

  return (
    <CartContext.Provider
      value={{
        items,
        detailedItems: cartDetails.detailedItems,
        count,
        subtotal: cartDetails.subtotal,
        discount: cartDetails.discount,
        total: cartDetails.total,
        loading: cartDetails.loading,
        addItem,
        updateQuantity,
        removeItem,
        clearCart,
        lastOrder,
        setLastOrder,
      }}
    >
      {children}
    </CartContext.Provider>
  )
}

export function useCart() {
  const context = useContext(CartContext)
  if (!context) throw new Error('useCart must be used within CartProvider')
  return context
}

export function formatINR(value) {
  return `₹${Number(value || 0).toLocaleString('en-IN')}`
}
