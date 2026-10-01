// import Link from 'next/link'
// export default function LoginPage() { return <main className="login-page"><div className="support-panel"><span className="eyebrow">SWIFTLY ACCOUNT</span><h1>Welcome back.</h1><p>Sign in will connect to the authentication API later.</p><Link className="button button-dark" href="/dashboard">Continue to dashboard</Link></div></main> }



//2nd time
"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import api from "../../lib/api";
import { isAuthenticated, isAdmin } from "../../lib/auth";

export default function LoginPage() {
    const router = useRouter();

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        if (typeof window !== "undefined") {
            const msg = sessionStorage.getItem("authMessage");
            if (msg) {
                setError(msg);
                sessionStorage.removeItem("authMessage");
            } else if (isAuthenticated()) {
                if (isAdmin()) {
                    router.push("/admin/dashboard");
                } else {
                    router.push("/dashboard");
                }
            }
        }
    }, [router]);

    const handleLogin = async (event) => {
        event.preventDefault();

        setError("");
        setLoading(true);

        try {
            const response = await api.post("/api/auth/login", {
                email,
                password,
            });

            // Backend response is wrapped inside ApiResponse.data
            const authData = response.data?.data ?? response.data;

            if (!authData?.accessToken) {
                throw new Error("Login response did not contain an access token.");
            }

            // Clear any legacy localStorage session data
            localStorage.removeItem("accessToken");
            localStorage.removeItem("refreshToken");
            localStorage.removeItem("user");

            // Store JWT in sessionStorage for per-tab isolation
            sessionStorage.setItem("accessToken", authData.accessToken);

            // Store refresh token if provided
            if (authData.refreshToken) {
                sessionStorage.setItem("refreshToken", authData.refreshToken);
            }

            // Store user information
            const user = {
                id: authData.userId,
                userId: authData.userId,
                email: authData.email,
                firstName: authData.firstName,
                lastName: authData.lastName,
                phone: authData.phone || "",
                address: authData.address || "",
                roles: Array.isArray(authData.roles)
                    ? authData.roles
                    : [],
            };

            sessionStorage.setItem("user", JSON.stringify(user));

            // Redirect according to role
            if (user.roles.includes("ROLE_ADMIN")) {
                router.push("/admin/dashboard");
            } else {
                router.push("/dashboard");
            }
        } catch (error) {
            console.error("Login failed:", error);

            const message =
                error.response?.data?.message ||
                error.response?.data?.errorDetails?.[0]?.message ||
                error.message ||
                "Login failed. Please check your email and password.";

            setError(message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <main className="login-page">
            <div className="support-panel">
                <span className="eyebrow">SWIFTLY ACCOUNT</span>

                <h1>Welcome back.</h1>

                <p>
                    Sign in to continue to your Swiftly account.
                </p>

                <form onSubmit={handleLogin}>
                    <div className="login-field">
                        <label htmlFor="email">Email</label>

                        <input
                            id="email"
                            type="email"
                            value={email}
                            onChange={(event) => setEmail(event.target.value)}
                            placeholder="Enter your email"
                            required
                            autoComplete="email"
                        />
                    </div>

                    <div className="login-field">
                        <label htmlFor="password">Password</label>

                        <input
                            id="password"
                            type="password"
                            value={password}
                            onChange={(event) => setPassword(event.target.value)}
                            placeholder="Enter your password"
                            required
                            autoComplete="current-password"
                        />
                    </div>

                    {error && (
                        <div className="login-error">
                            {error}
                        </div>
                    )}

                    <button
                        type="submit"
                        className="button button-dark"
                        disabled={loading}
                    >
                        {loading ? "Signing in..." : "Sign in"}
                    </button>
                </form>

                <p style={{ marginTop: "20px", textAlign: "center", fontSize: "13px", color: "#666" }}>
                    Don't have an account?{" "}
                    <Link href="/register" style={{ color: "#000", fontWeight: 600, textDecoration: "underline" }}>
                        Create account
                    </Link>
                </p>
            </div>
        </main>
    );
}