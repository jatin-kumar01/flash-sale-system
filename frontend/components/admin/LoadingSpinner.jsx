"use client";

import React from "react";

export default function LoadingSpinner({ label = "Loading...", size = "md" }) {
  const sizeClasses = {
    sm: "w-4 h-4 border-2",
    md: "w-8 h-8 border-3",
    lg: "w-12 h-12 border-4",
  };

  return (
    <div className="flex flex-col items-center justify-center p-8 text-slate-400">
      <div
        className={`${sizeClasses[size] || sizeClasses.md} rounded-full border-amber-400/20 border-t-amber-400 animate-spin`}
      />
      {label && <p className="mt-3 text-sm font-medium tracking-wide text-slate-400">{label}</p>}
    </div>
  );
}
