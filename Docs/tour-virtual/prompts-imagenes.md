# Tour virtual — The Royale · Prompts de imágenes

Panoramas **parciales** para Pannellum: `haov: 160`, `vaov: 55`, relación **3:1**
(160/55 ≈ 2.9). Resolución objetivo: 4800×1600 px, `.jpg`.
10 escenas. Imagen maestra: `recepcion.jpg` (generar primero y adjuntarla como
referencia de estilo en todas las demás).

## 1. Inventario de espacios

Extraído de `DataLoader.java` (tipos de habitación y servicios) y de `landing.html`.

| Espacio | Origen | ¿Escena? |
|---|---|---|
| Normal, Executive, VIP, Luxury, Presidential Suite | `TipoHabitacion` (5 tipos) | Sí (5 escenas) |
| Wellness: piscina climatizada, sauna, hammam, baño de vapor, gym | Servicio *Wellness* | Sí (1 escena; sauna/hammam/gym visibles al fondo) |
| Dining: restaurante, bar de coctelería, comedor privado | Servicio *Dining* | Sí (1 escena, los tres juntos) |
| Business: salas de reunión, executive lounge, business center | Servicio *Business* | Sí (1 escena) |
| Concierge: mostrador de conserjería | Servicio *Concierge* | No independiente — visible en `recepcion` |
| Recepción / lobby | `landing.html` (`lobby.jpg`, `entrada2.PNG`) | Sí (escena maestra) |
| Pasillo de habitaciones | **sugerido** (no existe en los datos; necesario como hub) | Sí |

**Excluidos por no tener espacio fotografiable:** servicio *Transportation* (traslados,
vehículos), Wi-Fi, room service 24h, lavandería, soporte secretarial, y las gestiones
del concierge (tickets, reservas).

## 2. Guía de estilo

- **Concepto:** *The Royale* — hotel boutique de lujo en Midtown Manhattan.
  Art Déco contemporáneo y contenido: «Luxury is not what is added. It is what is understood.»
- **Paleta:** carbón `#252525` · blanco roto `#fafaf9` · latón cepillado `#c6a15b`
  (acento) · nogal oscuro `#3b2b20` · greige suave `#cfcfcf`.
- **Materiales:** mármol crema pulido mate con vetas grises (pisos y mostradores);
  paneles verticales de nogal oscuro ranurado con juntas finas de latón cepillado;
  carpintería y marcos de ventana en acero carbón; vidrio ranurado (*fluted glass*);
  alfombra de lana taupe con geometría discreta; tapicería en bouclé crema y cuero camel.
- **Mobiliario:** siluetas bajas y redondeadas, mid-century con aire déco; patas
  cónicas de nogal, butacas curvas en bouclé, mesas de mármol con base de latón.
- **Iluminación:** golden hour tardío (~17:00), luz natural suave desde ventanales
  + luz interior cálida 2700 K; charcos de luz cálida, sin sombras duras ni contraluces.
- **Elemento firma (se repite en todas las escenas):** muro de **lamas verticales de
  nogal oscuro con juntas de latón cepillado**.
  Motivos secundarios recurrentes: apliques globo de latón y vidrio opalino;
  orquídeas blancas en jarrón de piedra negra.
- **Cámara:** altura de ojos 1,60 m, horizonte nivelado sin inclinación, gran angular
  16 mm (equiv. full frame), f/8.

## 3. Bloque STYLE (pegar idéntico al final de cada prompt)

```
STYLE: ultra-wide panoramic photo, 3:1 aspect ratio, 160-degree horizontal field of view, eye-level camera at 1.6 m, level horizon, no tilt, 16mm full-frame wide-angle lens, f/8, photorealistic architectural photography. The Royale, a boutique luxury hotel in Midtown Manhattan: palette of charcoal #252525, off-white #fafaf9 and brushed brass #c6a15b, with dark walnut and soft greige. Materials: honed cream marble with grey veining, fluted dark-walnut vertical slat panelling with slim brushed-brass reveals, charcoal steel window frames, fluted glass, taupe patterned wool carpet, cream boucle and camel leather upholstery. Furniture: low rounded mid-century art-deco silhouettes, tapered walnut legs, brass-base marble-top tables. Lighting: late golden-hour daylight through large windows plus warm 2700K interior light, soft warm pools of light, no harsh shadows. Recurring details: brass and opal-glass globe sconces, white orchids in black stone vases. Clean, uncluttered, calm. No people, no text, no logos, no signage, no numbers, no watermarks, no camera reflections.
```

