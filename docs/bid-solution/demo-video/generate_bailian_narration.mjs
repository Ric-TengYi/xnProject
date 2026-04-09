#!/usr/bin/env node

import fs from 'node:fs/promises';
import path from 'node:path';
import process from 'node:process';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';

import { readParagraphsFromFile, buildSpeechPayload } from './bailianTts.mjs';

const execFileAsync = promisify(execFile);

const [, , inputPathArg, partsDirArg] = process.argv;

if (!inputPathArg || !partsDirArg) {
  console.error('Usage: node generate_bailian_narration.mjs <input-text-file> <parts-dir>');
  process.exit(1);
}

const apiKey = process.env.DASHSCOPE_API_KEY;

if (!apiKey) {
  console.error('DASHSCOPE_API_KEY is required');
  process.exit(1);
}

const model = process.env.BAILIAN_TTS_MODEL || 'cosyvoice-v3-flash';
const voice = process.env.BAILIAN_TTS_VOICE || 'longanwen_v3';
const sampleRate = Number(process.env.BAILIAN_TTS_SAMPLE_RATE || 24000);
const format = 'wav';
const instruction = process.env.BAILIAN_TTS_INSTRUCTION || '请使用中文成年职业女性音色，以正式、专业、自然、可信的项目汇报口吻进行讲解。语气温和沉稳，表达清晰，停顿自然，避免播音腔、营销腔和机械电子感。';
const inputPath = path.resolve(inputPathArg);
const partsDir = path.resolve(partsDirArg);

const paragraphRates = [
  0.68, 0.7, 0.73, 0.73, 0.72, 0.71, 0.72,
  0.73, 0.73, 0.72, 0.71, 0.71, 0.7, 0.7,
  0.72, 0.73, 0.72, 0.72, 0.71, 0.69, 0.67,
];

const paragraphPitches = [
  0.98, 0.99, 1.0, 1.0, 0.99, 0.99, 1.0,
  1.0, 1.0, 0.99, 0.99, 0.99, 0.98, 0.98,
  1.0, 1.0, 1.0, 1.0, 0.99, 0.98, 0.98,
];

async function ensureCleanDirectory(dirPath) {
  await fs.mkdir(dirPath, { recursive: true });
  const entries = await fs.readdir(dirPath);
  await Promise.all(
    entries.map((entry) => fs.rm(path.join(dirPath, entry), { force: true, recursive: true })),
  );
}

async function fetchJson(url, payload, attempt = 1) {
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${apiKey}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const body = await response.text();
    if (attempt < 3 && response.status >= 500) {
      await sleep(800 * attempt);
      return fetchJson(url, payload, attempt + 1);
    }
    throw new Error(`Bailian TTS failed: ${response.status} ${body}`);
  }

  return response.json();
}

async function downloadFile(url, filePath, attempt = 1) {
  const response = await fetch(url);

  if (!response.ok) {
    if (attempt < 3 && response.status >= 500) {
      await sleep(600 * attempt);
      return downloadFile(url, filePath, attempt + 1);
    }
    throw new Error(`Download audio failed: ${response.status} ${url}`);
  }

  const arrayBuffer = await response.arrayBuffer();
  await fs.writeFile(filePath, Buffer.from(arrayBuffer));
}

async function normalizeWaveFile(inputPath, outputPath) {
  await execFileAsync('ffmpeg', [
    '-hide_banner',
    '-y',
    '-i',
    inputPath,
    '-af',
    'highpass=f=70,lowpass=f=7600',
    '-ar',
    '44100',
    '-ac',
    '1',
    outputPath,
  ]);
}

function buildInstruction(modelName, voiceName) {
  if (!instruction) {
    return undefined;
  }

  if ((modelName === 'cosyvoice-v3-flash' || modelName === 'cosyvoice-v3-plus') && voiceName === 'longanyang') {
    return instruction;
  }

  return undefined;
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

const paragraphs = await readParagraphsFromFile(inputPath);

await ensureCleanDirectory(partsDir);

for (const [index, paragraph] of paragraphs.entries()) {
  const payload = buildSpeechPayload({
    text: paragraph,
    model,
    voice,
    format,
    sampleRate,
    rate: paragraphRates[index] ?? 0.94,
    pitch: paragraphPitches[index] ?? 0.99,
    volume: 52,
    seed: 100 + index,
    instruction: buildInstruction(model, voice),
  });

  const result = await fetchJson(
    'https://dashscope.aliyuncs.com/api/v1/services/audio/tts/SpeechSynthesizer',
    payload,
  );

  const audioUrl = result?.output?.audio?.url;
  if (!audioUrl) {
    throw new Error(`Missing audio url in Bailian response for paragraph ${index + 1}`);
  }

  const tempPath = path.join(partsDir, `part-${index + 1}.raw.wav`);
  const outputPath = path.join(partsDir, `part-${index + 1}.wav`);
  await downloadFile(audioUrl, tempPath);
  await normalizeWaveFile(tempPath, outputPath);
  await fs.rm(tempPath, { force: true });
  console.log(`generated paragraph ${index + 1}/${paragraphs.length}`);
}
