import React, { useState } from 'react';
import { registerUser, loginUser } from '../api';

export default function AuthModal({ onLoginSuccess }) {
    const [isRegistering, setIsRegistering] = useState(false);
    const [formData, setFormData] = useState({ email: '', password: '', firstName: '', lastName: '' });
    const [error, setError] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            if (isRegistering) {
                await registerUser(formData);
                alert('Registration successful! Please sign in.');
                setIsRegistering(false);
            } else {
                const res = await loginUser({ email: formData.email, password: formData.password });
                onLoginSuccess(res.data);
            }
        } catch (err) {
            setError(err.message);
        }
    };

    return (
        <div className="flex flex-col items-center justify-center min-h-screen bg-gray-900 text-white">
            <div className="w-full max-w-md p-8 bg-gray-800 rounded-lg shadow-lg">
                <h2 className="text-2xl font-bold mb-6 text-center">
                    {isRegistering ? 'Create Account' : 'Flash Sale Login'}
                </h2>
                {error && <p className="mb-4 text-red-500 text-sm text-center">{error}</p>}

                <form onSubmit={handleSubmit} className="space-y-4">
                    {isRegistering && (
                        <>
                            <div>
                                <label className="block text-sm mb-1">First Name</label>
                                <input
                                    type="text"
                                    className="w-full p-2 rounded bg-gray-700 border border-gray-600 focus:outline-none"
                                    onChange={e => setFormData({...formData, firstName: e.target.value})}
                                    required
                                />
                            </div>
                            <div>
                                <label className="block text-sm mb-1">Last Name</label>
                                <input
                                    type="text"
                                    className="w-full p-2 rounded bg-gray-700 border border-gray-600 focus:outline-none"
                                    onChange={e => setFormData({...formData, lastName: e.target.value})}
                                    required
                                />
                            </div>
                        </>
                    )}
                    <div>
                        <label className="block text-sm mb-1">Email Address</label>
                        <input
                            type="email"
                            className="w-full p-2 rounded bg-gray-700 border border-gray-600 focus:outline-none"
                            onChange={e => setFormData({...formData, email: e.target.value})}
                            required
                        />
                    </div>
                    <div>
                        <label className="block text-sm mb-1">Password</label>
                        <input
                            type="password"
                            className="w-full p-2 rounded bg-gray-700 border border-gray-600 focus:outline-none"
                            onChange={e => setFormData({...formData, password: e.target.value})}
                            required
                        />
                    </div>
                    <button
                        type="submit"
                        className="w-full py-2 bg-indigo-600 hover:bg-indigo-700 rounded font-semibold transition"
                    >
                        {isRegistering ? 'Sign Up' : 'Sign In'}
                    </button>
                </form>

                <button
                    onClick={() => setIsRegistering(!isRegistering)}
                    className="w-full mt-4 text-sm text-indigo-400 hover:underline text-center"
                >
                    {isRegistering ? 'Already have an account? Sign In' : "Need an account? Register"}
                </button>
            </div>
        </div>
    );
}