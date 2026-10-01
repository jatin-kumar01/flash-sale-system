"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  Package,
  Boxes,
  ShoppingBag,
  CreditCard,
  UserCheck,
  ArrowLeft,
  ShieldCheck
} from "lucide-react";

export default function AdminSidebar() {
  const pathname = usePathname();

  const navItems = [
    { name: "Overview", href: "/admin", icon: LayoutDashboard },
    { name: "Products", href: "/admin/products", icon: Package },
    { name: "Inventory Stock", href: "/admin/inventory", icon: Boxes },
    { name: "Global Orders", href: "/admin/orders", icon: ShoppingBag },
    { name: "Payment Audit", href: "/admin/payments", icon: CreditCard },
  ];

  return (
    <aside className="w-full lg:w-64 flex-shrink-0 bg-slate-900/60 border border-slate-800/80 rounded-2xl p-4 backdrop-blur-md self-start space-y-6">
      {/* Admin Title Header */}
      <div className="flex items-center gap-2.5 pb-4 border-b border-slate-800">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-purple-500/20 text-purple-400 font-bold border border-purple-500/30">
          <ShieldCheck className="h-5 w-5" />
        </div>
        <div>
          <h3 className="text-sm font-bold text-white">Admin Console</h3>
          <span className="text-[10px] font-semibold text-purple-400 uppercase tracking-wider">System Control</span>
        </div>
      </div>

      {/* Navigation List */}
      <nav className="space-y-1">
        {navItems.map((item) => {
          const Icon = item.icon;
          const isActive = pathname === item.href;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-xs font-semibold transition-all ${
                isActive
                  ? "bg-purple-500/20 text-purple-300 border border-purple-500/40 shadow-sm"
                  : "text-slate-400 hover:bg-slate-800/80 hover:text-slate-200"
              }`}
            >
              <Icon className={`h-4 w-4 ${isActive ? "text-purple-400" : "text-slate-400"}`} />
              {item.name}
            </Link>
          );
        })}
      </nav>

      {/* Return to Store Link */}
      <div className="pt-4 border-t border-slate-800">
        <Link
          href="/"
          className="flex items-center gap-2 rounded-xl border border-slate-800 bg-slate-950/60 px-3.5 py-2 text-xs font-medium text-slate-400 hover:text-white hover:border-slate-700 transition"
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          Return to Customer View
        </Link>
      </div>
    </aside>
  );
}
