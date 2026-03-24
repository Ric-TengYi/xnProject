import type { MapPoint } from '../components/TiandituMap';

function isFiniteNumber(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value);
}

function toMapPoint(value: unknown): MapPoint | null {
  if (!Array.isArray(value) || value.length < 2) {
    return null;
  }
  const [lng, lat] = value;
  if (!isFiniteNumber(lng) || !isFiniteNumber(lat)) {
    return null;
  }
  return [lng, lat];
}

export function parseGeoJsonPolygon(geoJson?: string | null): MapPoint[] {
  if (!geoJson) {
    return [];
  }
  try {
    const parsed = JSON.parse(geoJson) as {
      type?: string;
      coordinates?: unknown;
    };
    if (parsed.type === 'Polygon' && Array.isArray(parsed.coordinates) && parsed.coordinates.length > 0) {
      return (parsed.coordinates[0] as unknown[]).map(toMapPoint).filter((item): item is MapPoint => item != null);
    }
    if (
      parsed.type === 'MultiPolygon' &&
      Array.isArray(parsed.coordinates) &&
      parsed.coordinates.length > 0 &&
      Array.isArray(parsed.coordinates[0]) &&
      parsed.coordinates[0].length > 0
    ) {
      return ((parsed.coordinates[0] as unknown[])[0] as unknown[])
        .map(toMapPoint)
        .filter((item): item is MapPoint => item != null);
    }
  } catch (error) {
    console.warn('Failed to parse site boundary GeoJSON', error);
  }
  return [];
}

export function parseGeoJsonLine(geoJson?: string | null): MapPoint[] {
  if (!geoJson) {
    return [];
  }
  try {
    const parsed = JSON.parse(geoJson) as {
      type?: string;
      coordinates?: unknown;
    };
    if (parsed.type === 'LineString' && Array.isArray(parsed.coordinates)) {
      return parsed.coordinates.map(toMapPoint).filter((item): item is MapPoint => item != null);
    }
    if (parsed.type === 'MultiLineString' && Array.isArray(parsed.coordinates) && parsed.coordinates.length > 0) {
      return (parsed.coordinates[0] as unknown[]).map(toMapPoint).filter((item): item is MapPoint => item != null);
    }
  } catch (error) {
    console.warn('Failed to parse route GeoJSON', error);
  }
  return [];
}
