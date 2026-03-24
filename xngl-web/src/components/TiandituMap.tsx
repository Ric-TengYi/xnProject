import React, { useEffect, useRef, useState } from 'react';
import type { CSSProperties } from 'react';

const TIANDITU_TOKEN = '56b7a41f8b172e519ed8061fcce0789b';
const TIANDITU_SCRIPT_ID = 'tianditu-sdk';
const TIANDITU_SCRIPT_SRC = `https://api.tianditu.gov.cn/api?v=4.0&tk=${TIANDITU_TOKEN}`;

type MapPoint = [number, number];

type MapMarker = {
  id: string;
  position: MapPoint;
  title?: string;
};

type MapPolyline = {
  id: string;
  path: MapPoint[];
  color?: string;
  weight?: number;
  opacity?: number;
};

type MapPolygon = {
  id: string;
  path: MapPoint[];
  color?: string;
  weight?: number;
  opacity?: number;
  fillColor?: string;
  fillOpacity?: number;
};

interface TiandituMapProps {
  center: MapPoint;
  zoom?: number;
  className?: string;
  style?: CSSProperties;
  markers?: MapMarker[];
  polylines?: MapPolyline[];
  polygons?: MapPolygon[];
  loadingText?: string;
}

declare global {
  interface Window {
    T?: any;
    __tiandituLoadingPromise?: Promise<any>;
  }
}

const loadTianditu = async () => {
  if (typeof window === 'undefined') {
    throw new Error('window is not available');
  }
  if (window.T) {
    return window.T;
  }
  if (window.__tiandituLoadingPromise) {
    return window.__tiandituLoadingPromise;
  }

  window.__tiandituLoadingPromise = new Promise((resolve, reject) => {
    const existingScript = document.getElementById(TIANDITU_SCRIPT_ID) as HTMLScriptElement | null;
    if (existingScript) {
      if (window.T) {
        resolve(window.T);
        return;
      }
      existingScript.addEventListener('load', () => resolve(window.T));
      existingScript.addEventListener('error', () => reject(new Error('天地图脚本加载失败')));
      return;
    }

    const script = document.createElement('script');
    script.id = TIANDITU_SCRIPT_ID;
    script.src = TIANDITU_SCRIPT_SRC;
    script.async = true;
    script.onload = () => {
      if (window.T) {
        resolve(window.T);
        return;
      }
      reject(new Error('天地图对象未初始化'));
    };
    script.onerror = () => reject(new Error('天地图脚本加载失败'));
    document.body.appendChild(script);
  });

  return window.__tiandituLoadingPromise;
};

const TiandituMap: React.FC<TiandituMapProps> = ({
  center,
  zoom = 11,
  className,
  style,
  markers = [],
  polylines = [],
  polygons = [],
  loadingText = '天地图加载中...'
}) => {
  const mapElementRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<any>(null);
  const overlayRef = useRef<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let disposed = false;

    const initMap = async () => {
      try {
        const T = await loadTianditu();
        if (disposed || !mapElementRef.current || mapInstanceRef.current) {
          return;
        }
        const map = new T.Map(mapElementRef.current);
        map.centerAndZoom(new T.LngLat(center[0], center[1]), zoom);
        if (typeof map.enableScrollWheelZoom === 'function') {
          map.enableScrollWheelZoom();
        }
        mapInstanceRef.current = map;
        setLoading(false);
      } catch (initError) {
        if (!disposed) {
          setError(initError instanceof Error ? initError.message : '地图初始化失败');
          setLoading(false);
        }
      }
    };

    initMap();

    return () => {
      disposed = true;
      const map = mapInstanceRef.current;
      if (map && typeof map.clearOverLays === 'function') {
        map.clearOverLays();
      }
      overlayRef.current = [];
      mapInstanceRef.current = null;
    };
  }, []);

  useEffect(() => {
    const map = mapInstanceRef.current;
    const T = typeof window !== 'undefined' ? window.T : undefined;
    if (!map || !T) {
      return;
    }

    if (typeof map.clearOverLays === 'function') {
      map.clearOverLays();
    }
    overlayRef.current = [];

    if (typeof map.centerAndZoom === 'function') {
      map.centerAndZoom(new T.LngLat(center[0], center[1]), zoom);
    }

    polylines.forEach((polyline) => {
      if (!polyline.path.length) {
        return;
      }
      const line = new T.Polyline(
        polyline.path.map(([lng, lat]) => new T.LngLat(lng, lat)),
        {
          color: polyline.color ?? '#1677ff',
          weight: polyline.weight ?? 4,
          opacity: polyline.opacity ?? 0.85,
        }
      );
      map.addOverLay(line);
      overlayRef.current.push(line);
    });

    polygons.forEach((polygon) => {
      if (polygon.path.length < 3) {
        return;
      }
      const overlay = new T.Polygon(
        polygon.path.map(([lng, lat]) => new T.LngLat(lng, lat)),
        {
          color: polygon.color ?? '#1677ff',
          weight: polygon.weight ?? 3,
          opacity: polygon.opacity ?? 0.9,
          fillColor: polygon.fillColor ?? '#91caff',
          fillOpacity: polygon.fillOpacity ?? 0.22,
        }
      );
      map.addOverLay(overlay);
      overlayRef.current.push(overlay);
    });

    markers.forEach((marker) => {
      const point = new T.LngLat(marker.position[0], marker.position[1]);
      const markerOverlay = new T.Marker(point);
      if (marker.title && typeof markerOverlay.setTitle === 'function') {
        markerOverlay.setTitle(marker.title);
      }
      map.addOverLay(markerOverlay);
      overlayRef.current.push(markerOverlay);
    });

    const viewportPoints = [
      ...markers.map((marker) => new T.LngLat(marker.position[0], marker.position[1])),
      ...polylines.flatMap((polyline) => polyline.path.map(([lng, lat]) => new T.LngLat(lng, lat))),
      ...polygons.flatMap((polygon) => polygon.path.map(([lng, lat]) => new T.LngLat(lng, lat))),
    ];

    if (viewportPoints.length > 1 && typeof map.setViewport === 'function') {
      map.setViewport(viewportPoints);
    }
  }, [center, zoom, markers, polylines, polygons]);

  return (
    <div className={className} style={style}>
      <div ref={mapElementRef} className="h-full w-full" />
      {loading && (
        <div className="absolute inset-0 g-bg-toolbar flex items-center justify-center pointer-events-none">
          <div className="text-center">
            <div className="text-lg g-text-primary font-semibold">{loadingText}</div>
            <div className="text-sm g-text-secondary mt-2">地图底图与覆盖物正在初始化</div>
          </div>
        </div>
      )}
      {error && (
        <div className="absolute inset-0 g-bg-toolbar flex items-center justify-center pointer-events-none">
          <div className="text-center">
            <div className="text-lg text-red-500 font-semibold">地图加载失败</div>
            <div className="text-sm g-text-secondary mt-2">{error}</div>
          </div>
        </div>
      )}
    </div>
  );
};

export type { MapMarker, MapPoint, MapPolygon, MapPolyline };
export default TiandituMap;
