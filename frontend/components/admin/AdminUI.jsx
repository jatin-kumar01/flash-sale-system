'use client'

import Link from 'next/link'
import { useState, useRef } from 'react'
import { Search, SlidersHorizontal, ArrowUpDown, X, Check, AlertTriangle, Upload } from 'lucide-react'
import api from '@/lib/api'

export function PageHeading({ eyebrow, title, description, action }) { return <div className="page-heading"><div><span className="eyebrow coral">{eyebrow}</span><h1>{title}</h1>{description && <p>{description}</p>}</div>{action}</div> }
export function StatCards({ items }) {
  return (
    <div className="stats-grid">
      {(items || []).map((item, index) => (
        <article
          className="stat-card"
          key={`stat-card-${index}`}
        >
          <div className="stat-top">
            <span>{item.label}</span>
            <i className={item.warning ? 'warning' : ''} />
          </div>
          <strong>{item.value}</strong>
          <div className="stat-meta">
            <b className={item.warning ? 'warning-text' : ''}>{item.note}</b>
            <span>{item.detail}</span>
          </div>
        </article>
      ))}
    </div>
  )
}
export function Toolbar({ search, setSearch, placeholder = 'Search...', filter, setFilter, filterOptions = [], sort, setSort }) { return <div className="toolbar"><label className="search-field"><Search size={16} /><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder={placeholder} /></label>{filter && <label className="select-field"><SlidersHorizontal size={15} /><select value={filter} onChange={(e) => setFilter(e.target.value)}><option value="All">All statuses</option>{filterOptions.map((option) => <option key={option}>{option}</option>)}</select></label>}{sort && <label className="select-field"><ArrowUpDown size={15} /><select value={sort} onChange={(e) => setSort(e.target.value)}><option value="default">Sort by</option><option value="name">Name A-Z</option><option value="price">Price high-low</option><option value="stock">Stock low-high</option></select></label>}</div> }
export function Panel({ title, eyebrow, action, children, className = '' }) { return <section className={`panel ${className}`}><div className="panel-heading"><div>{eyebrow && <span className="eyebrow">{eyebrow}</span>}<h2>{title}</h2></div>{action}</div>{children}</section> }
export function StatusPill({ children }) { return <span className={`status-pill ${String(children).toLowerCase()}`}>{children}</span> }

