"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import api from "@/lib/api";

export default function LoginPage() {
    const router = useRouter();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    const handleLogin = async (e) => {
        e.preventDefault();

        setError("");
        setLoading(true);

        try {
            const response = await api.post("/auth/login", {
                email,
                password,
            });

            console.log("Login response:", response.data);

            // We will finalize the exact token path after checking
            // your AuthResponse structure.
            const accessToken = response.data?.data?.accessToken;

            if (!accessToken) {
                throw new Error("Access token was not returned by the server.");
            }

            localStorage.setItem("accessToken", accessToken);

            router.push("/");
        } catch (error) {
            console.error("Login error:", error);

            const message =
                error.response?.data?.message ||
                "Invalid email or password.";

            setError(message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <main className="min-h-screen bg-[#020617] text-white flex items-center justify-center px-4">
            <div className="w-full max-w-md">
                {/* Logo */}
                <div className="text-center mb-8">
                    <div className="flex justify-center mb-4">
                        <div className="w-14 h-14 rounded-full bg-[#fbbf24] flex items-center justify-center text-black text-2xl font-bold">
                            ⚡
                        </div>
                    </div>

                    <h1 className="text-3xl font-bold">
                        Flash<span className="text-[#fbbf24]">Sale</span> Engine
                    </h1>

                    <p className="text-slate-400 mt-2">
                        Sign in to continue
                    </p>
                </div>

                {/* Login Card */}
                <div className="bg-[#0f172a] border border-slate-800 rounded-2xl p-7 shadow-2xl">
                    <h2 className="text-2xl font-semibold mb-6">
                        Welcome back
                    </h2>

                    <form onSubmit={handleLogin} className="space-y-5">
                        {/* Email */}
                        <div>
                            <label className="block text-sm text-slate-300 mb-2">
                                Email
                            </label>

                            <input
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                placeholder="you@example.com"
                                required
                                className="w-full rounded-lg border border-slate-700 bg-[#020617] px-4 py-3 text-white outline-none focus:border-[#fbbf24]"
                            />
                        </div>

                        {/* Password */}
                        <div>
                            <label className="block text-sm text-slate-300 mb-2">
                                Password
                            </label>

                            <input
                                type="password"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="••••••••"
                                required
                                className="w-full rounded-lg border border-slate-700 bg-[#020617] px-4 py-3 text-white outline-none focus:border-[#fbbf24]"
                            />
                        </div>

                        {/* Error */}
                        {error && (
                            <div className="rounded-lg border border-red-900 bg-red-950/40 px-4 py-3 text-sm text-red-400">
                                {error}
                            </div>
                        )}

                        {/* Button */}
                        <button
                            type="submit"
                            disabled={loading}
                            className="w-full rounded-lg bg-[#fbbf24] px-4 py-3 font-semibold text-black transition hover:bg-[#f59e0b] disabled:cursor-not-allowed disabled:opacity-60"
                        >
                            {loading ? "Signing in..." : "Sign In"}
                        </button>
                    </form>

                    {/* Register */}
                    <div className="mt-6 text-center text-sm text-slate-400">
                        Don't have an account?{" "}
                        <a
                            href="/register"
                            className="font-semibold text-[#fbbf24] hover:underline"
                        >
                            Create account
                        </a>
                    </div>
                </div>
            </div>
        </main>
    );
}