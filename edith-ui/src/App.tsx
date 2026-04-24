import { useState } from 'react'

function App() {
  const [booting, setBooting] = useState(false)
  const [sessionActive, setSessionActive] = useState(false)
  const [sysLog, setSysLog] = useState<string[]>([])

  const addLog = (msg: string) => {
    setSysLog(prev => [...prev, `[${new Date().toLocaleTimeString()}] ${msg}`])
  }

  const handleBoot = () => {
    setBooting(true)
    addLog('SYSTEM INITIALIZING...')
    setTimeout(() => addLog('LOADING NEURAL NET...'), 1000)
    setTimeout(() => addLog('ESTABLISHING SECURE CONNECTION...'), 2000)
    setTimeout(() => {
      addLog('E.D.I.T.H. ONLINE')
      setSessionActive(true)
      setBooting(false)
    }, 3500)
  }

  return (
    <div className="min-h-screen bg-edith-dark text-white font-edith overflow-hidden flex flex-col items-center justify-center relative">
      {/* Background Grid */}
      <div className="absolute inset-0 z-0 opacity-20 pointer-events-none bg-edith-grid"></div>

      {/* Top Bar */}
      <header className="absolute top-0 w-full p-6 flex justify-between items-center z-10">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-full border-2 border-edith-cyan flex items-center justify-center animate-pulse shadow-[0_0_15px_rgba(0,243,255,0.5)]">
            <div className="w-4 h-4 bg-edith-cyan rounded-full"></div>
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-widest text-edith-cyan drop-shadow-[0_0_5px_rgba(0,243,255,0.8)]">EDITH</h1>
            <p className="text-[10px] tracking-widest text-edith-cyan/70">V3.0 // WEB_INTERFACE</p>
          </div>
        </div>
        
        <button className="px-6 py-2 border border-edith-cyan text-edith-cyan hover:bg-edith-cyan/10 hover:shadow-[0_0_15px_rgba(0,243,255,0.4)] transition-all duration-300 relative group overflow-hidden">
          <span className="relative z-10 tracking-wider text-sm uppercase">Diagnostics</span>
          <div className="absolute inset-0 bg-edith-cyan/20 translate-y-full group-hover:translate-y-0 transition-transform duration-300"></div>
        </button>
      </header>

      {/* Main Content Area */}
      <main className="relative z-10 flex flex-col items-center justify-center w-full max-w-4xl px-8">
        {!sessionActive ? (
          <div className="flex flex-col items-center animate-fade-in">
            <div className="relative mb-12 flex justify-center items-center">
              {/* Central Arc Reactor-like Element */}
              <div className="w-64 h-64 rounded-full border border-edith-cyan/30 flex items-center justify-center relative">
                <div className={`w-48 h-48 rounded-full border-2 border-edith-cyan/50 flex items-center justify-center ${booting ? 'animate-spin-slow' : ''}`}>
                  <div className="w-32 h-32 rounded-full border-4 border-edith-cyan shadow-[0_0_30px_rgba(0,243,255,0.8)] flex items-center justify-center bg-edith-cyan/10 backdrop-blur-sm">
                     <div className="w-16 h-16 rounded-full bg-edith-cyan shadow-[0_0_50px_rgba(0,243,255,1)] animate-pulse"></div>
                  </div>
                </div>
                {/* Orbital Rings */}
                <div className="absolute inset-0 rounded-full border border-edith-cyan/20 scale-[1.2]"></div>
                <div className="absolute inset-0 rounded-full border border-edith-cyan/10 scale-[1.4]"></div>
              </div>
            </div>

            <h2 className="text-5xl font-bold tracking-[0.2em] mb-4 text-transparent bg-clip-text bg-gradient-to-r from-edith-cyan to-blue-500 drop-shadow-[0_0_10px_rgba(0,243,255,0.5)]">
              E.D.I.T.H.
            </h2>
            <p className="text-sm tracking-[0.3em] text-edith-cyan/80 mb-12 uppercase">
              Enhanced Defensive Intelligent Tactical Helper
            </p>

            <button 
              onClick={handleBoot}
              disabled={booting}
              className={`group relative px-12 py-4 bg-transparent border-2 border-edith-cyan overflow-hidden transition-all duration-300 ${booting ? 'opacity-50 cursor-not-allowed' : 'hover:shadow-[0_0_20px_rgba(0,243,255,0.6)]'}`}
            >
              <div className="absolute inset-0 bg-edith-cyan/10 translate-x-[-100%] group-hover:translate-x-0 transition-transform duration-500"></div>
              <span className="relative z-10 text-edith-cyan tracking-widest uppercase font-bold text-lg">
                {booting ? 'INITIALIZING...' : 'INITIATE SESSION'}
              </span>
              <div className="absolute bottom-0 left-0 w-full h-[2px] bg-edith-cyan scale-x-0 group-hover:scale-x-100 transition-transform origin-left duration-300"></div>
              <div className="absolute top-0 right-0 w-full h-[2px] bg-edith-cyan scale-x-0 group-hover:scale-x-100 transition-transform origin-right duration-300"></div>
            </button>
            
            {/* System Logs (Visible during boot) */}
            {booting && (
               <div className="mt-8 text-left w-full max-w-md border border-edith-cyan/30 bg-edith-dark/80 p-4 font-mono text-xs text-edith-cyan h-32 overflow-y-auto custom-scrollbar shadow-[inset_0_0_10px_rgba(0,243,255,0.1)]">
                 {sysLog.map((log, i) => (
                   <div key={i} className="mb-1">{`> ${log}`}</div>
                 ))}
               </div>
            )}
          </div>
        ) : (
          <div className="w-full flex flex-col items-center animate-fade-in-up">
            {/* Active Session Interface */}
            <div className="w-full grid grid-cols-3 gap-6 mb-8">
              {/* Left Panel */}
              <div className="col-span-1 border border-edith-cyan/30 bg-edith-panel backdrop-blur-md p-6 relative overflow-hidden group">
                 <div className="absolute top-0 left-0 w-8 h-[1px] bg-edith-cyan"></div>
                 <div className="absolute top-0 left-0 w-[1px] h-8 bg-edith-cyan"></div>
                 <h3 className="text-edith-cyan tracking-widest text-sm mb-4 border-b border-edith-cyan/30 pb-2 uppercase">System Status</h3>
                 <ul className="space-y-4 text-xs text-gray-300">
                   <li className="flex justify-between items-center">
                     <span>CORE TEMP</span>
                     <span className="text-edith-cyan">42°C</span>
                   </li>
                   <li className="flex justify-between items-center">
                     <span>NETWORK</span>
                     <span className="text-green-400">SECURE</span>
                   </li>
                   <li className="flex justify-between items-center">
                     <span>MEMORY</span>
                     <span className="text-edith-cyan">18%</span>
                   </li>
                 </ul>
              </div>

              {/* Center Visualization */}
              <div className="col-span-1 flex items-center justify-center">
                 <div className="w-48 h-48 rounded-full border border-edith-cyan shadow-[0_0_20px_rgba(0,243,255,0.3)] flex items-center justify-center relative">
                    {/* Simulated Voice Waveform */}
                    <div className="flex gap-1 items-center h-16">
                      {[1, 2, 3, 4, 5, 4, 3, 2, 1].map((val, i) => (
                        <div key={i} className={`w-1 bg-edith-cyan waveform-bar waveform-height-${val} waveform-delay-${i}`}></div>
                      ))}
                    </div>
                 </div>
              </div>

              {/* Right Panel */}
              <div className="col-span-1 border border-edith-cyan/30 bg-edith-panel backdrop-blur-md p-6 relative">
                 <div className="absolute top-0 right-0 w-8 h-[1px] bg-edith-cyan"></div>
                 <div className="absolute top-0 right-0 w-[1px] h-8 bg-edith-cyan"></div>
                 <h3 className="text-edith-cyan tracking-widest text-sm mb-4 border-b border-edith-cyan/30 pb-2 uppercase">Recent Activity</h3>
                  <ul className="space-y-3 text-xs text-gray-400 font-mono">
                   <li>&gt; Loaded subroutines</li>
                   <li>&gt; Synchronized with backend</li>
                   <li>&gt; Awaiting voice input...</li>
                 </ul>
              </div>
            </div>

            {/* Input Area */}
            <div className="w-full max-w-2xl border border-edith-cyan/50 bg-edith-panel p-2 flex items-center backdrop-blur-sm relative shadow-[0_0_15px_rgba(0,243,255,0.1)]">
              <div className="w-2 h-full bg-edith-cyan/50 mr-4"></div>
              <input 
                type="text" 
                placeholder="Awaiting command..." 
                className="w-full bg-transparent border-none outline-none text-white font-mono tracking-wide placeholder-gray-500"
              />
              <button 
                className="ml-4 p-3 bg-edith-cyan/10 hover:bg-edith-cyan/30 text-edith-cyan border border-edith-cyan/50 transition-colors"
                aria-label="Send command"
                title="Send command"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="22" y1="2" x2="11" y2="13"></line><polygon points="22 2 15 22 11 13 2 9 22 2"></polygon></svg>
              </button>
            </div>
          </div>
        )}
      </main>

      {/* Footer Status */}
      <footer className="absolute bottom-0 w-full p-4 flex justify-between text-[10px] tracking-widest text-edith-cyan/50 font-mono">
        <div>LATENCY: 12ms</div>
        <div className="flex gap-4">
          <span className="flex items-center gap-2"><div className="w-1.5 h-1.5 bg-green-500 rounded-full"></div> SYSTEMS NOMINAL</span>
          <span className="flex items-center gap-2"><div className="w-1.5 h-1.5 bg-edith-cyan rounded-full animate-pulse"></div> LISTENING</span>
        </div>
      </footer>
    </div>
  )
}

export default App
