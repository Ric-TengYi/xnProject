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
      geometry?: { type?: string; coordinates?: unknown };
      features?: Array<{ geometry?: { type?: string; coordinates?: unknown } }>;
    };
    
    let geom = parsed;
    if (parsed.type === 'FeatureCollection' && Array.isArray(parsed.features) && parsed.features.length > 0) {
      geom = parsed.features[0].geometry as any;
    } else if (parsed.type === 'Feature' && parsed.geometry) {
      geom = parsed.geometry as any;
    }

    if (!geom) return [];

    if (geom.type === 'Polygon' && Array.isArray(geom.coordinates) && geom.coordinates.length > 0) {
      return (geom.coordinates[0] as unknown[]).map(toMapPoint).filter((item): item is MapPoint => item != null);
    }
    if (
      geom.type === 'MultiPolygon' &&
      Array.isArray(geom.coordinates) &&
      geom.coordinates.length > 0 &&
      Array.isArray(geom.coordinates[0]) &&
      geom.coordinates[0].length > 0
    ) {
      return ((geom.coordinates[0] as unknown[])[0] as unknown[])
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
      geometry?: { type?: string; coordinates?: unknown };
      features?: Array<{ geometry?: { type?: string; coordinates?: unknown } }>;
    };

    let geom = parsed;
    if (parsed.type === 'FeatureCollection' && Array.isArray(parsed.features) && parsed.features.length > 0) {
      geom = parsed.features[0].geometry as any;
    } else if (parsed.type === 'Feature' && parsed.geometry) {
      geom = parsed.geometry as any;
    }

    if (!geom) return [];

    if (geom.type === 'LineString' && Array.isArray(geom.coordinates)) {
      return geom.coordinates.map(toMapPoint).filter((item): item is MapPoint => item != null);
    }
    if (geom.type === 'MultiLineString' && Array.isArray(geom.coordinates) && geom.coordinates.length > 0) {
      return (geom.coordinates[0] as unknown[]).map(toMapPoint).filter((item): item is MapPoint => item != null);
    }
  } catch (error) {
    console.warn('Failed to parse route GeoJSON', error);
  }
  return [];
}
