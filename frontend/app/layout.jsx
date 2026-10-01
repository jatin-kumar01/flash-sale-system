import './globals.css'
import './user.css'
import './orders.css'
import './profile.css'
import './cart-checkout.css'
import { CartProvider } from '@/components/user/CartProvider'

export const metadata = {
  title: 'Swiftly | E-Commerce Platform',
  description: 'Shop flash-sale products and manage your store operations.',
}

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>
        <CartProvider>
          {children}
        </CartProvider>
      </body>
    </html>
  )
}
