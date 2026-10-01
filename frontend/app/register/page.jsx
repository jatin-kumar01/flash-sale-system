"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import api from "../../lib/api";
import { isAuthenticated, isAdmin } from "../../lib/auth";

export default function RegisterPage() {
    const router = useRouter();

    const [firstName, setFirstName] = useState("");
    const [lastName, setLastName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");

    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [successMessage, setSuccessMessage] = useState("");

    useEffect(() => {
        if (typeof window !== "undefined" && isAuthenticated()) {
            if (isAdmin()) {
                router.push("/admin/dashboard");
            } else {
                router.push("/dashboard");
            }
        }
    }, [router]);

    const validateForm = () => {
        if (!firstName.trim()) {
            setError("First name is required.");
            return false;
        }
        if (!lastName.trim()) {
            setError("Last name is required.");
            return false;
        }
        if (!email.trim()) {
            setError("Email is required.");
            return false;
        }
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email.trim())) {
            setError("Please enter a valid email address.");
            return false;
        }
        if (!password) {
            setError("Password is required.");
            return false;
        }
        if (password.length < 8) {
            setError("Password must be at least 8 characters long.");
            return false;
        }
        if (password !== confirmPassword) {
            setError("Passwords do not match.");
            return false;
        }

        return true;
    };

    const handleRegister = async (event) => {
        event.preventDefault();
        setError("");
        setSuccessMessage("");

        if (!validateForm()) {
            return;
        }

        setLoading(true);

        try {
            await api.post("/api/auth/register", {
                firstName: firstName.trim(),
                lastName: lastName.trim(),
                email: email.trim().toLowerCase(),
                password: password,
            });

            setSuccessMessage("Account created successfully. Redirecting to login...");

            setTimeout(() => {
                router.push("/login");
            }, 1500);
        } catch (err) {
            console.error("Registration failed:", err);

            const msg =
                err.response?.data?.message ||
                err.response?.data?.errorDetails?.[0]?.message ||
                err.message ||
                "Registration failed. Please try again.";

            if (msg.toLowerCase().includes("already registered") || msg.toLowerCase().includes("duplicate")) {
                setError("An account with this email already exists.");
            } else {
                setError(msg);
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <main className="login-page">
            <div className="support-panel" style={{ maxWidth: "440px" }}>
                <span className="eyebrow">SWIFTLY ACCOUNT</span>

                <h1>Create account.</h1>

                <p>
                    Register for a Swiftly account to start shopping.
                </p>

                <form onSubmit={handleRegister}>
                    <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                        <div className="login-field">
                            <label htmlFor="firstName">First Name</label>
                            <input
                                id="firstName"
                                type="text"
                                value={firstName}
                                onChange={(e) => setFirstName(e.target.value)}
                                placeholder="First name"
                                required
                                disabled={loading}
                            />
                        </div>

                        <div className="login-field">
                            <label htmlFor="lastName">Last Name</label>
                            <input
                                id="lastName"
                                type="text"
                                value={lastName}
                                onChange={(e) => setLastName(e.target.value)}
                                placeholder="Last name"
                                required
                                disabled={loading}
                            />
                        </div>
                    </div>

                    <div className="login-field">
                        <label htmlFor="email">Email</label>
                        <input
                            id="email"
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="Enter your email"
                            required
                            autoComplete="email"
                            disabled={loading}
                        />
                    </div>

                    <div className="login-field">
                        <label htmlFor="password">Password</label>
                        <input
                            id="password"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="Min 8 characters"
                            required
                            autoComplete="new-password"
                            disabled={loading}
                        />
                    </div>

                    <div className="login-field">
                        <label htmlFor="confirmPassword">Confirm Password</label>
                        <input
                            id="confirmPassword"
                            type="password"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            placeholder="Re-enter password"
                            required
                            autoComplete="new-password"
                            disabled={loading}
                        />
                    </div>

                    {error && (
                        <div className="login-error">
                            {error}
                        </div>
                    )}

                    {successMessage && (
                        <div style={{ color: "#16a34a", backgroundColor: "#f0fdf4", border: "1px solid #bbf7d0", padding: "10px", borderRadius: "6px", fontSize: "13px", marginBottom: "16px" }}>
                            {successMessage}
                        </div>
                    )}

                    <button
                        type="submit"
                        className="button button-dark"
                        disabled={loading}
                        style={{ width: "100%" }}
                    >
                        {loading ? "Creating account..." : "Create account"}
                    </button>
                </form>

                <p style={{ marginTop: "20px", textAlign: "center", fontSize: "13px", color: "#666" }}>
                    Already have an account?{" "}
                    <Link href="/login" style={{ color: "#000", fontWeight: 600, textDecoration: "underline" }}>
                        Sign in
                    </Link>
                </p>
            </div>
        </main>
    );
}
