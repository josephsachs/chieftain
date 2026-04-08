import { Character, CharacterTitle } from '../models/Character';
import { CultureGroup } from '../models/Clan';

interface CharacterPanelProps {
  character: Character;
  onClose: () => void;
}

const CULTURE_LABELS: Record<string, string> = {
  [CultureGroup.CANAANITE]: 'Canaanite',
  [CultureGroup.EGYPTIAN]: 'Egyptian',
  [CultureGroup.SHASU]: 'Shasu',
  [CultureGroup.HURRIAN]: 'Hurrian',
  [CultureGroup.AMORITE]: 'Amorite',
  [CultureGroup.HABIRU]: 'Habiru',
  [CultureGroup.UNASSIGNED]: 'Unknown',
};

const TITLE_LABELS: Record<string, string> = {
  [CharacterTitle.NONE]: '',
  [CharacterTitle.CHIEFTAIN]: 'Chieftain',
  [CharacterTitle.PRINCE]: 'Prince',
  [CharacterTitle.GOVERNOR]: 'Governor',
  [CharacterTitle.GENERAL]: 'General',
};

const PERSONALITY_LABELS: Record<string, [string, string]> = {
  cooperatorVsDefector: ['Cooperator', 'Defector'],
  lawfulVsChaotic: ['Lawful', 'Chaotic'],
  grandioseVsInsecure: ['Grandiose', 'Insecure'],
  riskyVsCautious: ['Risky', 'Cautious'],
  ethicalVsAmoral: ['Ethical', 'Amoral'],
  sumptuousVsPrudent: ['Sumptuous', 'Prudent'],
};

function PersonalityBar({ label, value }: { label: [string, string]; value: number }) {
  const pct = Math.round(value * 100);
  return (
    <div className="mb-2">
      <div className="flex justify-between text-xs text-gray-400 mb-1">
        <span>{label[0]}</span>
        <span>{label[1]}</span>
      </div>
      <div className="w-full h-2 bg-gray-700 rounded">
        <div
          className="h-2 rounded"
          style={{
            width: `${pct}%`,
            background: `linear-gradient(90deg, #4FC3F7, #FF8A65)`,
          }}
        />
      </div>
    </div>
  );
}

function StatRow({ name, value }: { name: string; value: number }) {
  return (
    <div className="flex justify-between py-0.5">
      <span className="text-gray-300 capitalize">{name}</span>
      <span className="text-white font-mono">{value}</span>
    </div>
  );
}

const CharacterPanel: React.FC<CharacterPanelProps> = ({ character, onClose }) => {
  const s = character.state;
  const title = s?.title ? TITLE_LABELS[s.title] || s.title : '';
  const culture = s?.culture ? CULTURE_LABELS[s.culture] || s.culture : '';

  return (
    <div className="fixed inset-0 z-50 flex">
      {/* Panel */}
      <div className="w-96 h-full bg-gray-900 border-r border-gray-700 shadow-2xl overflow-y-auto">
        {/* Header */}
        <div className="p-4 border-b border-gray-700">
          <div className="flex justify-between items-start">
            <div>
              <h2 className="text-xl font-bold text-white">{s?.name || 'Unknown'}</h2>
              <div className="text-sm text-gray-400 mt-1">
                {title && <span>{title} &middot; </span>}
                <span>{culture}</span>
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

        {/* Traits */}
        {s?.traits && s.traits.length > 0 && (
          <div className="p-4 border-b border-gray-700">
            <h3 className="text-sm font-semibold text-gray-400 uppercase mb-2">Traits</h3>
            <div className="flex flex-wrap gap-2">
              {s.traits.map((trait: string) => (
                <span
                  key={trait}
                  className="px-2 py-1 bg-gray-700 text-gray-200 text-xs rounded"
                >
                  {trait.charAt(0) + trait.slice(1).toLowerCase()}
                </span>
              ))}
            </div>
          </div>
        )}

        {/* Stats */}
        {s?.stats && (
          <div className="p-4 border-b border-gray-700">
            <h3 className="text-sm font-semibold text-gray-400 uppercase mb-2">Stats</h3>
            <div className="text-sm">
              {Object.entries(s.stats)
                .filter(([, v]) => typeof v === 'number')
                .map(([key, value]) => (
                  <StatRow key={key} name={key} value={value as number} />
                ))}
            </div>
          </div>
        )}

        {/* Personality */}
        {s?.personality && (
          <div className="p-4">
            <h3 className="text-sm font-semibold text-gray-400 uppercase mb-3">Personality</h3>
            {Object.entries(s.personality)
              .filter(([key]) => key in PERSONALITY_LABELS)
              .map(([key, value]) => (
                <PersonalityBar
                  key={key}
                  label={PERSONALITY_LABELS[key]}
                  value={value as number}
                />
              ))}
          </div>
        )}
      </div>

      {/* Backdrop */}
      <div className="flex-1 bg-black bg-opacity-50" onClick={onClose} />
    </div>
  );
};

export default CharacterPanel;