## 4. Escenas y conexiones

Orden de generación = orden de la tabla. `recepcion` es el hub; todas las escenas
vuelven a ella en ≤ 2 saltos.

| # | ID escena | Archivo | Prioridad | Lleva a (zona del hotspot) |
|---|---|---|---|---|
| 1 | `recepcion` | `recepcion.jpg` | **esencial** | `restaurante` (izq) · `pasillo` (centro, ascensores) · `spa-piscina` (der) · `salon-ejecutivo` (der, más al fondo) |
| 2 | `pasillo` | `pasillo.jpg` | **esencial** | `recepcion` (centro, ascensores) · `hab-normal` (izq cercana) · `hab-executive` (izq lejana) · `hab-vip` (der cercana) · `hab-luxury` (der media) · `suite-presidencial` (der, puerta doble al fondo) |
| 3 | `hab-normal` | `habitacion-normal.jpg` | opcional | `pasillo` (izq, puerta de entrada) |
| 4 | `hab-executive` | `habitacion-executive.jpg` | opcional | `pasillo` (der, puerta de entrada) |
| 5 | `hab-vip` | `habitacion-vip.jpg` | opcional | `pasillo` (der, vestíbulo de entrada) |
| 6 | `hab-luxury` | `habitacion-luxury.jpg` | opcional | `pasillo` (der, puerta de entrada) |
| 7 | `suite-presidencial` | `suite-presidencial.jpg` | **esencial** | `pasillo` (der, puerta doble del hall) |
| 8 | `restaurante` | `restaurante.jpg` | **esencial** | `recepcion` (der, puerta acristalada) |
| 9 | `spa-piscina` | `spa-piscina.jpg` | **esencial** | `recepcion` (der, corredor de salida) |
| 10 | `salon-ejecutivo` | `salon-ejecutivo.jpg` | opcional | `recepcion` (der, arco del corredor) |

### 4.1 Verificación del recorrido

Grafo completo (10 escenas): `recepcion` alcanza directamente 4 escenas; `pasillo`
abre las 5 habitaciones; cada habitación y cada espacio público tiene exactamente un
hotspot de retorno. Profundidad máxima desde `recepcion`: 2 saltos
(`recepcion` → `pasillo` → habitación). Sin escenas aisladas ni callejones sin salida.

### 4.2 Prioridad: por qué estas 5 son esenciales

Las 5 escenas esenciales forman **un tour completo y navegable por sí solas**, sin
ninguna opcional: `recepcion` → `pasillo` → `suite-presidencial`, y
`recepcion` → `restaurante` / `spa-piscina`. Todas alcanzables, todas con retorno a
recepción. Cubren lobby + conserjería, el hub de habitaciones, la habitación insignia
y los servicios *Dining* y *Wellness*.

- `pasillo` es esencial **porque es el único acceso a las habitaciones**: sin él
  ninguna habitación es alcanzable. No lo dejes para el final.
- `suite-presidencial` es la única habitación esencial: es la insignia
  («Unmatched exclusivity») y evita un `pasillo` sin ninguna puerta que abrir.

Orden sugerido para las opcionales, si vas añadiendo:
`hab-executive` (sección *Business Travel* del landing) → `hab-luxury` (jacuzzi y
vista) → `salon-ejecutivo` (completa el servicio *Business*) → `hab-vip` →
`hab-normal` (las dos más redundantes visualmente).

**Al generar solo un subconjunto:** quita de la configuración de Pannellum los hotspots
que apunten a escenas no generadas (los de `pasillo` hacia las habitaciones que falten
y los de `recepcion` hacia `salon-ejecutivo`). Las puertas siguen apareciendo en la
imagen; simplemente no llevan a ningún lado.

## 5. Prompts

> En las escenas 2–10 adjuntar **`recepcion.jpg`**. En las habitaciones se puede
> adjuntar además la habitación generada antes, para reforzar la continuidad.

### 1 · `recepcion` → `recepcion.jpg` (maestra) — **esencial**

