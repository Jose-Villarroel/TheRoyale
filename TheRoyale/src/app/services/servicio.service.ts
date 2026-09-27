import { Injectable } from '@angular/core';
import { Servicio } from '../models/servicio.model';

@Injectable({ providedIn: 'root' })
export class ServicioService {

  // ===== "Base de datos" quemada, calcada del DataLoader del backend =====
  private servicios: Servicio[] = [
    {
      id: 1,
      nombre: 'Wellness',
      descripcion: 'Immerse yourself in total relaxation at The Royale Spa. Our full wellness sanctuary is designed around your wellbeing - from ancient thermal rituals to modern fitness, every detail is curated for the discerning guest.',
      precio: 45,
      imagenUrl: '/images/spa.webp',
      caracteristicas: [
        'Heated Indoor Pool - Open daily 06:00 to 22:00',
        'Finnish Sauna - Dry heat up to 90 degrees, private sessions available',
        'Turkish Hammam - Traditional steam bath with aromatic oils',
        'Aromatherapy Steam Room - Eucalyptus and lavender infusions',
        'Full Gym 24h - Technogym equipment, personal trainer on request',
        'Signature Massage - 60 or 90 min, Swedish, deep tissue and hot stone',
        'Organic Facials - Premium skincare with ESPA and La Mer products',
        'Private Couple Suite - Exclusive treatment room for two'
      ],
      galeriaUrls: ['/images/spa.webp', '/images/architecture.jpg', '/images/new_spa.jpg']
    },
    {
      id: 2,
      nombre: 'Dining',
      descripcion: 'Experience exceptional cuisine without leaving The Royale. Our signature restaurant serves contemporary New York cuisine from breakfast through late-night dining. In-room service is available 24 hours a day.',
      precio: 60,
      imagenUrl: '/images/dinningroom-1.webp',
      caracteristicas: [
        '24h Room Service - Full a la carte menu delivered to your suite',
        'Signature Restaurant - Contemporary New York cuisine, breakfast to dinner',
        'Private Dining Room - Exclusive setting for up to 12 guests',
        'Premium Wine List - Over 200 labels from world-renowned vineyards',
        'Craft Cocktail Bar - Handcrafted cocktails and spirits by our mixologists',
        'In-Room Minibar - Curated selection refreshed daily',
        'Private Bar Service - Butler-attended bar set up in your suite',
        'Dietary Menus - Vegan, gluten-free and allergen-aware options available'
      ],
      galeriaUrls: ['/images/dinningroom-1.webp', '/images/restaurant.jpg', '/images/new_dining.jpg']
    },
    {
      id: 3,
      nombre: 'Business',
      descripcion: 'Stay productive from the heart of Manhattan. The Royale Business Center delivers everything corporate guests need - from state-of-the-art meeting technology to full executive support, all within steps of your room.',
      precio: 35,
      imagenUrl: '/images/meeting-room.jpg',
      caracteristicas: [
        'Private Meeting Rooms - Up to 3 rooms, capacity 4 to 20 persons',
        '4K Video Conferencing - Integrated Zoom and Teams, global connectivity',
        'High-Speed Fiber Wi-Fi - Dedicated bandwidth up to 1 Gbps',
        'Executive Lounge - Reserved workspace with panoramic Manhattan views',
        'Printing and Secretarial - On-demand document handling and admin support',
        'Business Center - 24h access, iMac workstations and ergonomic seating',
        'Event Planning - Full AV setup and catering coordination',
        'Concierge Business Support - Courier, notary and translation services'
      ],
      galeriaUrls: ['/images/meeting-room.jpg', '/images/skyline.jpg', '/images/new_lounge.jpg']
    },
    {
      id: 4,
      nombre: 'Concierge',
      descripcion: 'Our multilingual concierge team is available around the clock to orchestrate every aspect of your New York experience. No request is too extraordinary - we specialise in turning the impossible into the unforgettable.',
      precio: 25,
      imagenUrl: '/images/concierge.jpg',
      caracteristicas: [
        'Broadway and Show Tickets - Priority access to sold-out performances',
        'Fine Dining Reservations - Michelin-starred restaurants and exclusive tables',
        'Private Museum Tours - After-hours access to MET, MoMA and more',
        'Helicopter Rides - Scenic Manhattan flights from the East River helipad',
        'Personal Shopping - Stylist-led experiences on Fifth Avenue',
        'Airport Transfers - Chauffeured luxury vehicles, 24h availability',
        'Floral and Gift Arrangements - Bespoke in-suite welcome experiences',
        'Multilingual Assistance - Staff fluent in 8+ languages'
      ],
      galeriaUrls: ['/images/concierge.jpg', '/images/broadway.jpg', '/images/new_concierge.jpg']
    },
    {
      id: 5,
      nombre: 'Transportation',
      descripcion: 'Move through New York with the same ease and discretion you expect inside The Royale. Our transportation service coordinates private transfers, chauffeured vehicles and curated city routes for guests who value punctuality, comfort and privacy.',
      precio: 80,
      imagenUrl: '/images/private-transport.jpg',
      caracteristicas: [
        'Airport Transfers - Private arrivals and departures from JFK, LaGuardia and Newark',
        'Chauffeured Vehicles - Luxury sedans and SUVs available by the hour',
        'City Routes - Tailored itineraries across Manhattan, Brooklyn and beyond',
        'Event Transfers - Coordinated pickups for galas, meetings and private dinners',
        'Family Transport - Spacious vehicles with child seats on request',
        'Executive Mobility - Quiet rides with Wi-Fi and bottled water',
        'Late-Night Service - Reserved transport for evenings out in the city',
        'Luggage Assistance - Door-to-door handling from room to vehicle'
      ],
      galeriaUrls: ['/images/private-transport.jpg', '/images/airport-transfer.jpg', '/images/private-transport.jpg']
    }
  ];

  obtenerTodos(): Servicio[] {
    return this.servicios;
  }
}