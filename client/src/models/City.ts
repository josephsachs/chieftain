import { GameEntity, filterEntitiesByType } from './GameEntity';
import { CultureGroup } from './Clan';

export interface City extends GameEntity {
  type: 'City';
  state?: {
    name?: string;
    location?: {
      x?: number;
      y?: number;
    };
    population?: number;
    culture?: CultureGroup;
    [key: string]: any;
  };
}

export function isCity(entity: GameEntity): entity is City {
  return entity.type === 'City';
}

export function getCities(entities: GameEntity[]): City[] {
  return filterEntitiesByType<City>(entities, 'City');
}

export function getCityCoordinates(city: City): [number, number] | null {
  const x = city.state?.location?.x;
  const y = city.state?.location?.y;

  if (x !== undefined && y !== undefined) {
    return [x, y];
  }

  return null;
}