```
Reception and lobby of a boutique luxury hotel in Midtown Manhattan, viewed from the centre of the lobby floor in honed cream marble. LEFT: a tall glazed double doorway in charcoal steel framing the warmly lit restaurant beyond, a dark walnut host stand beside it, and a pair of curved cream boucle armchairs around a round marble coffee table with a brass base. CENTRE: a long reception desk in honed cream marble on a brushed-brass plinth, backed by a full-height fluted dark-walnut slat wall with slim brass reveals; to its side a lower walnut concierge desk with a white orchid in a black stone vase; behind them a recessed elevator lobby with brushed-brass lift doors. RIGHT: a wide marble corridor passing under an arched opening and receding away from the lobby, with brass and opal-glass globe sconces on the walnut-panelled wall and a glimpse of a glass-walled lounge at its far end.

STYLE: <bloque STYLE>
```

### 2 · `pasillo` → `pasillo.jpg` *(espacio sugerido)* — **esencial**

```
Use the attached image as style reference: same hotel, same materials, colors and lighting.

Guest-room corridor of a boutique luxury hotel, seen standing in the middle of the corridor on taupe patterned wool carpet, walls in fluted dark-walnut slat panelling with slim brass reveals. LEFT: two guest-room doors in dark walnut with brushed-brass lever handles and blank brass plates, a brass and opal-glass globe sconce between them. CENTRE: the corridor recedes to a brighter elevator lobby in cream marble with brushed-brass lift doors and a marble console holding white orchids in a black stone vase. RIGHT: two more walnut guest-room doors and, at the far end of the corridor, a wider double door with tall brushed-brass handles; warm golden-hour light spills from a window at the end of the corridor.

STYLE: <bloque STYLE>
```

### 3 · `hab-normal` → `habitacion-normal.jpg` — opcional

```
Use the attached image as style reference: same hotel, same materials, colors and lighting.

A refined standard hotel room for two guests in a boutique luxury hotel, seen from the foot of the bed. LEFT: the walnut entry door standing ajar, revealing a glimpse of the carpeted corridor outside, with a camel leather luggage bench and a slim walnut wardrobe with fluted glass doors beside it. CENTRE: a double bed with crisp cream linen and a tall upholstered headboard set against a fluted dark-walnut slat panel, flanked by two brass and opal-glass globe sconces and small marble bedside tables. RIGHT: a floor-to-ceiling window in a charcoal steel frame with golden-hour light and a soft view of Manhattan rooftops, a curved boucle armchair with a small brass-base marble table, and a partly open bathroom door showing cream marble and brass fittings.

STYLE: <bloque STYLE>
```

### 4 · `hab-executive` → `habitacion-executive.jpg` — opcional

```
Use the attached image as style reference: same hotel, same materials, colors and lighting.

An executive hotel room designed for working travellers in a boutique luxury hotel, seen from the middle of the room. LEFT: a dark walnut writing desk against a tall window in a charcoal steel frame, with a camel leather task chair, a brass desk lamp and golden-hour light raking across the desktop. CENTRE: a king bed with cream linen and a wide upholstered headboard against a fluted dark-walnut slat panel, two brass and opal-glass globe sconces above the marble bedside tables. RIGHT: the walnut entry door with a brushed-brass lever handle standing slightly ajar onto the carpeted corridor, next to a fluted-glass wardrobe, a camel leather luggage bench and a console with white orchids in a black stone vase.

STYLE: <bloque STYLE>
```

### 5 · `hab-vip` → `habitacion-vip.jpg` — opcional

```
Use the attached image as style reference: same hotel, same materials, colors and lighting.

A VIP suite for up to three guests with a separate living area, in a boutique luxury hotel, seen from the living area. LEFT: floor-to-ceiling windows in charcoal steel frames filling the room with golden-hour light over the Manhattan skyline, a curved cream boucle sofa and a low marble coffee table on a brass base. CENTRE: a partial partition of fluted glass and dark-walnut slats separating the living area from the bedroom, where a king bed with cream linen is partly visible under two brass globe sconces. RIGHT: a small entry foyer with the walnut suite door ajar onto the carpeted corridor, a walnut console with white orchids in a black stone vase and a tall brass-framed mirror above it.

STYLE: <bloque STYLE>
```