export function ProductThumb({ product, size = 'small' }) {
  const rawSource =
    product?.imageUrl ||
    product?.image ||
    '/placeholder.jpg'

  const imageSource = rawSource.startsWith('/uploads')
    ? `${process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'}${rawSource}`
    : rawSource

  return (
    <img
      className={`product-thumb ${size}`}
      src={imageSource}
      alt={`${product?.name || product?.title || 'Product'} product`}
      onError={(e) => {
        e.currentTarget.src = '/placeholder.jpg'
      }}
    />
  )
}
export function Modal({ title, children, onClose, onConfirm, confirmLabel = 'Save changes', danger = false }) { return <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}><div className="modal-card" role="dialog" aria-modal="true"><div className="modal-header"><h2>{title}</h2><button onClick={onClose} aria-label="Close"><X size={18} /></button></div>{children}<div className="modal-actions"><button className="button secondary" onClick={onClose}>Cancel</button>{onConfirm && <button className={`button ${danger ? 'danger' : 'primary'}`} onClick={onConfirm}>{confirmLabel}</button>}</div></div></div> }
export function EmptyState({ text }) { return <div className="empty-state"><AlertTriangle size={22} /><p>{text}</p></div> }
//export function ProductForm({ initial, onSave, onCancel }) { const [form, setForm] = useState(initial || { name: '', originalPrice: '', salePrice: '', stock: '', image: '/images/aeropods.jpg', sale: 'Scheduled', status: 'Draft' }); const update = (key, value) => setForm({ ...form, [key]: value }); return <div className="form-grid">{[['name','Product name','text'],['originalPrice','Original price','number'],['salePrice','Flash sale price','number'],['stock','Initial stock','number']].map(([key,label,type]) => <label key={key}>{label}<input type={type} value={form[key]} onChange={(e) => update(key, e.target.value)} /></label>)}<label>Flash sale start<input type="datetime-local" /></label><label>Flash sale end<input type="datetime-local" /></label><div className="image-selector"><span>Choose product image</span><div>{Object.entries({AirPods:'/images/aeropods.jpg','iPhone 17':'/images/iphone.jpg','Lumen Lamp':'/images/lumen-lamp.jpg','Nova Camera':'/images/nova-camera.jpg','Orbit Mechanical':'/images/orbit-mechanical.jpg','Pulse Smartwatch':'/images/pulse-smartwatch.jpg','Velocity Runner':'/images/velocity-runner.jpg'}).map(([name,image]) => <button type="button" key={name} className={form.image === image ? 'selected' : ''} onClick={() => update('image', image)}><img src={image} alt={name} onError={(e) => { e.currentTarget.src = '/placeholder.jpg' }} /><small>{name}</small></button>)}</div></div><div className="modal-actions form-actions"><button className="button secondary" onClick={onCancel}>Cancel</button><button className="button primary" onClick={() => onSave({ ...form, originalPrice: Number(form.originalPrice), salePrice: Number(form.salePrice), stock: Number(form.stock), id: form.id || `p-${Date.now()}`, category: form.category || 'General' })}><Check size={15} />Save product</button></div></div> }
// export function ProductForm({ initial, onSave, onCancel }) {
//   const [form, setForm] = useState(() => ({
//     id: initial?.id || '',
//     name: initial?.name || initial?.title || '',
//     description: initial?.description || '',
//     originalPrice: initial?.originalPrice ?? '',
//     salePrice:
//       initial?.salePrice ??
//       initial?.flashSalePrice ??
//       '',
//     stock:
//       initial?.stock ??
//       initial?.initialStock ??
//       '',
//     image:
//       initial?.imageUrl ||
//       initial?.image ||
//       '/images/aeropods.jpg',
//     startTime: initial?.startTime
//       ? formatDateTimeLocal(initial.startTime)
//       : '',
//     endTime: initial?.endTime
//       ? formatDateTimeLocal(initial.endTime)
//       : '',
//     status: initial?.status || 'DRAFT',
//   }))

//   const [error, setError] = useState('')

//   const update = (key, value) => {
//     setForm((current) => ({
//       ...current,
//       [key]: value,
//     }))
//   }

//   const handleSave = () => {
//     setError('')

//     if (!form.name.trim()) {
//       setError('Product name is required.')
//       return
//     }

//     if (!form.originalPrice || Number(form.originalPrice) <= 0) {
//       setError('Original price must be greater than 0.')
//       return
//     }

//     if (!form.salePrice || Number(form.salePrice) <= 0) {
//       setError('Flash sale price must be greater than 0.')
//       return
//     }

//     if (!form.stock || Number(form.stock) < 1) {
//       setError('Initial stock must be at least 1.')
//       return
//     }

//     if (!form.startTime) {
//       setError('Flash sale start time is required.')
//       return
//     }

//     if (!form.endTime) {
//       setError('Flash sale end time is required.')
//       return
//     }

//     const start = new Date(form.startTime)
//     const end = new Date(form.endTime)

//     if (end <= start) {
//       setError('Flash sale end time must be after start time.')
//       return
//     }

//     if (Number(form.salePrice) >= Number(form.originalPrice)) {
//       setError(
//         'Flash sale price should be lower than the original price.'
//       )
//       return
//     }

//     const product = {
//       ...(form.id ? { id: form.id } : {}),

//       title: form.name.trim(),

//       description: form.description.trim(),

//       originalPrice: Number(form.originalPrice),

//       flashSalePrice: Number(form.salePrice),

