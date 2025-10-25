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

interface RenderOptions {
  showSelection?: boolean;
  selectedHex?: { x: number; y: number } | null;
}

class MapRenderer {
  private canvas: HTMLCanvasElement;
  private ctx: CanvasRenderingContext2D;
  
  // Hex grid constants
  private readonly HEX_RADIUS = 48;
  private readonly BORDER_PADDING = 50;
  
  // Terrain colors
  private readonly TERRAIN_COLORS: { [key: string]: string } = {
    GRASSLAND: '#4CAF50',
    MEADOW: '#8BC34A',
    SCRUB: '#CDDC39',
    WOODLAND: '#2E7D32',
    ROCKLAND: '#795548',
    DRYLAND: '#FFC107',
  };
  
  private readonly DEFAULT_COLOR = '#9E9E9E';

  constructor(canvas: HTMLCanvasElement) {
    this.canvas = canvas;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      throw new Error('Could not get 2D context from canvas');
    }
    this.ctx = ctx;
  }

  render(mapData: MapZone[], pan: { x: number; y: number }, options: RenderOptions = {}) {
    this.clearCanvas();
    this.applyTransform(pan);
    
    const { maxX, maxY, zoneMap } = this.processMapData(mapData);
    
    // Render terrain layer
    this.renderTerrainLayer(maxX, maxY, zoneMap, options);
    
    // Future layers will go here:
    // this.renderTileLayer(maxX, maxY, zoneMap, options);
    // this.renderOverlayLayer(maxX, maxY, zoneMap, options);
    
    this.restoreTransform();
  }

  private clearCanvas() {
    this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
  }

  private applyTransform(pan: { x: number; y: number }) {
    this.ctx.save();
    this.ctx.translate(pan.x, pan.y);
  }

  private restoreTransform() {
    this.ctx.restore();
  }

  private processMapData(mapData: MapZone[]) {
    let maxX = 0;
    let maxY = 0;
    
    if (mapData.length > 0) {
      mapData.forEach(zone => {
        if (zone.state?.location?.x !== undefined && zone.state?.location?.y !== undefined) {
          maxX = Math.max(maxX, zone.state.location.x);
          maxY = Math.max(maxY, zone.state.location.y);
        }
      });
    }
    
    const zoneMap = new Map<string, MapZone>();
    mapData.forEach(zone => {
      if (zone.state?.location?.x !== undefined && zone.state?.location?.y !== undefined) {
        const key = `${zone.state.location.x},${zone.state.location.y}`;
        zoneMap.set(key, zone);
      }
    });
    
    return { maxX, maxY, zoneMap };
  }

  private renderTerrainLayer(maxX: number, maxY: number, zoneMap: Map<string, MapZone>, options: RenderOptions) {
    for (let y = 0; y <= maxY; y++) {
      for (let x = 0; x <= maxX; x++) {
        const [pixelX, pixelY] = this.gridToPixel(x, y);
        const key = `${x},${y}`;
        const zone = zoneMap.get(key);
        
        let color = this.DEFAULT_COLOR;
        if (zone?.state?.terrainType) {
          color = this.TERRAIN_COLORS[zone.state.terrainType] || this.DEFAULT_COLOR;
        }
        
        this.drawHex(pixelX, pixelY, color);
        
        // Handle selection highlighting
        if (options.showSelection && options.selectedHex && 
            options.selectedHex.x === x && options.selectedHex.y === y) {
          this.drawSelectionHighlight(pixelX, pixelY);
        }
      }
    }
  }

  private drawHex(centerX: number, centerY: number, color: string) {
    const vertices = this.getHexVertices(centerX, centerY);
    
    this.ctx.beginPath();
    this.ctx.moveTo(vertices[0][0], vertices[0][1]);
    for (let i = 1; i < vertices.length; i++) {
      this.ctx.lineTo(vertices[i][0], vertices[i][1]);
    }
    this.ctx.closePath();
    
    // Fill hex
    this.ctx.fillStyle = color;
    this.ctx.fill();
    
    // Draw border
    this.ctx.strokeStyle = '#333';
    this.ctx.lineWidth = 1;
    this.ctx.stroke();
  }

  private drawSelectionHighlight(centerX: number, centerY: number) {
    const vertices = this.getHexVertices(centerX, centerY);
    
    this.ctx.beginPath();
    this.ctx.moveTo(vertices[0][0], vertices[0][1]);
    for (let i = 1; i < vertices.length; i++) {
      this.ctx.lineTo(vertices[i][0], vertices[i][1]);
    }
    this.ctx.closePath();
    
    this.ctx.strokeStyle = '#FFD700'; // Gold color for selection
    this.ctx.lineWidth = 3;
    this.ctx.stroke();
  }

  private getHexVertices(centerX: number, centerY: number): [number, number][] {
    const vertices: [number, number][] = [];
    for (let i = 0; i < 6; i++) {
      const angle = (Math.PI / 3) * i;
      const x = centerX + this.HEX_RADIUS * Math.cos(angle);
      const y = centerY + this.HEX_RADIUS * Math.sin(angle);
      vertices.push([x, y]);
    }
    return vertices;
  }

  private gridToPixel(gridX: number, gridY: number): [number, number] {
    const hexWidth = this.HEX_RADIUS * 2;
    const hexHeight = this.HEX_RADIUS * Math.sqrt(3);
    
    let pixelX = gridX * hexWidth * 0.75;
    let pixelY = gridY * hexHeight;
    
    // Offset odd columns (vertical offset)
    if (gridX % 2 === 1) {
      pixelY += hexHeight * 0.5;
    }
    
    // Add border padding
    pixelX += this.BORDER_PADDING;
    pixelY += this.BORDER_PADDING;
    
    return [pixelX, pixelY];
  }
}

export default MapRenderer;