### 6 · `hab-luxury` → `habitacion-luxury.jpg` — opcional

```
Use the attached image as style reference: same hotel, same materials, colors and lighting.

A luxury suite for up to four guests with a jacuzzi and privileged city views, in a boutique luxury hotel, seen from the centre of the suite. LEFT: a freestanding oval jacuzzi tub on a honed cream marble platform beside a floor-to-ceiling charcoal steel window, brushed-brass filler tap, rolled white towels on a walnut stand and golden-hour light on the water. CENTRE: the living area with a curved cream boucle sofa, a camel leather lounge chair and a marble coffee table in front of a fluted dark-walnut slat wall with slim brass reveals. RIGHT: a wide opening into the bedroom with a king bed and upholstered headboard, and beside it the walnut entry door of the suite standing ajar onto the carpeted corridor.

STYLE: <bloque STYLE>
```

### 7 · `suite-presidencial` → `suite-presidencial.jpg` — **esencial**

```
Use the attached image as style reference: same hotel, same materials, colors and lighting.

The presidential suite of a boutique luxury hotel, for up to six guests, with private dining and a private terrace, seen from the centre of the main salon. LEFT: a private dining table in dark walnut set for eight with cream linen and brass candlesticks, a marble sideboard behind it and a discreet walnut butler-pantry door. CENTRE: the grand living room with double-height windows in charcoal steel frames opening onto a panoramic golden-hour Manhattan skyline, a long cream boucle sofa, camel leather armchairs and a large marble coffee table on a brass base. RIGHT: tall glazed doors leading out to a private terrace with low planters and outdoor lounge seating, and beside them the entry hall of the suite with its double walnut door open onto the carpeted corridor.

STYLE: <bloque STYLE>
```

### 8 · `restaurante` → `restaurante.jpg` — **esencial**

```
Use the attached image as style reference: same hotel, same materials, colors and lighting.

The signature restaurant and cocktail bar of a boutique luxury hotel, seen from the middle of the dining room. LEFT: a craft cocktail bar with a honed cream marble counter, brushed-brass footrail, camel leather stools and backlit brass shelving holding unlabelled bottles and glassware. CENTRE: the dining room with cream boucle banquettes, walnut tables set with white linen, brass cutlery and single white orchids, under a row of brass and opal-glass pendant globes, with tall charcoal steel windows letting in golden-hour light. RIGHT: a private dining room enclosed in fluted glass and dark-walnut slats with a long table for twelve, and beside it the tall glazed double doorway in charcoal steel leading back out to the marble lobby.

STYLE: <bloque STYLE>
```

### 9 · `spa-piscina` → `spa-piscina.jpg` — **esencial**

```
Use the attached image as style reference: same hotel, same materials, colors and lighting.

The wellness floor of a boutique luxury hotel, seen from the long side of the heated indoor pool. LEFT: the still dark-turquoise water of the heated pool reflecting warm light, cream marble coping, and a row of camel leather loungers with rolled white towels on walnut stands. CENTRE: the far end of the pool against a fluted dark-walnut slat wall with slim brass reveals and a backlit marble niche, a stack of folded towels and white orchids in black stone vases. RIGHT: three doorways along the marble wall - a wood-framed glass door into the Finnish sauna, a marble arch into the Turkish hammam with aromatic steam, and a glass-walled gym with dark equipment visible beyond - and a corridor leading back towards the hotel lobby.

STYLE: <bloque STYLE>
```

### 10 · `salon-ejecutivo` → `salon-ejecutivo.jpg` — opcional

```
Use the attached image as style reference: same hotel, same materials, colors and lighting.

The business centre and executive lounge of a boutique luxury hotel, seen from the centre of the lounge. LEFT: a private meeting room behind a glass wall with dark-walnut mullions, a long walnut conference table with camel leather chairs and a blank dark display panel on the fluted slat wall. CENTRE: the executive lounge with cream boucle armchairs and low camel leather sofas around brass-base marble tables, facing a panoramic window wall in charcoal steel frames overlooking the golden-hour Manhattan skyline, with a walnut bar cart to one side. RIGHT: a quiet work nook with two walnut workstations, ergonomic leather chairs and a brass task lamp, and beyond it the arched marble corridor leading back to the hotel lobby.

STYLE: <bloque STYLE>
```
