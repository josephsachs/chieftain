import React, { useState, useEffect } from 'react';
import { io, Socket } from 'socket.io-client';

function App() {
  const [socket, setSocket] = useState<Socket | null>(null);
  const [connected, setConnected] = useState(false);
  const [username, setUsername] = useState('');
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  useEffect(() => {
    // Socket connection will be initialized here when needed
    return () => {
      if (socket) {
        socket.disconnect();
      }
    };
  }, [socket]);

  const handleConnect = () => {
    const newSocket = io('http://localhost:8080', {
      transports: ['websocket'],
    });

    newSocket.on('connect', () => {
      console.log('Connected to server');
      setConnected(true);
    });

    newSocket.on('disconnect', () => {
      console.log('Disconnected from server');
      setConnected(false);
    });

    setSocket(newSocket);
  };

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    if (username.trim()) {
      setIsLoggedIn(true);
      if (!socket) {
        handleConnect();
      }
    }
  };

  return (
    <div className="min-h-screen bg-gray-900 text-white">
      <header className="bg-gray-800 p-4 shadow-lg">
        <h1 className="text-2xl font-bold">Chieftain</h1>
        {connected && (
          <span className="text-xs text-green-400 ml-4">● Connected</span>
        )}
      </header>

      <main className="container mx-auto p-4">
        {!isLoggedIn ? (
          <div className="max-w-md mx-auto mt-20">
            <div className="bg-gray-800 p-8 rounded-lg shadow-xl">
              <h2 className="text-xl mb-4">Enter Game</h2>
              <form onSubmit={handleLogin}>
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="Choose a username"
                  className="w-full p-2 mb-4 bg-gray-700 rounded border border-gray-600 focus:border-blue-500 focus:outline-none"
                  autoFocus
                />
                <button
                  type="submit"
                  className="w-full p-2 bg-blue-600 rounded hover:bg-blue-700 transition-colors"
                >
                  Join Game
                </button>
              </form>
            </div>
          </div>
        ) : (
          <div>
            <p className="mb-4">Welcome, {username}!</p>
            <div className="bg-gray-800 p-4 rounded-lg">
              <p className="text-gray-400">Game board will render here</p>
              {/* This is where you'll add your Konva canvas component */}
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

export default App;