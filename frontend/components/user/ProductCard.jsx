import Link from 'next/link'
import { useCurrency } from '@/lib/currency'
import { useCart } from '@/components/user/CartProvider'

export default function ProductCard({ product, compact = false }) {
  const { formatPrice } = useCurrency()
  const { addItem } = useCart()

  const isOutOfStock = Boolean(
    product?.isOutOfStock || product?.stock === 'Out of stock'
  )

  const salePriceDisplay = product?.flashSalePrice ?? product?.salePrice ?? product?.rawSalePrice ?? product?.price
  const origPriceDisplay = product?.originalPrice ?? product?.rawOriginalPrice ?? product?.original

  return (
    <article className={compact ? 'user-product-card compact' : 'user-product-card'}>
      <Link href={`/products/${product.id}`} className="product-image">
        <img
          src={product.image}
          alt={product.name}
          onError={(event) => {
            event.currentTarget.style.opacity = '0'
            event.currentTarget.alt = ''
          }}
        />
        <span className="image-fallback">
          {(product.name || 'P').slice(0, 2)}
        </span>
      </Link>
      <div className="product-card-content">
        <span className="sale-pill">{product.discount}</span>
        <Link href={`/products/${product.id}`}>
          <h3>{product.name}</h3>
        </Link>
        <span className="stock-label">{product.stock}</span>
        <div className="price-row">
          <b>{formatPrice(salePriceDisplay)}</b>
          {origPriceDisplay && <del>{formatPrice(origPriceDisplay)}</del>}
        </div>
        {!compact && (
          <div className="product-actions">
            <Link className="button button-light" href={`/products/${product.id}`}>
              View Product
            </Link>
            {isOutOfStock ? (
              <button
                className="button button-dark"
                disabled
                style={{ opacity: 0.6, cursor: 'not-allowed' }}
                onClick={(e) => e.preventDefault()}
              >
                Out of stock
              </button>
            ) : (
              <Link
                className="button button-dark"
                href={`/checkout/${product.id}`}
                onClick={() => addItem(product.id, 1)}
              >
                Buy Now
              </Link>
            )}
          </div>
        )}
      </div>
    </article>
  )
}
