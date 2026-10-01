'use client'

import { useEffect, useState } from 'react'
import { PageHeading } from '@/components/admin/AdminUI'
import { getAdminProfile, getStoreName, updateAdminProfile, updateStoreSettings } from '@/lib/auth'

export default function SettingsPage() {
  const [saved, setSaved] = useState(false)
  const [userRole, setUserRole] = useState('Administrator')
  const [displayNameInput, setDisplayNameInput] = useState('Jatin')
  const [storeNameInput, setStoreNameInput] = useState('Swiftly')
  const [currency] = useState('INR')

  useEffect(() => {
    const adminProf = getAdminProfile()
    if (adminProf) {
      setDisplayNameInput(adminProf.displayName || 'Jatin')
      setUserRole(adminProf.role || 'Administrator')
    }
    setStoreNameInput(getStoreName())
  }, [])

  const handleSave = () => {
    // 1. Save Admin Display Name in localStorage "swiftlyAdminProfile" (independent from customer "user")
    updateAdminProfile(displayNameInput)

    // 2. Save Store Name in localStorage "swiftlyAdminSettings"
    updateStoreSettings(storeNameInput)

    setSaved(true)
  }

  return (
    <>
      <PageHeading
        eyebrow="Workspace"
        title="Settings"
        description="Manage your admin profile and store preferences."
      />
      <div className="settings-grid">
        <section className="panel settings-section">
          <span className="eyebrow coral">Profile</span>
          <h2>Admin profile</h2>
          <label>
            Display name
            <input
              value={displayNameInput}
              onChange={(e) => {
                setDisplayNameInput(e.target.value)
                setSaved(false)
              }}
            />
          </label>
          <label>
            Role
            <input value={userRole} readOnly />
          </label>
        </section>

        <section className="panel settings-section">
          <span className="eyebrow coral">Store settings</span>
          <h2>Store details</h2>
          <label>
            Store name
            <input
              value={storeNameInput}
              onChange={(e) => {
                setStoreNameInput(e.target.value)
                setSaved(false)
              }}
            />
          </label>
          <label>
            Currency
            <select
              value={currency}
              disabled
              readOnly
            >
              <option value="INR">INR — Indian Rupee</option>
              <option value="USD">USD — US Dollar</option>
              <option value="EUR">EUR — Euro</option>
              <option value="GBP">GBP — British Pound</option>
            </select>
          </label>
        </section>
      </div>

      <div className="settings-footer">
        <span>
          {saved
            ? 'Settings saved successfully.'
            : 'Settings are saved locally for this administrator.'}
        </span>
        <button className="button primary" onClick={handleSave}>
          Save settings
        </button>
      </div>
    </>
  )
}
