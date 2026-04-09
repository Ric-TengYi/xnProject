import fs from 'node:fs/promises';

export function splitNarrationParagraphs(text) {
  return text
    .split(/\n\s*\n+/)
    .map((part) => part.replace(/\s+/g, ' ').trim())
    .filter(Boolean);
}

export function buildSpeechPayload({
  text,
  model = 'cosyvoice-v3-plus',
  voice = 'longanhuan',
  format = 'wav',
  sampleRate = 44100,
  rate = 0.95,
  pitch = 1,
  volume = 50,
  seed,
  instruction,
}) {
  const input = {
    text,
    voice,
    format,
    sample_rate: sampleRate,
    rate,
    pitch,
    volume,
    language_hints: ['zh'],
  };

  if (typeof seed === 'number') {
    input.seed = seed;
  }

  if (instruction) {
    input.instruction = instruction;
  }

  return {
    model,
    input,
  };
}

export async function readParagraphsFromFile(filePath) {
  const raw = await fs.readFile(filePath, 'utf8');
  return splitNarrationParagraphs(raw);
}
