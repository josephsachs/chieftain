import { Clan, ClanBehavior, ClanHealth, ClanSkills, getClanColor } from '../models/Clan';
import { GameEntity } from '../models/GameEntity';
import { Character, isCharacter } from '../models/Character';

interface ClanPanelProps {
  clan: Clan;
  entities: GameEntity[];
  onClose: () => void;
  onOpenCharacter: (character: Character) => void;
}

const BEHAVIOR_LABELS: Record<string, string> = {
  [ClanBehavior.NONE]: 'Idle',
  [ClanBehavior.WANDERING]: 'Wandering',
  [ClanBehavior.TRAVELING]: 'Traveling',
  [ClanBehavior.LABORING]: 'Laboring',
  [ClanBehavior.TRADING]: 'Trading',
  [ClanBehavior.FIGHTING]: 'Fighting',
  [ClanBehavior.RECOVERING]: 'Recovering',
  [ClanBehavior.HOLIDAY]: 'Holiday',
};

function HealthBar({ label, value }: { label: string; value: number }) {
  const color =
    value > 66 ? 'bg-green-500' :
    value > 33 ? 'bg-yellow-500' :
    'bg-red-500';

  return (
    <div className="mb-2">
      <div className="flex justify-between text-xs text-gray-400 mb-1">
        <span>{label}</span>
        <span>{value}</span>
      </div>
      <div className="w-full h-2 bg-gray-700 rounded">
        <div className={`h-2 rounded ${color}`} style={{ width: `${value}%` }} />
      </div>
    </div>
  );
}

function SkillRow({ name, value }: { name: string; value: number }) {
  if (value === 0) return null;
  return (
    <div className="flex justify-between py-0.5">
      <span className="text-gray-300 capitalize">{name}</span>
      <span className="text-white font-mono">{value}</span>
    </div>
  );
}

const ClanPanel: React.FC<ClanPanelProps> = ({ clan, entities, onClose, onOpenCharacter }) => {
  const s = clan.state;
  const color = getClanColor(clan);
  const behavior = clan.properties?.behavior || 'NONE';
  const health: ClanHealth = s?.health || {};
  const skills: ClanSkills = s?.skills || {};

  // Find the chieftain Character entity by chieftainId
  const chieftainId = s?.chieftainId as string | undefined;
  const chieftainEntity = chieftainId
    ? entities.find(e => e._id === chieftainId && isCharacter(e)) as Character | undefined
    : undefined;

  const chieftainName = chieftainEntity?.state?.name || chieftainId || 'None';

  // Depot is serialized as { contents: { FOOD: { CORN: 0, ... }, ... } }
  const depotContents = s?.depot?.contents as Record<string, Record<string, number>> | undefined;

  // Count non-zero skills
  const hasSkills = Object.values(skills).some(v => typeof v === 'number' && v > 0);

  return (
    <div className="fixed inset-0 z-50 flex">
      {/* Panel */}
      <div className="w-96 h-full bg-gray-900 border-r border-gray-700 shadow-2xl overflow-y-auto">
        {/* Header */}
        <div className="p-4 border-b border-gray-700">
          <div className="flex justify-between items-start">
            <div className="flex items-center gap-3">
              <div
                className="w-8 h-8 rounded-full border-2 border-gray-600"
                style={{ backgroundColor: color }}
              />
              <div>
                <h2 className="text-xl font-bold text-white">{s?.name || 'Unknown'}</h2>
                <div className="text-sm text-gray-400">
                  {s?.culture || 'Unknown'} &middot; Pop. {s?.population || 0}
                </div>
              </div>
            </div>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-white text-lg px-2"
            >
              &times;
            </button>
          </div>
        </div>

        {/* Behavior */}
        <div className="p-4 border-b border-gray-700">
          <div className="flex justify-between items-center">
            <span className="text-sm text-gray-400">Behavior</span>
            <span className="text-sm text-white">{BEHAVIOR_LABELS[behavior] || behavior}</span>
          </div>
          <div className="flex justify-between items-center mt-1">
            <span className="text-sm text-gray-400">Location</span>
            <span className="text-sm text-white font-mono">
              ({s?.location?.x}, {s?.location?.y})
            </span>
          </div>
        </div>

        {/* Chieftain link */}
        <div className="p-4 border-b border-gray-700">
          <h3 className="text-sm font-semibold text-gray-400 uppercase mb-2">Chieftain</h3>
          {chieftainEntity ? (
            <button
              onClick={() => onOpenCharacter(chieftainEntity)}
              className="text-blue-400 hover:text-blue-300 text-sm underline cursor-pointer"
            >
              {chieftainName}
            </button>
          ) : (
            <span className="text-sm text-gray-300">{chieftainName}</span>
          )}
        </div>

        {/* Health */}
        <div className="p-4 border-b border-gray-700">
          <h3 className="text-sm font-semibold text-gray-400 uppercase mb-2">Health</h3>
          <HealthBar label="Satiety" value={health.satiety ?? 100} />
          <HealthBar label="Stamina" value={health.stamina ?? 100} />
          <HealthBar label="Heart" value={health.heart ?? 100} />
        </div>

        {/* Skills */}
        {hasSkills && (
          <div className="p-4 border-b border-gray-700">
            <h3 className="text-sm font-semibold text-gray-400 uppercase mb-2">Skills</h3>
            <div className="text-sm">
              {Object.entries(skills)
                .filter(([, v]) => typeof v === 'number' && v > 0)
                .map(([key, value]) => (
                  <SkillRow key={key} name={key} value={value as number} />
                ))}
            </div>
          </div>
        )}

        {/* Depot summary */}
        {depotContents && Object.keys(depotContents).length > 0 && (
          <div className="p-4">
            <h3 className="text-sm font-semibold text-gray-400 uppercase mb-2">Depot</h3>
            <div className="text-sm">
              {Object.entries(depotContents).map(([group, items]) => {
                if (typeof items !== 'object' || items === null) return null;
                const nonZero = Object.entries(items as Record<string, number>)
                  .filter(([, v]) => v > 0);
                if (nonZero.length === 0) return null;

                return (
                  <div key={group} className="mb-2">
                    <div className="text-xs text-gray-500 uppercase mb-1">{group}</div>
                    {nonZero.map(([resource, qty]) => (
                      <div key={resource} className="flex justify-between py-0.5 pl-2">
                        <span className="text-gray-300 capitalize">
                          {resource.toLowerCase()}
                        </span>
                        <span className="text-white font-mono">{qty}</span>
                      </div>
                    ))}
                  </div>
                );
              })}
            </div>
          </div>
        )}
      </div>

      {/* Backdrop */}
      <div className="flex-1 bg-black bg-opacity-50" onClick={onClose} />
    </div>
  );
};

export default ClanPanel;
