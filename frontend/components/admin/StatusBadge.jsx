"use client";

import React from "react";

export default function StatusBadge({ status, size = "md" }) {
  if (!status) return null;

  const normalized = String(status).toUpperCase();

  const config = {
    // Order / Payment Statuses
    PENDING_PAYMENT: {
      label: "Pending Payment",
      styles: "bg-amber-500/10 text-amber-400 border-amber-500/20",
    },
    PAID: {
      label: "Paid",
      styles: "bg-emerald-500/10 text-emerald-400 border-emerald-500/20",
    },
    SUCCESS: {
      label: "Success",
      styles: "bg-emerald-500/10 text-emerald-400 border-emerald-500/20",
    },
    CANCELLED: {
      label: "Cancelled",
      styles: "bg-slate-500/10 text-slate-400 border-slate-500/20",
    },
    EXPIRED: {
      label: "Expired",
      styles: "bg-rose-500/10 text-rose-400 border-rose-500/20",
    },
    FAILED: {
      label: "Failed",
      styles: "bg-rose-500/10 text-rose-400 border-rose-500/20",
    },

    // Product Statuses
    ACTIVE: {
      label: "Sale Active",
      styles: "bg-emerald-500/10 text-emerald-400 border-emerald-500/20",
    },
    UPCOMING: {
      label: "Upcoming",
      styles: "bg-cyan-500/10 text-cyan-400 border-cyan-500/20",
    },
    ENDED: {
      label: "Sale Ended",
      styles: "bg-slate-500/10 text-slate-400 border-slate-500/20",
    },
    DISABLED: {
      label: "Disabled",
      styles: "bg-rose-500/10 text-rose-400 border-rose-500/20",
    },
  };

  const badgeConfig = config[normalized] || {
    label: status,
    styles: "bg-slate-500/10 text-slate-300 border-slate-500/20",
  };

  const sizeClasses = {
    sm: "px-2 py-0.5 text-[10px]",
    md: "px-2.5 py-1 text-xs",
    lg: "px-3.5 py-1.5 text-sm",
  };

  return (
    <span
      className={`inline-flex items-center font-medium rounded-full border ${badgeConfig.styles} ${sizeClasses[size] || sizeClasses.md}`}
    >
      <span className="w-1.5 h-1.5 rounded-full bg-current mr-1.5 animate-pulse" />
      {badgeConfig.label}
    </span>
  );
}
