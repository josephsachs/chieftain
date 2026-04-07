import { GameEntity, filterEntitiesByType } from './GameEntity';
import { CultureGroup } from './Clan';

export interface CharacterStats {
  speech?: number;
  peacekeeping?: number;
  fighting?: number;
  pathfinding?: number;
  trading?: number;
  overseeing?: number;
  scouting?: number;
  intrigue?: number;
  mysticism?: number;
  erudition?: number;
}

export interface CharacterPersonality {
  cooperatorVsDefector?: number;
  lawfulVsChaotic?: number;
  grandioseVsInsecure?: number;
  riskyVsCautious?: number;
  ethicalVsAmoral?: number;
  sumptuousVsPrudent?: number;
}

export enum CharacterTitle {
  NONE = 'NONE',
  CHIEFTAIN = 'CHIEFTAIN',
  PRINCE = 'PRINCE',
  GOVERNOR = 'GOVERNOR',
  GENERAL = 'GENERAL'
}

export enum CharacterTraits {
  RESTLESS = 'RESTLESS',
  GREEDY = 'GREEDY',
  COMPASSIONATE = 'COMPASSIONATE',
  CALLOUS = 'CALLOUS',
  LAWGIVER = 'LAWGIVER',
  ARBITRARY = 'ARBITRARY',
  LITERATE = 'LITERATE',
  STRONG = 'STRONG'
}

export interface Character extends GameEntity {
  type: 'Character';
  state?: {
    name?: string;
    culture?: CultureGroup;
    title?: CharacterTitle;
    stats?: CharacterStats;
    personality?: CharacterPersonality;
    traits?: CharacterTraits[];
    [key: string]: any;
  };
}

export function isCharacter(entity: GameEntity): entity is Character {
  return entity.type === 'Character';
}

export function getCharacters(entities: GameEntity[]): Character[] {
  return filterEntitiesByType<Character>(entities, 'Character');
}
