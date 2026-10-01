'use client'

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { PageHeading, ProductForm } from '@/components/admin/AdminUI'
import api from '@/lib/api'

export default function EditProductPage() {
  const params = useParams()
  const router = useRouter()
  const productId = params?.id

  const [product, setProduct] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (productId) {
      loadProduct()
    }
  }, [productId])

  const loadProduct = async () => {
    try {
      setLoading(true)
      setError('')

      const response = await api.get(`/api/products/${productId}?_t=${Date.now()}`)
      console.log('Single product API response:', response.data)

      const productData = response.data?.data ?? response.data
      if (!productData || !productData.id) {
        throw new Error('Product not found.')
      }

      let currentInv = productData.initialStock ?? 0
      try {
        const invRes = await api.get(`/api/inventory/${productId}?_t=${Date.now()}`)
        const invData = invRes.data?.data ?? invRes.data
        if (invData && invData.availableStock != null) {
          currentInv = Number(invData.availableStock)
        }
      } catch (invErr) {
        console.warn(`Could not fetch inventory for product ${productId}:`, invErr.message)
      }

      setProduct({
        ...productData,
        currentStock: currentInv,
      })
    } catch (err) {
      console.error('Failed to load product details:', err)
      const message =
        err.response?.data?.message ||
        err.response?.data?.errorDetails?.[0]?.message ||
        err.message ||
        'Failed to load product details from server.'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  const handleUpdate = async (formData) => {
    setError('')

    const origPrice = Number(formData.originalPrice ?? formData.original_price)
    const salePrice = Number(formData.flashSalePrice ?? formData.salePrice)
    const catalogStock = Number(formData.initialStock ?? formData.stock ?? product.initialStock)
    const addStockQty = Number(formData.addStock ?? 0)

    const payload = {
      title: String(formData.title || formData.name || '').trim(),
      description: String(formData.description || '').trim(),
      originalPrice: isNaN(origPrice) ? null : origPrice,
      flashSalePrice: isNaN(salePrice) ? null : salePrice,
      initialStock: isNaN(catalogStock) ? product.initialStock : catalogStock,
      imageUrl: formData.imageUrl || formData.image || null,
      startTime: formData.startTime || null,
      endTime: formData.endTime || null,
      status: String(formData.status || 'ACTIVE').toUpperCase(),
    }

    console.log("PRODUCT EDIT ID:", productId);
    console.log("PRODUCT EDIT PAYLOAD:", payload);
    console.log("ADD STOCK QUANTITY:", addStockQty);
    console.log(
      "PRODUCT EDIT URL:",
      `/api/products/${productId}`
    );

    // STEP 1: Update Product Information
    try {
      const response = await api.put(
        `/api/products/${productId}`,
        payload
      );

      console.log("PRODUCT EDIT RESPONSE STATUS:", response.status);
      console.log("PRODUCT EDIT RESPONSE:", response.data);
    } catch (error) {
      console.error("PRODUCT EDIT FAILED:", error);
      console.error("STATUS:", error.response?.status);
      console.error("DATA:", error.response?.data);

      const message =
        error.response?.data?.message ||
        error.response?.data?.errorDetails?.[0]?.message ||
        error.message ||
        'Failed to update product.';
      setError(message);
      throw error;
    }

    // STEP 2: Replenish Inventory if Add Stock > 0
    if (addStockQty > 0) {
      try {
        console.log(`REPLENISHING INVENTORY: productId=${productId}, quantity=${addStockQty}`);
        const invRes = await api.post(`/api/inventory/replenish?productId=${productId}&quantity=${addStockQty}`);
        console.log("REPLENISH RESPONSE STATUS:", invRes.status);
        console.log("REPLENISH RESPONSE DATA:", invRes.data);
      } catch (invErr) {
        console.error("INVENTORY REPLENISH FAILED:", invErr);
        console.error("STATUS:", invErr.response?.status);
        console.error("DATA:", invErr.response?.data);

        const partialMessage = "Product details were updated, but stock could not be added.";
        setError(partialMessage);
        const customErr = new Error(partialMessage);
        customErr.response = invErr.response;
        throw customErr;
      }
    }

    // STEP 4: Both operations complete successfully -> Hard navigate back to Admin Products list
    window.location.href = '/admin/products';
  }

  if (loading) {
    return (
      <>
        <PageHeading
          eyebrow="Catalog"
          title="Edit product"
          description="Loading product details..."
        />
        <section className="panel form-panel">
          <div className="empty-state">
            <p>Loading product...</p>
          </div>
        </section>
      </>
    )
  }

  if (error || !product) {
    return (
      <>
        <PageHeading
          eyebrow="Catalog"
          title="Edit product"
          description="Error loading product."
        />
        <section className="panel form-panel">
          <div className="empty-state">
            <p>{error || 'Product not found.'}</p>
            <button className="button primary" onClick={loadProduct}>
              Retry
            </button>
            <button
              className="button secondary"
              style={{ marginLeft: '10px' }}
              onClick={() => router.push('/admin/products')}
            >
              Back to Products
            </button>
          </div>
        </section>
      </>
    )
  }

  // Format initial values for ProductForm
  const initialFormValues = {
    id: product.id,
    name: product.title || product.name || '',
    description: product.description || '',
    originalPrice: product.originalPrice ?? '',
    salePrice: product.flashSalePrice ?? product.salePrice ?? '',
    stock: product.initialStock ?? '',
    currentStock: product.currentStock ?? 0,
    image: product.imageUrl || product.image || '/placeholder.jpg',
    startTime: product.startTime || '',
    endTime: product.endTime || '',
    status: product.status || 'ACTIVE',
  }

  return (
    <>
      <PageHeading
        eyebrow="Catalog"
        title={`Edit ${product.title || product.name || 'Product'}`}
        description={`Update product #${product.id} details in the Swiftly catalog.`}
      />
      <section className="panel form-panel">
        <ProductForm
          initial={initialFormValues}
          onSave={handleUpdate}
          onCancel={() => router.push('/admin/products')}
        />
      </section>
    </>
  )
}
