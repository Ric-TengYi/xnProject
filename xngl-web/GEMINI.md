# xngl-web Project Context

## Project Overview
This is a frontend web application built using **React** (v19) and **TypeScript**, powered by the **Vite** build tool. Based on the directory structure and dependencies, it appears to be a comprehensive enterprise management dashboard system (handling domains like contracts, vehicles, sites, alerts, and projects). The UI is constructed using a combination of **Ant Design** (antd) for complex components and **Tailwind CSS** for utility-first styling.

## Key Technologies
- **Framework:** React 19
- **Language:** TypeScript
- **Build Tool:** Vite
- **Routing:** React Router DOM
- **UI Library:** Ant Design (antd)
- **Styling:** Tailwind CSS, Less, and CSS Modules
- **HTTP Client:** Axios
- **Charts/Visualization:** Recharts
- **Animation:** Framer Motion

## Directory Structure Highlights
- `src/components/`: Reusable UI components (e.g., Maps, Menus).
- `src/pages/`: Route components representing different views in the application (Dashboards, Management tables, Settings).
- `src/layouts/`: Application layout wrappers (e.g., `MainLayout.tsx`).
- `src/contexts/`: React Context providers (e.g., `ThemeContext.tsx`).
- `src/utils/`: Utility functions, API service wrappers (e.g., `request.ts`, various `*Api.ts` files), and helpers.

## Building and Running
The project uses `npm` (inferred from `package-lock.json`).

- **Install dependencies:** `npm install`
- **Start development server:** `npm run dev` (Runs on port `5173` with an API proxy configured for `/api` targeting `http://127.0.0.1:8090`)
- **Build for production:** `npm run build`
- **Preview production build:** `npm run preview`
- **Run linter:** `npm run lint`

## Development Conventions
- **Component Architecture:** Modern React with functional components and hooks.
- **API Integration:** API calls are abstracted into domain-specific service files within `src/utils/` (e.g., `userApi.ts`, `fleetApi.ts`), utilizing `axios` via a central `request.ts` configuration.
- **Path Aliasing:** Configured to use `@/` as an alias for the `src/` directory to avoid relative path hell.
- **Styling Strategy:** Relies heavily on Tailwind CSS for layout and utilities, integrated alongside Ant Design components for complex interactions.
