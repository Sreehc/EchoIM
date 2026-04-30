import type { StickerDefinition } from '@/types/chat'

export const STICKER_LIBRARY: StickerDefinition[] = [
  {
    stickerId: 'orbit_note',
    title: 'Orbit Note',
    accent: '#f56b5d',
    svg: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 240 240"><rect width="240" height="240" rx="54" fill="#fff7f0"/><circle cx="120" cy="120" r="74" fill="#f56b5d"/><circle cx="92" cy="102" r="12" fill="#1b1a18"/><circle cx="148" cy="102" r="12" fill="#1b1a18"/><path d="M84 150c18 17 54 17 72 0" fill="none" stroke="#1b1a18" stroke-width="12" stroke-linecap="round"/></svg>`,
  },
  {
    stickerId: 'soft_signal',
    title: 'Soft Signal',
    accent: '#0f7b74',
    svg: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 240 240"><rect width="240" height="240" rx="54" fill="#eefcf9"/><path d="M52 168c0-43 30-96 68-96s68 53 68 96" fill="#0f7b74"/><circle cx="120" cy="108" r="34" fill="#fff"/><path d="M104 106h32M110 126h20" stroke="#0f7b74" stroke-width="10" stroke-linecap="round"/></svg>`,
  },
  {
    stickerId: 'midnight_ping',
    title: 'Midnight Ping',
    accent: '#4f46e5',
    svg: `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 240 240"><rect width="240" height="240" rx="54" fill="#f5f4ff"/><circle cx="120" cy="120" r="78" fill="#4f46e5"/><circle cx="92" cy="104" r="10" fill="#fff"/><circle cx="148" cy="104" r="10" fill="#fff"/><path d="M84 144c11-9 21-13 36-13s25 4 36 13" fill="none" stroke="#fff" stroke-width="12" stroke-linecap="round"/></svg>`,
  },
]

const STICKERS_BY_ID = new Map(STICKER_LIBRARY.map((sticker) => [sticker.stickerId, sticker]))

export function findStickerDefinition(stickerId: string | null | undefined) {
  if (!stickerId) return null
  return STICKERS_BY_ID.get(stickerId) ?? null
}
