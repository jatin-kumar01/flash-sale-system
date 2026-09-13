'use client'

import { useEffect, useMemo, useState } from 'react'
import { useRouter } from 'next/navigation'
import api from '@/lib/api'


import {
    Activity, Bell, Box, Check, ChevronDown, ChevronLeft, CircleDollarSign, Clock3,
    CreditCard, Filter, Home, KeyRound, LayoutDashboard, LogOut, Menu, Package, Plus,
    Search, Settings, ShoppingBag, ShoppingCart, Sparkles, Trash2, TrendingUp,
    Truck, UserPlus, Users, Webhook, X, Zap,
} from 'lucide-react'

const products = [
    {
        id: 1,
        name: 'AeroPods Max',
        category: 'Audio',
        price: 249,
        original: 399,
        stock: 18,
        sold: 82,
        color: 'from-amber-400 to-orange-600',
        specs: ['Adaptive noise cancellation', '40-hour battery', 'Spatial audio'],
        variants: {
            Color: ['Midnight', 'Silver', 'Citrus'],
            Size: ['Standard']
        }
    },
    {
        id: 2,
        name: 'Velocity Runner',
        category: 'Footwear',
        price: 89,
        original: 140,
        stock: 31,
        sold: 69,
        color: 'from-cyan-400 to-blue-600',
        specs: ['Carbon foam sole', 'Breathable knit upper', 'Reflective heel'],
        variants: {
            Color: ['Ocean', 'Volt', 'Black'],
            Size: ['8', '9', '10', '11', '12']
        }
    },
    {
        id: 3,
        name: 'Orbit Mechanical',
        category: 'Workspace',
        price: 129,
        original: 189,
        stock: 12,
        sold: 88,
        color: 'from-fuchsia-400 to-violet-600',
        specs: ['Hot-swappable switches', 'RGB backlight', 'Aluminum frame'],
        variants: {
            Color: ['Violet', 'Graphite'],
            Size: ['75%', 'TKL']
        }
    },
    {
        id: 4,
        name: 'Nova Camera Kit',
        category: 'Creative',
        price: 599,
        original: 749,
        stock: 7,
        sold: 93,
        color: 'from-emerald-400 to-teal-600',
        specs: ['4K 120fps video', '24MP sensor', 'Magnetic lens mount'],
        variants: {
            Color: ['Forest', 'Black'],
            Size: ['Body only', 'Creator kit']
        }
    },
    {
        id: 5,
        name: 'Pulse Smartwatch',
        category: 'Wearables',
        price: 179,
        original: 249,
        stock: 24,
        sold: 76,
        color: 'from-rose-400 to-red-600',
        specs: ['7-day battery', 'Sleep tracking', 'Water resistant 50m'],
        variants: {
            Color: ['Coral', 'Slate'],
            Size: ['40mm', '44mm']
        }
    },
    {
        id: 6,
        name: 'Lumen Desk Lamp',
        category: 'Workspace',
        price: 64,
        original: 99,
        stock: 42,
        sold: 58,
        color: 'from-yellow-300 to-amber-600',
        specs: ['Adaptive brightness', 'USB-C charging', 'Touch controls'],
        variants: {
            Color: ['Sunrise', 'Charcoal'],
            Size: ['One size']
        }
    },
]


function Metric({ icon: Icon, label, value, detail, tone = 'amber' }) {
    return (
        <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5 shadow-2xl shadow-black/10">
            <div className="flex items-start justify-between">
        <span
            className={`rounded-xl p-2.5 ${
                tone === 'amber'
                    ? 'bg-amber-400/10 text-amber-300'
                    : tone === 'emerald'
                        ? 'bg-emerald-400/10 text-emerald-300'
                        : 'bg-cyan-400/10 text-cyan-300'
            }`}
        >
          <Icon size={19} />
        </span>
                <TrendingUp size={16} className="text-emerald-400" />
            </div>

            <p className="mt-5 text-sm text-slate-400">{label}</p>
            <p className="mt-1 text-2xl font-semibold tracking-tight text-white">{value}</p>
            <p className="mt-1 text-xs text-emerald-400">{detail}</p>
        </div>
    )
}

