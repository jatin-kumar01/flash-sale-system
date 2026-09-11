// 'use client'
//
// import { useMemo, useState } from 'react'
// import {
//   Activity, Bell, Box, Check, ChevronDown, ChevronLeft, CircleDollarSign, Clock3,
//   CreditCard, Filter, Home, KeyRound, LayoutDashboard, Menu, Package, Plus,
//   Search, Settings, ShoppingBag, ShoppingCart, Sparkles, Trash2, TrendingUp,
//   Truck, UserPlus, Users, Webhook, X, Zap,
// } from 'lucide-react'
//
// const products = [
//   { id: 1, name: 'AeroPods Max', category: 'Audio', price: 249, original: 399, stock: 18, sold: 82, color: 'from-amber-400 to-orange-600', specs: ['Adaptive noise cancellation', '40-hour battery', 'Spatial audio'], variants: { Color: ['Midnight', 'Silver', 'Citrus'], Size: ['Standard'] } },
//   { id: 2, name: 'Velocity Runner', category: 'Footwear', price: 89, original: 140, stock: 31, sold: 69, color: 'from-cyan-400 to-blue-600', specs: ['Carbon foam sole', 'Breathable knit upper', 'Reflective heel'], variants: { Color: ['Ocean', 'Volt', 'Black'], Size: ['8', '9', '10', '11', '12'] } },
//   { id: 3, name: 'Orbit Mechanical', category: 'Workspace', price: 129, original: 189, stock: 12, sold: 88, color: 'from-fuchsia-400 to-violet-600', specs: ['Hot-swappable switches', 'RGB backlight', 'Aluminum frame'], variants: { Color: ['Violet', 'Graphite'], Size: ['75%', 'TKL'] } },
//   { id: 4, name: 'Nova Camera Kit', category: 'Creative', price: 599, original: 749, stock: 7, sold: 93, color: 'from-emerald-400 to-teal-600', specs: ['4K 120fps video', '24MP sensor', 'Magnetic lens mount'], variants: { Color: ['Forest', 'Black'], Size: ['Body only', 'Creator kit'] } },
//   { id: 5, name: 'Pulse Smartwatch', category: 'Wearables', price: 179, original: 249, stock: 24, sold: 76, color: 'from-rose-400 to-red-600', specs: ['7-day battery', 'Sleep tracking', 'Water resistant 50m'], variants: { Color: ['Coral', 'Slate'], Size: ['40mm', '44mm'] } },
//   { id: 6, name: 'Lumen Desk Lamp', category: 'Workspace', price: 64, original: 99, stock: 42, sold: 58, color: 'from-yellow-300 to-amber-600', specs: ['Adaptive brightness', 'USB-C charging', 'Touch controls'], variants: { Color: ['Sunrise', 'Charcoal'], Size: ['One size'] } },
// ]
// const navItems = [['home', 'Overview', Home], ['products', 'Products', Package], ['sale', 'Flash Sale', Zap], ['orders', 'Orders', ShoppingBag], ['cart', 'Cart & Checkout', ShoppingCart], ['notifications', 'Notifications', Bell], ['admin', 'Admin Console', LayoutDashboard], ['settings', 'Settings', Settings]]
//
// function Metric({ icon: Icon, label, value, detail, tone = 'amber' }) { return <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5 shadow-2xl shadow-black/10"><div className="flex items-start justify-between"><span className={`rounded-xl p-2.5 ${tone === 'amber' ? 'bg-amber-400/10 text-amber-300' : tone === 'emerald' ? 'bg-emerald-400/10 text-emerald-300' : 'bg-cyan-400/10 text-cyan-300'}`}><Icon size={19} /></span><TrendingUp size={16} className="text-emerald-400" /></div><p className="mt-5 text-sm text-slate-400">{label}</p><p className="mt-1 text-2xl font-semibold tracking-tight text-white">{value}</p><p className="mt-1 text-xs text-emerald-400">{detail}</p></div> }
//
// function ProductCard({ product, onAdd, onOpen }) { return <article onClick={() => onOpen(product)} className="group cursor-pointer overflow-hidden rounded-2xl border border-slate-800 bg-slate-900/80 transition hover:-translate-y-1 hover:border-amber-400/50"><div className={`relative flex h-40 items-center justify-center bg-gradient-to-br ${product.color}`}><Box size={60} className="text-white/85 transition group-hover:scale-110" strokeWidth={1.2} /><span className="absolute right-3 top-3 rounded-full bg-slate-950/70 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-white">-{Math.round((1 - product.price / product.original) * 100)}%</span></div><div className="p-4"><p className="text-xs text-slate-500">{product.category}</p><h3 className="mt-1 font-medium text-white">{product.name}</h3><div className="mt-4 flex items-end justify-between"><div><span className="text-lg font-semibold text-amber-300">${product.price}</span><span className="ml-2 text-xs text-slate-600 line-through">${product.original}</span></div><button onClick={e => { e.stopPropagation(); onAdd(product) }} className="rounded-lg bg-amber-400 p-2 text-slate-950 transition hover:bg-amber-300" aria-label={`Add ${product.name} to cart`}><ShoppingCart size={16} /></button></div></div></article> }
//
// function Overview({ setView, onAdd, onOpen }) { return <div className="space-y-6"><section className="relative overflow-hidden rounded-3xl border border-amber-400/20 bg-gradient-to-br from-slate-900 via-slate-900 to-amber-950/40 p-7 md:p-10"><div className="relative z-10 max-w-2xl"><div className="mb-4 inline-flex items-center gap-2 rounded-full border border-amber-400/30 bg-amber-400/10 px-3 py-1.5 text-xs font-medium text-amber-300"><Sparkles size={14} /> Live commerce control room</div><h1 className="text-4xl font-semibold tracking-tight text-white md:text-6xl">Sell faster.<br /><span className="text-amber-300">Stay ahead.</span></h1><p className="mt-5 max-w-lg leading-7 text-slate-400">The high-concurrency engine for flash commerce. Monitor demand, move inventory, and turn every second into momentum.</p><div className="mt-7 flex flex-wrap gap-3"><button onClick={() => setView('sale')} className="rounded-xl bg-amber-400 px-5 py-3 text-sm font-semibold text-slate-950 hover:bg-amber-300">Enter flash sale <Zap className="ml-2 inline" size={16} /></button><button onClick={() => setView('products')} className="rounded-xl border border-slate-700 px-5 py-3 text-sm font-semibold text-white hover:bg-slate-800">Browse catalog</button></div></div></section><div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4"><Metric icon={CircleDollarSign} label="Gross revenue" value="$48,290" detail="+18.4% this week" /><Metric icon={Users} label="Active shoppers" value="2,841" detail="+12.8% live now" tone="cyan" /><Metric icon={Activity} label="Conversion rate" value="8.42%" detail="+2.1% vs. last sale" tone="emerald" /><Metric icon={Truck} label="Orders shipped" value="1,248" detail="94% on time" tone="cyan" /></div><div className="grid gap-6 lg:grid-cols-[1.3fr_0.7fr]"><div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6"><div className="flex items-center justify-between"><div><p className="font-medium text-white">Live catalog</p><p className="mt-1 text-sm text-slate-500">Top drops moving right now</p></div><button onClick={() => setView('products')} className="text-xs text-amber-300">View all</button></div><div className="mt-5 grid gap-3 sm:grid-cols-3">{products.slice(0, 3).map(p => <ProductCard key={p.id} product={p} onAdd={onAdd} onOpen={onOpen} />)}</div></div><div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6"><p className="font-medium text-white">System pulse</p><div className="mt-5 space-y-4">{['Payments processing normally', 'Inventory sync completed', '2,841 shoppers browsing'].map((x, i) => <div key={x} className="flex gap-3 text-sm"><span className={`mt-1.5 h-2 w-2 rounded-full ${i === 2 ? 'bg-amber-400' : 'bg-emerald-400'}`} /><span className="text-slate-400">{x}</span></div>)}</div></div></div></div> }
//
// function Products({ onAdd, onOpen }) { const [query, setQuery] = useState(''); const [category, setCategory] = useState('All'); const cats = ['All', ...new Set(products.map(p => p.category))]; const filtered = products.filter(p => p.name.toLowerCase().includes(query.toLowerCase()) && (category === 'All' || p.category === category)); return <div className="space-y-6"><div className="flex flex-col justify-between gap-4 md:flex-row md:items-end"><div><p className="text-sm font-medium text-amber-300">Live catalog</p><h1 className="mt-1 text-3xl font-semibold text-white">Products</h1></div><div className="relative"><Search size={16} className="absolute left-3 top-3 text-slate-500" /><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Search products" className="w-full rounded-xl border border-slate-700 bg-slate-900 py-2.5 pl-9 pr-4 text-sm text-white outline-none focus:border-amber-400 md:w-64" /></div></div><div className="flex flex-wrap gap-2">{cats.map(cat => <button key={cat} onClick={() => setCategory(cat)} className={`rounded-full px-4 py-2 text-xs font-medium transition ${category === cat ? 'bg-amber-400 text-slate-950' : 'border border-slate-700 text-slate-400 hover:border-slate-500'}`}>{cat}</button>)}</div><div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{filtered.map(product => <ProductCard key={product.id} product={product} onAdd={onAdd} onOpen={onOpen} />)}</div></div> }
//
// function FlashSale({ onAdd, onOpen }) { return <div className="space-y-6"><div className="flex items-center justify-between"><div><p className="text-sm font-medium text-amber-300">Live event</p><h1 className="mt-1 text-3xl font-semibold text-white">Cyber Sprint</h1></div><span className="flex items-center gap-2 rounded-full bg-rose-400/10 px-3 py-2 text-xs font-semibold text-rose-300"><span className="h-2 w-2 animate-pulse rounded-full bg-rose-400" /> 2,841 watching</span></div><div className="rounded-3xl border border-amber-400/25 bg-amber-400/10 p-6 md:p-8"><p className="text-sm text-amber-200/70">Sale ends in</p><div className="mt-2 flex gap-2 text-3xl font-semibold text-white md:text-5xl">{[['02', 'H'], ['41', 'M'], ['18', 'S']].map(([n, l]) => <div key={l}><span className="rounded-lg bg-slate-950/70 px-3 py-2 text-amber-300">{n}</span><small className="ml-1 text-xs text-slate-400">{l}</small></div>)}</div></div><div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{products.slice(0, 3).map(p => <div key={p.id}><ProductCard product={p} onAdd={onAdd} onOpen={onOpen} /><div className="mt-3 px-1"><div className="mb-2 flex justify-between text-xs"><span className="text-slate-500">{p.stock} left</span><span className="text-amber-300">{p.sold}% claimed</span></div><div className="h-2 overflow-hidden rounded-full bg-slate-800"><div style={{ width: `${p.sold}%` }} className="h-full rounded-full bg-amber-400" /></div></div></div>)}</div></div> }
//
// function ProductDetail({ product, onBack, onAdd, notify }) { const [image, setImage] = useState(0); const [qty, setQty] = useState(1); const [selected, setSelected] = useState({ Color: product.variants.Color[0], Size: product.variants.Size[0] }); return <div className="space-y-6"><button onClick={onBack} className="flex items-center gap-2 text-sm text-slate-400 hover:text-white"><ChevronLeft size={17} /> Back to catalog</button><div className="grid gap-8 lg:grid-cols-2"><div><div className={`flex h-96 items-center justify-center rounded-3xl bg-gradient-to-br ${product.color}`}><Box size={150} className="text-white/80" strokeWidth={1} /></div><div className="mt-3 grid grid-cols-3 gap-3">{[0, 1, 2].map(i => <button key={i} onClick={() => setImage(i)} className={`h-20 rounded-xl bg-gradient-to-br ${product.color} ${image === i ? 'ring-2 ring-amber-400' : 'opacity-60'}`}><Box size={24} className="mx-auto text-white" /></button>)}</div></div><div><p className="text-sm text-amber-300">{product.category} / Limited drop</p><h1 className="mt-2 text-4xl font-semibold text-white">{product.name}</h1><div className="mt-4 flex items-center gap-3"><span className="text-3xl font-semibold text-amber-300">${product.price}</span><span className="text-slate-600 line-through">${product.original}</span><span className="rounded-full bg-rose-400/10 px-2 py-1 text-xs text-rose-300">{product.stock} left</span></div><div className="mt-6 rounded-2xl border border-amber-400/25 bg-amber-400/10 p-4"><div className="flex items-center gap-2 text-sm text-amber-200"><Clock3 size={16} /> Personal stock reservation</div><p className="mt-2 text-2xl font-semibold text-white">08:42 <span className="text-xs font-normal text-slate-400">remaining</span></p></div><div className="mt-7 space-y-5">{Object.entries(product.variants).map(([key, values]) => <div key={key}><p className="mb-2 text-sm font-medium text-white">{key}</p><div className="flex flex-wrap gap-2">{values.map(value => <button key={value} onClick={() => setSelected({ ...selected, [key]: value })} className={`rounded-lg border px-3 py-2 text-sm ${selected[key] === value ? 'border-amber-400 bg-amber-400 text-slate-950' : 'border-slate-700 text-slate-400'}`}>{value}</button>)}</div></div>)}</div><div className="mt-7 flex gap-3"><div className="flex items-center rounded-xl border border-slate-700"><button onClick={() => setQty(Math.max(1, qty - 1))} className="px-4 py-3 text-white">−</button><span className="w-8 text-center text-white">{qty}</span><button onClick={() => setQty(qty + 1)} className="px-4 py-3 text-white">+</button></div><button onClick={() => { onAdd({ ...product, selected }); notify(`${product.name} added to cart`) }} className="flex-1 rounded-xl bg-amber-400 px-5 py-3 font-semibold text-slate-950">Add to cart · ${product.price * qty}</button></div></div></div><div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6"><h2 className="font-medium text-white">Specifications</h2><div className="mt-4 grid gap-3 sm:grid-cols-3">{product.specs.map(spec => <div key={spec} className="rounded-xl bg-slate-950 p-4 text-sm text-slate-400"><Check className="mb-2 text-emerald-400" size={16} />{spec}</div>)}</div></div></div> }
//
// function Checkout({ cart, setCart, setView, notify }) { const [step, setStep] = useState(0); const [shipping, setShipping] = useState({ name: '', address: '', city: '', zip: '' }); const [payment, setPayment] = useState('card'); const total = cart.reduce((sum, item) => sum + item.price * item.qty, 0); if (cart.length === 0) return <div className="space-y-6"><h1 className="text-3xl font-semibold text-white">Cart & checkout</h1><div className="rounded-2xl border border-dashed border-slate-700 p-12 text-center"><ShoppingCart className="mx-auto text-slate-600" size={42} /><p className="mt-4 text-slate-400">Your cart is waiting for a good deal.</p><button onClick={() => setView('products')} className="mt-5 rounded-xl bg-amber-400 px-5 py-3 text-sm font-semibold text-slate-950">Shop products</button></div></div>; if (step === 3) return <div className="mx-auto max-w-xl rounded-3xl border border-emerald-400/20 bg-emerald-400/5 p-10 text-center"><div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-emerald-400 text-slate-950"><Check size={32} /></div><h1 className="mt-6 text-3xl font-semibold text-white">Order confirmed</h1><p className="mt-3 leading-7 text-slate-400">Order #FS-10504 is being prepared. A receipt has been sent to your inbox.</p><button onClick={() => { setCart([]); setView('orders') }} className="mt-7 rounded-xl bg-amber-400 px-5 py-3 font-semibold text-slate-950">Track order</button></div>; return <div className="space-y-6"><div><p className="text-sm font-medium text-amber-300">Secure checkout</p><h1 className="mt-1 text-3xl font-semibold text-white">Cart & checkout</h1></div><div className="flex gap-2">{['Cart', 'Shipping', 'Payment', 'Complete'].map((label, i) => <div key={label} className={`flex-1 border-b-2 pb-3 text-xs ${i <= step ? 'border-amber-400 text-amber-300' : 'border-slate-800 text-slate-600'}`}>{i + 1}. {label}</div>)}</div>{step === 0 && <div className="grid gap-6 lg:grid-cols-[1.4fr_0.8fr]"><div className="space-y-3">{cart.map(item => <div key={item.id} className="flex items-center gap-4 rounded-2xl border border-slate-800 bg-slate-900/70 p-4"><div className={`h-16 w-16 rounded-xl bg-gradient-to-br ${item.color} flex items-center justify-center`}><Box size={26} className="text-white" /></div><div className="flex-1"><p className="font-medium text-white">{item.name}</p><p className="text-sm text-amber-300">${item.price} · Qty {item.qty}</p></div><button onClick={() => setCart(cart.filter(x => x.id !== item.id))} className="text-slate-500 hover:text-rose-300"><Trash2 size={17} /></button></div>)}</div><Summary total={total} action="Continue to shipping" onAction={() => setStep(1)} /></div>}{step === 1 && <div className="max-w-2xl rounded-2xl border border-slate-800 bg-slate-900/70 p-6"><h2 className="font-medium text-white">Shipping address</h2><div className="mt-5 grid gap-4 sm:grid-cols-2">{[['name', 'Full name'], ['address', 'Street address'], ['city', 'City'], ['zip', 'ZIP code']].map(([key, label]) => <input key={key} value={shipping[key]} onChange={e => setShipping({ ...shipping, [key]: e.target.value })} placeholder={label} className="rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-sm text-white outline-none focus:border-amber-400" />)}</div><button onClick={() => shipping.name && shipping.address && setStep(2)} className="mt-6 rounded-xl bg-amber-400 px-5 py-3 font-semibold text-slate-950">Continue to payment</button></div>}{step === 2 && <div className="max-w-2xl rounded-2xl border border-slate-800 bg-slate-900/70 p-6"><h2 className="font-medium text-white">Payment method</h2><div className="mt-5 grid gap-3 sm:grid-cols-2"><button onClick={() => setPayment('card')} className={`rounded-xl border p-4 text-left ${payment === 'card' ? 'border-amber-400 bg-amber-400/10' : 'border-slate-700'}`}><CreditCard className="text-amber-300" size={20} /><p className="mt-2 text-sm text-white">Card ending in 4242</p><p className="text-xs text-slate-500">Visa · Default</p></button><button onClick={() => setPayment('wallet')} className={`rounded-xl border p-4 text-left ${payment === 'wallet' ? 'border-amber-400 bg-amber-400/10' : 'border-slate-700'}`}><CircleDollarSign className="text-emerald-300" size={20} /><p className="mt-2 text-sm text-white">Store wallet</p><p className="text-xs text-slate-500">$120.00 available</p></button></div><button onClick={() => { setStep(3); notify('Payment authorized successfully') }} className="mt-6 rounded-xl bg-amber-400 px-5 py-3 font-semibold text-slate-950">Place order · ${total.toFixed(2)}</button></div>}</div> }
// function Summary({ total, action, onAction }) { return <div className="h-fit rounded-2xl border border-slate-800 bg-slate-900/70 p-6"><p className="font-medium text-white">Order summary</p><div className="mt-5 space-y-3 text-sm"><div className="flex justify-between text-slate-400"><span>Subtotal</span><span>${total.toFixed(2)}</span></div><div className="flex justify-between text-slate-400"><span>Shipping</span><span className="text-emerald-400">Free</span></div><div className="border-t border-slate-800 pt-3 flex justify-between text-lg font-semibold text-white"><span>Total</span><span>${total.toFixed(2)}</span></div></div><button onClick={onAction} className="mt-6 w-full rounded-xl bg-amber-400 py-3 font-semibold text-slate-950">{action}</button></div> }
//
// function Orders() { return <div className="space-y-6"><div><p className="text-sm font-medium text-amber-300">Fulfillment center</p><h1 className="mt-1 text-3xl font-semibold text-white">Orders</h1></div><div className="overflow-hidden rounded-2xl border border-slate-800 bg-slate-900/70">{[['#FS-10482', 'AeroPods Max', '$249.00', 'In transit', 'text-cyan-300'], ['#FS-10479', 'Velocity Runner', '$89.00', 'Delivered', 'text-emerald-300'], ['#FS-10465', 'Nova Camera Kit', '$599.00', 'Processing', 'text-amber-300']].map(order => <div key={order[0]} className="grid gap-3 border-b border-slate-800 px-5 py-5 last:border-0 md:grid-cols-5 md:items-center"><div><span className="text-xs text-slate-500">Order</span><p className="font-medium text-white">{order[0]}</p></div><div><span className="text-xs text-slate-500">Items</span><p className="text-sm text-slate-300">{order[1]}</p></div><div><span className="text-xs text-slate-500">Total</span><p className="text-sm text-white">{order[2]}</p></div><div><span className="text-xs text-slate-500">Status</span><p className={`text-sm ${order[4]}`}>{order[3]}</p></div><p className="text-xs text-slate-500">1h ago</p></div>)}</div><div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6"><p className="font-medium text-white">Tracking timeline · #FS-10482</p><div className="mt-6 flex flex-col gap-5 md:flex-row md:items-center md:justify-between">{['Order placed', 'Packed', 'In transit', 'Delivered'].map((label, i) => <div key={label} className="flex items-center gap-3 md:flex-col md:gap-2"><span className={`flex h-8 w-8 items-center justify-center rounded-full ${i < 3 ? 'bg-emerald-400 text-slate-950' : 'bg-slate-800 text-slate-500'}`}>{i < 3 ? <Check size={15} /> : i + 1}</span><span className="text-sm text-slate-400">{label}</span></div>)}</div></div></div> }
//
// function Notifications({ notify }) { const [read, setRead] = useState(false); return <div className="space-y-6"><div className="flex items-end justify-between"><div><p className="text-sm font-medium text-amber-300">Stay in the loop</p><h1 className="mt-1 text-3xl font-semibold text-white">Notifications</h1></div><button onClick={() => { setRead(true); notify('All notifications marked as read') }} className="text-xs text-slate-400 hover:text-white">Mark all read</button></div><div className="space-y-3">{[['Flash sale is live', 'Cyber Sprint just started. 3 items are moving fast.', '2 min ago', Zap, 'amber'], ['Order #FS-10482 shipped', 'Your AeroPods Max are on the way.', '1 hour ago', Truck, 'cyan'], ['Weekly performance report', 'Revenue is up 24.6% compared to last week.', 'Yesterday', TrendingUp, 'emerald']].map(([title, body, time, Icon, tone], i) => <div key={title} className={`flex gap-4 rounded-2xl border p-5 ${!read && i === 0 ? 'border-amber-400/30 bg-amber-400/5' : 'border-slate-800 bg-slate-900/60'}`}><span className={`rounded-xl p-3 ${tone === 'amber' ? 'bg-amber-400/10 text-amber-300' : tone === 'cyan' ? 'bg-cyan-400/10 text-cyan-300' : 'bg-emerald-400/10 text-emerald-300'}`}><Icon size={18} /></span><div className="flex-1"><div className="flex justify-between gap-4"><p className="font-medium text-white">{title}</p><span className="shrink-0 text-xs text-slate-600">{time}</span></div><p className="mt-1 text-sm text-slate-400">{body}</p></div>{!read && i === 0 && <span className="mt-2 h-2 w-2 rounded-full bg-amber-400" />}</div>)}</div></div> }
//
// function Analytics({ range, setRange }) { const data = range === '7d' ? [42, 58, 48, 74, 66, 88, 82] : range === '30d' ? [32, 48, 42, 68, 54, 76, 64, 92, 72, 86] : [22, 38, 30, 52, 44, 66, 58, 80, 70, 96]; return <div className="space-y-6"><div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end"><div><p className="text-sm font-medium text-amber-300">Operations</p><h1 className="mt-1 text-3xl font-semibold text-white">Admin console</h1></div><div className="flex rounded-xl border border-slate-800 bg-slate-900 p-1">{[['7d', '7 days'], ['30d', '30 days'], ['90d', '90 days']].map(([value, label]) => <button key={value} onClick={() => setRange(value)} className={`rounded-lg px-3 py-2 text-xs ${range === value ? 'bg-amber-400 text-slate-950' : 'text-slate-400'}`}>{label}</button>)}</div></div><div className="grid gap-4 md:grid-cols-3"><Metric icon={Activity} label="API throughput" value="18.4k req/s" detail="Healthy · 99.99% uptime" tone="cyan" /><Metric icon={Users} label="Concurrent users" value="8,492" detail="Peak: 12,104" tone="emerald" /><Metric icon={CircleDollarSign} label="GMV in range" value={range === '7d' ? '$84,290' : range === '30d' ? '$312,840' : '$908,120'} detail="+31.2% vs. previous" /></div><div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6"><div className="flex items-center justify-between"><div><p className="font-medium text-white">Revenue trend</p><p className="mt-1 text-sm text-slate-500">Gross merchandise value · {range}</p></div><TrendingUp className="text-emerald-400" size={18} /></div><div className="mt-8 flex h-56 items-end gap-2 border-b border-slate-800">{data.map((height, i) => <div key={i} className="group flex flex-1 flex-col justify-end"><div style={{ height: `${height}%` }} className="rounded-t-lg bg-gradient-to-t from-amber-500 to-amber-300 transition group-hover:from-cyan-400 group-hover:to-cyan-300" /></div>)}</div><div className="mt-3 flex justify-between text-xs text-slate-600"><span>Earlier</span><span>Today</span></div></div><div className="grid gap-6 lg:grid-cols-2"><div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6"><p className="font-medium text-white">Top products</p><div className="mt-5 space-y-4">{products.slice(0, 4).map((p, i) => <div key={p.id} className="flex items-center gap-3"><span className="w-5 text-xs text-slate-600">0{i + 1}</span><div className={`h-9 w-9 rounded-lg bg-gradient-to-br ${p.color}`} /><div className="flex-1"><div className="flex justify-between text-sm"><span className="text-white">{p.name}</span><span className="text-slate-400">{p.sold * 12} sold</span></div><div className="mt-2 h-1.5 rounded-full bg-slate-800"><div style={{ width: `${p.sold}%` }} className="h-full rounded-full bg-amber-400" /></div></div></div>)}</div></div><div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6"><p className="font-medium text-white">System events</p><div className="mt-5 space-y-4 text-sm">{['Stock sync completed across 24 warehouses', 'Rate limit increased for sale traffic', 'Payment processor latency normalized', 'New admin signed into console'].map((event, i) => <div key={event} className="flex gap-3"><span className={`mt-1 h-2 w-2 rounded-full ${i === 1 ? 'bg-amber-400' : 'bg-emerald-400'}`} /><p className="text-slate-300">{event}</p></div>)}</div></div></div></div> }
//
// function SettingsView({ notify }) { const [tab, setTab] = useState('store'); const [saved, setSaved] = useState(false); return <div className="space-y-6"><div><p className="text-sm font-medium text-amber-300">Workspace control</p><h1 className="mt-1 text-3xl font-semibold text-white">Settings</h1></div><div className="grid gap-6 lg:grid-cols-[220px_1fr]"><div className="space-y-2">{[['store', Settings, 'Store preferences'], ['team', Users, 'Team permissions'], ['api', KeyRound, 'API keys'], ['webhooks', Webhook, 'Webhooks']].map(([id, Icon, label]) => <button key={id} onClick={() => setTab(id)} className={`flex w-full items-center gap-3 rounded-xl px-3 py-3 text-left text-sm ${tab === id ? 'bg-amber-400 font-semibold text-slate-950' : 'text-slate-400 hover:bg-slate-900'}`}><Icon size={17} />{label}</button>)}</div><div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-6">{tab === 'store' && <><h2 className="font-medium text-white">Store preferences</h2><p className="mt-1 text-sm text-slate-500">Control how your storefront behaves during drops.</p><div className="mt-6 space-y-4">{['Enable low-stock alerts', 'Auto-pause sold out products', 'Require address verification'].map((label, i) => <label key={label} className="flex items-center justify-between rounded-xl border border-slate-800 p-4 text-sm text-slate-300"><span>{label}</span><input type="checkbox" defaultChecked={i !== 2} className="h-4 w-4 accent-amber-400" /></label>)}</div></>}{tab === 'team' && <><h2 className="font-medium text-white">Team permissions</h2><div className="mt-5 space-y-3">{[['Alex Kim', 'Owner', 'AK'], ['Mina Patel', 'Operations', 'MP'], ['Jordan Lee', 'Analyst', 'JL']].map(row => <div key={row[0]} className="flex items-center gap-3 rounded-xl border border-slate-800 p-4"><div className="flex h-9 w-9 items-center justify-center rounded-full bg-cyan-400/20 text-xs text-cyan-300">{row[2]}</div><div className="flex-1"><p className="text-sm text-white">{row[0]}</p><p className="text-xs text-slate-500">{row[1]}</p></div><ChevronDown size={16} className="text-slate-500" /></div>)}</div><button className="mt-5 flex items-center gap-2 text-sm text-amber-300"><UserPlus size={16} /> Invite teammate</button></>}{tab === 'api' && <><h2 className="font-medium text-white">API keys</h2><p className="mt-1 text-sm text-slate-500">Keep private keys secure and rotate them regularly.</p><div className="mt-6 flex items-center justify-between rounded-xl border border-slate-800 p-4"><div><p className="font-mono text-sm text-white">fse_live_••••••••••••9A42</p><p className="mt-1 text-xs text-slate-500">Created 12 days ago · Last used now</p></div><button onClick={() => notify('New API key generated')} className="rounded-lg bg-amber-400 px-3 py-2 text-xs font-semibold text-slate-950">Rotate</button></div></>}{tab === 'webhooks' && <><h2 className="font-medium text-white">Webhook integrations</h2><div className="mt-5 space-y-3">{['Order placed · Active', 'Inventory sync · Active', 'Refund issued · Paused'].map((label, i) => <div key={label} className="flex items-center justify-between rounded-xl border border-slate-800 p-4 text-sm text-slate-300"><span>{label}</span><span className={`rounded-full px-2 py-1 text-xs ${i === 2 ? 'bg-slate-800 text-slate-500' : 'bg-emerald-400/10 text-emerald-300'}`}>{i === 2 ? 'Paused' : 'Live'}</span></div>)}</div></>}<button onClick={() => { setSaved(true); notify('Settings saved successfully') }} className="mt-7 rounded-xl bg-amber-400 px-5 py-3 text-sm font-semibold text-slate-950">{saved ? 'Saved' : 'Save changes'}</button></div></div></div> }
//
// export default function Page() { const [view, setView] = useState('home'); const [cart, setCart] = useState([]); const [mobileOpen, setMobileOpen] = useState(false); const [detail, setDetail] = useState(null); const [toast, setToast] = useState(''); const [range, setRange] = useState('7d'); const notify = message => { setToast(message); window.setTimeout(() => setToast(''), 2800) }; const addToCart = product => { setCart(current => current.some(item => item.id === product.id) ? current.map(item => item.id === product.id ? { ...item, qty: item.qty + 1 } : item) : [...current, { ...product, qty: product.qty || 1 }]); notify(`${product.name} added to cart`) }; const openProduct = product => setDetail(product); const go = id => { setDetail(null); setView(id); setMobileOpen(false) }; const activeLabel = detail ? 'Product detail' : navItems.find(item => item[0] === view)?.[1]; let content; if (detail) content = <ProductDetail product={detail} onBack={() => setDetail(null)} onAdd={addToCart} notify={notify} />; else if (view === 'home') content = <Overview setView={go} onAdd={addToCart} onOpen={openProduct} />; else if (view === 'products') content = <Products onAdd={addToCart} onOpen={openProduct} />; else if (view === 'sale') content = <FlashSale onAdd={addToCart} onOpen={openProduct} />; else if (view === 'cart') content = <Checkout cart={cart} setCart={setCart} setView={go} notify={notify} />; else if (view === 'orders') content = <Orders />; else if (view === 'notifications') content = <Notifications notify={notify} />; else if (view === 'admin') content = <Analytics range={range} setRange={setRange} />; else content = <SettingsView notify={notify} />; return <main className="min-h-screen bg-slate-950 text-slate-100"><aside className={`fixed inset-y-0 left-0 z-40 flex w-72 flex-col border-r border-slate-800 bg-slate-950 p-5 transition-transform lg:translate-x-0 ${mobileOpen ? 'translate-x-0' : '-translate-x-full'}`}><div className="flex items-center justify-between"><div className="flex items-center gap-3"><div className="rounded-xl bg-amber-400 p-2 text-slate-950"><Zap size={19} fill="currentColor" /></div><span className="font-semibold tracking-tight text-white">FlashSale <span className="text-amber-300">Engine</span></span></div><button onClick={() => setMobileOpen(false)} className="text-slate-500 lg:hidden"><X /></button></div><div className="mt-10 space-y-1">{navItems.map(([id, label, Icon]) => <button key={id} onClick={() => go(id)} className={`flex w-full items-center gap-3 rounded-xl px-3 py-3 text-left text-sm transition ${view === id && !detail ? 'bg-amber-400 font-semibold text-slate-950' : 'text-slate-400 hover:bg-slate-900 hover:text-white'}`}><Icon size={18} /><span>{label}</span>{id === 'cart' && cart.length > 0 && <span className="ml-auto rounded-full bg-slate-950 px-2 py-0.5 text-[10px] text-amber-300">{cart.reduce((sum, item) => sum + item.qty, 0)}</span>}</button>)}</div><div className="mt-auto rounded-2xl border border-slate-800 bg-slate-900/70 p-4"><div className="flex items-center gap-2 text-xs text-emerald-300"><span className="h-2 w-2 animate-pulse rounded-full bg-emerald-400" /> All systems operational</div><p className="mt-3 text-xs leading-5 text-slate-500">Edge nodes are synced and ready for your next drop.</p></div><div className="mt-4 flex items-center gap-3 border-t border-slate-800 pt-4"><div className="flex h-9 w-9 items-center justify-center rounded-full bg-cyan-400/20 text-sm font-semibold text-cyan-300">AK</div><div className="flex-1"><p className="text-sm font-medium text-white">Alex Kim</p><p className="text-xs text-slate-500">Store owner</p></div><button onClick={() => go('settings')} aria-label="Open settings"><Settings size={16} className="text-slate-50" /></button></div></aside><div className="lg:pl-72"><header className="sticky top-0 z-30 flex h-20 items-center justify-between border-b border-slate-800/80 bg-slate-950/90 px-5 backdrop-blur md:px-8"><div className="flex items-center gap-3"><button onClick={() => setMobileOpen(true)} className="text-slate-400 lg:hidden"><Menu /></button><div><p className="text-xs uppercase tracking-[0.2em] text-slate-600">FlashSale Engine</p><h2 className="mt-1 text-sm font-medium text-white">{activeLabel}</h2></div></div><div className="flex items-center gap-3"><span className="hidden items-center gap-2 text-xs text-emerald-300 sm:flex"><span className="h-2 w-2 rounded-full bg-emerald-400" /> Live</span><button onClick={() => go('notifications')} className="rounded-xl border border-slate-800 p-2.5 text-slate-400 hover:text-white"><Bell size={17} /></button></div></header><div className="mx-auto max-w-7xl p-5 md:p-8">{content}</div></div>{toast && <div role="status" className="fixed bottom-6 right-6 z-50 flex items-center gap-3 rounded-xl border border-amber-400/30 bg-slate-900 px-4 py-3 text-sm text-white shadow-2xl"><Check size={17} className="text-emerald-400" />{toast}<button onClick={() => setToast('')} className="text-slate-500"><X size={15} /></button></div>}</main> }



