"use client";

/**
 * Utility functions for managing user authentication and roles.
 */

export function getStoredUser() {
  if (typeof window === "undefined") return null;
  try {
    const userStr = sessionStorage.getItem("user");
    return userStr ? JSON.parse(userStr) : null;
  } catch (e) {
    console.error("Error reading stored user:", e);
    return null;
  }
}

export function getAccessToken() {
  if (typeof window === "undefined") return null;
  return sessionStorage.getItem("accessToken");
}

export function isAuthenticated() {
  if (typeof window === "undefined") return false;
  const token = sessionStorage.getItem("accessToken");
  return Boolean(token && token.trim() !== "");
}

export function isAdmin() {
  if (typeof window === "undefined") return false;
  const user = getStoredUser();
  if (!user || !user.roles) return false;
  return Array.isArray(user.roles)
    ? user.roles.includes("ROLE_ADMIN")
    : user.roles === "ROLE_ADMIN";
}

export function logout() {
  if (typeof window === "undefined") return;
  sessionStorage.removeItem("accessToken");
  sessionStorage.removeItem("refreshToken");
  sessionStorage.removeItem("user");
  window.location.href = "/login";
}

export function getStoreName() {
  if (typeof window === "undefined") return "Swiftly";
  try {
    const settingsStr = localStorage.getItem("swiftlyAdminSettings");
    if (settingsStr) {
      const parsed = JSON.parse(settingsStr);
      if (parsed && parsed.storeName) return parsed.storeName;
    }
  } catch (e) {
    console.error("Error reading store name:", e);
  }
  return "Swiftly";
}

export function getAdminProfile() {
  if (typeof window === "undefined") return { displayName: "Jatin", role: "Administrator" };
  try {
    const profileStr = localStorage.getItem("swiftlyAdminProfile");
    if (profileStr) {
      const parsed = JSON.parse(profileStr);
      if (parsed && (parsed.displayName || parsed.name)) {
        return {
          displayName: parsed.displayName || parsed.name,
          role: parsed.role || "Administrator",
        };
      }
    }
  } catch (e) {
    console.error("Error reading admin profile:", e);
  }
  return { displayName: "Jatin", role: "Administrator" };
}

export function updateAdminProfile(displayName) {
  if (typeof window === "undefined") return null;
  try {
    const cleanName = (displayName || "").trim() || "Jatin";
    const profile = {
      displayName: cleanName,
      role: "Administrator",
    };
    localStorage.setItem("swiftlyAdminProfile", JSON.stringify(profile));
    window.dispatchEvent(new Event("swiftlyAdminProfileUpdated"));
    window.dispatchEvent(new Event("swiftlySettingsUpdated"));
    return profile;
  } catch (e) {
    console.error("Error updating admin profile:", e);
    return null;
  }
}

export function updateUserProfile(displayName) {
  if (typeof window === "undefined") return null;
  try {
    const user = getStoredUser() || {};
    const parts = (displayName || "").trim().split(/\s+/);
    const firstName = parts[0] || "";
    const lastName = parts.slice(1).join(" ") || "";

    const updatedUser = {
      ...user,
      firstName,
      lastName,
    };
    sessionStorage.setItem("user", JSON.stringify(updatedUser));
    window.dispatchEvent(new Event("swiftlySettingsUpdated"));
    return updatedUser;
  } catch (e) {
    console.error("Error updating user profile:", e);
    return null;
  }
}

export function updateStoreSettings(storeName) {
  if (typeof window === "undefined") return;
  try {
    const settingsStr = localStorage.getItem("swiftlyAdminSettings");
    let settings = {};
    if (settingsStr) {
      try {
        settings = JSON.parse(settingsStr) || {};
      } catch (err) {}
    }
    settings.storeName = storeName;
    localStorage.setItem("swiftlyAdminSettings", JSON.stringify(settings));
    window.dispatchEvent(new Event("swiftlySettingsUpdated"));
  } catch (e) {
    console.error("Error updating store settings:", e);
  }
}