//       initialStock: Number(form.stock),

//       imageUrl: form.image || null,

//       startTime: new Date(form.startTime).toISOString(),

//       endTime: new Date(form.endTime).toISOString(),

//       ...(initial?.id
//         ? { status: form.status }
//         : {}),
//     }

//     onSave(product)
//   }

//   return (
//     <div className="form-grid">

//       <label>
//         Product name

//         <input
//           type="text"
//           value={form.name}
//           onChange={(e) =>
//             update('name', e.target.value)
//           }
//           placeholder="Enter product name"
//         />
//       </label>

//       <label>
//         Original price

//         <input
//           type="number"
//           min="0.01"
//           step="0.01"
//           value={form.originalPrice}
//           onChange={(e) =>
//             update('originalPrice', e.target.value)
//           }
//           placeholder="79999"
//         />
//       </label>

//       <label>
//         Flash sale price

//         <input
//           type="number"
//           min="0.01"
//           step="0.01"
//           value={form.salePrice}
//           onChange={(e) =>
//             update('salePrice', e.target.value)
//           }
//           placeholder="69999"
//         />
//       </label>

//       <label>
//         Initial stock

//         <input
//           type="number"
//           min="1"
//           value={form.stock}
//           onChange={(e) =>
//             update('stock', e.target.value)
//           }
//           placeholder="100"
//         />
//       </label>

//       <label className="form-full-width">
//         Description

//         <textarea
//           value={form.description}
//           onChange={(e) =>
//             update('description', e.target.value)
//           }
//           placeholder="Enter product description"
//           rows={4}
//         />
//       </label>

//       <label>
//         Flash sale start

//         <input
//           type="datetime-local"
//           value={form.startTime}
//           onChange={(e) =>
//             update('startTime', e.target.value)
//           }
//         />
//       </label>

//       <label>
//         Flash sale end

//         <input
//           type="datetime-local"
//           value={form.endTime}
//           onChange={(e) =>
//             update('endTime', e.target.value)
//           }
//         />
//       </label>

//       {initial?.id && (
//         <label>
//           Product status

//           <select
//             value={form.status}
//             onChange={(e) =>
//               update('status', e.target.value)
//             }
//           >
//             <option value="DRAFT">DRAFT</option>
//             <option value="ACTIVE">ACTIVE</option>
//             <option value="INACTIVE">INACTIVE</option>
//             <option value="SOLD_OUT">SOLD_OUT</option>
//           </select>
//         </label>
//       )}

//       <div className="image-selector">

//         <span>
//           Choose product image
//         </span>

//         <div>
//           {Object.entries({
//             AirPods: '/images/aeropods.jpg',
//             'iPhone 17': '/images/iphone.jpg',
//             'Lumen Lamp': '/images/lumen-lamp.jpg',
//             'Nova Camera': '/images/nova-camera.jpg',
//             'Orbit Mechanical': '/images/orbit-mechanical.jpg',
//             'Pulse Smartwatch': '/images/pulse-smartwatch.jpg',
//             'Velocity Runner': '/images/velocity-runner.jpg',
//           }).map(([name, image]) => (
//             <button
//               type="button"
//               key={name}
//               className={
//                 form.image === image
//                   ? 'selected'
//                   : ''
//               }
//               onClick={() =>
//                 update('image', image)
//               }
//             >
//               <img
//                 src={image}
//                 alt={name}
//                 onError={(e) => {
//                   e.currentTarget.src =
//                     '/placeholder.jpg'
//                 }}
//               />

//               <small>
//                 {name}
//               </small>
//             </button>
//           ))}
//         </div>

//       </div>

//       {error && (
//         <div className="login-error">
//           {error}
//         </div>
//       )}

//       <div className="modal-actions form-actions">

//         <button
//           type="button"
//           className="button secondary"
//           onClick={onCancel}
//         >
//           Cancel
//         </button>

