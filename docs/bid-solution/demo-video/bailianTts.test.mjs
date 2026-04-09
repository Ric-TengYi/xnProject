import test from 'node:test';
import assert from 'node:assert/strict';

import {
  splitNarrationParagraphs,
  buildSpeechPayload,
} from './bailianTts.mjs';

test('splitNarrationParagraphs normalizes whitespace and preserves paragraphs', () => {
  const input = `第一段第一句。
第一段第二句。


第二段   有多余空格。`;

  assert.deepEqual(splitNarrationParagraphs(input), [
    '第一段第一句。 第一段第二句。',
    '第二段 有多余空格。',
  ]);
});

test('buildSpeechPayload builds a Bailian cosyvoice request with overrides', () => {
  const payload = buildSpeechPayload({
    text: '各位领导，大家好。',
    model: 'cosyvoice-v3-plus',
    voice: 'longanhuan',
    format: 'wav',
    sampleRate: 44100,
    rate: 0.94,
    pitch: 0.98,
    volume: 55,
    seed: 42,
    instruction: '请用沉稳自然的语气进行正式汇报。',
  });

  assert.equal(payload.model, 'cosyvoice-v3-plus');
  assert.equal(payload.input.text, '各位领导，大家好。');
  assert.equal(payload.input.voice, 'longanhuan');
  assert.equal(payload.input.format, 'wav');
  assert.equal(payload.input.sample_rate, 44100);
  assert.equal(payload.input.rate, 0.94);
  assert.equal(payload.input.pitch, 0.98);
  assert.equal(payload.input.volume, 55);
  assert.equal(payload.input.seed, 42);
  assert.equal(payload.input.instruction, '请用沉稳自然的语气进行正式汇报。');
  assert.deepEqual(payload.input.language_hints, ['zh']);
});
