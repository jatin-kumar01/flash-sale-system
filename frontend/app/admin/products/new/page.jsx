'use client'

import { useRouter } from 'next/navigation'
import { PageHeading, ProductForm } from '@/components/admin/AdminUI'
import api from '@/lib/api'

export default function NewProductPage() {
  const router = useRouter()

  const handleCreate = async (formData) => {
    const payload = {
      title: formData.title || formData.name,
      description: formData.description || '',
      originalPrice: Number(formData.originalPrice),
      flashSalePrice: Number(formData.flashSalePrice),
      initialStock: Number(formData.initialStock),
      imageUrl: formData.imageUrl || formData.image || null,
      startTime: formData.startTime,
      endTime: formData.endTime,
    }

    console.log('Sending POST create product request:', payload)

    await api.post('/api/products', payload)
    router.push('/admin/products')
  }

  return (
    <>
      <PageHeading
        eyebrow="Catalog"
        title="Add product"
        description="Create a new listing using the Swiftly catalog."
      />
      <section className="panel form-panel">
        <ProductForm
          onSave={handleCreate}
          onCancel={() => router.back()}
        />
      </section>
    </>
  )
}
