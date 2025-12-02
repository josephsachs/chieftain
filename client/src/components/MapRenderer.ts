import { GameEntity } from '../models/GameEntity';
import { MapZone, getMapZoneCoordinates, getTerrainColor } from '../models/MapZone';
import { Clan, getClanCoordinates, getClanColor } from '../models/Clan';

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

  constructor(canvas: HTMLCanvasElement) {
    this.canvas = canvas;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      throw new Error('Could not get 2D context from canvas');
    }
    this.ctx = ctx;
  }

  render(
    entities: GameEntity[],
    pan: { x: number; y: number },
    options: RenderOptions = {}
  ) {
    this.clearCanvas();
    this.applyTransform(pan);

    // Process map data
    const { maxX, maxY, zoneMap } = this.processMapData(entities);
    const clanMap = this.processClans(entities);

    // Render terrain layer
    this.renderTerrainLayer(maxX, maxY, zoneMap, options);

    // Render clans on top of terrain
    this.renderClans(clanMap);

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

  private processMapData(entities: GameEntity[]) {
    let maxX = 0;
    let maxY = 0;

    const mapZones = entities.filter(e => e.type === 'MapZone') as MapZone[];
    const zoneMap = new Map<string, MapZone>();

    mapZones.forEach(zone => {
      const coords = getMapZoneCoordinates(zone);
      if (coords) {
        const [x, y] = coords;
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);

        const key = `${x},${y}`;
        zoneMap.set(key, zone);
      }
    });

    return { maxX, maxY, zoneMap };
  }

  private processClans(entities: GameEntity[]) {
    const clans = entities.filter(e => e.type === 'Clan') as Clan[];
    const clanMap = new Map<string, Clan[]>();

    clans.forEach(clan => {
      const coords = getClanCoordinates(clan);
      if (coords) {
        const [x, y] = coords;
        const key = `${x},${y}`;

        if (!clanMap.has(key)) {
          clanMap.set(key, []);
        }

        clanMap.get(key)?.push(clan);
      }
    });

    return clanMap;
  }

  private renderTerrainLayer(maxX: number, maxY: number, zoneMap: Map<string, MapZone>, options: RenderOptions) {
    for (let y = 0; y <= maxY; y++) {
      for (let x = 0; x <= maxX; x++) {
        const [pixelX, pixelY] = this.gridToPixel(x, y);
        const key = `${x},${y}`;
        const zone = zoneMap.get(key);

        let color = '#9E9E9E'; // Default color
        if (zone?.state?.terrainType) {
          color = getTerrainColor(zone.state.terrainType);
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

  private renderClans(clanMap: Map<string, Clan[]>) {
    clanMap.forEach((clans, key) => {
      const [x, y] = key.split(',').map(Number);
      const [pixelX, pixelY] = this.gridToPixel(x, y);

      // If there's only one clan at this location
      if (clans.length === 1) {
        this.drawClan(pixelX, pixelY, clans[0]);
      }
      // If there are multiple clans at this location
      else if (clans.length > 1) {
        this.drawMultipleClans(pixelX, pixelY, clans);
      }
    });
  }

  private drawClan(centerX: number, centerY: number, clan: Clan) {
    const color = getClanColor(clan);
    const radius = this.HEX_RADIUS * 0.4; // Clan size relative to hex

    // Draw clan circle
    this.ctx.beginPath();
    this.ctx.arc(centerX, centerY, radius, 0, Math.PI * 2);
    this.ctx.fillStyle = color;
    this.ctx.fill();

    // Draw border
    this.ctx.strokeStyle = '#333';
    this.ctx.lineWidth = 2;
    this.ctx.stroke();

    // Draw clan name if available
    if (clan.state?.name) {
      this.ctx.font = '12px Arial';
      this.ctx.textAlign = 'center';
      this.ctx.textBaseline = 'middle';
      this.ctx.fillStyle = '#FFF';
      this.ctx.fillText(clan.state.name, centerX, centerY);
    }
  }

  private drawMultipleClans(centerX: number, centerY: number, clans: Clan[]) {
    // Draw a stacked appearance for multiple clans
    const baseRadius = this.HEX_RADIUS * 0.4;
    const sliceAngle = (Math.PI * 2) / clans.length;

    // Draw a segmented circle for the clans
    for (let i = 0; i < clans.length; i++) {
      const startAngle = i * sliceAngle;
      const endAngle = (i + 1) * sliceAngle;

      this.ctx.beginPath();
      this.ctx.moveTo(centerX, centerY);
      this.ctx.arc(centerX, centerY, baseRadius, startAngle, endAngle);
      this.ctx.closePath();

      this.ctx.fillStyle = getClanColor(clans[i]);
      this.ctx.fill();

      this.ctx.strokeStyle = '#333';
      this.ctx.lineWidth = 1;
      this.ctx.stroke();
    }

    // Draw count in the middle
    this.ctx.font = '12px Arial';
    this.ctx.textAlign = 'center';
    this.ctx.textBaseline = 'middle';
    this.ctx.fillStyle = '#FFF';
    this.ctx.fillText(clans.length.toString(), centerX, centerY);
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