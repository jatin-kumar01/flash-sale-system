"use client";

import React from "react";
import { AlertCircle } from "lucide-react";

export default function ErrorMessage({ title = "Error Occurred", message, onRetry }) {
  if (!message) return null;

  return (
    <div className="rounded-xl border border-rose-900/50 bg-rose-950/30 p-4 text-rose-200 backdrop-blur-sm">
      <div className="flex items-start gap-3">
        <AlertCircle className="mt-0.5 h-5 w-5 flex-shrink-0 text-rose-400" />
        <div className="flex-1">
          <h4 className="text-sm font-semibold text-rose-300">{title}</h4>
          <p className="mt-1 text-xs text-rose-300/80">{message}</p>
          {onRetry && (
            <button
              onClick={onRetry}
              className="mt-3 rounded-md bg-rose-900/40 px-3 py-1 text-xs font-medium text-rose-200 hover:bg-rose-900/60 transition-colors"
            >
              Try Again
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
