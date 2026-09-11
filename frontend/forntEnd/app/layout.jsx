import { Analytics } from '@vercel/analytics/next'
import './globals.css'

export const metadata = {
  title: 'FlashSale Engine — High-Concurrency E-Commerce',
  description: 'Production-grade flash sale and e-commerce engine with real-time stock simulation.',
}

export const viewport = {
  colorScheme: 'dark',
  themeColor: '#020617',
}

export default function RootLayout({ children }) {
  return <html lang="en" className="dark"><body className="min-h-screen bg-slate-950 text-slate-100 antialiased">{children}{process.env.NODE_ENV === 'production' && <Analytics />}</body></html>
}