//         <button
//           type="button"
//           className="button primary"
//           onClick={handleSave}
//         >
//           <Check size={15} />

//           {initial?.id
//             ? 'Save changes'
//             : 'Save product'}
//         </button>

//       </div>

//     </div>
//   )
// }

export function ProductForm({ initial, onSave, onCancel }) {
  const [form, setForm] = useState(() => ({
    id: initial?.id || '',
    name: initial?.name || initial?.title || '',
    description: initial?.description || '',
    originalPrice: initial?.originalPrice ?? '',
    salePrice:
      initial?.salePrice ??
      initial?.flashSalePrice ??
      '',
    stock:
      initial?.stock ??
      initial?.initialStock ??
      '',
    currentStock: initial?.currentStock ?? initial?.stock ?? 0,
    addStock: '',
    image:
      initial?.imageUrl ||
      initial?.image ||
      '/images/aeropods.jpg',
    startTime: initial?.startTime
      ? formatDateTimeLocal(initial.startTime)
      : '',
    endTime: initial?.endTime
      ? formatDateTimeLocal(initial.endTime)
      : '',
    status: initial?.status || 'DRAFT',
  }))

  const [selectedFile, setSelectedFile] = useState(null)
  const [previewUrl, setPreviewUrl] = useState(null)
  const fileInputRef = useRef(null)

  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const update = (key, value) => {
    setForm((current) => ({
      ...current,
      [key]: value,
    }))
  }

  const handleFileChange = (e) => {
    const file = e.target.files?.[0]
    if (!file) return

    const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp']
    if (!validTypes.includes(file.type)) {
      setError('Please select a valid image file (JPG, PNG, or WEBP).')
      return
    }

    if (file.size > 20 * 1024 * 1024) {
      setError('Image size must not exceed 20 MB.')
      return
    }

    setError('')
    setSelectedFile(file)

    if (previewUrl) {
      URL.revokeObjectURL(previewUrl)
    }
    const newPreviewUrl = URL.createObjectURL(file)
    setPreviewUrl(newPreviewUrl)
    update('image', '')
  }

  const handleSelectPredefinedImage = (image) => {
    setSelectedFile(null)
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl)
      setPreviewUrl(null)
    }
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
    update('image', image)
  }

  const handleSave = async () => {
    if (saving) {
      return
    }

    setError('')

    if (!form.name.trim()) {
      setError('Product name is required.')
      return
    }

    if (!form.originalPrice || Number(form.originalPrice) <= 0) {
      setError('Original price must be greater than 0.')
      return
    }

    if (!form.salePrice || Number(form.salePrice) <= 0) {
      setError('Flash sale price must be greater than 0.')
      return
    }

    if (!initial?.id) {
      if (!form.stock || Number(form.stock) < 1) {
        setError('Initial stock must be at least 1.')
        return
      }
    } else {
      if (form.addStock !== undefined && form.addStock !== null && String(form.addStock).trim() !== '') {
        const addQty = Number(form.addStock)
        if (isNaN(addQty) || !Number.isInteger(addQty) || addQty <= 0) {
          setError('Add stock must be a positive whole number.')
          return
        }
      }
    }

    if (!form.startTime) {
      setError('Flash sale start time is required.')
      return
    }

    if (!form.endTime) {
      setError('Flash sale end time is required.')
      return
    }

    const start = new Date(form.startTime)
    const end = new Date(form.endTime)

    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) {
      setError('Please enter valid start and end times.')
      return
    }

    if (end <= start) {
      setError('Flash sale end time must be after start time.')
      return
    }

    if (Number(form.salePrice) >= Number(form.originalPrice)) {
      setError(
        'Flash sale price should be lower than the original price.'
      )
      return
    }

    try {
      setSaving(true)

      let finalImageUrl = form.image

      if (selectedFile) {
        const formData = new FormData()
        formData.append('image', selectedFile)

        const uploadResponse = await api.post('/api/products/upload-image', formData)
        const uploadResult = uploadResponse.data?.data ?? uploadResponse.data

        if (uploadResult?.imageUrl) {
          finalImageUrl = uploadResult.imageUrl
        } else {
          throw new Error('Image upload failed: invalid response from server.')
        }
      }

      const product = {
        ...(form.id ? { id: form.id } : {}),
        title: form.name.trim(),
        description: form.description.trim(),
        originalPrice: Number(form.originalPrice),
        flashSalePrice: Number(form.salePrice),
        initialStock: Number(form.stock),
        addStock: form.addStock && String(form.addStock).trim() !== '' ? Number(form.addStock) : 0,
        imageUrl: finalImageUrl || null,
        startTime: start.toISOString(),
        endTime: end.toISOString(),
        ...(initial?.id ? { status: form.status } : {}),
      }

      console.log(
        initial?.id
          ? 'Updating product:'
          : 'Creating product:',
        product
      )

      await onSave(product)
    } catch (err) {
      console.error('Product save failed:', err)

      setError(
        err?.response?.data?.message ||
        err?.response?.data?.errorDetails?.[0]?.message ||
        err?.message ||
        'Failed to save product.'
      )
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="form-grid">

      <label>
        Product name

        <input
          type="text"
          value={form.name}
          onChange={(e) =>
            update('name', e.target.value)
          }
          placeholder="Enter product name"
          disabled={saving}
        />
      </label>

      <label>
        Original price

        <input
          type="number"
          min="0.01"
          step="0.01"
          value={form.originalPrice}
          onChange={(e) =>
            update('originalPrice', e.target.value)
          }
          placeholder="79999"
          disabled={saving}
        />
      </label>

      <label>
        Flash sale price

        <input
          type="number"
          min="0.01"
          step="0.01"
          value={form.salePrice}
          onChange={(e) =>
            update('salePrice', e.target.value)
          }
          placeholder="69999"
          disabled={saving}
        />
      </label>

      {initial?.id ? (
        <>
          <label>
            Current stock
            <input
              type="text"
              value={form.currentStock}
              readOnly
              disabled
              style={{ backgroundColor: 'var(--color-bg-subtle, #f5f5f7)', cursor: 'not-allowed', color: '#555' }}
            />
          </label>

          <label>
            Add stock
            <input
              type="number"
              min="1"
              step="1"
              value={form.addStock}
              onChange={(e) => update('addStock', e.target.value)}
              placeholder="0"
              disabled={saving}
            />
          </label>
        </>
      ) : (
        <label>
          Initial stock

          <input
            type="number"
            min="1"
            value={form.stock}
            onChange={(e) =>
              update('stock', e.target.value)
            }
            placeholder="100"
            disabled={saving}
          />
        </label>
      )}

      <label className="form-full-width">
        Description

        <textarea
          value={form.description}
          onChange={(e) =>
            update('description', e.target.value)
          }
          placeholder="Enter product description"
          rows={4}
          disabled={saving}
        />
      </label>

      <label>
        Flash sale start

        <input
          type="datetime-local"
          value={form.startTime}
          onChange={(e) =>
            update('startTime', e.target.value)
          }
          disabled={saving}
        />
      </label>

      <label>
        Flash sale end

        <input
          type="datetime-local"
          value={form.endTime}
          onChange={(e) =>
            update('endTime', e.target.value)
          }
          disabled={saving}
        />
      </label>

      {initial?.id && (
        <label>
          Product status

          <select
            value={form.status}
            onChange={(e) =>
              update('status', e.target.value)
            }
            disabled={saving}
          >
            <option value="DRAFT">DRAFT</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="INACTIVE">INACTIVE</option>
            <option value="SOLD_OUT">SOLD_OUT</option>
          </select>
        </label>
      )}

      <div className="image-selector">

        <span>
          Choose product image
        </span>

        <div style={{ marginBottom: '16px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <input
            type="file"
            ref={fileInputRef}
            style={{ display: 'none' }}
            accept="image/jpeg,image/png,image/webp"
            onChange={handleFileChange}
            disabled={saving}
          />

          <button
            type="button"
            className={`button secondary ${selectedFile || (form.image && form.image.startsWith('/uploads')) ? 'selected' : ''}`}
            onClick={() => fileInputRef.current?.click()}
            disabled={saving}
            style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', alignSelf: 'flex-start' }}
          >
            <Upload size={16} />
            {selectedFile ? 'Change uploaded image' : 'Upload image from PC'}
          </button>

          {previewUrl ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginTop: '4px' }}>
              <img
                src={previewUrl}
                alt="Selected preview"
                style={{ width: '56px', height: '56px', objectFit: 'cover', borderRadius: '6px', border: '1px solid #ddd' }}
              />
              <span style={{ fontSize: '0.85rem', color: '#555' }}>
                Selected file: <strong>{selectedFile.name}</strong> ({(selectedFile.size / 1024 / 1024).toFixed(2)} MB)
              </span>
            </div>
          ) : form.image && form.image.startsWith('/uploads') ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginTop: '4px' }}>
              <img
                src={`${process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'}${form.image}`}
                alt="Current uploaded product image"
                style={{ width: '56px', height: '56px', objectFit: 'cover', borderRadius: '6px', border: '1px solid #ddd' }}
              />
              <span style={{ fontSize: '0.85rem', color: '#555' }}>
                Current image: <strong>Uploaded PC file</strong>
              </span>
            </div>
          ) : null}
        </div>

        <span style={{ fontSize: '0.85rem', color: '#666', marginBottom: '8px', display: 'block' }}>
          Or select from catalog presets:
        </span>

        <div>
          {Object.entries({
            AirPods: '/images/aeropods.jpg',
            'iPhone 17': '/images/iphone.jpg',
            'Lumen Lamp': '/images/lumen-lamp.jpg',
            'Nova Camera': '/images/nova-camera.jpg',
            'Orbit Mechanical': '/images/orbit-mechanical.jpg',
            'Pulse Smartwatch': '/images/pulse-smartwatch.jpg',
            'Velocity Runner': '/images/velocity-runner.jpg',
          }).map(([name, image]) => (
            <button
              type="button"
              key={name}
              className={
                !selectedFile && form.image === image
                  ? 'selected'
                  : ''
              }
              onClick={() => handleSelectPredefinedImage(image)}
              disabled={saving}
            >
              <img
                src={image}
                alt={name}
                onError={(e) => {
                  e.currentTarget.src =
                    '/placeholder.jpg'
                }}
              />

              <small>
                {name}
              </small>
            </button>
          ))}
        </div>

      </div>

      {error && (
        <div className="login-error">
          {error}
        </div>
      )}

      <div className="modal-actions form-actions">

        <button
          type="button"
          className="button secondary"
          onClick={onCancel}
          disabled={saving}
        >
          Cancel
        </button>

        <button
          type="button"
          className="button primary"
          onClick={handleSave}
          disabled={saving}
        >
          <Check size={15} />

          {saving
            ? 'Saving...'
            : initial?.id
              ? 'Save changes'
              : 'Save product'}
        </button>

      </div>

    </div>
  )
}

function formatDateTimeLocal(value) {
  try {
    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
      return ''
    }

    const year = date.getFullYear()
    const month = String(
      date.getMonth() + 1
    ).padStart(2, '0')
    const day = String(
      date.getDate()
    ).padStart(2, '0')
    const hours = String(
      date.getHours()
    ).padStart(2, '0')
    const minutes = String(
      date.getMinutes()
    ).padStart(2, '0')

    return `${year}-${month}-${day}T${hours}:${minutes}`
  } catch {
    return ''
  }
}
export function LinkButton({ href, children, variant = 'primary' }) { return <Link className={`button ${variant}`} href={href}>{children}</Link> }