'use client'

import { useEffect, useMemo, useState } from 'react'
import api from '@/lib/api'


import {
  Activity, Bell, Box, Check, ChevronDown, ChevronLeft, CircleDollarSign, Clock3,
  CreditCard, Filter, Home, KeyRound, LayoutDashboard, Menu, Package, Plus,
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

const navItems = [
  ['home', 'Overview', Home],
  ['products', 'Products', Package],
  ['sale', 'Flash Sale', Zap],
  ['orders', 'Orders', ShoppingBag],
  ['cart', 'Cart & Checkout', ShoppingCart],
  ['notifications', 'Notifications', Bell],
  ['admin', 'Admin Console', LayoutDashboard],
  ['settings', 'Settings', Settings]
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
  return (
      <article
          onClick={() => onOpen(product)}
          className="group cursor-pointer overflow-hidden rounded-2xl border border-slate-800 bg-slate-900/80 transition hover:-translate-y-1 hover:border-amber-400/50"
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
        </div>

        <div className="p-4">
          <p className="text-xs text-slate-500">{product.category}</p>
          <h3 className="mt-1 font-medium text-white">{product.name}</h3>

          <div className="mt-4 flex items-end justify-between">
            <div>
            <span className="text-lg font-semibold text-amber-300">
              ${product.price}
            </span>

              <span className="ml-2 text-xs text-slate-600 line-through">
              ${product.original}
            </span>
            </div>

            <button
                onClick={(e) => {
                  e.stopPropagation()
                  onAdd(product)
                }}
                className="rounded-lg bg-amber-400 p-2 text-slate-950 transition hover:bg-amber-300"
                aria-label={`Add ${product.name} to cart`}
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

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        setLoading(true)
        setError('')

        const response = await api.get('/products')
        const backendProducts = response.data?.data?.content || []

        const colors = [
          'from-amber-400 to-orange-600',
          'from-cyan-400 to-blue-600',
          'from-fuchsia-400 to-violet-600',
          'from-emerald-400 to-teal-600',
          'from-rose-400 to-red-600',
          'from-yellow-300 to-amber-600',
        ]

        const mappedProducts = backendProducts.map((product, index) => ({
          id: product.id,
          name: product.title,
          category: product.category || 'Flash Sale',
          price: Number(product.flashSalePrice ?? product.originalPrice ?? 0),
          original: Number(product.originalPrice ?? product.flashSalePrice ?? 0),
          stock: Number(product.initialStock ?? 0),
          sold: 0,
          color: colors[index % colors.length],
          description: product.description || '',
          imageUrl: product.imageUrl || '',
          startTime: product.startTime,
          endTime: product.endTime,
          status: product.status,
          saleActive: product.saleActive,
          specs: [
            product.saleActive ? 'Flash sale is currently active' : 'Flash sale product',
            `Available stock: ${Number(product.initialStock ?? 0)}`,
            product.status || 'AVAILABLE',
          ],
          variants: {
            Color: ['Default'],
            Size: ['Standard'],
          },
        }))

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

    fetchProducts()
  }, [])

  const cats = ['All', ...new Set(products.map((p) => p.category))]

  const filtered = products.filter(
      (p) =>
          p.name.toLowerCase().includes(query.toLowerCase()) &&
          (category === 'All' || p.category === category)
  )

  return (
      <div className="space-y-6">
        <div className="flex flex-col justify-between gap-4 md:flex-row md:items-end">
          <div>
            <p className="text-sm font-medium text-amber-300">Live catalog</p>
            <h1 className="mt-1 text-3xl font-semibold text-white">Products</h1>
            <p className="mt-1 text-sm text-slate-500">
              Products loaded from the Spring Boot backend
            </p>
          </div>

          <div className="relative">
            <Search
                size={16}
                className="absolute left-3 top-3 text-slate-500"
            />

            <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search products"
                className="w-full rounded-xl border border-slate-700 bg-slate-900 py-2.5 pl-9 pr-4 text-sm text-white outline-none focus:border-amber-400 md:w-64"
            />
          </div>
        </div>

        {loading && (
            <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-8 text-center text-slate-400">
              Loading products from backend...
            </div>
        )}

        {error && !loading && (
            <div className="rounded-2xl border border-rose-400/20 bg-rose-400/5 p-6">
              <p className="font-medium text-rose-300">Failed to load products</p>
              <p className="mt-2 text-sm text-slate-400">{error}</p>
              <p className="mt-2 text-xs text-slate-500">
                Check that the backend and API Gateway are running.
              </p>
            </div>
        )}

        {!loading && !error && products.length === 0 && (
            <div className="rounded-2xl border border-dashed border-slate-700 p-12 text-center">
              <Package className="mx-auto text-slate-600" size={42} />
              <p className="mt-4 text-slate-400">No products found.</p>
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
                    <Search className="mx-auto text-slate-600" size={38} />
                    <p className="mt-4 text-slate-400">No matching products.</p>
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
  return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-sm font-medium text-amber-300">Live event</p>
            <h1 className="mt-1 text-3xl font-semibold text-white">
              Cyber Sprint
            </h1>
          </div>

          <span className="flex items-center gap-2 rounded-full bg-rose-400/10 px-3 py-2 text-xs font-semibold text-rose-300">
          <span className="h-2 w-2 animate-pulse rounded-full bg-rose-400" />
          2,841 watching
        </span>
        </div>

        <div className="rounded-3xl border border-amber-400/25 bg-amber-400/10 p-6 md:p-8">
          <p className="text-sm text-amber-200/70">Sale ends in</p>

          <div className="mt-2 flex gap-2 text-3xl font-semibold text-white md:text-5xl">
            {[
              ['02', 'H'],
              ['41', 'M'],
              ['18', 'S']
            ].map(([n, l]) => (
                <div key={l}>
              <span className="rounded-lg bg-slate-950/70 px-3 py-2 text-amber-300">
                {n}
              </span>
                  <small className="ml-1 text-xs text-slate-400">{l}</small>
                </div>
            ))}
          </div>
        </div>

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {products.slice(0, 3).map((p) => (
              <div key={p.id}>
                <ProductCard product={p} onAdd={onAdd} onOpen={onOpen} />

                <div className="mt-3 px-1">
                  <div className="mb-2 flex justify-between text-xs">
                    <span className="text-slate-500">{p.stock} left</span>
                    <span className="text-amber-300">{p.sold}% claimed</span>
                  </div>

                  <div className="h-2 overflow-hidden rounded-full bg-slate-800">
                    <div
                        style={{ width: `${p.sold}%` }}
                        className="h-full rounded-full bg-amber-400"
                    />
                  </div>
                </div>
              </div>
          ))}
        </div>
      </div>
  )
}
// function ProductDetail({ product, onBack, onAdd, notify }) {
//   const [image, setImage] = useState(0)
//   const [qty, setQty] = useState(1)
//
//   const [productData, setProductData] = useState(product)
//   const [loading, setLoading] = useState(true)
//   const [error, setError] = useState('')
//
//   const [selected, setSelected] = useState({
//     Color: 'Default',
//     Size: 'Standard'
//   })
//
//   useEffect(() => {
//     const fetchProductDetails = async () => {
//       try {
//         setLoading(true)
//         setError('')
//
//         console.log(`Fetching product details for ID: ${product.id}`)
//
//         const response = await api.get(`/products/${product.id}`)
//
//         console.log('Product details response:', response.data)
//
//         const backendProduct = response.data?.data
//
//         if (!backendProduct) {
//           throw new Error('Product details were not returned by the backend.')
//         }
//
//         const colors = [
//           'from-amber-400 to-orange-600',
//           'from-cyan-400 to-blue-600',
//           'from-fuchsia-400 to-violet-600',
//           'from-emerald-400 to-teal-600',
//           'from-rose-400 to-red-600',
//           'from-yellow-300 to-amber-600'
//         ]
//
//         const mappedProduct = {
//           id: backendProduct.id,
//
//           name: backendProduct.title,
//
//           category:
//               backendProduct.category ||
//               product.category ||
//               'Flash Sale',
//
//           price: Number(
//               backendProduct.flashSalePrice ??
//               backendProduct.originalPrice ??
//               0
//           ),
//
//           original: Number(
//               backendProduct.originalPrice ??
//               backendProduct.flashSalePrice ??
//               0
//           ),
//
//           stock: Number(
//               backendProduct.initialStock ?? 0
//           ),
//
//           color:
//               product.color ||
//               colors[Number(backendProduct.id) % colors.length],
//
//           description:
//               backendProduct.description || '',
//
//           imageUrl:
//               backendProduct.imageUrl || '',
//
//           startTime:
//           backendProduct.startTime,
//
//           endTime:
//           backendProduct.endTime,
//
//           status:
//           backendProduct.status,
//
//           saleActive:
//           backendProduct.saleActive,
//
//           specs: [
//             backendProduct.saleActive
//                 ? 'Flash sale is currently active'
//                 : 'Flash sale product',
//
//             `Available stock: ${Number(
//                 backendProduct.initialStock ?? 0
//             )}`,
//
//             backendProduct.status || 'AVAILABLE'
//           ],
//
//           variants: {
//             Color: ['Default'],
//             Size: ['Standard']
//           },
//
//           sold: 0
//         }
//
//         setProductData(mappedProduct)
//
//         setSelected({
//           Color: mappedProduct.variants.Color[0],
//           Size: mappedProduct.variants.Size[0]
//         })
//       } catch (err) {
//         console.error('Product details API error:', err)
//
//         setError(
//             err.response?.data?.message ||
//             err.message ||
//             'Unable to load product details.'
//         )
//       } finally {
//         setLoading(false)
//       }
//     }
//
//     if (product?.id) {
//       fetchProductDetails()
//     }
//   }, [product?.id])
//
//   if (loading) {
//     return (
//         <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-12 text-center">
//           <div className="mx-auto h-10 w-10 animate-spin rounded-full border-2 border-slate-700 border-t-amber-400" />
//
//           <p className="mt-5 text-slate-400">
//             Loading product details from backend...
//           </p>
//         </div>
//     )
//   }
//
//   if (error) {
//     return (
//         <div className="space-y-5">
//           <button
//               onClick={onBack}
//               className="flex items-center gap-2 text-sm text-slate-400 hover:text-white"
//           >
//             <ChevronLeft size={17} />
//             Back to catalog
//           </button>
//
//           <div className="rounded-2xl border border-rose-400/20 bg-rose-400/5 p-8">
//             <h2 className="text-lg font-semibold text-rose-300">
//               Failed to load product
//             </h2>
//
//             <p className="mt-2 text-sm text-slate-400">
//               {error}
//             </p>
//
//             <p className="mt-2 text-xs text-slate-500">
//               Product ID: {product?.id}
//             </p>
//           </div>
//         </div>
//     )
//   }
//
//   const currentProduct = productData
//
//   const discount =
//       currentProduct.original > 0
//           ? Math.round(
//               (1 -
//                   currentProduct.price /
//                   currentProduct.original) *
//               100
//           )
//           : 0
//
//   return (
//       <div className="space-y-6">
//
//         {/* Back */}
//         <button
//             onClick={onBack}
//             className="flex items-center gap-2 text-sm text-slate-400 hover:text-white"
//         >
//           <ChevronLeft size={17} />
//           Back to catalog
//         </button>
//
//         <div className="grid gap-8 lg:grid-cols-2">
//
//           {/* Product image */}
//           <div>
//
//             <div
//                 className={`flex h-96 items-center justify-center rounded-3xl bg-gradient-to-br ${currentProduct.color}`}
//             >
//               <Box
//                   size={150}
//                   className="text-white/80"
//                   strokeWidth={1}
//               />
//             </div>
//
//             {/* Image thumbnails */}
//             <div className="mt-3 grid grid-cols-3 gap-3">
//
//               {[0, 1, 2].map((i) => (
//                   <button
//                       key={i}
//                       onClick={() => setImage(i)}
//                       className={`h-20 rounded-xl bg-gradient-to-br ${
//                           currentProduct.color
//                       } ${
//                           image === i
//                               ? 'ring-2 ring-amber-400'
//                               : 'opacity-60'
//                       }`}
//                   >
//                     <Box
//                         size={24}
//                         className="mx-auto text-white"
//                     />
//                   </button>
//               ))}
//
//             </div>
//           </div>
//
//           {/* Product information */}
//           <div>
//
//             <p className="text-sm text-amber-300">
//               {currentProduct.category} / Limited drop
//             </p>
//
//             <h1 className="mt-2 text-4xl font-semibold text-white">
//               {currentProduct.name}
//             </h1>
//
//             {/* Price */}
//             <div className="mt-4 flex flex-wrap items-center gap-3">
//
//             <span className="text-3xl font-semibold text-amber-300">
//               ₹{currentProduct.price.toLocaleString('en-IN')}
//             </span>
//
//               {currentProduct.original >
//                   currentProduct.price && (
//                       <>
//                 <span className="text-slate-600 line-through">
//                   ₹{currentProduct.original.toLocaleString(
//                     'en-IN'
//                 )}
//                 </span>
//
//                         <span className="rounded-full bg-emerald-400/10 px-2 py-1 text-xs text-emerald-300">
//                   {discount}% OFF
//                 </span>
//                       </>
//                   )}
//
//               <span className="rounded-full bg-rose-400/10 px-2 py-1 text-xs text-rose-300">
//               {currentProduct.stock} left
//             </span>
//
//             </div>
//
//             {/* Description */}
//             {currentProduct.description && (
//                 <p className="mt-5 leading-7 text-slate-400">
//                   {currentProduct.description}
//                 </p>
//             )}
//
//             {/* Flash sale status */}
//             <div className="mt-6 rounded-2xl border border-amber-400/25 bg-amber-400/10 p-4">
//
//               <div className="flex items-center gap-2 text-sm text-amber-200">
//                 <Clock3 size={16} />
//
//                 {currentProduct.saleActive
//                     ? 'Flash sale is currently active'
//                     : 'Flash sale product'}
//               </div>
//
//               {currentProduct.endTime && (
//                   <p className="mt-2 text-sm text-slate-400">
//                     Sale ends:{' '}
//                     {new Date(
//                         currentProduct.endTime
//                     ).toLocaleString()}
//                   </p>
//               )}
//
//             </div>
//
//             {/* Variants */}
//             <div className="mt-7 space-y-5">
//
//               {Object.entries(
//                   currentProduct.variants
//               ).map(([key, values]) => (
//                   <div key={key}>
//
//                     <p className="mb-2 text-sm font-medium text-white">
//                       {key}
//                     </p>
//
//                     <div className="flex flex-wrap gap-2">
//
//                       {values.map((value) => (
//                           <button
//                               key={value}
//                               onClick={() =>
//                                   setSelected({
//                                     ...selected,
//                                     [key]: value
//                                   })
//                               }
//                               className={`rounded-lg border px-3 py-2 text-sm ${
//                                   selected[key] === value
//                                       ? 'border-amber-400 bg-amber-400 text-slate-950'
//                                       : 'border-slate-700 text-slate-400'
//                               }`}
//                           >
//                             {value}
//                           </button>
//                       ))}
//
//                     </div>
//                   </div>
//               ))}
//
//             </div>
//
//             {/* Quantity + Cart */}
//             <div className="mt-7 flex gap-3">
//
//               <div className="flex items-center rounded-xl border border-slate-700">
//
//                 <button
//                     onClick={() =>
//                         setQty(Math.max(1, qty - 1))
//                     }
//                     className="px-4 py-3 text-white"
//                 >
//                   −
//                 </button>
//
//                 <span className="w-8 text-center text-white">
//                 {qty}
//               </span>
//
//                 <button
//                     onClick={() =>
//                         setQty(
//                             Math.min(
//                                 currentProduct.stock || 1,
//                                 qty + 1
//                             )
//                         )
//                     }
//                     className="px-4 py-3 text-white"
//                 >
//                   +
//                 </button>
//
//               </div>
//
//               <button
//                   disabled={currentProduct.stock <= 0}
//                   onClick={() => {
//                     onAdd({
//                       ...currentProduct,
//                       selected,
//                       qty
//                     })
//
//                     notify(
//                         `${currentProduct.name} added to cart`
//                     )
//                   }}
//                   className="flex-1 rounded-xl bg-amber-400 px-5 py-3 font-semibold text-slate-950 disabled:cursor-not-allowed disabled:opacity-50"
//               >
//                 {currentProduct.stock <= 0
//                     ? 'Out of stock'
//                     : `Add to cart · ₹${(
//                         currentProduct.price * qty
//                     ).toLocaleString('en-IN')}`}
//               </button>
//
//             </div>
//           </div>
//         </div>
//
//         {/* Specifications */}
//         <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">
//
//           <h2 className="font-medium text-white">
//             Product information
//           </h2>
//
//           <div className="mt-4 grid gap-3 sm:grid-cols-3">
//
//             {currentProduct.specs.map((spec) => (
//                 <div
//                     key={spec}
//                     className="rounded-xl bg-slate-950 p-4 text-sm text-slate-400"
//                 >
//                   <Check
//                       className="mb-2 text-emerald-400"
//                       size={16}
//                   />
//
//                   {spec}
//                 </div>
//             ))}
//
//           </div>
//
//         </div>
//
//       </div>
//   )
// }
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

  // ============================================
  // STEP 6 - INVENTORY API
  // ============================================
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
  }, [product?.id])

  // Use real inventory when available.
  // Fall back to product stock while loading/error occurs.
  const availableStock =
      inventory?.availableStock ??
      product.stock ??
      0

  const lockedStock =
      inventory?.lockedStock ?? 0

  const totalStock =
      inventory?.totalStock ?? product.stock ?? 0

  const discount =
      product.original > 0
          ? Math.round(
              (1 - product.price / product.original) * 100
          )
          : 0

  return (
      <div className="space-y-6">

        {/* Back */}
        <button
            onClick={onBack}
            className="flex items-center gap-2 text-sm text-slate-400 hover:text-white"
        >
          <ChevronLeft size={17} />
          Back to catalog
        </button>

        <div className="grid gap-8 lg:grid-cols-2">

          {/* Product image */}
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

          {/* Product information */}
          <div>

            <p className="text-sm text-amber-300">
              {product.category} / Limited drop
            </p>

            <h1 className="mt-2 text-4xl font-semibold text-white">
              {product.name}
            </h1>

            {/* Price */}
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

              {/* REAL INVENTORY */}
              <span className="rounded-full bg-rose-400/10 px-2 py-1 text-xs text-rose-300">
              {inventoryLoading
                  ? 'Checking stock...'
                  : `${availableStock} available`}
            </span>

            </div>

            {/* Description */}
            {product.description && (
                <p className="mt-5 leading-7 text-slate-400">
                  {product.description}
                </p>
            )}

            {/* Inventory information */}
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
                    <span className="flex items-center gap-2 text-xs text-emerald-300">
                  <span className="h-2 w-2 rounded-full bg-emerald-400" />
                  Live
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

            {/* Flash sale */}
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

            {/* Variants */}
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
                              className={`rounded-lg border px-3 py-2 text-sm ${
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

            {/* Quantity + Add to Cart */}
            <div className="mt-7 flex gap-3">

              <div className="flex items-center rounded-xl border border-slate-700">

                <button
                    onClick={() =>
                        setQty(Math.max(1, qty - 1))
                    }
                    className="px-4 py-3 text-white"
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
                    disabled={availableStock <= 0}
                    className="px-4 py-3 text-white disabled:opacity-40"
                >
                  +
                </button>

              </div>

              <button
                  disabled={
                      inventoryLoading ||
                      availableStock <= 0
                  }
                  onClick={() => {

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
                    : availableStock <= 0
                        ? 'Out of stock'
                        : `Add to cart · ₹${(
                            product.price * qty
                        ).toLocaleString('en-IN')}`}
              </button>

            </div>

          </div>
        </div>

        {/* Specifications */}
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
// function Checkout({ cart, setCart, setView, notify }) {
//   const [step, setStep] = useState(0)
//
//   const [shipping, setShipping] = useState({
//     name: '',
//     address: '',
//     city: '',
//     zip: ''
//   })
//
//   const [payment, setPayment] = useState('card')
//
//   const total = cart.reduce(
//       (sum, item) => sum + item.price * item.qty,
//       0
//   )
//
//   if (cart.length === 0) {
//     return (
//         <div className="space-y-6">
//           <h1 className="text-3xl font-semibold text-white">
//             Cart & checkout
//           </h1>
//
//           <div className="rounded-2xl border border-dashed border-slate-700 p-12 text-center">
//             <ShoppingCart
//                 className="mx-auto text-slate-600"
//                 size={42}
//             />
//
//             <p className="mt-4 text-slate-400">
//               Your cart is waiting for a good deal.
//             </p>
//
//             <button
//                 onClick={() => setView('products')}
//                 className="mt-5 rounded-xl bg-amber-400 px-5 py-3 text-sm font-semibold text-slate-950"
//             >
//               Shop products
//             </button>
//           </div>
//         </div>
//     )
//   }
//
//   if (step === 3) {
//     return (
//         <div className="mx-auto max-w-xl rounded-3xl border border-emerald-400/20 bg-emerald-400/5 p-10 text-center">
//           <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-emerald-400 text-slate-950">
//             <Check size={32} />
//           </div>
//
//           <h1 className="mt-6 text-3xl font-semibold text-white">
//             Order confirmed
//           </h1>
//
//           <p className="mt-3 leading-7 text-slate-400">
//             Order #FS-10504 is being prepared. A receipt has been sent to your inbox.
//           </p>
//
//           <button
//               onClick={() => {
//                 setCart([])
//                 setView('orders')
//               }}
//               className="mt-7 rounded-xl bg-amber-400 px-5 py-3 font-semibold text-slate-950"
//           >
//             Track order
//           </button>
//         </div>
//     )
//   }
//
//   return (
//       <div className="space-y-6">
//         <div>
//           <p className="text-sm font-medium text-amber-300">
//             Secure checkout
//           </p>
//
//           <h1 className="mt-1 text-3xl font-semibold text-white">
//             Cart & checkout
//           </h1>
//         </div>
//
//         <div className="flex gap-2">
//           {['Cart', 'Shipping', 'Payment', 'Complete'].map(
//               (label, i) => (
//                   <div
//                       key={label}
//                       className={`flex-1 border-b-2 pb-3 text-xs ${
//                           i <= step
//                               ? 'border-amber-400 text-amber-300'
//                               : 'border-slate-800 text-slate-600'
//                       }`}
//                   >
//                     {i + 1}. {label}
//                   </div>
//               )
//           )}
//         </div>
//
//         {step === 0 && (
//             <div className="grid gap-6 lg:grid-cols-[1.4fr_0.8fr]">
//               <div className="space-y-3">
//                 {cart.map((item) => (
//                     <div
//                         key={item.id}
//                         className="flex items-center gap-4 rounded-2xl border border-slate-800 bg-slate-900/70 p-4"
//                     >
//                       <div
//                           className={`flex h-16 w-16 items-center justify-center rounded-xl bg-gradient-to-br ${item.color}`}
//                       >
//                         <Box size={26} className="text-white" />
//                       </div>
//
//                       <div className="flex-1">
//                         <p className="font-medium text-white">
//                           {item.name}
//                         </p>
//
//                         <p className="text-sm text-amber-300">
//                           ${item.price} · Qty {item.qty}
//                         </p>
//                       </div>
//
//                       <button
//                           onClick={() =>
//                               setCart(
//                                   cart.filter((x) => x.id !== item.id)
//                               )
//                           }
//                           className="text-slate-500 hover:text-rose-300"
//                       >
//                         <Trash2 size={17} />
//                       </button>
//                     </div>
//                 ))}
//               </div>
//
//               <Summary
//                   total={total}
//                   action="Continue to shipping"
//                   onAction={() => setStep(1)}
//               />
//             </div>
//         )}
//
//         {step === 1 && (
//             <div className="max-w-2xl rounded-2xl border border-slate-800 bg-slate-900/70 p-6">
//               <h2 className="font-medium text-white">
//                 Shipping address
//               </h2>
//
//               <div className="mt-5 grid gap-4 sm:grid-cols-2">
//                 {[
//                   ['name', 'Full name'],
//                   ['address', 'Street address'],
//                   ['city', 'City'],
//                   ['zip', 'ZIP code']
//                 ].map(([key, label]) => (
//                     <input
//                         key={key}
//                         value={shipping[key]}
//                         onChange={(e) =>
//                             setShipping({
//                               ...shipping,
//                               [key]: e.target.value
//                             })
//                         }
//                         placeholder={label}
//                         className="rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-sm text-white outline-none focus:border-amber-400"
//                     />
//                 ))}
//               </div>
//
//               <button
//                   onClick={() =>
//                       shipping.name &&
//                       shipping.address &&
//                       setStep(2)
//                   }
//                   className="mt-6 rounded-xl bg-amber-400 px-5 py-3 font-semibold text-slate-950"
//               >
//                 Continue to payment
//               </button>
//             </div>
//         )}
//
//         {step === 2 && (
//             <div className="max-w-2xl rounded-2xl border border-slate-800 bg-slate-900/70 p-6">
//               <h2 className="font-medium text-white">
//                 Payment method
//               </h2>
//
//               <div className="mt-5 grid gap-3 sm:grid-cols-2">
//                 <button
//                     onClick={() => setPayment('card')}
//                     className={`rounded-xl border p-4 text-left ${
//                         payment === 'card'
//                             ? 'border-amber-400 bg-amber-400/10'
//                             : 'border-slate-700'
//                     }`}
//                 >
//                   <CreditCard
//                       className="text-amber-300"
//                       size={20}
//                   />
//
//                   <p className="mt-2 text-sm text-white">
//                     Card ending in 4242
//                   </p>
//
//                   <p className="text-xs text-slate-500">
//                     Visa · Default
//                   </p>
//                 </button>
//
//                 <button
//                     onClick={() => setPayment('wallet')}
//                     className={`rounded-xl border p-4 text-left ${
//                         payment === 'wallet'
//                             ? 'border-amber-400 bg-amber-400/10'
//                             : 'border-slate-700'
//                     }`}
//                 >
//                   <CircleDollarSign
//                       className="text-emerald-300"
//                       size={20}
//                   />
//
//                   <p className="mt-2 text-sm text-white">
//                     Store wallet
//                   </p>
//
//                   <p className="text-xs text-slate-500">
//                     $120.00 available
//                   </p>
//                 </button>
//               </div>
//
//               <button
//                   onClick={() => {
//                     setStep(3)
//                     notify('Payment authorized successfully')
//                   }}
//                   className="mt-6 rounded-xl bg-amber-400 px-5 py-3 font-semibold text-slate-950"
//               >
//                 Place order · ${total.toFixed(2)}
//               </button>
//             </div>
//         )}
//       </div>
//   )
// }
// function Checkout({ cart, setCart, setView, notify }) {
//   const [step, setStep] = useState(0)
//
//   const [shipping, setShipping] = useState({
//     name: '',
//     address: '',
//     city: '',
//     zip: ''
//   })
//
//   const [payment, setPayment] = useState('card')
//
//   const [placingOrder, setPlacingOrder] = useState(false)
//   const [orderError, setOrderError] = useState('')
//   const [orderReference, setOrderReference] = useState('')
//
//   const total = cart.reduce(
//       (sum, item) => sum + item.price * item.qty,
//       0
//   )
//
//   // Create real order through Order Service
//   const handlePlaceOrder = async () => {
//     if (placingOrder || cart.length === 0) {
//       return
//     }
//
//     setPlacingOrder(true)
//     setOrderError('')
//
//     try {
//       const createdOrders = []
//
//       /*
//        * Current backend OrderCreateRequest supports
//        * one product per order.
//        *
//        * Therefore, create one order for each cart item.
//        */
//       for (const item of cart) {
//         const idempotencyKey = crypto.randomUUID()
//
//         const response = await api.post('/orders', {
//           productId: item.id,
//           quantity: item.qty,
//           idempotencyKey
//         })
//
//         const order = response.data?.data
//
//         if (order) {
//           createdOrders.push(order)
//         }
//       }
//
//       if (createdOrders.length === 0) {
//         throw new Error('Order was not created')
//       }
//
//       // Store the real backend order reference
//       if (createdOrders.length === 1) {
//         setOrderReference(createdOrders[0].orderReference)
//       } else {
//         setOrderReference(`${createdOrders.length} orders`)
//       }
//
//       notify('Order created successfully')
//
//       // Move to confirmation screen
//       setStep(3)
//
//     } catch (error) {
//       console.error('Create order error:', error)
//
//       const message =
//           error.response?.data?.message ||
//           error.message ||
//           'Unable to place order. Please try again.'
//
//       setOrderError(message)
//       notify(message)
//
//     } finally {
//       setPlacingOrder(false)
//     }
//   }
//
//   /*
//    * Empty cart
//    */
//   if (cart.length === 0) {
//     return (
//         <div className="space-y-6">
//           <h1 className="text-3xl font-semibold text-white">
//             Cart & checkout
//           </h1>
//
//           <div className="rounded-2xl border border-dashed border-slate-700 p-12 text-center">
//             <ShoppingCart
//                 className="mx-auto text-slate-600"
//                 size={42}
//             />
//
//             <p className="mt-4 text-slate-400">
//               Your cart is waiting for a good deal.
//             </p>
//
//             <button
//                 onClick={() => setView('products')}
//                 className="mt-5 rounded-xl bg-amber-400 px-5 py-3 text-sm font-semibold text-slate-950"
//             >
//               Shop products
//             </button>
//           </div>
//         </div>
//     )
//   }
//
//   /*
//    * Order confirmation
//    */
//   if (step === 3) {
//     return (
//         <div className="mx-auto max-w-xl rounded-3xl border border-emerald-400/20 bg-emerald-400/5 p-10 text-center">
//           <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-emerald-400 text-slate-950">
//             <Check size={32} />
//           </div>
//
//           <h1 className="mt-6 text-3xl font-semibold text-white">
//             Order confirmed
//           </h1>
//
//           <p className="mt-3 leading-7 text-slate-400">
//             Your order{' '}
//             {orderReference && (
//                 <span className="font-medium text-white">
//               #{orderReference}
//             </span>
//             )}{' '}
//             has been created successfully and is waiting for payment.
//           </p>
//
//           <button
//               onClick={() => {
//                 setCart([])
//                 setView('orders')
//               }}
//               className="mt-7 rounded-xl bg-amber-400 px-5 py-3 font-semibold text-slate-950"
//           >
//             Track order
//           </button>
//         </div>
//     )
//   }
//
//   /*
//    * Main checkout
//    */
//   return (
//       <div className="space-y-6">
//
//         {/* Header */}
//         <div>
//           <p className="text-sm font-medium text-amber-300">
//             Secure checkout
//           </p>
//
//           <h1 className="mt-1 text-3xl font-semibold text-white">
//             Cart & checkout
//           </h1>
//         </div>
//
//         {/* Checkout steps */}
//         <div className="flex gap-2">
//           {['Cart', 'Shipping', 'Payment', 'Complete'].map(
//               (label, i) => (
//                   <div
//                       key={label}
//                       className={`flex-1 border-b-2 pb-3 text-xs ${
//                           i <= step
//                               ? 'border-amber-400 text-amber-300'
//                               : 'border-slate-800 text-slate-600'
//                       }`}
//                   >
//                     {i + 1}. {label}
//                   </div>
//               )
//           )}
//         </div>
//
//         {/* =========================
//           STEP 0 - CART
//           ========================= */}
//         {step === 0 && (
//             <div className="grid gap-6 lg:grid-cols-[1.4fr_0.8fr]">
//
//               <div className="space-y-3">
//                 {cart.map((item) => (
//                     <div
//                         key={item.id}
//                         className="flex items-center gap-4 rounded-2xl border border-slate-800 bg-slate-900/70 p-4"
//                     >
//                       <div
//                           className={`flex h-16 w-16 items-center justify-center rounded-xl bg-gradient-to-br ${item.color}`}
//                       >
//                         <Box
//                             size={26}
//                             className="text-white"
//                         />
//                       </div>
//
//                       <div className="flex-1">
//                         <p className="font-medium text-white">
//                           {item.name}
//                         </p>
//
//                         <p className="text-sm text-amber-300">
//                           ${item.price} · Qty {item.qty}
//                         </p>
//                       </div>
//
//                       <button
//                           onClick={() =>
//                               setCart(
//                                   cart.filter(
//                                       (x) => x.id !== item.id
//                                   )
//                               )
//                           }
//                           className="text-slate-500 hover:text-rose-300"
//                       >
//                         <Trash2 size={17} />
//                       </button>
//                     </div>
//                 ))}
//               </div>
//
//               <Summary
//                   total={total}
//                   action="Continue to shipping"
//                   onAction={() => setStep(1)}
//               />
//             </div>
//         )}
//
//         {/* =========================
//           STEP 1 - SHIPPING
//           ========================= */}
//         {step === 1 && (
//             <div className="max-w-2xl rounded-2xl border border-slate-800 bg-slate-900/70 p-6">
//
//               <h2 className="font-medium text-white">
//                 Shipping address
//               </h2>
//
//               <div className="mt-5 grid gap-4 sm:grid-cols-2">
//                 {[
//                   ['name', 'Full name'],
//                   ['address', 'Street address'],
//                   ['city', 'City'],
//                   ['zip', 'ZIP code']
//                 ].map(([key, label]) => (
//                     <input
//                         key={key}
//                         value={shipping[key]}
//                         onChange={(e) =>
//                             setShipping({
//                               ...shipping,
//                               [key]: e.target.value
//                             })
//                         }
//                         placeholder={label}
//                         className="rounded-xl border border-slate-700 bg-slate-950 px-4 py-3 text-sm text-white outline-none focus:border-amber-400"
//                     />
//                 ))}
//               </div>
//
//               <button
//                   onClick={() => {
//                     if (
//                         shipping.name &&
//                         shipping.address
//                     ) {
//                       setStep(2)
//                     }
//                   }}
//                   className="mt-6 rounded-xl bg-amber-400 px-5 py-3 font-semibold text-slate-950"
//               >
//                 Continue to payment
//               </button>
//             </div>
//         )}
//
//         {/* =========================
//           STEP 2 - PAYMENT
//           ========================= */}
//         {step === 2 && (
//             <div className="max-w-2xl rounded-2xl border border-slate-800 bg-slate-900/70 p-6">
//
//               <h2 className="font-medium text-white">
//                 Payment method
//               </h2>
//
//               <div className="mt-5 grid gap-3 sm:grid-cols-2">
//
//                 {/* Card */}
//                 <button
//                     onClick={() => setPayment('card')}
//                     className={`rounded-xl border p-4 text-left ${
//                         payment === 'card'
//                             ? 'border-amber-400 bg-amber-400/10'
//                             : 'border-slate-700'
//                     }`}
//                 >
//                   <CreditCard
//                       className="text-amber-300"
//                       size={20}
//                   />
//
//                   <p className="mt-2 text-sm text-white">
//                     Card ending in 4242
//                   </p>
//
//                   <p className="text-xs text-slate-500">
//                     Visa · Default
//                   </p>
//                 </button>
//
//                 {/* Wallet */}
//                 <button
//                     onClick={() => setPayment('wallet')}
//                     className={`rounded-xl border p-4 text-left ${
//                         payment === 'wallet'
//                             ? 'border-amber-400 bg-amber-400/10'
//                             : 'border-slate-700'
//                     }`}
//                 >
//                   <CircleDollarSign
//                       className="text-emerald-300"
//                       size={20}
//                   />
//
//                   <p className="mt-2 text-sm text-white">
//                     Store wallet
//                   </p>
//
//                   <p className="text-xs text-slate-500">
//                     $120.00 available
//                   </p>
//                 </button>
//               </div>
//
//               {/* Backend error */}
//               {orderError && (
//                   <div className="mt-4 rounded-xl border border-rose-400/30 bg-rose-400/10 px-4 py-3 text-sm text-rose-300">
//                     {orderError}
//                   </div>
//               )}
//
//               {/* Place order */}
//               <button
//                   onClick={handlePlaceOrder}
//                   disabled={placingOrder}
//                   className="mt-6 rounded-xl bg-amber-400 px-5 py-3 font-semibold text-slate-950 disabled:cursor-not-allowed disabled:opacity-50"
//               >
//                 {placingOrder
//                     ? 'Creating order...'
//                     : `Place order · $${total.toFixed(2)}`}
//               </button>
//             </div>
//         )}
//       </div>
//   )
// }


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

  // Backend currently supports one product per order.
  // Keep checkout limited to one cart item for now.
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

      // -----------------------------------------
      // 1. CREATE ORDER
      // -----------------------------------------
      const orderIdempotencyKey = crypto.randomUUID()

      notify('Creating your order...')

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

      // -----------------------------------------
      // 2. PROCESS PAYMENT
      // -----------------------------------------
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

      // -----------------------------------------
      // 3. CHECK PAYMENT RESULT
      // -----------------------------------------
      if (createdPayment.status !== 'SUCCESS') {
        notify('Payment failed. Please try again.')
        return
      }

      notify('Payment authorized successfully')

      // Move to confirmation page
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

  // -----------------------------------------
  // EMPTY CART
  // -----------------------------------------
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

  // -----------------------------------------
  // ORDER COMPLETE
  // -----------------------------------------
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
            Your order has been successfully placed and payment has been completed.
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
                ${Number(order.totalAmount).toFixed(2)}
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

  // -----------------------------------------
  // CHECKOUT UI
  // -----------------------------------------
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

        {/* CHECKOUT STEPS */}
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

        {/* -----------------------------------------
          STEP 0 - CART
      ----------------------------------------- */}
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
                          ${item.price} · Qty {item.qty}
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

        {/* -----------------------------------------
          STEP 1 - SHIPPING
      ----------------------------------------- */}
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

        {/* -----------------------------------------
          STEP 2 - PAYMENT
      ----------------------------------------- */}
        {step === 2 && (
            <div className="max-w-2xl rounded-2xl border border-slate-800 bg-slate-900/70 p-6">

              <h2 className="font-medium text-white">
                Payment method
              </h2>

              <div className="mt-5 grid gap-3 sm:grid-cols-2">

                {/* CARD */}
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

                {/* WALLET */}
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
                    $120.00 available
                  </p>

                </button>

              </div>

              {/* PLACE ORDER */}
              <button
                  onClick={handlePlaceOrder}
                  disabled={placingOrder}
                  className="mt-6 w-full rounded-xl bg-amber-400 px-5 py-3 font-semibold text-slate-950 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {placingOrder
                    ? 'Processing...'
                    : `Place order · $${total.toFixed(2)}`
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
            <span>${total.toFixed(2)}</span>
          </div>

          <div className="flex justify-between text-slate-400">
            <span>Shipping</span>
            <span className="text-emerald-400">Free</span>
          </div>

          <div className="flex justify-between border-t border-slate-800 pt-3 text-lg font-semibold text-white">
            <span>Total</span>
            <span>${total.toFixed(2)}</span>
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

function Orders() {
  return (
      <div className="space-y-6">
        <div>
          <p className="text-sm font-medium text-amber-300">
            Fulfillment center
          </p>

          <h1 className="mt-1 text-3xl font-semibold text-white">
            Orders
          </h1>
        </div>

        <div className="overflow-hidden rounded-2xl border border-slate-800 bg-slate-900/70">
          {[
            ['#FS-10482', 'AeroPods Max', '$249.00', 'In transit', 'text-cyan-300'],
            ['#FS-10479', 'Velocity Runner', '$89.00', 'Delivered', 'text-emerald-300'],
            ['#FS-10465', 'Nova Camera Kit', '$599.00', 'Processing', 'text-amber-300']
          ].map((order) => (
              <div
                  key={order[0]}
                  className="grid gap-3 border-b border-slate-800 px-5 py-5 last:border-0 md:grid-cols-5 md:items-center"
              >
                <div>
                  <span className="text-xs text-slate-500">Order</span>
                  <p className="font-medium text-white">{order[0]}</p>
                </div>

                <div>
                  <span className="text-xs text-slate-500">Items</span>
                  <p className="text-sm text-slate-300">{order[1]}</p>
                </div>

                <div>
                  <span className="text-xs text-slate-500">Total</span>
                  <p className="text-sm text-white">{order[2]}</p>
                </div>

                <div>
                  <span className="text-xs text-slate-500">Status</span>
                  <p className={`text-sm ${order[4]}`}>{order[3]}</p>
                </div>

                <p className="text-xs text-slate-500">1h ago</p>
              </div>
          ))}
        </div>

        <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-6">
          <p className="font-medium text-white">
            Tracking timeline · #FS-10482
          </p>

          <div className="mt-6 flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
            {['Order placed', 'Packed', 'In transit', 'Delivered'].map(
                (label, i) => (
                    <div
                        key={label}
                        className="flex items-center gap-3 md:flex-col md:gap-2"
                    >
                <span
                    className={`flex h-8 w-8 items-center justify-center rounded-full ${
                        i < 3
                            ? 'bg-emerald-400 text-slate-950'
                            : 'bg-slate-800 text-slate-500'
                    }`}
                >
                  {i < 3 ? <Check size={15} /> : i + 1}
                </span>

                      <span className="text-sm text-slate-400">
                  {label}
                </span>
                    </div>
                )
            )}
          </div>
        </div>
      </div>
  )
}

function Notifications({ notify }) {
  const [read, setRead] = useState(false)

  return (
      <div className="space-y-6">
        <div className="flex items-end justify-between">
          <div>
            <p className="text-sm font-medium text-amber-300">
              Stay in the loop
            </p>

            <h1 className="mt-1 text-3xl font-semibold text-white">
              Notifications
            </h1>
          </div>

          <button
              onClick={() => {
                setRead(true)
                notify('All notifications marked as read')
              }}
              className="text-xs text-slate-400 hover:text-white"
          >
            Mark all read
          </button>
        </div>

        <div className="space-y-3">
          {[
            [
              'Flash sale is live',
              'Cyber Sprint just started. 3 items are moving fast.',
              '2 min ago',
              Zap,
              'amber'
            ],
            [
              'Order #FS-10482 shipped',
              'Your AeroPods Max are on the way.',
              '1 hour ago',
              Truck,
              'cyan'
            ],
            [
              'Weekly performance report',
              'Revenue is up 24.6% compared to last week.',
              'Yesterday',
              TrendingUp,
              'emerald'
            ]
          ].map(([title, body, time, Icon, tone], i) => (
              <div
                  key={title}
                  className={`flex gap-4 rounded-2xl border p-5 ${
                      !read && i === 0
                          ? 'border-amber-400/30 bg-amber-400/5'
                          : 'border-slate-800 bg-slate-900/60'
                  }`}
              >
            <span
                className={`rounded-xl p-3 ${
                    tone === 'amber'
                        ? 'bg-amber-400/10 text-amber-300'
                        : tone === 'cyan'
                            ? 'bg-cyan-400/10 text-cyan-300'
                            : 'bg-emerald-400/10 text-emerald-300'
                }`}
            >
              <Icon size={18} />
            </span>

                <div className="flex-1">
                  <div className="flex justify-between gap-4">
                    <p className="font-medium text-white">{title}</p>
                    <span className="shrink-0 text-xs text-slate-600">
                  {time}
                </span>
                  </div>

                  <p className="mt-1 text-sm text-slate-400">
                    {body}
                  </p>
                </div>

                {!read && i === 0 && (
                    <span className="mt-2 h-2 w-2 rounded-full bg-amber-400" />
                )}
              </div>
          ))}
        </div>
      </div>
  )
}

function Analytics({ range, setRange }) {
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
                      style={{ height: `${height}%` }}
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
                            style={{ width: `${p.sold}%` }}
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

function SettingsView({ notify }) {
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
                  <Icon size={17} />
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
                          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-cyan-400/20 text-xs text-cyan-300">
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
                    <UserPlus size={16} />
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

                  <div className="mt-6 flex items-center justify-between rounded-xl border border-slate-800 p-4">
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

  // ============================================
  // STEP 3 - JWT INTERCEPTOR TEST
  // ============================================


  const [view, setView] = useState('home')
  const [cart, setCart] = useState([])
  const [mobileOpen, setMobileOpen] = useState(false)
  const [detail, setDetail] = useState(null)
  const [toast, setToast] = useState('')
  const [range, setRange] = useState('7d')

  const notify = (message) => {
    setToast(message)
    window.setTimeout(() => setToast(''), 2800)
  }

  const addToCart = (product) => {
    setCart((current) =>
        current.some((item) => item.id === product.id)
            ? current.map((item) =>
                item.id === product.id
                    ? {
                      ...item,
                      qty: item.qty + 1
                    }
                    : item
            )
            : [
              ...current,
              {
                ...product,
                qty: product.qty || 1
              }
            ]
    )

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
    content = <Orders/>
  } else if (view === 'notifications') {
    content = <Notifications notify={notify}/>
  } else if (view === 'admin') {
    content = (
        <Analytics
            range={range}
            setRange={setRange}
        />
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
                      <span className="ml-auto rounded-full bg-slate-950 px-2 py-0.5 text-[10px] text-amber-300">
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
              AK
            </div>

            <div className="flex-1">
              <p className="text-sm font-medium text-white">
                Alex Kim
              </p>

              <p className="text-xs text-slate-500">
                Store owner
              </p>
            </div>

            <button
                onClick={() => go('settings')}
                aria-label="Open settings"
            >
              <Settings
                  size={16}
                  className="text-slate-50"
              />
            </button>
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

              {/* ============================================
                TEMPORARY STEP 3 TEST BUTTON
                ============================================ */}


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