function ProductCard({ product, onAdd, onOpen }) {
    const outOfStock =
        product.status === 'DISABLED' ||
        Number(product.stock ?? 0) <= 0

    return (
        <article
            onClick={() => onOpen(product)}
            className={`group overflow-hidden rounded-2xl border border-slate-800 bg-slate-900/80 transition ${
                outOfStock
                    ? 'cursor-pointer opacity-75'
                    : 'cursor-pointer hover:-translate-y-1 hover:border-amber-400/50'
            }`}
        >
            <div
                className={`relative flex h-40 items-center justify-center bg-gradient-to-br ${product.color}`}
            >
                <Box
                    size={60}
                    className="text-white/85 transition group-hover:scale-110"
                    strokeWidth={1.2}
                />

                <span className="absolute right-3 top-3 rounded-full bg-slate-950/70 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-white">
                    -{Math.round((1 - product.price / product.original) * 100)}%
                </span>

                {outOfStock && (
                    <span className="absolute left-3 top-3 rounded-full bg-rose-500/90 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-white">
                        SOLD OUT
                    </span>
                )}
            </div>

            <div className="p-4">
                <p className="text-xs text-slate-500">
                    {product.category}
                </p>

                <h3 className="mt-1 font-medium text-white">
                    {product.name}
                </h3>

                <div className="mt-4 flex items-end justify-between">
                    <div>
                        <span className="text-lg font-semibold text-amber-300">
                            ₹{Number(product.price).toLocaleString('en-IN')}
                        </span>

                        <span className="ml-2 text-xs text-slate-600 line-through">
                            ₹{Number(product.original).toLocaleString('en-IN')}
                        </span>

                        <p
                            className={`mt-1 text-xs font-medium ${
                                outOfStock
                                    ? 'text-rose-300'
                                    : 'text-emerald-300'
                            }`}
                        >
                            {outOfStock
                                ? 'Out of stock'
                                : `${product.stock} available`}
                        </p>
                    </div>

                    <button
                        disabled={outOfStock}
                        onClick={(e) => {
                            e.stopPropagation()

                            if (outOfStock) return

                            onAdd(product)
                        }}
                        className="rounded-lg bg-amber-400 p-2 text-slate-950 transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:bg-slate-700 disabled:text-slate-500 disabled:hover:bg-slate-700"
                        aria-label={
                            outOfStock
                                ? `${product.name} is out of stock`
                                : `Add ${product.name} to cart`
                        }
                    >
                        <ShoppingCart size={16} />
                    </button>
                </div>
            </div>
        </article>
    )
}
function Overview({ setView, onAdd, onOpen }) {
    return (
        <div className="space-y-6">
            <section className="relative overflow-hidden rounded-3xl border border-amber-400/20 bg-gradient-to-br from-slate-900 via-slate-900 to-amber-950/40 p-7 md:p-10">
                <div className="relative z-10 max-w-2xl">
                    <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-amber-400/30 bg-amber-400/10 px-3 py-1.5 text-xs font-medium text-amber-300">
                        <Sparkles size={14} />
                        Live commerce control room
                    </div>

                    <h1 className="text-4xl font-semibold tracking-tight text-white md:text-6xl">
                        Sell faster.
                        <br />
                        <span className="text-amber-300">Stay ahead.</span>
                    </h1>

                    <p className="mt-5 max-w-lg leading-7 text-slate-400">
                        The high-concurrency engine for flash commerce. Monitor demand,
                        move inventory, and turn every second into momentum.
                    </p>

                    <div className="mt-7 flex flex-wrap gap-3">
                        <button
                            onClick={() => setView('sale')}
                            className="rounded-xl bg-amber-400 px-5 py-3 text-sm font-semibold text-slate-950 hover:bg-amber-300"
                        >
                            Enter flash sale <Zap className="ml-2 inline" size={16} />
                        </button>

                        <button
                            onClick={() => setView('products')}
                            className="rounded-xl border border-slate-700 px-5 py-3 text-sm font-semibold text-white hover:bg-slate-800"
                        >
                            Browse catalog
                        </button>
                    </div>
                </div>
            </section>

            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <Metric
                    icon={CircleDollarSign}
                    label="Gross revenue"
                    value="$48,290"
                    detail="+18.4% this week"
                />

                <Metric
                    icon={Users}
                    label="Active shoppers"
                    value="2,841"
                    detail="+12.8% live now"
                    tone="cyan"
                />

                <Metric
                    icon={Activity}
                    label="Conversion rate"
                    value="8.42%"
                    detail="+2.1% vs. last sale"
                    tone="emerald"
                />

                <Metric
                    icon={Truck}
                    label="Orders shipped"
                    value="1,248"
                    detail="94% on time"
                    tone="cyan"
                />
            </div>

            <div className="grid gap-6 lg:grid-cols-[1.3fr_0.7fr]">
                <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">
                    <div className="flex items-center justify-between">
                        <div>
                            <p className="font-medium text-white">Live catalog</p>
                            <p className="mt-1 text-sm text-slate-500">
                                Top drops moving right now
                            </p>
                        </div>

                        <button
                            onClick={() => setView('products')}
                            className="text-xs text-amber-300"
                        >
                            View all
                        </button>
                    </div>

                    <div className="mt-5 grid gap-3 sm:grid-cols-3">
                        {products.slice(0, 3).map((p) => (
                            <ProductCard
                                key={p.id}
                                product={p}
                                onAdd={onAdd}
                                onOpen={onOpen}
                            />
                        ))}
                    </div>
                </div>

                <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">
                    <p className="font-medium text-white">System pulse</p>

                    <div className="mt-5 space-y-4">
                        {[
                            'Payments processing normally',
                            'Inventory sync completed',
                            '2,841 shoppers browsing'
                        ].map((x, i) => (
                            <div key={x} className="flex gap-3 text-sm">
                <span
                    className={`mt-1.5 h-2 w-2 rounded-full ${
                        i === 2 ? 'bg-amber-400' : 'bg-emerald-400'
                    }`}
                />

                                <span className="text-slate-400">{x}</span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    )
}

function Products({ onAdd, onOpen }) {
    const [query, setQuery] = useState('')
    const [category, setCategory] = useState('All')
    const [products, setProducts] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    const colors = [
        'from-amber-400 to-orange-600',
        'from-cyan-400 to-blue-600',
        'from-fuchsia-400 to-violet-600',
        'from-emerald-400 to-teal-600',
        'from-rose-400 to-red-600',
        'from-yellow-300 to-amber-600',
    ]

    const fetchProducts = async () => {
        try {
            setError('')

            const response = await api.get('/products')
            const backendProducts =
                response.data?.data?.content || []

            const mappedProducts = await Promise.all(
                backendProducts.map(async (product, index) => {
                    let inventory = null

                    try {
                        const inventoryResponse = await api.get(
                            `/inventory/${product.id}`
                        )

                        inventory =
                            inventoryResponse.data?.data ||
                            inventoryResponse.data
                    } catch (inventoryError) {
                        console.error(
                            `Inventory error for product ${product.id}:`,
                            inventoryError
                        )
                    }

                    const availableStock = Number(
                        inventory?.availableStock ??
                        product.initialStock ??
                        0
                    )

                    const totalStock = Number(
                        inventory?.totalStock ??
                        product.initialStock ??
                        0
                    )

                    return {
                        id: product.id,
                        name: product.title,
                        category: product.category || 'Flash Sale',

                        price: Number(
                            product.flashSalePrice ??
                            product.originalPrice ??
                            0
                        ),

                        original: Number(
                            product.originalPrice ??
                            product.flashSalePrice ??
                            0
                        ),

                        stock: availableStock,
                        totalStock,

                        sold: Math.max(
                            0,
                            totalStock - availableStock
                        ),

                        color: colors[index % colors.length],

                        description: product.description || '',
                        imageUrl: product.imageUrl || '',
                        startTime: product.startTime,
                        endTime: product.endTime,
                        status: product.status,
                        saleActive: product.saleActive,

                        specs: [
                            product.saleActive
                                ? 'Flash sale is currently active'
                                : 'Flash sale product',

                            `Available stock: ${availableStock}`,

                            product.status || 'AVAILABLE',
                        ],

                        variants: {
                            Color: ['Default'],
                            Size: ['Standard'],
                        },
                    }
                })
            )

            setProducts(mappedProducts)

        } catch (err) {
            console.error('Products API error:', err)

            setError(
                err.response?.data?.message ||
                'Unable to load products from the backend.'
            )
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        setLoading(true)
        fetchProducts()

        const interval = setInterval(() => {
            fetchProducts()
        }, 5000)

        return () => clearInterval(interval)
    }, [])

    const cats = [
        'All',
        ...new Set(products.map((p) => p.category))
    ]

    const filtered = products.filter(
        (p) =>
            p.name
                .toLowerCase()
                .includes(query.toLowerCase()) &&
            (category === 'All' ||
                p.category === category)
    )

    return (
        <div className="space-y-6">

            <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">

                <div>
                    <p className="text-sm font-medium text-amber-300">
                        Live catalog
                    </p>

                    <h1 className="mt-1 text-3xl font-semibold text-white">
                        Products
                    </h1>

                    <p className="mt-1 text-sm text-slate-500">
                        Products and live inventory from the backend
                    </p>
                </div>

                <div className="relative">

                    <Search
                        size={16}
                        className="absolute left-3 top-3 text-slate-500"
                    />

                    <input
                        value={query}
                        onChange={(e) =>
                            setQuery(e.target.value)
                        }
                        placeholder="Search products"
                        className="w-full rounded-xl border border-slate-700 bg-slate-900 py-2.5 pl-9 pr-4 text-sm text-white outline-none focus:border-amber-400 md:w-64"
                    />

                </div>
            </div>

            {loading && (
                <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-8 text-center text-slate-400">
                    Loading products and live inventory...
                </div>
            )}

            {error && !loading && (
                <div className="rounded-2xl border border-rose-400/20 bg-rose-400/5 p-6">

                    <p className="font-medium text-rose-300">
                        Failed to load products
                    </p>

                    <p className="mt-2 text-sm text-slate-400">
                        {error}
                    </p>

                </div>
            )}

            {!loading && !error && products.length === 0 && (
                <div className="rounded-2xl border border-dashed border-slate-700 p-12 text-center">

                    <Package
                        className="mx-auto text-slate-600"
                        size={42}
                    />

                    <p className="mt-4 text-slate-400">
                        No products found.
                    </p>

                </div>
            )}

            {!loading && products.length > 0 && (
                <>

                    <div className="flex flex-wrap gap-2">

                        {cats.map((cat) => (
                            <button
                                key={cat}
                                onClick={() => setCategory(cat)}
                                className={`rounded-full px-4 py-2 text-xs font-medium transition ${
                                    category === cat
                                        ? 'bg-amber-400 text-slate-950'
                                        : 'border border-slate-700 text-slate-400 hover:border-slate-500'
                                }`}
                            >
                                {cat}
                            </button>
                        ))}

                    </div>

                    {filtered.length === 0 ? (
                        <div className="rounded-2xl border border-dashed border-slate-700 p-12 text-center">

                            <Search
                                className="mx-auto text-slate-600"
                                size={38}
                            />

                            <p className="mt-4 text-slate-400">
                                No matching products.
                            </p>

                        </div>
                    ) : (
                        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">

                            {filtered.map((product) => (
                                <ProductCard
                                    key={product.id}
                                    product={product}
                                    onAdd={onAdd}
                                    onOpen={onOpen}
                                />
                            ))}

                        </div>
                    )}

                </>
            )}

        </div>
    )
}
function FlashSale({ onAdd, onOpen }) {
    const [products, setProducts] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')

    const colors = [
        'from-amber-400 to-orange-600',
        'from-cyan-400 to-blue-600',
        'from-fuchsia-400 to-violet-600',
        'from-emerald-400 to-teal-600',
        'from-rose-400 to-red-600',
        'from-yellow-300 to-amber-600',
    ]

    const fetchFlashSaleProducts = async () => {
        try {
            setError('')

            const response = await api.get('/products/flash-sales')

            const backendProducts =
                response.data?.data?.content || []

            const mappedProducts = await Promise.all(
                backendProducts.map(async (product, index) => {
                    let inventory = null

                    try {
                        const inventoryResponse = await api.get(
                            `/inventory/${product.id}`
                        )

                        inventory =
                            inventoryResponse.data?.data ||
                            inventoryResponse.data
                    } catch (inventoryError) {
                        console.error(
                            `Inventory error for product ${product.id}:`,
                            inventoryError
                        )
                    }

                    const availableStock = Number(
                        inventory?.availableStock ??
                        product.initialStock ??
                        0
                    )

                    return {
                        id: product.id,
                        name: product.title,
                        category: product.category || 'Flash Sale',
                        price: Number(
                            product.flashSalePrice ??
                            product.originalPrice ??
                            0
                        ),
                        original: Number(
                            product.originalPrice ??
                            product.flashSalePrice ??
                            0
                        ),
                        stock: availableStock,
                        color: colors[index % colors.length],
                        description: product.description || '',
                        imageUrl: product.imageUrl || '',
                        startTime: product.startTime,
                        endTime: product.endTime,
                        status: product.status,
                        saleActive: product.saleActive,
                        variants: {
                            Color: ['Default'],
                            Size: ['Standard'],
                        },
                    }
                })
            )

            setProducts(mappedProducts)
        } catch (err) {
            console.error('Flash sale API error:', err)

            setError(
                err.response?.data?.message ||
                'Unable to load flash sale products.'
            )
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        setLoading(true)
        fetchFlashSaleProducts()

        const interval = setInterval(() => {
            fetchFlashSaleProducts()
        }, 5000)

        return () => clearInterval(interval)
    }, [])

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <div>
                    <p className="text-sm font-medium text-amber-300">
                        Live event
                    </p>

                    <h1 className="mt-1 text-3xl font-semibold text-white">
                        Flash Sale
                    </h1>

                    <p className="mt-1 text-sm text-slate-500">
                        Live products and inventory from the backend
                    </p>
                </div>

                <span className="flex items-center gap-2 rounded-full bg-rose-400/10 px-3 py-2 text-xs font-semibold text-rose-300">
                    <span className="h-2 w-2 animate-pulse rounded-full bg-rose-400" />
                    LIVE
                </span>
            </div>

            <div className="rounded-3xl border border-amber-400/25 bg-amber-400/10 p-6 md:p-8">
                <p className="text-sm text-amber-200/70">
                    Live flash sale
                </p>

                <h2 className="mt-2 text-3xl font-semibold text-white">
                    Limited-time deals
                </h2>

                <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-400">
                    Products added by the owner appear here automatically.
                    Inventory is refreshed every 5 seconds.
                </p>
            </div>

            {loading && (
                <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-8 text-center text-slate-400">
                    Loading live flash sale...
                </div>
            )}

            {error && !loading && (
                <div className="rounded-2xl border border-rose-400/20 bg-rose-400/5 p-6">
                    <p className="font-medium text-rose-300">
                        Failed to load flash sale
                    </p>

                    <p className="mt-2 text-sm text-slate-400">
                        {error}
                    </p>
                </div>
            )}

            {!loading && !error && products.length === 0 && (
                <div className="rounded-2xl border border-dashed border-slate-700 p-12 text-center">
                    <Zap
                        className="mx-auto text-slate-600"
                        size={42}
                    />

                    <p className="mt-4 text-slate-400">
                        No active flash sale products.
                    </p>
                </div>
            )}

            {!loading && products.length > 0 && (
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                    {products.map((product) => (
                        <ProductCard
                            key={product.id}
                            product={product}
                            onAdd={onAdd}
                            onOpen={onOpen}
                        />
                    ))}
                </div>
            )}
        </div>
    )
}
function ProductDetail({ product, onBack, onAdd, notify }) {
    const [image, setImage] = useState(0)
    const [qty, setQty] = useState(1)

    const [inventory, setInventory] = useState(null)
    const [inventoryLoading, setInventoryLoading] = useState(true)
    const [inventoryError, setInventoryError] = useState('')

    const [selected, setSelected] = useState({
        Color: product.variants?.Color?.[0] || 'Default',
        Size: product.variants?.Size?.[0] || 'Standard'
    })

    useEffect(() => {
        const fetchInventory = async () => {
            try {
                setInventoryLoading(true)
                setInventoryError('')

                console.log(
                    `Fetching inventory for product ID: ${product.id}`
                )

                const response = await api.get(
                    `/inventory/${product.id}`
                )

                console.log(
                    'Inventory API response:',
                    response.data
                )

                const inventoryData =
                    response.data?.data || response.data

                setInventory(inventoryData)
            } catch (error) {
                console.error(
                    'Inventory API error:',
                    error
                )

                if (error.response?.status === 401) {
                    setInventoryError(
                        'Authentication required. Please login again.'
                    )
                } else {
                    setInventoryError(
                        error.response?.data?.message ||
                        'Unable to load inventory.'
                    )
                }
            } finally {
                setInventoryLoading(false)
            }
        }

        if (product?.id) {
            fetchInventory()
        }

        const interval = setInterval(() => {
            if (product?.id) {
                fetchInventory()
            }
        }, 5000)

        return () => clearInterval(interval)
    }, [product?.id])

    const availableStock =
        inventory?.availableStock ??
        product.stock ??
        0

    const isSoldOut =
        product.status === 'DISABLED' ||
        availableStock <= 0

    const lockedStock =
        inventory?.lockedStock ?? 0

    const totalStock =
        inventory?.totalStock ??
        product.stock ??
        0

    const discount =
        product.original > 0
            ? Math.round(
                (1 - product.price / product.original) * 100
            )
            : 0

    return (
        <div className="space-y-6">

            <button
                onClick={onBack}
                className="flex items-center gap-2 text-sm text-slate-400 hover:text-white"
            >
                <ChevronLeft size={17} />
                Back to catalog
            </button>

            <div className="grid gap-8 lg:grid-cols-2">

                <div>

                    <div
                        className={`flex h-96 items-center justify-center rounded-3xl bg-gradient-to-br ${product.color}`}
                    >
                        <Box
                            size={150}
                            className="text-white/80"
                            strokeWidth={1}
                        />
                    </div>

                    <div className="mt-3 grid grid-cols-3 gap-3">
                        {[0, 1, 2].map((i) => (
                            <button
                                key={i}
                                onClick={() => setImage(i)}
                                className={`h-20 rounded-xl bg-gradient-to-br ${
                                    product.color
                                } ${
                                    image === i
                                        ? 'ring-2 ring-amber-400'
                                        : 'opacity-60'
                                }`}
                            >
                                <Box
                                    size={24}
                                    className="mx-auto text-white"
                                />
                            </button>
                        ))}
                    </div>

                </div>

                <div>

                    <p className="text-sm text-amber-300">
                        {product.category} / Limited drop
                    </p>

                    <h1 className="mt-2 text-4xl font-semibold text-white">
                        {product.name}
                    </h1>

                    <div className="mt-4 flex flex-wrap items-center gap-3">

                        <span className="text-3xl font-semibold text-amber-300">
                            ₹{Number(product.price).toLocaleString('en-IN')}
                        </span>

                        {product.original > product.price && (
                            <>
                                <span className="text-slate-600 line-through">
                                    ₹{Number(product.original).toLocaleString('en-IN')}
                                </span>

                                <span className="rounded-full bg-emerald-400/10 px-2 py-1 text-xs text-emerald-300">
                                    {discount}% OFF
                                </span>
                            </>
                        )}

                        <span
                            className={`rounded-full px-2 py-1 text-xs ${
                                isSoldOut
                                    ? 'bg-rose-400/10 text-rose-300'
                                    : 'bg-emerald-400/10 text-emerald-300'
                            }`}
                        >
                            {inventoryLoading
                                ? 'Checking stock...'
                                : isSoldOut
                                    ? 'Sold Out'
                                    : `${availableStock} available`}
                        </span>

                    </div>

                    {product.description && (
                        <p className="mt-5 leading-7 text-slate-400">
                            {product.description}
                        </p>
                    )}

                    <div className="mt-6 rounded-2xl border border-slate-800 bg-slate-900/70 p-5">

                        <div className="flex items-center justify-between">

                            <div>
                                <p className="text-sm font-medium text-white">
                                    Live inventory
                                </p>

                                <p className="mt-1 text-xs text-slate-500">
                                    Stock information from Inventory Service
                                </p>
                            </div>

                            {inventoryLoading ? (
                                <div className="h-5 w-5 animate-spin rounded-full border-2 border-slate-700 border-t-amber-400" />
                            ) : (
                                <span
                                    className={`flex items-center gap-2 text-xs ${
                                        isSoldOut
                                            ? 'text-rose-300'
                                            : 'text-emerald-300'
                                    }`}
                                >
                                    <span
                                        className={`h-2 w-2 rounded-full ${
                                            isSoldOut
                                                ? 'bg-rose-400'
                                                : 'bg-emerald-400'
                                        }`}
                                    />
                                    {isSoldOut ? 'Unavailable' : 'Live'}
                                </span>
                            )}

                        </div>

                        {inventoryError ? (
                            <div className="mt-4 rounded-xl border border-rose-400/20 bg-rose-400/5 p-3">
                                <p className="text-sm text-rose-300">
                                    {inventoryError}
                                </p>
                            </div>
                        ) : (
                            <div className="mt-5 grid grid-cols-3 gap-3">

                                <div className="rounded-xl bg-slate-950 p-3">
                                    <p className="text-xs text-slate-500">
                                        Available
                                    </p>

                                    <p className="mt-1 text-lg font-semibold text-emerald-300">
                                        {inventoryLoading
                                            ? '...'
                                            : availableStock}
                                    </p>
                                </div>

                                <div className="rounded-xl bg-slate-950 p-3">
                                    <p className="text-xs text-slate-500">
                                        Locked
                                    </p>

                                    <p className="mt-1 text-lg font-semibold text-amber-300">
                                        {inventoryLoading
                                            ? '...'
                                            : lockedStock}
                                    </p>
                                </div>

                                <div className="rounded-xl bg-slate-950 p-3">
                                    <p className="text-xs text-slate-500">
                                        Total
                                    </p>

                                    <p className="mt-1 text-lg font-semibold text-white">
                                        {inventoryLoading
                                            ? '...'
                                            : totalStock}
                                    </p>
                                </div>

                            </div>
                        )}

                    </div>

                    {isSoldOut && !inventoryLoading && (
                        <div className="mt-4 rounded-2xl border border-rose-400/20 bg-rose-400/5 p-4">
                            <p className="font-medium text-rose-300">
                                This product is currently sold out.
                            </p>

                            <p className="mt-1 text-sm text-slate-400">
                                It is not available for purchase right now.
                            </p>
                        </div>
                    )}

                    <div className="mt-4 rounded-2xl border border-amber-400/25 bg-amber-400/10 p-4">

                        <div className="flex items-center gap-2 text-sm text-amber-200">
                            <Clock3 size={16} />

                            {product.saleActive
                                ? 'Flash sale is currently active'
                                : 'Flash sale product'}
                        </div>

                        {product.endTime && (
                            <p className="mt-2 text-sm text-slate-400">
                                Sale ends:{' '}
                                {new Date(
                                    product.endTime
                                ).toLocaleString()}
                            </p>
                        )}

                    </div>

                    <div className="mt-7 space-y-5">

                        {Object.entries(
                            product.variants || {}
                        ).map(([key, values]) => (

                            <div key={key}>

                                <p className="mb-2 text-sm font-medium text-white">
                                    {key}
                                </p>

                                <div className="flex flex-wrap gap-2">

                                    {values.map((value) => (
                                        <button
                                            key={value}
                                            onClick={() =>
                                                setSelected({
                                                    ...selected,
                                                    [key]: value
                                                })
                                            }
                                            disabled={isSoldOut}
                                            className={`rounded-lg border px-3 py-2 text-sm disabled:cursor-not-allowed disabled:opacity-50 ${
                                                selected[key] === value
                                                    ? 'border-amber-400 bg-amber-400 text-slate-950'
                                                    : 'border-slate-700 text-slate-400'
                                            }`}
                                        >
                                            {value}
                                        </button>
                                    ))}

                                </div>

                            </div>

                        ))}

                    </div>

                    <div className="mt-7 flex gap-3">

                        <div className="flex items-center rounded-xl border border-slate-700">

                            <button
                                onClick={() =>
                                    setQty(Math.max(1, qty - 1))
                                }
                                disabled={isSoldOut}
                                className="px-4 py-3 text-white disabled:cursor-not-allowed disabled:opacity-40"
                            >
                                −
                            </button>

                            <span className="w-8 text-center text-white">
                                {qty}
                            </span>

                            <button
                                onClick={() =>
                                    setQty(
                                        Math.min(
                                            Math.max(availableStock, 1),
                                            qty + 1
                                        )
                                    )
                                }
                                disabled={isSoldOut}
                                className="px-4 py-3 text-white disabled:cursor-not-allowed disabled:opacity-40"
                            >
                                +
                            </button>

                        </div>

                        <button
                            disabled={
                                inventoryLoading ||
                                isSoldOut
                            }
                            onClick={() => {

                                if (isSoldOut) {
                                    notify(
                                        `${product.name} is sold out`
                                    )
                                    return
                                }

                                onAdd({
                                    ...product,
                                    stock: availableStock,
                                    selected,
                                    qty
                                })

                                notify(
                                    `${product.name} added to cart`
                                )
                            }}
                            className="flex-1 rounded-xl bg-amber-400 px-5 py-3 font-semibold text-slate-950 disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            {inventoryLoading
                                ? 'Checking stock...'
                                : isSoldOut
                                    ? 'Sold Out'
                                    : `Add to cart · ₹${(
                                        product.price * qty
                                    ).toLocaleString('en-IN')}`}
                        </button>

                    </div>

                </div>
            </div>

            <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">

                <h2 className="font-medium text-white">
                    Product information
                </h2>

                <div className="mt-4 grid gap-3 sm:grid-cols-3">

                    {(product.specs || []).map((spec) => (
                        <div
                            key={spec}
                            className="rounded-xl bg-slate-950 p-4 text-sm text-slate-400"
                        >
                            <Check
                                className="mb-2 text-emerald-400"
                                size={16}
                            />

                            {spec}
                        </div>
                    ))}

                </div>

            </div>

        </div>
    )
}
function Checkout({ cart, setCart, setView, notify }) {
    const [step, setStep] = useState(0)

    const [shipping, setShipping] = useState({
        name: '',
        address: '',
        city: '',
        zip: ''
    })

    const [payment, setPayment] = useState('card')

    const [placingOrder, setPlacingOrder] = useState(false)
    const [order, setOrder] = useState(null)
    const [paymentResult, setPaymentResult] = useState(null)

    const total = cart.reduce(
        (sum, item) => sum + item.price * item.qty,
        0
    )

    const handlePlaceOrder = async () => {
        if (cart.length !== 1) {
            notify('Please checkout one product at a time.')
            return
        }

        const item = cart[0]

        if (!shipping.name || !shipping.address || !shipping.city || !shipping.zip) {
            notify('Please complete your shipping address.')
            setStep(1)
            return
        }

        try {
            setPlacingOrder(true)

            const orderIdempotencyKey = crypto.randomUUID()

            notify('Creating your order...')

            const inventoryResponse = await api.get(`/inventory/${item.id}`)
            const inventory = inventoryResponse.data?.data || inventoryResponse.data

            const availableStock = Number(inventory?.availableStock ?? 0)

            if (item.status === 'DISABLED') {
                notify('This product is currently unavailable.')
                return
            }

            if (availableStock < item.qty) {
                notify(
                    availableStock > 0
                        ? `Only ${availableStock} item${availableStock === 1 ? '' : 's'} available.`
                        : 'This product is out of stock.'
                )
                return
            }

            const orderResponse = await api.post('/orders', {
                productId: item.id,
                quantity: item.qty,
                idempotencyKey: orderIdempotencyKey
            })

            const createdOrder = orderResponse.data?.data

            if (!createdOrder?.orderReference) {
                throw new Error('Order was created but order details were not returned.')
            }

            setOrder(createdOrder)

            const paymentIdempotencyKey = crypto.randomUUID()

            notify('Processing payment...')

            const paymentResponse = await api.post('/payments', {
                orderReference: createdOrder.orderReference,
                amount: createdOrder.totalAmount,
                paymentMethod: payment,
                idempotencyKey: paymentIdempotencyKey
            })

            const createdPayment = paymentResponse.data?.data

            if (!createdPayment) {
                throw new Error('Payment response was not returned.')
            }

            setPaymentResult(createdPayment)

            if (createdPayment.status !== 'SUCCESS') {
                notify('Payment failed. Please try again.')
                return
            }

            notify('Payment authorized successfully')

            setStep(3)

        } catch (error) {
            console.error('Checkout error:', error)

            if (error.response?.status === 401) {
                notify('Authentication required. Please login again.')
            } else if (error.response?.status === 400) {
                notify(
                    error.response?.data?.message ||
                    'Invalid checkout request.'
                )
            } else if (error.response?.status === 409) {
                notify(
                    error.response?.data?.message ||
                    'The product is no longer available.'
                )
            } else {
                notify(
                    error.response?.data?.message ||
                    error.message ||
                    'Something went wrong while placing the order.'
                )
            }
        } finally {
            setPlacingOrder(false)
        }
    }

    if (cart.length === 0) {
        return (
            <div className="space-y-6">
                <h1 className="text-3xl font-semibold text-white">
                    Cart & checkout
                </h1>

                <div className="rounded-2xl border border-dashed border-slate-700 p-12 text-center">
                    <ShoppingCart
                        className="mx-auto text-slate-600"
                        size={42}
                    />

                    <p className="mt-4 text-slate-400">
                        Your cart is waiting for a good deal.
                    </p>

                    <button
                        onClick={() => setView('products')}
                        className="mt-5 rounded-xl bg-amber-400 px-5 py-3 text-sm font-semibold text-slate-950"
                    >
                        Shop products
                    </button>
                </div>
            </div>
        )
    }

    if (step === 3) {
        return (
            <div className="mx-auto max-w-xl rounded-3xl border border-emerald-400/20 bg-emerald-400/5 p-10 text-center">

                <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-emerald-400 text-slate-950">
                    <Check size={32} />
                </div>

                <h1 className="mt-6 text-3xl font-semibold text-white">
                    Order confirmed
                </h1>

                <p className="mt-3 leading-7 text-slate-400">
                    Your order has been placed and your payment was successful. Order status will update automatically.
                </p>

                {order && (
                    <div className="mt-6 rounded-2xl border border-slate-800 bg-slate-900/70 p-5 text-left">
                        <div className="flex justify-between gap-4">
              <span className="text-sm text-slate-500">
                Order
              </span>

                            <span className="text-sm font-medium text-white">
                {order.orderReference}
              </span>
                        </div>

                        <div className="mt-3 flex justify-between gap-4">
              <span className="text-sm text-slate-500">
                Amount
              </span>

                            <span className="text-sm font-medium text-amber-300">
                ₹{Number(order.totalAmount).toLocaleString('en-IN')}
              </span>
                        </div>

                        {paymentResult?.transactionId && (
                            <div className="mt-3 flex justify-between gap-4">
                <span className="text-sm text-slate-500">
                  Transaction
                </span>

                                <span className="text-sm font-medium text-white">
                  {paymentResult.transactionId}
                </span>
                            </div>
                        )}

                        <div className="mt-3 flex justify-between gap-4">
              <span className="text-sm text-slate-500">
                Payment
              </span>

                            <span className="text-sm font-medium text-emerald-300">
                {paymentResult?.status || 'SUCCESS'}
              </span>
                        </div>
                    </div>
                )}

                <button
                    onClick={() => {
                        setCart([])
                        setView('orders')
                    }}
                    className="mt-7 rounded-xl bg-amber-400 px-5 py-3 font-semibold text-slate-950"
                >
                    Track order
                </button>
            </div>
        )
    }

    return (
        <div className="space-y-6">

            <div>
                <p className="text-sm font-medium text-amber-300">
                    Secure checkout
                </p>

                <h1 className="mt-1 text-3xl font-semibold text-white">
                    Cart & checkout
                </h1>
            </div>

            <div className="flex gap-2">
                {['Cart', 'Shipping', 'Payment', 'Complete'].map(
                    (label, i) => (
                        <div
                            key={label}
                            className={`flex-1 border-b-2 pb-3 text-xs ${
                                i <= step
                                    ? 'border-amber-400 text-amber-300'
                                    : 'border-slate-800 text-slate-600'
                            }`}
                        >
                            {i + 1}. {label}
                        </div>
                    )
                )}
            </div>

            {step === 0 && (
                <div className="grid gap-6 lg:grid-cols-[1.4fr_0.8fr]">

                    <div className="space-y-3">

                        {cart.map((item) => (
                            <div
                                key={item.id}
                                className="flex items-center gap-4 rounded-2xl border border-slate-800 bg-slate-900/70 p-4"
                            >

                                <div
                                    className={`flex h-16 w-16 items-center justify-center rounded-xl bg-gradient-to-br ${item.color}`}
                                >
                                    <Box
                                        size={26}
                                        className="text-white"
                                    />
                                </div>

                                <div className="flex-1">

                                    <p className="font-medium text-white">
                                        {item.name}
                                    </p>

                                    <p className="text-sm text-amber-300">
                                        ₹{Number(item.price).toLocaleString('en-IN')} · Qty {item.qty}
                                    </p>

                                </div>

                                <button
                                    onClick={() =>
                                        setCart(
                                            cart.filter(
                                                (x) => x.id !== item.id
                                            )
                                        )
                                    }
                                    className="text-slate-500 hover:text-rose-300"
                                >
                                    <Trash2 size={17} />
                                </button>

                            </div>
                        ))}

                    </div>

                    <Summary
                        total={total}
                        action="Continue to shipping"
                        onAction={() => setStep(1)}
                    />

                </div>
            )}

            {step === 1 && (
                <div className="max-w-2xl rounded-2xl border border-slate-800 bg-slate-900/70 p-6">

                    <h2 className="font-medium text-white">
                        Shipping address
                    </h2>

                    <div className="mt-5 grid gap-4 sm:grid-cols-2">

                        {[
                            ['name', 'Full name'],
                            ['address', 'Street address'],
                            ['city', 'City'],
                            ['zip', 'ZIP code']
                        ].map(([key, label]) => (
                            <input
                                key={key}
                                value={shipping[key]}
                                onChange={(e) =>
                                    setShipping({
                                        ...shipping,
                                        [key]: e.target.value
                                    })
                                }
                                placeholder={label}
                                className="rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-sm text-white outline-none focus:border-amber-400"
                            />
                        ))}

                    </div>

                    <button
                        onClick={() => {
                            if (
                                shipping.name &&
                                shipping.address &&
                                shipping.city &&
                                shipping.zip
                            ) {
                                setStep(2)
                            } else {
                                notify('Please complete your shipping address.')
                            }
                        }}
                        className="mt-6 rounded-xl bg-amber-400 px-5 py-3 font-semibold text-slate-950"
                    >
                        Continue to payment
                    </button>

                </div>
            )}

            {step === 2 && (
                <div className="max-w-2xl rounded-2xl border border-slate-800 bg-slate-900/70 p-6">

                    <h2 className="font-medium text-white">
                        Payment method
                    </h2>

                    <div className="mt-5 grid gap-3 sm:grid-cols-2">

                        <button
                            onClick={() => setPayment('card')}
                            disabled={placingOrder}
                            className={`rounded-xl border p-4 text-left ${
                                payment === 'card'
                                    ? 'border-amber-400 bg-amber-400/10'
                                    : 'border-slate-700'
                            }`}
                        >

                            <CreditCard
                                className="text-amber-300"
                                size={20}
                            />

                            <p className="mt-2 text-sm text-white">
                                Card ending in 4242
                            </p>

                            <p className="text-xs text-slate-500">
                                Visa · Default
                            </p>

                        </button>

                        <button
                            onClick={() => setPayment('wallet')}
                            disabled={placingOrder}
                            className={`rounded-xl border p-4 text-left ${
                                payment === 'wallet'
                                    ? 'border-amber-400 bg-amber-400/10'
                                    : 'border-slate-700'
                            }`}
                        >

                            <CircleDollarSign
                                className="text-emerald-300"
                                size={20}
                            />

                            <p className="mt-2 text-sm text-white">
                                Store wallet
                            </p>

                            <p className="text-xs text-slate-500">
                                ₹120 available
                            </p>

                        </button>

                    </div>

                    <button
                        onClick={handlePlaceOrder}
                        disabled={placingOrder}
                        className="mt-6 w-full rounded-xl bg-amber-400 px-5 py-3 font-semibold text-slate-950 disabled:cursor-not-allowed disabled:opacity-60"
                    >
                        {placingOrder
                            ? 'Processing...'
                            : `Place order · ₹${total.toLocaleString('en-IN')}`
                        }
                    </button>

                </div>
            )}

        </div>
    )
}


function Summary({ total, action, onAction }) {
    return (
        <div className="h-fit rounded-2xl border border-slate-800 bg-slate-900/70 p-6">
            <p className="font-medium text-white">Order summary</p>

            <div className="mt-5 space-y-3 text-sm">
                <div className="flex justify-between text-slate-400">
                    <span>Subtotal</span>
                    <span>₹{total.toLocaleString('en-IN')}</span>
                </div>

                <div className="flex justify-between text-slate-400">
                    <span>Shipping</span>
                    <span className="text-emerald-400">Free</span>
                </div>

                <div className="flex justify-between border-t border-slate-800 pt-3 text-lg font-semibold text-white">
                    <span>Total</span>
                    <span>₹{total.toLocaleString('en-IN')}</span>
                </div>
            </div>

            <button
                onClick={onAction}
                className="mt-6 w-full rounded-xl bg-amber-400 py-3 font-semibold text-slate-950"
            >
                {action}
            </button>
        </div>
    )
}

function Orders({ notify }) {
    const [orders, setOrders] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const [productsMap, setProductsMap] = useState({})
    const [selectedOrder, setSelectedOrder] = useState(null)
    const [cancelling, setCancelling] = useState(false)

    useEffect(() => {
        const fetchOrders = async () => {
            try {
                setLoading(true)
                setError('')

                const [ordersResponse, productsResponse] = await Promise.all([
                    api.get('/orders/my-orders', {
                        params: {page: 0, size: 10, sort: 'createdAt,desc'}
                    }),
                    api.get('/products', {
                        params: {page: 0, size: 100}
                    })
                ])

                const orderData = ordersResponse.data?.data
                const orderItems = orderData?.content || []
                const productItems = productsResponse.data?.data?.content || []

                const productLookup = productItems.reduce((map, product) => {
                    map[product.id] = product.title
                    return map
                }, {})

                setProductsMap(productLookup)
                setOrders(orderItems)

                setSelectedOrder((currentSelected) => {
                    if (!currentSelected) {
                        return orderItems[0] || null
                    }

                    return (
                        orderItems.find(
                            (order) =>
                                order.orderReference ===
                                currentSelected.orderReference
                        ) || orderItems[0] || null
                    )
                })
            } catch (err) {
                console.error('Orders API error:', err)

                if (err.response?.status === 401) {
                    setError('Authentication required. Please login again.')
                } else {
                    setError(
                        err.response?.data?.message ||
                        'Unable to load your orders from the backend.'
                    )
                }
            } finally {
                setLoading(false)
            }
        }

        fetchOrders()

        const interval = setInterval(() => {
            fetchOrders()
        }, 5000)

        return () => clearInterval(interval)
    }, [])

    const handleCancel = async (orderReference) => {
        try {
            setCancelling(true)

            const response = await api.post(`/orders/${orderReference}/cancel`)
            const updatedOrder = response.data?.data

            setOrders((current) =>
                current.map((order) =>
                    order.orderReference === orderReference ? updatedOrder : order
                )
            )
            setSelectedOrder((current) =>
                current?.orderReference === orderReference ? updatedOrder : current
            )

            notify('Order cancelled successfully')
        } catch (err) {
            console.error('Cancel order error:', err)
            notify(
                err.response?.data?.message ||
                'Unable to cancel this order.'
            )
        } finally {
            setCancelling(false)
        }
    }

    const getStatusClass = (status) => {
        switch (status) {
            case 'PAID':
                return 'text-emerald-300'
            case 'PENDING_PAYMENT':
                return 'text-amber-300'
            case 'CANCELLED':
            case 'EXPIRED':
                return 'text-rose-300'
            default:
                return 'text-cyan-300'
        }
    }

    const formatStatus = (status) =>
        (status || 'UNKNOWN')
            .toLowerCase()
            .split('_')
            .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
            .join(' ')

    const formatDate = (value) => {
        if (!value) return '—'
        const date = new Date(value)
        return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString()
    }

    const timeline = [
        {
            label: 'Order placed',
            active: true,
        },
        {
            label: 'Payment pending',
            active:
                selectedOrder?.status === 'PENDING_PAYMENT',
        },
        {
            label: 'Paid',
            active:
                selectedOrder?.status === 'PAID',
        },
        {
            label: 'Cancelled',
            active:
                selectedOrder?.status === 'CANCELLED',
        },
        {
            label: 'Expired',
            active:
                selectedOrder?.status === 'EXPIRED',
        },
    ]

    return (
        <div className="space-y-6">
            <div>
                <p className="text-sm font-medium text-amber-300">
                    Your purchase history
                </p>

                <h1 className="mt-1 text-3xl font-semibold text-white">
                    Orders
                </h1>

                <p className="mt-1 text-sm text-slate-500">
                    Orders loaded from the Order Service
                </p>
            </div>

            {loading && (
                <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-8 text-center text-slate-400">
                    Loading your orders...
                </div>
            )}

            {error && !loading && (
                <div className="rounded-2xl border border-rose-400/20 bg-rose-400/5 p-6">
                    <p className="font-medium text-rose-300">Failed to load orders</p>
                    <p className="mt-2 text-sm text-slate-400">{error}</p>
                </div>
            )}

            {!loading && !error && orders.length === 0 && (
                <div className="rounded-2xl border border-dashed border-slate-700 p-12 text-center">
                    <ShoppingBag className="mx-auto text-slate-600" size={42}/>
                    <p className="mt-4 text-slate-400">You have no orders yet.</p>
                    <p className="mt-1 text-sm text-slate-600">
                        Complete a checkout to see your order here.
                    </p>
                </div>
            )}

            {!loading && !error && orders.length > 0 && (
                <>
                    <div className="overflow-hidden rounded-2xl border border-slate-800 bg-slate-900/70">
                        {orders.map((order) => (
                            <button
                                key={order.orderReference}
                                type="button"
                                onClick={() => setSelectedOrder(order)}
                                className={`grid w-full gap-3 border-b border-slate-800 px-5 py-5 text-left transition last:border-0 md:grid-cols-6 md:items-center ${
                                    selectedOrder?.orderReference === order.orderReference
                                        ? 'bg-slate-800/60'
                                        : 'hover:bg-slate-800/30'
                                }`}
                            >
                                <div>
                                    <span className="text-xs text-slate-500">Order</span>
                                    <p className="font-medium text-white">
                                        {order.orderReference}
                                    </p>
                                </div>

                                <div>
                                    <span className="text-xs text-slate-500">Product</span>
                                    <p className="text-sm text-slate-300">
                                        {productsMap[order.productId] || `Product #${order.productId}`}
                                    </p>
                                </div>

                                <div>
                                    <span className="text-xs text-slate-500">Qty</span>
                                    <p className="text-sm text-white">{order.quantity}</p>
                                </div>

                                <div>
                                    <span className="text-xs text-slate-500">Total</span>
                                    <p className="text-sm text-amber-300">
                                        ₹{Number(order.totalAmount || 0).toLocaleString('en-IN')}
                                    </p>
                                </div>

                                <div>
                                    <span className="text-xs text-slate-500">Status</span>
                                    <p className={`text-sm ${getStatusClass(order.status)}`}>
                                        {formatStatus(order.status)}
                                    </p>
                                </div>

                                <p className="text-xs text-slate-500">
                                    {formatDate(order.createdAt)}
                                </p>
                            </button>
                        ))}
                    </div>

                    {selectedOrder && (
                        <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">
                            <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                                <div>
                                    <p className="font-medium text-white">
                                        Order details · {selectedOrder.orderReference}
                                    </p>
                                    <p className="mt-1 text-sm text-slate-500">
                                        {productsMap[selectedOrder.productId] || `Product #${selectedOrder.productId}`}
                                    </p>
                                </div>

                                <div className="flex items-center gap-3">
                  <span className={`text-sm font-medium ${getStatusClass(selectedOrder.status)}`}>
                    {formatStatus(selectedOrder.status)}
                  </span>

                                    {selectedOrder.status === 'PENDING_PAYMENT' && (
                                        <button
                                            type="button"
                                            onClick={() => handleCancel(selectedOrder.orderReference)}
                                            disabled={cancelling}
                                            className="rounded-lg border border-rose-400/30 px-3 py-2 text-xs font-semibold text-rose-300 hover:bg-rose-400/10 disabled:cursor-not-allowed disabled:opacity-50"
                                        >
                                            {cancelling ? 'Cancelling...' : 'Cancel order'}
                                        </button>
                                    )}
                                </div>
                            </div>

                            <div className="mt-6 grid gap-3 sm:grid-cols-4">
                                <div className="rounded-xl bg-slate-950 p-4">
                                    <p className="text-xs text-slate-500">Quantity</p>
                                    <p className="mt-1 text-lg font-semibold text-white">
                                        {selectedOrder.quantity}
                                    </p>
                                </div>

                                <div className="rounded-xl bg-slate-950 p-4">
                                    <p className="text-xs text-slate-500">Unit price</p>
                                    <p className="mt-1 text-lg font-semibold text-white">
                                        ₹{Number(selectedOrder.unitPrice || 0).toLocaleString('en-IN')}
                                    </p>
                                </div>

                                <div className="rounded-xl bg-slate-950 p-4">
                                    <p className="text-xs text-slate-500">Total amount</p>
                                    <p className="mt-1 text-lg font-semibold text-amber-300">
                                        ₹{Number(selectedOrder.totalAmount || 0).toLocaleString('en-IN')}
                                    </p>
                                </div>

                                <div className="rounded-xl bg-slate-950 p-4">
                                    <p className="text-xs text-slate-500">Payment deadline</p>
                                    <p className="mt-1 text-sm font-medium text-white">
                                        {formatDate(selectedOrder.paymentDeadline)}
                                    </p>
                                </div>
                            </div>

                            <div className="mt-6">
                                <p className="font-medium text-white">Order timeline</p>

                                <div
                                    className="mt-5 flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
                                    {timeline.map(({ label, active }, index) => (
                                        <div
                                            key={label}
                                            className="flex items-center gap-3 md:flex-col md:gap-2"
                                        >
                      <span
                          className={`flex h-8 w-8 items-center justify-center rounded-full ${
                              active
                                  ? 'bg-emerald-400 text-slate-950'
                                  : 'bg-slate-800 text-slate-500'
                          }`}
                      >
                        {active ? <Check size={15}/> : index + 1}
                      </span>

                                            <span className="text-sm text-slate-400">
                        {label}
                      </span>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        </div>
                    )}
                </>
            )}
        </div>
    )


}
function Notifications({notify}) {
    const [notifications, setNotifications] = useState([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const [readIds, setReadIds] = useState(() => new Set())

    const fallbackNotifications = [
        {
            id: 'sale-live',
            title: 'Flash sale is live',
            body: 'Check the latest products and available stock.',
            time: 'Now',
            Icon: Zap,
            tone: 'amber'
        },
        {
            id: 'order-updates',
            title: 'Order tracking is live',
            body: 'Your order status updates automatically from the Order Service.',
            time: 'Now',
            Icon: Truck,
            tone: 'cyan'
        }
    ]

    const loadNotifications = async () => {
        try {
            setError('')

            const [ordersResponse, productsResponse] = await Promise.all([
                api.get('/orders/my-orders', {
                    params: {
                        page: 0,
                        size: 20,
                        sort: 'createdAt,desc'
                    }
                }),
                api.get('/products', {
                    params: {
                        page: 0,
                        size: 100
                    }
                })
            ])

            const orders =
                ordersResponse.data?.data?.content || []

            const products =
                productsResponse.data?.data?.content || []

            const productMap = products.reduce((map, product) => {
                map[product.id] = product.title
                return map
            }, {})

            const orderNotifications = orders.slice(0, 10).map((order) => {
                const productName =
                    productMap[order.productId] ||
                    `Product #${order.productId}`

                let title = 'Order updated'
                let body = `${productName} · Order ${order.orderReference}`
                let Icon = ShoppingBag
                let tone = 'cyan'

                if (order.status === 'PAID') {
                    title = 'Payment completed'
                    body = `${productName} · Order ${order.orderReference} is paid.`
                    Icon = Check
                    tone = 'emerald'
                } else if (order.status === 'PENDING_PAYMENT') {
                    title = 'Payment pending'
                    body = `${productName} · Complete payment before the deadline.`
                    Icon = Clock3
                    tone = 'amber'
                } else if (order.status === 'CANCELLED') {
                    title = 'Order cancelled'
                    body = `${productName} · Order ${order.orderReference} was cancelled.`
                    Icon = X
                    tone = 'amber'
                } else if (order.status === 'EXPIRED') {
                    title = 'Order expired'
                    body = `${productName} · The payment deadline has passed.`
                    Icon = Clock3
                    tone = 'amber'
                }

                return {
                    id: `order-${order.orderReference}`,
                    title,
                    body,
                    time: order.createdAt
                        ? new Date(order.createdAt).toLocaleString()
                        : 'Recent',
                    Icon,
                    tone
                }
            })

            const soldOutProducts = products
                .filter((product) => product.status === 'DISABLED')
                .slice(0, 10)
                .map((product) => ({
                    id: `sold-out-${product.id}`,
                    title: 'Product unavailable',
                    body: `${product.title} is currently marked as sold out.`,
                    time: 'Current',
                    Icon: X,
                    tone: 'amber'
                }))

            const nextNotifications = [
                ...orderNotifications,
                ...soldOutProducts
            ]

            setNotifications(
                nextNotifications.length > 0
                    ? nextNotifications
                    : fallbackNotifications
            )
        } catch (err) {
            console.error('Notifications API error:', err)

            setError(
                err.response?.data?.message ||
                'Unable to load live notifications.'
            )

            setNotifications(fallbackNotifications)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        loadNotifications()

        const interval = setInterval(() => {
            loadNotifications()
        }, 5000)

        return () => clearInterval(interval)
    }, [])

    const markAllRead = () => {
        setReadIds(new Set(notifications.map((item) => item.id)))
        notify('All notifications marked as read')
    }

    const markRead = (id) => {
        setReadIds((current) => {
            const next = new Set(current)
            next.add(id)
            return next
        })
    }

    const unreadCount = notifications.filter(
        (item) => !readIds.has(item.id)
    ).length

    return (
        <div className="space-y-6">
            <div className="flex items-end justify-between gap-4">
                <div>
                    <p className="text-sm font-medium text-amber-300">
                        Stay in the loop
                    </p>

                    <h1 className="mt-1 text-3xl font-semibold text-white">
                        Notifications
                    </h1>

                    <p className="mt-1 text-sm text-slate-500">
                        Live updates from your orders and products
                    </p>
                </div>

                <button
                    type="button"
                    onClick={markAllRead}
                    disabled={notifications.length === 0 || unreadCount === 0}
                    className="text-xs text-slate-400 hover:text-white disabled:cursor-not-allowed disabled:opacity-40"
                >
                    Mark all read
                </button>
            </div>

            {error && (
                <div className="rounded-xl border border-amber-400/20 bg-amber-400/5 p-4 text-sm text-amber-300">
                    {error}
                </div>
            )}

            {loading ? (
                <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-8 text-center text-slate-400">
                    Loading notifications...
                </div>
            ) : notifications.length === 0 ? (
                <div className="rounded-2xl border border-dashed border-slate-700 p-12 text-center">
                    <Bell
                        className="mx-auto text-slate-600"
                        size={42}
                    />

                    <p className="mt-4 text-slate-400">
                        No notifications yet.
                    </p>
                </div>
            ) : (
                <div className="space-y-3">
                    {notifications.map((item) => {
                        const isRead = readIds.has(item.id)
                        const Icon = item.Icon

                        return (
                            <button
                                key={item.id}
                                type="button"
                                onClick={() => markRead(item.id)}
                                className={`flex w-full gap-4 rounded-2xl border p-5 text-left transition ${
                                    !isRead
                                        ? 'border-amber-400/30 bg-amber-400/5'
                                        : 'border-slate-800 bg-slate-900/60 hover:bg-slate-900'
                                }`}
                            >
                                <span
                                    className={`rounded-xl p-3 ${
                                        item.tone === 'amber'
                                            ? 'bg-amber-400/10 text-amber-300'
                                            : item.tone === 'cyan'
                                                ? 'bg-cyan-400/10 text-cyan-300'
                                                : 'bg-emerald-400/10 text-emerald-300'
                                    }`}
                                >
                                    <Icon size={18}/>
                                </span>

                                <div className="flex-1">
                                    <div className="flex justify-between gap-4">
                                        <p className="font-medium text-white">
                                            {item.title}
                                        </p>

                                        <span className="shrink-0 text-xs text-slate-600">
                                            {item.time}
                                        </span>
                                    </div>

                                    <p className="mt-1 text-sm text-slate-400">
                                        {item.body}
                                    </p>
                                </div>

                                {!isRead && (
                                    <span className="mt-2 h-2 w-2 shrink-0 rounded-full bg-amber-400"/>
                                )}
                            </button>
                        )
                    })}
                </div>
            )}
        </div>
    )
}

function Analytics({range, setRange}) {
    const data =
        range === '7d'
            ? [42, 58, 48, 74, 66, 88, 82]
            : range === '30d'
                ? [32, 48, 42, 68, 54, 76, 64, 92, 72, 86]
                : [22, 38, 30, 52, 44, 66, 58, 80, 70, 96]

    return (
        <div className="space-y-6">
            <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
                <div>
                    <p className="text-sm font-medium text-amber-300">
                        Operations
                    </p>

                    <h1 className="mt-1 text-3xl font-semibold text-white">
                        Admin console
                    </h1>
                </div>

                <div className="flex rounded-xl border border-slate-800 bg-slate-900 p-1">
                    {[
                        ['7d', '7 days'],
                        ['30d', '30 days'],
                        ['90d', '90 days']
                    ].map(([value, label]) => (
                        <button
                            key={value}
                            onClick={() => setRange(value)}
                            className={`rounded-lg px-3 py-2 text-xs ${
                                range === value
                                    ? 'bg-amber-400 text-slate-950'
                                    : 'text-slate-400'
                            }`}
                        >
                            {label}
                        </button>
                    ))}
                </div>
            </div>

            <div className="grid gap-4 md:grid-cols-3">
                <Metric
                    icon={Activity}
                    label="API throughput"
                    value="18.4k req/s"
                    detail="Healthy · 99.99% uptime"
                    tone="cyan"
                />

                <Metric
                    icon={Users}
                    label="Concurrent users"
                    value="8,492"
                    detail="Peak: 12,104"
                    tone="emerald"
                />

                <Metric
                    icon={CircleDollarSign}
                    label="GMV in range"
                    value={
                        range === '7d'
                            ? '$84,290'
                            : range === '30d'
                                ? '$312,840'
                                : '$908,120'
                    }
                    detail="+31.2% vs. previous"
                />
            </div>

            <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">
                <div className="flex items-center justify-between">
                    <div>
                        <p className="font-medium text-white">Revenue trend</p>
                        <p className="mt-1 text-sm text-slate-500">
                            Gross merchandise value · {range}
                        </p>
                    </div>

                    <TrendingUp
                        className="text-emerald-400"
                        size={18}
                    />
                </div>

                <div className="mt-8 flex h-56 items-end gap-2 border-b border-slate-800">
                    {data.map((height, i) => (
                        <div
                            key={i}
                            className="group flex flex-1 flex-col justify-end"
                        >
                            <div
                                style={{height: `${height}%`}}
                                className="rounded-t-lg bg-gradient-to-t from-amber-500 to-amber-300 transition group-hover:from-cyan-400 group-hover:to-cyan-300"
                            />
                        </div>
                    ))}
                </div>

                <div className="mt-3 flex justify-between text-xs text-slate-600">
                    <span>Earlier</span>
                    <span>Today</span>
                </div>
            </div>

            <div className="grid gap-6 lg:grid-cols-2">
                <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">
                    <p className="font-medium text-white">Top products</p>

                    <div className="mt-5 space-y-4">
                        {products.slice(0, 4).map((p, i) => (
                            <div
                                key={p.id}
                                className="flex items-center gap-3"
                            >
                <span className="w-5 text-xs text-slate-600">
                  0{i + 1}
                </span>

                                <div
                                    className={`h-9 w-9 rounded-lg bg-gradient-to-br ${p.color}`}
                                />

                                <div className="flex-1">
                                    <div className="flex justify-between text-sm">
                                        <span className="text-white">{p.name}</span>
                                        <span className="text-slate-400">
                      {p.sold * 12} sold
                    </span>
                                    </div>

                                    <div className="mt-2 h-1.5 rounded-full bg-slate-800">
                                        <div
                                            style={{width: `${p.sold}%`}}
                                            className="h-full rounded-full bg-amber-400"
                                        />
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>

                <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">
                    <p className="font-medium text-white">System events</p>

                    <div className="mt-5 space-y-4 text-sm">
                        {[
                            'Stock sync completed across 24 warehouses',
                            'Rate limit increased for sale traffic',
                            'Payment processor latency normalized',
                            'New admin signed into console'
                        ].map((event, i) => (
                            <div key={event} className="flex gap-3">
                <span
                    className={`mt-1 h-2 w-2 rounded-full ${
                        i === 1
                            ? 'bg-amber-400'
                            : 'bg-emerald-400'
                    }`}
                />

                                <p className="text-slate-300">{event}</p>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    )
}

function AdminInventory({ notify }) {
    const [products, setProducts] = useState([])
    const [showForm, setShowForm] = useState(false)
    const [loading, setLoading] = useState(true)
    const [saving, setSaving] = useState(false)
    const [error, setError] = useState('')
    const [updatingProduct, setUpdatingProduct] = useState(null)

    const [editingProduct, setEditingProduct] = useState(null)

    const [form, setForm] = useState({
        title: '',
        description: '',
        originalPrice: '',
        flashSalePrice: '',
        initialStock: '',
        imageUrl: '',
        startTime: '',
        endTime: ''
    })

    const [editForm, setEditForm] = useState({
        title: '',
        description: '',
        originalPrice: '',
        flashSalePrice: '',
        imageUrl: '',
        startTime: '',
        endTime: ''
    })

    const fetchProducts = async () => {
        try {
            setLoading(true)
            setError('')

            const response = await api.get('/products')

            const data =
                response.data?.data?.content || []

            setProducts(data)

        } catch (err) {
            console.error(
                'Admin products error:',
                err
            )

            setError(
                err.response?.data?.message ||
                'Unable to load products.'
            )
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchProducts()
    }, [])

    const handleChange = (e) => {
        setForm({
            ...form,
            [e.target.name]: e.target.value
        })
    }

    const handleEditChange = (e) => {
        setEditForm({
            ...editForm,
            [e.target.name]: e.target.value
        })
    }

    // ==========================================
    // CREATE PRODUCT
    // ==========================================

    const handleSubmit = async (e) => {
        e.preventDefault()

        try {
            setSaving(true)
            setError('')

            const originalPrice =
                Number(form.originalPrice)

            const flashSalePrice =
                Number(form.flashSalePrice)

            const initialStock =
                Number(form.initialStock)

            if (
                !Number.isFinite(originalPrice) ||
                originalPrice <= 0
            ) {
                setError(
                    'Original price must be greater than zero.'
                )
                return
            }

            if (
                !Number.isFinite(flashSalePrice) ||
                flashSalePrice <= 0
            ) {
                setError(
                    'Flash sale price must be greater than zero.'
                )
                return
            }

            if (flashSalePrice >= originalPrice) {
                setError(
                    'Flash sale price must be lower than the original price.'
                )
                return
            }

            if (
                !Number.isInteger(initialStock) ||
                initialStock <= 0
            ) {
                setError(
                    'Initial stock must be greater than zero.'
                )
                return
            }

            const startTime =
                new Date(form.startTime)

            const endTime =
                new Date(form.endTime)

            if (
                Number.isNaN(startTime.getTime()) ||
                Number.isNaN(endTime.getTime())
            ) {
                setError(
                    'Please provide valid sale start and end times.'
                )
                return
            }

            if (endTime <= startTime) {
                setError(
                    'Sale end time must be after the start time.'
                )
                return
            }

            const response = await api.post(
                '/products',
                {
                    title: form.title.trim(),
                    description: form.description,
                    originalPrice,
                    flashSalePrice,
                    initialStock,
                    imageUrl: form.imageUrl || null,
                    startTime:
                        startTime.toISOString(),
                    endTime:
                        endTime.toISOString()
                }
            )

            const createdProduct =
                response.data?.data

            if (!createdProduct?.id) {
                throw new Error(
                    'Product was created but no product ID was returned.'
                )
            }

            await api.post(
                `/inventory/replenish?productId=${createdProduct.id}&quantity=${initialStock}`
            )

            notify(
                'Product and inventory added successfully'
            )

            setForm({
                title: '',
                description: '',
                originalPrice: '',
                flashSalePrice: '',
                initialStock: '',
                imageUrl: '',
                startTime: '',
                endTime: ''
            })

            setShowForm(false)

            await fetchProducts()

        } catch (err) {
            console.error(
                'Create product error:',
                err
            )

            setError(
                err.response?.data?.message ||
                err.message ||
                'Unable to create product.'
            )
        } finally {
            setSaving(false)
        }
    }

    // ==========================================
    // START EDIT
    // ==========================================

    const handleEditStart = (product) => {
        const toDateTimeLocal = (value) => {
            if (!value) {
                return ''
            }

            const date = new Date(value)

            if (Number.isNaN(date.getTime())) {
                return ''
            }

            const offset =
                date.getTimezoneOffset()

            const localDate =
                new Date(
                    date.getTime() -
                    offset * 60000
                )

            return localDate
                .toISOString()
                .slice(0, 16)
        }

        setEditingProduct(product)

        setEditForm({
            title: product.title || '',
            description: product.description || '',
            originalPrice:
                product.originalPrice ?? '',
            flashSalePrice:
                product.flashSalePrice ?? '',
            imageUrl:
                product.imageUrl || '',
            startTime:
                toDateTimeLocal(
                    product.startTime
                ),
            endTime:
                toDateTimeLocal(
                    product.endTime
                )
        })

        setError('')
    }

    // ==========================================
    // UPDATE PRODUCT
    // ==========================================

    const handleEditSubmit = async (e) => {
        e.preventDefault()

        if (!editingProduct) {
            return
        }

        try {
            setUpdatingProduct(
                editingProduct.id
            )

            setError('')

            const title =
                editForm.title.trim()

            if (!title) {
                setError(
                    'Product title cannot be blank.'
                )
                return
            }

            const originalPrice =
                Number(editForm.originalPrice)

            const flashSalePrice =
                Number(editForm.flashSalePrice)

            if (
                !Number.isFinite(originalPrice) ||
                originalPrice <= 0
            ) {
                setError(
                    'Original price must be greater than zero.'
                )
                return
            }

            if (
                !Number.isFinite(flashSalePrice) ||
                flashSalePrice <= 0
            ) {
                setError(
                    'Flash sale price must be greater than zero.'
                )
                return
            }

            if (flashSalePrice >= originalPrice) {
                setError(
                    'Flash sale price must be lower than the original price.'
                )
                return
            }

            const startTime =
                new Date(editForm.startTime)

            const endTime =
                new Date(editForm.endTime)

            if (
                Number.isNaN(startTime.getTime()) ||
                Number.isNaN(endTime.getTime())
            ) {
                setError(
                    'Please provide valid sale start and end times.'
                )
                return
            }

            if (endTime <= startTime) {
                setError(
                    'Sale end time must be after the start time.'
                )
                return
            }

            const response = await api.put(
                `/products/${editingProduct.id}`,
                {
                    title,
                    description:
                    editForm.description,
                    originalPrice,
                    flashSalePrice,
                    imageUrl:
                        editForm.imageUrl || null,
                    startTime:
                        startTime.toISOString(),
                    endTime:
                        endTime.toISOString()
                }
            )

            const updatedProduct =
                response.data?.data

            if (!updatedProduct) {
                throw new Error(
                    'Updated product was not returned.'
                )
            }

            setProducts(
                (currentProducts) =>
                    currentProducts.map(
                        (product) =>
                            product.id ===
                            editingProduct.id
                                ? updatedProduct
                                : product
                    )
            )

            setEditingProduct(null)

            notify(
                `${editingProduct.title} updated successfully`
            )

        } catch (err) {
            console.error(
                'Update product error:',
                err
            )

            setError(
                err.response?.data?.message ||
                err.message ||
                'Unable to update product.'
            )
        } finally {
            setUpdatingProduct(null)
        }
    }

    // ==========================================
    // DELETE PRODUCT
    // ==========================================

    const handleDelete = async (product) => {
        const confirmed =
            window.confirm(
                `Are you sure you want to permanently delete "${product.title}"?`
            )

        if (!confirmed) {
            return
        }

        try {
            setUpdatingProduct(product.id)
            setError('')

            await api.delete(
                `/products/${product.id}`
            )

            setProducts(
                (currentProducts) =>
                    currentProducts.filter(
                        (item) =>
                            item.id !== product.id
                    )
            )

            notify(
                `${product.title} deleted successfully`
            )

        } catch (err) {
            console.error(
                'Delete product error:',
                err
            )

            setError(
                err.response?.data?.message ||
                'Unable to delete product.'
            )
        } finally {
            setUpdatingProduct(null)
        }
    }

    // ==========================================
    // STATUS CHANGE
    // ==========================================

    const handleStatusChange = async (product) => {
        const nextStatus =
            product.status === 'DISABLED'
                ? 'ACTIVE'
                : 'DISABLED'

        try {
            setUpdatingProduct(product.id)

            setError('')

            await api.put(
                `/products/${product.id}/status?status=${nextStatus}`
            )

            setProducts(
                (currentProducts) =>
                    currentProducts.map(
                        (item) =>
                            item.id === product.id
                                ? {
                                    ...item,
                                    status:
                                    nextStatus
                                }
                                : item
                    )
            )

            notify(
                nextStatus === 'DISABLED'
                    ? `${product.title} marked as sold out`
                    : `${product.title} enabled successfully`
            )

        } catch (err) {
            console.error(
                'Update product status error:',
                err
            )

            notify(
                err.response?.data?.message ||
                'Unable to update product status.'
            )
        } finally {
            setUpdatingProduct(null)
        }
    }

    // ==========================================
    // REPLENISH STOCK
    // ==========================================

    const handleReplenish = async (product) => {
        const input =
            window.prompt(
                `How many units do you want to add to "${product.title}"?`
            )

        if (input === null) {
            return
        }

        const quantity =
            Number(input)

        if (
            !Number.isInteger(quantity) ||
            quantity <= 0
        ) {
            notify(
                'Please enter a valid quantity greater than 0.'
            )
            return
        }

        try {
            setUpdatingProduct(product.id)

            setError('')

            await api.post(
                `/inventory/replenish?productId=${product.id}&quantity=${quantity}`
            )

            notify(
                `${quantity} units added to ${product.title}`
            )

        } catch (err) {
            console.error(
                'Inventory replenishment error:',
                err
            )

            notify(
                err.response?.data?.message ||
                'Unable to add stock.'
            )
        } finally {
            setUpdatingProduct(null)
        }
    }

    // ==========================================
    // CHECK STOCK
    // ==========================================

    const handleCheckStock = async (product) => {
        try {
            setUpdatingProduct(product.id)

            const response =
                await api.get(
                    `/inventory/${product.id}`
                )

            const inventory =
                response.data?.data ||
                response.data

            notify(
                `${product.title}: ${inventory.availableStock} available, ${inventory.lockedStock} locked`
            )

        } catch (err) {
            console.error(
                'Inventory check error:',
                err
            )

            notify(
                err.response?.data?.message ||
                'Unable to check inventory.'
            )
        } finally {
            setUpdatingProduct(null)
        }
    }

    return (
        <div className="space-y-6">

            {/* Header */}

            <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">

                <div>

                    <p className="text-sm font-medium text-amber-300">
                        Store management
                    </p>

                    <h1 className="mt-1 text-3xl font-semibold text-white">
                        Inventory
                    </h1>

                    <p className="mt-1 text-sm text-slate-500">
                        Add products and manage store inventory
                    </p>

                </div>

                <button
                    onClick={() =>
                        setShowForm(!showForm)
                    }
                    className="flex items-center justify-center gap-2 rounded-xl bg-amber-400 px-4 py-3 text-sm font-semibold text-slate-950 hover:bg-amber-300"
                >
                    <Plus size={17} />
                    Add Product
                </button>

            </div>

            {/* Error */}

            {error && (
                <div className="rounded-xl border border-rose-400/20 bg-rose-400/5 p-4 text-sm text-rose-300">
                    {error}
                </div>
            )}

            {/* Add Product Form */}

            {showForm && (
                <form
                    onSubmit={handleSubmit}
                    className="rounded-2xl border border-slate-800 bg-slate-900/70 p-6"
                >

                    <h2 className="text-lg font-semibold text-white">
                        Add New Product
                    </h2>

                    <div className="mt-5 grid gap-4 md:grid-cols-2">

                        <input
                            name="title"
                            value={form.title}
                            onChange={handleChange}
                            placeholder="Product title"
                            required
                            className="rounded-xl border border-slate-700 bg-slate-950 p-3 text-sm text-white outline-none focus:border-amber-400"
                        />

                        <input
                            name="imageUrl"
                            value={form.imageUrl}
                            onChange={handleChange}
                            placeholder="Image URL (optional)"
                            className="rounded-xl border border-slate-700 bg-slate-950 p-3 text-sm text-white outline-none focus:border-amber-400"
                        />

                        <textarea
                            name="description"
                            value={form.description}
                            onChange={handleChange}
                            placeholder="Product description"
                            rows={3}
                            className="rounded-xl border border-slate-700 bg-slate-950 p-3 text-sm text-white outline-none focus:border-amber-400 md:col-span-2"
                        />

                        <input
                            name="originalPrice"
                            type="number"
                            min="0.01"
                            step="0.01"
                            value={form.originalPrice}
                            onChange={handleChange}
                            placeholder="Original price"
                            required
                            className="rounded-xl border border-slate-700 bg-slate-950 p-3 text-sm text-white outline-none focus:border-amber-400"
                        />

                        <input
                            name="flashSalePrice"
                            type="number"
                            min="0.01"
                            step="0.01"
                            value={form.flashSalePrice}
                            onChange={handleChange}
                            placeholder="Flash sale price"
                            required
                            className="rounded-xl border border-slate-700 bg-slate-950 p-3 text-sm text-white outline-none focus:border-amber-400"
                        />

                        <input
                            name="initialStock"
                            type="number"
                            min="1"
                            value={form.initialStock}
                            onChange={handleChange}
                            placeholder="Initial stock"
                            required
                            className="rounded-xl border border-slate-700 bg-slate-950 p-3 text-sm text-white outline-none focus:border-amber-400"
                        />

                        <div>

                            <label className="mb-2 block text-xs text-slate-500">
                                Sale starts
                            </label>

                            <input
                                name="startTime"
                                type="datetime-local"
                                value={form.startTime}
                                onChange={handleChange}
                                required
                                className="w-full rounded-xl border border-slate-700 bg-slate-950 p-3 text-sm text-white outline-none focus:border-amber-400"
                            />

                        </div>

                        <div>

                            <label className="mb-2 block text-xs text-slate-500">
                                Sale ends
                            </label>

                            <input
                                name="endTime"
                                type="datetime-local"
                                value={form.endTime}
                                onChange={handleChange}
                                required
                                className="w-full rounded-xl border border-slate-700 bg-slate-950 p-3 text-sm text-white outline-none focus:border-amber-400"
                            />

                        </div>

                    </div>

                    <div className="mt-5 flex gap-3">

                        <button
                            type="submit"
                            disabled={saving}
                            className="rounded-xl bg-amber-400 px-5 py-3 text-sm font-semibold text-slate-950 disabled:opacity-50"
                        >
                            {saving
                                ? 'Adding...'
                                : 'Add Product'}
                        </button>

                        <button
                            type="button"
                            onClick={() =>
                                setShowForm(false)
                            }
                            className="rounded-xl border border-slate-700 px-5 py-3 text-sm text-slate-300 hover:bg-slate-800"
                        >
                            Cancel
                        </button>

                    </div>

                </form>
            )}

            {/* Edit Product Form */}

            {editingProduct && (
                <form
                    onSubmit={handleEditSubmit}
                    className="rounded-2xl border border-blue-400/20 bg-slate-900/70 p-6"
                >

                    <div className="flex items-center justify-between">

                        <div>
                            <p className="text-sm font-medium text-blue-300">
                                Edit product
                            </p>

                            <h2 className="mt-1 text-lg font-semibold text-white">
                                {editingProduct.title}
                            </h2>
                        </div>

                        <button
                            type="button"
                            onClick={() =>
                                setEditingProduct(null)
                            }
                            className="rounded-lg p-2 text-slate-500 hover:bg-slate-800 hover:text-white"
                        >
                            <X size={18} />
                        </button>

                    </div>

                    <div className="mt-5 grid gap-4 md:grid-cols-2">

                        <input
                            name="title"
                            value={editForm.title}
                            onChange={handleEditChange}
                            placeholder="Product title"
                            required
                            className="rounded-xl border border-slate-700 bg-slate-950 p-3 text-sm text-white outline-none focus:border-blue-400"
                        />

                        <input
                            name="imageUrl"
                            value={editForm.imageUrl}
                            onChange={handleEditChange}
                            placeholder="Image URL (optional)"
                            className="rounded-xl border border-slate-700 bg-slate-950 p-3 text-sm text-white outline-none focus:border-blue-400"
                        />

                        <textarea
                            name="description"
                            value={editForm.description}
                            onChange={handleEditChange}
                            placeholder="Product description"
                            rows={3}
                            className="rounded-xl border border-slate-700 bg-slate-950 p-3 text-sm text-white outline-none focus:border-blue-400 md:col-span-2"
                        />

                        <input
                            name="originalPrice"
                            type="number"
                            min="0.01"
                            step="0.01"
                            value={editForm.originalPrice}
                            onChange={handleEditChange}
                            placeholder="Original price"
                            required
                            className="rounded-xl border border-slate-700 bg-slate-950 p-3 text-sm text-white outline-none focus:border-blue-400"
                        />

                        <input
                            name="flashSalePrice"
                            type="number"
                            min="0.01"
                            step="0.01"
                            value={editForm.flashSalePrice}
                            onChange={handleEditChange}
                            placeholder="Flash sale price"
                            required
                            className="rounded-xl border border-slate-700 bg-slate-950 p-3 text-sm text-white outline-none focus:border-blue-400"
                        />

                        <div>

                            <label className="mb-2 block text-xs text-slate-500">
                                Sale starts
                            </label>

                            <input
                                name="startTime"
                                type="datetime-local"
                                value={editForm.startTime}
                                onChange={handleEditChange}
                                required
                                className="w-full rounded-xl border border-slate-700 bg-slate-950 p-3 text-sm text-white outline-none focus:border-blue-400"
                            />

                        </div>

                        <div>

                            <label className="mb-2 block text-xs text-slate-500">
                                Sale ends
                            </label>

                            <input
                                name="endTime"
                                type="datetime-local"
                                value={editForm.endTime}
                                onChange={handleEditChange}
                                required
                                className="w-full rounded-xl border border-slate-700 bg-slate-950 p-3 text-sm text-white outline-none focus:border-blue-400"
                            />

                        </div>

                    </div>

                    <div className="mt-5 flex gap-3">

                        <button
                            type="submit"
                            disabled={
                                updatingProduct ===
                                editingProduct.id
                            }
                            className="rounded-xl bg-blue-400 px-5 py-3 text-sm font-semibold text-slate-950 disabled:opacity-50"
                        >
                            {updatingProduct ===
                            editingProduct.id
                                ? 'Updating...'
                                : 'Save Changes'}
                        </button>

                        <button
                            type="button"
                            onClick={() =>
                                setEditingProduct(null)
                            }
                            className="rounded-xl border border-slate-700 px-5 py-3 text-sm text-slate-300 hover:bg-slate-800"
                        >
                            Cancel
                        </button>

                    </div>

                </form>
            )}

            {/* Product List */}

            <div className="overflow-hidden rounded-2xl border border-slate-800 bg-slate-900/70">

                {loading ? (

                    <div className="p-8 text-center text-slate-400">
                        Loading inventory...
                    </div>

                ) : products.length === 0 ? (

                    <div className="p-8 text-center text-slate-400">
                        No products available.
                    </div>

                ) : (

                    products.map((product) => {

                        const isSoldOut =
                            product.status === 'DISABLED'

                        const isUpdating =
                            updatingProduct ===
                            product.id

                        return (
                            <div
                                key={product.id}
                                className="grid gap-4 border-b border-slate-800 p-5 last:border-0 md:grid-cols-5 md:items-center"
                            >

                                <div className="md:col-span-2">

                                    <p className="text-xs text-slate-500">
                                        Product
                                    </p>

                                    <p className="font-medium text-white">
                                        {product.title}
                                    </p>

                                    <p className="mt-1 text-xs text-slate-500">
                                        ID: {product.id}
                                    </p>

                                </div>

                                <div>

                                    <p className="text-xs text-slate-500">
                                        Flash Price
                                    </p>

                                    <p className="font-semibold text-amber-300">
                                        ₹{Number(
                                        product.flashSalePrice
                                    ).toLocaleString('en-IN')}
                                    </p>

                                </div>

                                <div>

                                    <p className="text-xs text-slate-500">
                                        Status
                                    </p>

                                    <span
                                        className={
                                            isSoldOut
                                                ? 'text-sm text-rose-300'
                                                : 'text-sm text-emerald-300'
                                        }
                                    >
                                        {isSoldOut
                                            ? 'SOLD OUT'
                                            : product.status}
                                    </span>

                                </div>

                                <div className="flex flex-wrap gap-2">

                                    {/* Edit */}

                                    <button
                                        onClick={() =>
                                            handleEditStart(
                                                product
                                            )
                                        }
                                        disabled={isUpdating}
                                        className="rounded-lg border border-blue-400/30 px-3 py-2 text-xs text-blue-300 hover:border-blue-400 hover:text-blue-200 disabled:cursor-not-allowed disabled:opacity-50"
                                    >
                                        Edit
                                    </button>

                                    {/* Delete */}

                                    <button
                                        onClick={() =>
                                            handleDelete(
                                                product
                                            )
                                        }
                                        disabled={isUpdating}
                                        className="rounded-lg border border-rose-400/30 px-3 py-2 text-xs text-rose-300 hover:border-rose-400 hover:text-rose-200 disabled:cursor-not-allowed disabled:opacity-50"
                                    >
                                        Delete
                                    </button>

                                    {/* Check Stock */}

                                    <button
                                        onClick={() =>
                                            handleCheckStock(
                                                product
                                            )
                                        }
                                        disabled={isUpdating}
                                        className="rounded-lg border border-slate-700 px-3 py-2 text-xs text-slate-300 hover:border-amber-400 hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
                                    >
                                        Check Stock
                                    </button>

                                    {/* Add Stock */}

                                    <button
                                        onClick={() =>
                                            handleReplenish(
                                                product
                                            )
                                        }
                                        disabled={isUpdating}
                                        className="rounded-lg border border-amber-400/30 px-3 py-2 text-xs text-amber-300 hover:border-amber-400 hover:text-amber-200 disabled:cursor-not-allowed disabled:opacity-50"
                                    >
                                        Add Stock
                                    </button>

                                    {/* Enable / Sold Out */}

                                    <button
                                        onClick={() =>
                                            handleStatusChange(
                                                product
                                            )
                                        }
                                        disabled={isUpdating}
                                        className={
                                            isSoldOut
                                                ? 'rounded-lg border border-emerald-400/30 px-3 py-2 text-xs text-emerald-300 hover:border-emerald-400 hover:text-emerald-200 disabled:cursor-not-allowed disabled:opacity-50'
                                                : 'rounded-lg border border-rose-400/30 px-3 py-2 text-xs text-rose-300 hover:border-rose-400 hover:text-rose-200 disabled:cursor-not-allowed disabled:opacity-50'
                                        }
                                    >
                                        {isUpdating
                                            ? 'Updating...'
                                            : isSoldOut
                                                ? 'Enable Product'
                                                : 'Mark Sold Out'}
                                    </button>

                                </div>

                            </div>
                        )
                    })
                )}

            </div>

        </div>
    )
}
function SettingsView({notify}) {
    const [tab, setTab] = useState('store')
    const [saved, setSaved] = useState(false)

    return (
        <div className="space-y-6">
            <div>
                <p className="text-sm font-medium text-amber-300">
                    Workspace control
                </p>

                <h1 className="mt-1 text-3xl font-semibold text-white">
                    Settings
                </h1>
            </div>

            <div className="grid gap-6 lg:grid-cols-[220px_1fr]">
                <div className="space-y-2">
                    {[
                        ['store', Settings, 'Store preferences'],
                        ['team', Users, 'Team permissions'],
                        ['api', KeyRound, 'API keys'],
                        ['webhooks', Webhook, 'Webhooks']
                    ].map(([id, Icon, label]) => (
                        <button
                            key={id}
                            onClick={() => setTab(id)}
                            className={`flex w-full items-center gap-3 rounded-xl px-3 py-3 text-left text-sm ${
                                tab === id
                                    ? 'bg-amber-400 font-semibold text-slate-950'
                                    : 'text-slate-400 hover:bg-slate-900'
                            }`}
                        >
                            <Icon size={17}/>
                            {label}
                        </button>
                    ))}
                </div>

                <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-6">
                    {tab === 'store' && (
                        <>
                            <h2 className="font-medium text-white">
                                Store preferences
                            </h2>

                            <p className="mt-1 text-sm text-slate-500">
                                Control how your storefront behaves during drops.
                            </p>

                            <div className="mt-6 space-y-4">
                                {[
                                    'Enable low-stock alerts',
                                    'Auto-pause sold out products',
                                    'Require address verification'
                                ].map((label, i) => (
                                    <label
                                        key={label}
                                        className="flex items-center justify-between rounded-xl border border-slate-800 p-4 text-sm text-slate-300"
                                    >
                                        <span>{label}</span>

                                        <input
                                            type="checkbox"
                                            defaultChecked={i !== 2}
                                            className="h-4 w-4 accent-amber-400"
                                        />
                                    </label>
                                ))}
                            </div>
                        </>
                    )}

                    {tab === 'team' && (
                        <>
                            <h2 className="font-medium text-white">
                                Team permissions
                            </h2>

                            <div className="mt-5 space-y-3">
                                {[
                                    ['Alex Kim', 'Owner', 'AK'],
                                    ['Mina Patel', 'Operations', 'MP'],
                                    ['Jordan Lee', 'Analyst', 'JL']
                                ].map((row) => (
                                    <div
                                        key={row[0]}
                                        className="flex items-center gap-3 rounded-xl border border-slate-800 p-4"
                                    >
                                        <div
                                            className="flex h-9 w-9 items-center justify-center rounded-full bg-cyan-400/20 text-xs text-cyan-300">
                                            {row[2]}
                                        </div>

                                        <div className="flex-1">
                                            <p className="text-sm text-white">
                                                {row[0]}
                                            </p>

                                            <p className="text-xs text-slate-500">
                                                {row[1]}
                                            </p>
                                        </div>

                                        <ChevronDown
                                            size={16}
                                            className="text-slate-500"
                                        />
                                    </div>
                                ))}
                            </div>

                            <button className="mt-5 flex items-center gap-2 text-sm text-amber-300">
                                <UserPlus size={16}/>
                                Invite teammate
                            </button>
                        </>
                    )}

                    {tab === 'api' && (
                        <>
                            <h2 className="font-medium text-white">
                                API keys
                            </h2>

                            <p className="mt-1 text-sm text-slate-500">
                                Keep private keys secure and rotate them regularly.
                            </p>

                            <div
                                className="mt-6 flex items-center justify-between rounded-xl border border-slate-800 p-4">
                                <div>
                                    <p className="font-mono text-sm text-white">
                                        fse_live_••••••••••••9A42
                                    </p>

                                    <p className="mt-1 text-xs text-slate-500">
                                        Created 12 days ago · Last used now
                                    </p>
                                </div>

                                <button
                                    onClick={() =>
                                        notify('New API key generated')
                                    }
                                    className="rounded-lg bg-amber-400 px-3 py-2 text-xs font-semibold text-slate-950"
                                >
                                    Rotate
                                </button>
                            </div>
                        </>
                    )}

                    {tab === 'webhooks' && (
                        <>
                            <h2 className="font-medium text-white">
                                Webhook integrations
                            </h2>

                            <div className="mt-5 space-y-3">
                                {[
                                    'Order placed · Active',
                                    'Inventory sync · Active',
                                    'Refund issued · Paused'
                                ].map((label, i) => (
                                    <div
                                        key={label}
                                        className="flex items-center justify-between rounded-xl border border-slate-800 p-4 text-sm text-slate-300"
                                    >
                                        <span>{label}</span>

                                        <span
                                            className={`rounded-full px-2 py-1 text-xs ${
                                                i === 2
                                                    ? 'bg-slate-800 text-slate-500'
                                                    : 'bg-emerald-400/10 text-emerald-300'
                                            }`}
                                        >
                      {i === 2 ? 'Paused' : 'Live'}
                    </span>
                                    </div>
                                ))}
                            </div>
                        </>
                    )}

                    <button
                        onClick={() => {
                            setSaved(true)
                            notify('Settings saved successfully')
                        }}
                        className="mt-7 rounded-xl bg-amber-400 px-5 py-3 text-sm font-semibold text-slate-950"
                    >
                        {saved ? 'Saved' : 'Save changes'}
                    </button>
                </div>
            </div>
        </div>
    )
}

export default function Page() {
    const router = useRouter()

    const [view, setView] = useState('home')
    const [cart, setCart] = useState([])
    const [mobileOpen, setMobileOpen] = useState(false)
    const [detail, setDetail] = useState(null)
    const [toast, setToast] = useState('')
    const [range, setRange] = useState('7d')
    const [currentUser, setCurrentUser] = useState(null)
    const [authReady, setAuthReady] = useState(false)

    useEffect(() => {
        const checkFrontendSession = async () => {
            try {
                const response = await fetch("/api/session");
                const {startedAt} = await response.json();

                const previousStartedAt = sessionStorage.getItem("frontendStartedAt");

                if (previousStartedAt !== String(startedAt)) {
                    localStorage.removeItem("accessToken");
                    localStorage.removeItem("user");
                    sessionStorage.setItem("frontendStartedAt", String(startedAt));

                    router.replace("/login");
                    return;
                }

                const accessToken = localStorage.getItem("accessToken");

                if (!accessToken) {
                    router.replace("/login");
                    return;
                }

                const storedUser = localStorage.getItem("user");

                if (storedUser) {
                    try {
                        setCurrentUser(JSON.parse(storedUser));
                    } catch (error) {
                        console.error("Failed to load user:", error);
                        localStorage.removeItem("user");
                    }
                }

                setAuthReady(true);
            } catch (error) {
                console.error("Failed to check frontend session:", error);
                router.replace("/login");
            }
        };

        checkFrontendSession();
    }, [router])


    if (!authReady) {
        return null
    }

    const fullName =
        [currentUser?.firstName, currentUser?.lastName]
            .filter(Boolean)
            .join(' ') ||
        currentUser?.email ||
        'User'

    const initials =
        [currentUser?.firstName, currentUser?.lastName]
            .filter(Boolean)
            .map((name) => name.charAt(0))
            .join('')
            .toUpperCase() ||
        currentUser?.email?.charAt(0).toUpperCase() ||
        'U'
    const userRoles = currentUser?.roles || []

    const isAdmin =
        userRoles.includes('ROLE_ADMIN') ||
        userRoles.includes('ADMIN') ||
        userRoles.includes('ROLE_OWNER') ||
        userRoles.includes('OWNER')
    const navItems = [
        ['home', 'Overview', Home],
        ['products', 'Products', Package],
        ['sale', 'Flash Sale', Zap],
        ['orders', 'Orders', ShoppingBag],
        ['cart', 'Cart & Checkout', ShoppingCart],
        ['notifications', 'Notifications', Bell],
        ...(isAdmin
            ? [['admin', 'Admin Console', LayoutDashboard]]
            : []),
        ['settings', 'Settings', Settings]
    ]
    const handleLogout = () => {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('user')
        router.replace('/login')
    }

    const notify = (message) => {
        setToast(message)
        window.setTimeout(() => setToast(''), 2800)
    }

    const addToCart = (product) => {
        const requestedQty = Number(product.qty || 1)
        const availableStock = Number(product.stock ?? 0)

        if (product.status === 'DISABLED' || availableStock <= 0) {
            notify(`${product.name} is sold out`)
            return
        }

        setCart((current) => {
            const existing = current.find((item) => item.id === product.id)

            if (existing) {
                const nextQty = Math.min(
                    existing.qty + requestedQty,
                    availableStock
                )

                if (nextQty === existing.qty) {
                    notify(`${product.name} has no more stock available`)
                    return current
                }

                return current.map((item) =>
                    item.id === product.id
                        ? {...item, qty: nextQty, stock: availableStock}
                        : item
                )
            }

            return [
                ...current,
                {
                    ...product,
                    qty: Math.min(requestedQty, availableStock),
                    stock: availableStock
                }
            ]
        })

        notify(`${product.name} added to cart`)
    }

    const openProduct = (product) => setDetail(product)

    const go = (id) => {
        setDetail(null)
        setView(id)
        setMobileOpen(false)
    }

    const activeLabel = detail
        ? 'Product detail'
        : navItems.find((item) => item[0] === view)?.[1]

    let content

    if (detail) {
        content = (
            <ProductDetail
                product={detail}
                onBack={() => setDetail(null)}
                onAdd={addToCart}
                notify={notify}
            />
        )
    } else if (view === 'home') {
        content = (
            <Overview
                setView={go}
                onAdd={addToCart}
                onOpen={openProduct}
            />
        )
    } else if (view === 'products') {
        content = (
            <Products
                onAdd={addToCart}
                onOpen={openProduct}
            />
        )
    } else if (view === 'sale') {
        content = (
            <FlashSale
                onAdd={addToCart}
                onOpen={openProduct}
            />
        )
    } else if (view === 'cart') {
        content = (
            <Checkout
                cart={cart}
                setCart={setCart}
                setView={go}
                notify={notify}
            />
        )
    } else if (view === 'orders') {
        content = <Orders notify={notify}/>
    } else if (view === 'notifications') {
        content = <Notifications notify={notify}/>
    } else if (view === 'admin') {
        content = isAdmin ? (
            <AdminInventory notify={notify}/>
        ) : (
            <div className="rounded-2xl border border-rose-400/20 bg-rose-400/5 p-8 text-center">
                <p className="text-lg font-semibold text-rose-300">
                    Access denied
                </p>
                <p className="mt-2 text-sm text-slate-400">
                    You do not have permission to access the Admin Console.
                </p>
                <button
                    onClick={() => go('home')}
                    className="mt-5 rounded-xl bg-amber-400 px-5 py-2.5 text-sm font-semibold text-slate-950"
                >
                    Back to dashboard
                </button>
            </div>
        )
    } else {
        content = <SettingsView notify={notify}/>
    }

    return (
        <main className="min-h-screen bg-slate-950 text-slate-100">

            <aside
                className={`fixed inset-y-0 left-0 z-40 flex w-72 flex-col border-r border-slate-800 bg-slate-950 p-5 transition-transform lg:translate-x-0 ${
                    mobileOpen
                        ? 'translate-x-0'
                        : '-translate-x-full'
                }`}
            >
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <div className="rounded-xl bg-amber-400 p-2 text-slate-950">
                            <Zap
                                size={19}
                                fill="currentColor"
                            />
                        </div>

                        <span className="font-semibold tracking-tight text-white">
              FlashSale{' '}
                            <span className="text-amber-300">
                Engine
              </span>
            </span>
                    </div>

                    <button
                        onClick={() => setMobileOpen(false)}
                        className="text-slate-500 lg:hidden"
                    >
                        <X/>
                    </button>
                </div>

                <div className="mt-10 space-y-1">
                    {navItems.map(([id, label, Icon]) => (
                        <button
                            key={id}
                            onClick={() => go(id)}
                            className={`flex w-full items-center gap-3 rounded-xl px-3 py-3 text-left text-sm transition ${
                                view === id && !detail
                                    ? 'bg-amber-400 font-semibold text-slate-950'
                                    : 'text-slate-400 hover:bg-slate-900 hover:text-white'
                            }`}
                        >
                            <Icon size={18}/>
                            <span>{label}</span>

                            {id === 'cart' && cart.length > 0 && (
                                <span
                                    className="ml-auto rounded-full bg-slate-950 px-2 py-0.5 text-[10px] text-amber-300">
                  {cart.reduce(
                      (sum, item) => sum + item.qty,
                      0
                  )}
                </span>
                            )}
                        </button>
                    ))}
                </div>

                <div className="mt-auto rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
                    <div className="flex items-center gap-2 text-xs text-emerald-300">
                        <span className="h-2 w-2 animate-pulse rounded-full bg-emerald-400"/>
                        All systems operational
                    </div>

                    <p className="mt-3 text-xs leading-5 text-slate-500">
                        Edge nodes are synced and ready for your next drop.
                    </p>
                </div>

                <div className="mt-4 flex items-center gap-3 border-t border-slate-800 pt-4">
                    <div
                        className="flex h-9 w-9 items-center justify-center rounded-full bg-cyan-400/20 text-sm font-semibold text-cyan-300">
                        {initials}
                    </div>

                    <div className="flex-1">
                        <p className="text-sm font-medium text-white">
                            {fullName}
                        </p>

                        <p className="text-xs text-slate-500">
                            {isAdmin ? 'Store owner' : 'Customer'}
                        </p>
                    </div>

                    <div className="flex items-center gap-2">
                        <button
                            onClick={() => go('settings')}
                            aria-label="Open settings"
                            className="rounded-lg p-2 hover:bg-slate-800"
                        >
                            <Settings
                                size={16}
                                className="text-slate-50"
                            />
                        </button>

                        <button
                            onClick={handleLogout}
                            aria-label="Logout"
                            className="rounded-lg p-2 hover:bg-slate-800"
                        >
                            <LogOut
                                size={16}
                                className="text-rose-300"
                            />
                        </button>
                    </div>
                </div>
            </aside>

            <div className="lg:pl-72">
                <header
                    className="sticky top-0 z-30 flex h-20 items-center justify-between border-b border-slate-800/80 bg-slate-950/90 px-5 backdrop-blur md:px-8">

                    <div className="flex items-center gap-3">
                        <button
                            onClick={() => setMobileOpen(true)}
                            className="text-slate-400 lg:hidden"
                        >
                            <Menu/>
                        </button>

                        <div>
                            <p className="text-xs uppercase tracking-[0.2em] text-slate-600">
                                FlashSale Engine
                            </p>

                            <h2 className="mt-1 text-sm font-medium text-white">
                                {activeLabel}
                            </h2>
                        </div>
                    </div>

                    <div className="flex items-center gap-3">



              <span className="hidden items-center gap-2 text-xs text-emerald-300 sm:flex">
              <span className="h-2 w-2 rounded-full bg-emerald-400"/>
              Live
            </span>

                        <button
                            onClick={() => go('notifications')}
                            className="rounded-xl border border-slate-800 p-2.5 text-slate-400 hover:text-white"
                        >
                            <Bell size={17}/>
                        </button>
                    </div>
                </header>

                <div className="mx-auto max-w-7xl p-5 md:p-8">
                    {content}
                </div>
            </div>

            {toast && (
                <div
                    role="status"
                    className="fixed bottom-6 right-6 z-50 flex items-center gap-3 rounded-xl border border-amber-400/30 bg-slate-900 px-4 py-3 text-sm text-white shadow-2xl"
                >
                    <Check
                        size={17}
                        className="text-emerald-400"
                    />

                    {toast}

                    <button
                        onClick={() => setToast('')}
                        className="text-slate-500"
                    >
                        <X size={15}/>
                    </button>
                </div>
            )}
        </main>
    )



}
