'use client'

import { useEffect, useState } from 'react'

export const CURRENCIES = {
  INR: { code: 'INR', label: 'INR (₹)', symbol: '₹', rate: 1.0, locale: 'en-IN' },
  USD: { code: 'USD', label: 'USD ($)', symbol: '$', rate: 0.012, locale: 'en-US' },
  EUR: { code: 'EUR', label: 'EUR (€)', symbol: '€', rate: 0.011, locale: 'de-DE' },
  GBP: { code: 'GBP', label: 'GBP (£)', symbol: '£', rate: 0.0093, locale: 'en-GB' },
}

export function parseCurrencyCode(value) {
  if (!value) return 'INR'
  const str = String(value).toUpperCase()
  if (str.includes('USD') || str.includes('$')) return 'USD'
  if (str.includes('EUR') || str.includes('€')) return 'EUR'
  if (str.includes('GBP') || str.includes('£')) return 'GBP'
  return 'INR'
}

export function getCurrency() {
  if (typeof window === 'undefined') return 'INR'
  let userKey = null
  try {
    const userStr = sessionStorage.getItem('user')
    if (userStr) {
      const u = JSON.parse(userStr)
      if (u && (u.id || u.userId)) {
        userKey = `swiftly_currency_${u.id || u.userId}`
      }
    }
  } catch (e) {}

  const saved = (userKey && localStorage.getItem(userKey)) || localStorage.getItem('swiftly_currency')
  return parseCurrencyCode(saved)
}

export function setCurrency(code) {
  const cleanCode = parseCurrencyCode(code)
  if (typeof window !== 'undefined') {
    try {
      const userStr = sessionStorage.getItem('user')
      if (userStr) {
        const u = JSON.parse(userStr)
        if (u && (u.id || u.userId)) {
          localStorage.setItem(`swiftly_currency_${u.id || u.userId}`, cleanCode)
        }
      }
    } catch (e) {}
    localStorage.setItem('swiftly_currency', cleanCode)
    window.dispatchEvent(new Event('swiftly_currency_change'))
  }
  return cleanCode
}

export function formatPrice(amountInINR, currencyCode = null) {
  if (amountInINR === null || amountInINR === undefined || amountInINR === '') {
    return ''
  }

  let num = 0
  if (typeof amountInINR === 'number') {
    num = amountInINR
  } else {
    const cleaned = String(amountInINR).replace(/[^0-9.]/g, '')
    num = parseFloat(cleaned)
  }

  if (isNaN(num)) return ''

  const code = currencyCode || getCurrency()
  const config = CURRENCIES[code] || CURRENCIES.INR
  const converted = num * config.rate

  if (code === 'INR') {
    return `${config.symbol}${Math.round(converted).toLocaleString(config.locale)}`
  }

  return `${config.symbol}${converted.toLocaleString(config.locale, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`
}

export function useCurrency() {
  const [currency, setCurrencyState] = useState(() => getCurrency())

  useEffect(() => {
    function handleChange() {
      setCurrencyState(getCurrency())
    }

    window.addEventListener('swiftly_currency_change', handleChange)
    window.addEventListener('storage', handleChange)
    return () => {
      window.removeEventListener('swiftly_currency_change', handleChange)
      window.removeEventListener('storage', handleChange)
    }
  }, [])

  const updateCurrency = (code) => {
    const newCode = setCurrency(code)
    setCurrencyState(newCode)
  }

  return {
    currency,
    currencyConfig: CURRENCIES[currency] || CURRENCIES.INR,
    setCurrency: updateCurrency,
    formatPrice: (amount) => formatPrice(amount, currency),
  }
}
