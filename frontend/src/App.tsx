import React, { useState, useEffect } from 'react';
import './App.css';

interface HealthResponse {
    status: string;
}

interface ErrorResponse {
    type: string;
    title: string;
    status: number;
    detail: string;
}

const App: React.FC = () => {
    const [healthStatus, setHealthStatus] = useState<string>('Checking...');
    const [token, setToken] = useState<string>('');
    const [message, setMessage] = useState<string | null>(null);
    const [isPopupOpen, setIsPopupOpen] = useState<boolean>(false);

    useEffect(() => {
        fetch('http://localhost:8080/actuator/health')
            .then(response => response.json())
            .then((data: HealthResponse) => {
                console.log('Parsed JSON:', data);
                setHealthStatus(`Backend status: ${data.status}`);
            })
            .catch(error => {
                console.error('Fetch error:', error);
                setHealthStatus(`Error: ${error.message} - backend offline`);
            });
    }, []);

    const handleCheckAuth = () => {
        if (!token.trim()) {
            setMessage('Please enter a token');
            setIsPopupOpen(true);
            return;
        }

        fetch('http://localhost:8080/api/auth/me', {
            headers: {
                Authorization: `Bearer ${token}`,
            },
        })
            .then(async response => {
                if (!response.ok) {
                    const error: ErrorResponse = await response.json();
                    throw new Error(error.detail || 'Unknown error');
                }
                return response.json();
            })
            .then(data => {
                setMessage(JSON.stringify(data, null, 2));
                setIsPopupOpen(true);
            })
            .catch(error => {
                console.error('Auth error:', error);
                setMessage(error.message);
                setIsPopupOpen(true);
            });
    };

    const closePopup = () => {
        setIsPopupOpen(false);
        setMessage(null);
    };

    return (
        <div className="App">
            <header className="App-header">
                <h1>Welcome to AI Chat Bridge</h1>
                <p>{healthStatus}</p>
                <input
                    type="text"
                    className="token-input"
                    value={token}
                    onChange={e => setToken(e.target.value)}
                    placeholder="Enter JWT token"
                />
                <button className="check-auth-button" onClick={handleCheckAuth}>
                    Check Authentication
                </button>
            </header>
            {isPopupOpen && (
                <div className="popup-overlay">
                    <div className="popup">
                        <h2>{message?.startsWith('{') ? 'Response' : 'Error'}</h2>
                        <pre>{message}</pre>
                        <button className="close-button" onClick={closePopup}>
                            Close
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default App;
