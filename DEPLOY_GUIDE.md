# 🚀 Guía de Despliegue en la Nube: The Royale (Vercel + Railway/Render)

Esta guía explica cómo desplegar la plataforma completa en producción de forma gratuita y en menos de 10 minutos.

---

## 1. Arquitectura de Producción

* **Frontend (Angular 19):** Alojado en **Vercel** (Global Edge CDN, SSL automático).
* **Backend (Spring Boot 3 + Java 17):** Alojado en **Railway** o **Render** (Servidor HTTP en contenedor).
* **Base de Datos:** PostgreSQL en la nube (Neon / Supabase / Railway Postgres) o H2 en memoria para demostraciones rápidas.

---

## 2. Despliegue del Frontend en Vercel

1. Ingresa a [vercel.com](https://vercel.com) e inicia sesión con tu cuenta de GitHub.
2. Haz clic en **"Add New..."** → **"Project"**.
3. Selecciona el repositorio **`TheRoyale`** y la rama **`feature/v2-enhancements`**.
4. En **Root Directory**, haz clic en *Edit* y selecciona la carpeta:
   ```text
   TheRoyale
   ```
5. Vercel detectará automáticamente **Angular**.
   * **Framework Preset:** Angular
   * **Build Command:** `npm run build`
   * **Output Directory:** `dist/the-royale/browser`
6. En **Environment Variables**, agrega:
   * `API_URL` = La URL de tu backend desplegado (ej. `https://the-royale-backend.up.railway.app/api`)
7. Haz clic en **"Deploy"**. En 1 minuto tendrás tu dominio activo con HTTPS:
   `https://the-royale-hotel.vercel.app`

El archivo [`TheRoyale/vercel.json`](./TheRoyale/vercel.json) ya incluye la regla de rewrite para el enrutamiento SPA.

---

## 3. Despliegue del Backend en Railway o Render

### Opción A: Railway (Recomendada - 1 clic)
1. Ingresa a [railway.app](https://railway.app).
2. Haz clic en **"New Project"** → **"Deploy from GitHub repo"**.
3. Selecciona **`TheRoyale`**.
4. Railway detectará el `pom.xml` y ejecutará:
   ```bash
   ./mvnw clean package -DskipTests
   java -jar target/backend-0.0.1-SNAPSHOT.jar
   ```
5. En la pestaña **Settings** → **Networking**, haz clic en **"Generate Domain"** (obtendrás una URL pública como `https://...up.railway.app`).

### Opción B: Render
1. Ingresa a [render.com](https://render.com) → **New Web Service**.
2. Conecta el repositorio GitHub.
3. Configuración:
   * **Runtime:** Java
   * **Build Command:** `./mvnw clean package -DskipTests`
   * **Start Command:** `java -Dserver.port=$PORT -jar target/backend-0.0.1-SNAPSHOT.jar`

---

## 4. Configurar URL en Angular (`environment.prod.ts`)

Actualiza [`TheRoyale/src/environments/environment.prod.ts`](./TheRoyale/src/environments/environment.prod.ts):
```typescript
export const environment = {
  production: true,
  apiUrl: 'https://tu-backend.up.railway.app/api'
};
```
¡Y listo! Tu plataforma completa The Royale estará en línea en todo el mundo.
