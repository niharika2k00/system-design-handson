# Parking Lot — LLD

## Entities

| Entity | Responsibility |
|--------|----------------|
| `ParkingLot` | Singleton. Orchestrates park/unpark. Owns levels. |
| `ParkingLevel` | Holds spots on a level. Finds available spot for vehicle. |
| `ParkingSpot` | Physical slot. Knows fit rules, occupancy. |
| `Vehicle` | License plate + type. |
| `Ticket` | Entry record. Links vehicle ↔ spot ↔ time. |
| `FeeStrategy` | Pluggable pricing (Strategy pattern). |
| `Payment` | Settles ticket amount. |

## ER Diagram

```
        ┌──────────────┐
        │  ParkingLot  │  (Singleton)
        │──────────────│
        │ floors[]     │
        │ feeStrategy  │
        │ tickets{}    │
        └──────┬───────┘
               │ 1..*
               ▼
        ┌──────────────┐
        │ ParkingLevel │
        │──────────────│
        │ floorNumber  │
        │ spots[]      │
        └──────┬───────┘
               │ 1..*
               ▼
        ┌──────────────┐        ┌──────────────┐
        │ ParkingSpot  │◄──────►│   Vehicle    │
        │──────────────│ assign │──────────────│
        │ id, type     │        │ plate, type  │
        │ occupied     │        └──────┬───────┘
        └──────┬───────┘               │
               │                       │
               └────────┬──────────────┘
                        ▼
                ┌──────────────┐
                │   Ticket     │
                │──────────────│
                │ id, status   │
                │ entry, exit  │
                │ amount       │
                └──────┬───────┘
                       │ uses
                       ▼
                ┌──────────────┐         ┌──────────────┐
                │ FeeStrategy  │◄────────│   Payment    │
                │ (interface)  │         │──────────────│
                │──────────────│         │ mode, amount │
                │ Hourly | Flat│         └──────────────┘
                └──────────────┘
```

## Flow

```
park(vehicle)
   │
   ▼
ParkingLot ──► Floor.findAvailableSpot ──► Spot.canFit?
   │                                          │ yes
   │                                          ▼
   │                                      Spot.assign
   │                                          │
   ▼                                          ▼
Ticket created ◄──────────────────────── stored in activeTickets


unpark(ticketId)
   │
   ▼
ParkingLot.lookup ──► FeeStrategy.calculate ──► Payment.process
                                                     │
                                                     ▼
                                              Spot.release
                                              Ticket.close
```

## Design Patterns

- **Singleton** — `ParkingLot` (single entry point).
- **Strategy** — `FeeStrategy` (swap pricing without changing lot).
- **Factory** *(extension)* — vehicle/spot creation.

## Extensibility hooks

- New `VehicleType` → add enum + spot fit rule.
- New pricing → new `FeeStrategy` impl.
- New `PaymentMode` → add enum + gateway impl.
- Multi-entry/exit gates → `EntryGate` / `ExitGate` classes wrapping lot.
