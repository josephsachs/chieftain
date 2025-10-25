import { useRef, useEffect, useState, useCallback } from 'react';
import MapRenderer from './MapRenderer';

interface MapZone {
  _id: string;
  type: string;
  state?: {
    location?: {
      x?: number;
      y?: number;
    };
    terrainType?: string;
    [key: string]: any;
  };
  [key: string]: any;
}

interface GameMapProps {
  mapData: MapZone[];
}

const GameMap: React.FC<GameMapProps> = ({ mapData }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const rendererRef = useRef<MapRenderer | null>(null);
  const [pan, setPan] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [lastMousePos, setLastMousePos] = useState({ x: 0, y: 0 });
  const [selectedHex, setSelectedHex] = useState<{ x: number; y: number; zone?: MapZone } | null>(null);
  const [showInfoPanel, setShowInfoPanel] = useState(false);
  const [infoPanelPos, setInfoPanelPos] = useState({ x: 0, y: 0 });

  // Hex grid constants
  const HEX_RADIUS = 48;
  const BORDER_PADDING = 50;

  // Initialize renderer when canvas is ready
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    
    try {
      rendererRef.current = new MapRenderer(canvas);
    } catch (error) {
      console.error('Failed to initialize MapRenderer:', error);
    }
  }, []);

  // Convert pixel coordinates back to hex grid coordinates
  const pixelToGrid = (pixelX: number, pixelY: number): [number, number] => {
    // Remove border padding and pan offset
    const adjustedX = pixelX - BORDER_PADDING - pan.x;
    const adjustedY = pixelY - BORDER_PADDING - pan.y;
    
    const hexWidth = HEX_RADIUS * 2;
    const hexHeight = HEX_RADIUS * Math.sqrt(3);
    
    // Rough grid coordinate estimate
    const gridX = Math.round(adjustedX / (hexWidth * 0.75));
    const gridY = Math.round(adjustedY / hexHeight);
    
    // Check nearby hexes to find the closest one
    const candidates = [];
    for (let dx = -1; dx <= 1; dx++) {
      for (let dy = -1; dy <= 1; dy++) {
        const testX = gridX + dx;
        const testY = gridY + dy;
        if (testX >= 0 && testY >= 0) {
          const [hexPixelX, hexPixelY] = gridToPixel(testX, testY);
          const distance = Math.sqrt(
            Math.pow(pixelX - hexPixelX - pan.x, 2) + 
            Math.pow(pixelY - hexPixelY - pan.y, 2)
          );
          candidates.push({ x: testX, y: testY, distance });
        }
      }
    }
    
    // Return the closest valid hex
    candidates.sort((a, b) => a.distance - b.distance);
    return candidates.length > 0 ? [candidates[0].x, candidates[0].y] : [gridX, gridY];
  };

  const gridToPixel = (gridX: number, gridY: number): [number, number] => {
    const hexWidth = HEX_RADIUS * 2;
    const hexHeight = HEX_RADIUS * Math.sqrt(3);
    
    let pixelX = gridX * hexWidth * 0.75;
    let pixelY = gridY * hexHeight;
    
    // Offset odd columns (vertical offset)
    if (gridX % 2 === 1) {
      pixelY += hexHeight * 0.5;
    }
    
    // Add border padding
    pixelX += BORDER_PADDING;
    pixelY += BORDER_PADDING;
    
    return [pixelX, pixelY];
  };

  // Render the map
  const renderMap = useCallback(() => {
    const renderer = rendererRef.current;
    if (!renderer) return;
    
    renderer.render(mapData, pan, {
      showSelection: true,
      selectedHex: selectedHex
    });
  }, [mapData, pan, selectedHex]);

  // Handle canvas resize
  const resizeCanvas = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * window.devicePixelRatio;
    canvas.height = rect.height * window.devicePixelRatio;
    
    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
    }
    
    renderMap();
  }, [renderMap]);

  // Mouse event handlers for panning and clicking
  const handleMouseDown = (e: React.MouseEvent) => {
    setIsDragging(true);
    setLastMousePos({ x: e.clientX, y: e.clientY });
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isDragging) return;
    
    const deltaX = e.clientX - lastMousePos.x;
    const deltaY = e.clientY - lastMousePos.y;
    
    setPan(prev => ({
      x: prev.x + deltaX,
      y: prev.y + deltaY
    }));
    
    setLastMousePos({ x: e.clientX, y: e.clientY });
  };

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  const handleClick = (e: React.MouseEvent) => {
    if (isDragging) return; // Don't process clicks if we were dragging
    
    const canvas = canvasRef.current;
    if (!canvas) return;
    
    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    
    const [gridX, gridY] = pixelToGrid(x, y);
    
    // Find the zone at this grid position
    const zone = mapData.find(z => 
      z.state?.location?.x === gridX && z.state?.location?.y === gridY
    );
    
    if (zone) {
      // Clicked on a valid hex with data
      setSelectedHex({ x: gridX, y: gridY, zone });
      setInfoPanelPos({ x: e.clientX, y: e.clientY });
      setShowInfoPanel(true);
    } else {
      // Clicked outside valid hex area
      setShowInfoPanel(false);
      setSelectedHex(null);
    }
  };

  const handleRightClick = (e: React.MouseEvent) => {
    e.preventDefault(); // Still prevent context menu just in case
  };

  // Keyboard panning
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      const panSpeed = 20;
      
      switch (e.key) {
        case 'ArrowUp':
          setPan(prev => ({ ...prev, y: prev.y + panSpeed }));
          break;
        case 'ArrowDown':
          setPan(prev => ({ ...prev, y: prev.y - panSpeed }));
          break;
        case 'ArrowLeft':
          setPan(prev => ({ ...prev, x: prev.x + panSpeed }));
          break;
        case 'ArrowRight':
          setPan(prev => ({ ...prev, x: prev.x - panSpeed }));
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Setup canvas and resize handling
  useEffect(() => {
    resizeCanvas();
    window.addEventListener('resize', resizeCanvas);
    return () => window.removeEventListener('resize', resizeCanvas);
  }, [resizeCanvas]);

  // Re-render when map data or pan changes
  useEffect(() => {
    renderMap();
  }, [renderMap]);

  return (
    <div className="w-full h-full relative">
      <canvas
        ref={canvasRef}
        className="w-full h-full cursor-grab active:cursor-grabbing"
        style={{ cursor: isDragging ? 'grabbing' : 'grab' }}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
        onClick={handleClick}
        onContextMenu={handleRightClick}
      />
      
      {/* Info Panel */}
      {showInfoPanel && selectedHex && (
        <div 
          className="absolute bg-gray-800 text-white p-3 rounded shadow-lg border border-gray-600 z-50"
          style={{ 
            left: infoPanelPos.x + 10, 
            top: infoPanelPos.y + 10,
            maxWidth: '200px'
          }}
        >
          <div className="text-sm font-semibold mb-2">Hex Information</div>
          <div className="text-xs space-y-1">
            <div>Coordinates: ({selectedHex.x}, {selectedHex.y})</div>
            {selectedHex.zone ? (
              <>
                <div>Terrain: {selectedHex.zone.state?.terrainType || 'Unknown'}</div>
                <div>ID: {selectedHex.zone._id}</div>
              </>
            ) : (
              <div className="text-gray-400">No zone data</div>
            )}
          </div>
          <div className="text-xs text-gray-400 mt-2">Left-click to dismiss</div>
        </div>
      )}
      
      {/* Debug info */}
      <div className="absolute top-2 right-2 bg-black bg-opacity-50 text-white text-xs p-2 rounded">
        <div>MapZones: {mapData.length}</div>
        <div>Pan: {pan.x.toFixed(0)}, {pan.y.toFixed(0)}</div>
        {selectedHex && (
          <div className="text-yellow-300">Selected: ({selectedHex.x}, {selectedHex.y})</div>
        )}
        <div className="text-gray-300 mt-1">Arrow keys or drag to pan</div>
        <div className="text-gray-300">Click hex for info</div>
      </div>
    </div>
  );
};

export default GameMap;