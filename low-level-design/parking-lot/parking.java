import java.util.*;
import java.time.*;

// ===== Enums =====
enum VehicleType {
  BIKE, CAR, TRUCK
}

enum SpotType {
  SMALL, MEDIUM, LARGE
}

enum TicketStatus {
  ACTIVE, PAID, CANCELLED
}

enum PaymentMode {
  CASH, CARD, UPI
}

// ===== Vehicle =====
abstract class Vehicle {
  private final String numberPlate;
  private final VehicleType type;

  protected Vehicle(String numberPlate, VehicleType type) {
    this.numberPlate = numberPlate;
    this.type = type;
  }

  public String getNumberPlate() { return numberPlate; }

  public VehicleType getType() { return type; }
}

class Bike extends Vehicle {
  public Bike(String numberPlate) {
    super(numberPlate, VehicleType.BIKE);
  }
}

class Car extends Vehicle {
  public Car(String numberPlate) {
    super(numberPlate, VehicleType.CAR);
  }
}

class Truck extends Vehicle {
  public Truck(String numberPlate) {
    super(numberPlate, VehicleType.TRUCK);
  }
}

// ===== Parking Spot =====
class ParkingSpot {
  private final String id;
  private final SpotType type;
  private boolean occupied;
  private Vehicle vehicle;

  public ParkingSpot(String id, SpotType type) {
    this.id = id;
    this.type = type;
  }

  public boolean canFit(Vehicle v) {
    if (occupied)
      return false;
    switch (v.getType()) {
    case BIKE:
      return true;
    case CAR:
      return type == SpotType.MEDIUM || type == SpotType.LARGE;
    case TRUCK:
      return type == SpotType.LARGE;
    default:
      return false;
    }
  }

  public void assign(Vehicle v) {
    this.vehicle = v;
    this.occupied = true;
  }

  public void release() {
    this.vehicle = null;
    this.occupied = false;
  }

  public String getId() { return id; }

  public SpotType getType() { return type; }

  public boolean isOccupied() { return occupied; }

  public Vehicle getVehicle() { return vehicle; }
}

// ===== Level =====
class ParkingLevel {
  private final int levelNumber;
  private final List<ParkingSpot> spots;

  public ParkingLevel(int levelNumber, List<ParkingSpot> spots) {
    this.levelNumber = levelNumber;
    this.spots = spots;
  }

  public ParkingSpot findAvailableSpot(Vehicle v) {
    for (ParkingSpot s : spots) {
      if (s.canFit(v))
        return s;
    }
    return null;
  }

  public int getLevelNumber() { return levelNumber; }

  public List<ParkingSpot> getSpots() { return spots; }
}

// ===== Ticket =====
class Ticket {
  private final String ticketId;
  private final Vehicle vehicle;
  private final ParkingSpot spot;
  private final LocalDateTime entryTime;
  private LocalDateTime exitTime;
  private TicketStatus status;
  private double amount;

  public Ticket(String ticketId, Vehicle vehicle, ParkingSpot spot) {
    this.ticketId = ticketId;
    this.vehicle = vehicle;
    this.spot = spot;
    this.entryTime = LocalDateTime.now();
    this.status = TicketStatus.ACTIVE;
  }

  // getters and setters
  public String getTicketId() { return ticketId; }

  public Vehicle getVehicle() { return vehicle; }

  public ParkingSpot getSpot() { return spot; }

  public LocalDateTime getEntryTime() { return entryTime; }

  public LocalDateTime getExitTime() { return exitTime; }

  public TicketStatus getStatus() { return status; }

  public double getAmount() { return amount; }

  public void close(double amount) {
    this.exitTime = LocalDateTime.now();
    this.amount = amount;
    this.status = TicketStatus.PAID;
  }
}

// ===== Fee Strategy (Strategy Pattern) =====
// Design Pattern: STRATEGY (Behavioral)
// Role: Strategy interface. Encapsulates fee-calculation algorithm so it can be
// swapped at runtime (Hourly, Flat, Tiered, etc.) without modifying ParkingLot.
// Honors OCP: add new pricing by adding a new implementation, no existing change.
interface FeeStrategy {
  double calculate(Ticket ticket);
}

// Design Pattern: STRATEGY (Concrete Strategy)
// Role: Concrete algorithm — per-hour rate keyed by VehicleType.
class HourlyFeeStrategy implements FeeStrategy {
  private static final Map<VehicleType, Double> RATE = Map.of(
      VehicleType.BIKE, 10.0,
      VehicleType.CAR, 20.0,
      VehicleType.TRUCK, 40.0);

  @Override
  public double calculate(Ticket ticket) {
    long hours = Math.max(1, Duration.between(ticket.getEntryTime(), LocalDateTime.now()).toHours());
    return hours * RATE.get(ticket.getVehicle().getType());
  }
}

// ===== Payment =====
class Payment {
  private final String paymentId;
  private final double amount;
  private final PaymentMode mode;

  public Payment(String paymentId, double amount, PaymentMode mode) {
    this.paymentId = paymentId;
    this.amount = amount;
    this.mode = mode;
  }

  public boolean process() {
    // gateway call stub
    return true;
  }
}

// ===== Parking Lot (Singleton) =====
// Design Pattern: SINGLETON (Creational)
// Role: Single shared instance for the whole lot — global access via getInstance().
// Lazy init guarded by `synchronized` on init(). Also acts as Context for the
// Strategy pattern (holds FeeStrategy and delegates calculation to it).
class ParkingLot {
  private static ParkingLot instance;
  private final List<ParkingLevel> levels;
  private final Map<String, Ticket> activeTickets = new HashMap<>();
  private final FeeStrategy feeStrategy;

  private ParkingLot(List<ParkingLevel> levels, FeeStrategy feeStrategy) {
    this.levels = levels;
    this.feeStrategy = feeStrategy;
  }

  public static synchronized ParkingLot init(List<ParkingLevel> levels, FeeStrategy strategy) {
    if (instance == null)
      instance = new ParkingLot(levels, strategy);
    return instance;
  }

  public static ParkingLot getInstance() { return instance; }

  public synchronized Ticket parkVehicle(Vehicle v) {
    for (ParkingLevel f : levels) {
      ParkingSpot spot = f.findAvailableSpot(v);
      if (spot != null) {
        spot.assign(v);
        Ticket t = new Ticket(UUID.randomUUID().toString(), v, spot);
        activeTickets.put(t.getTicketId(), t);
        return t;
      }
    }
    throw new RuntimeException("Lot full");
  }

  public synchronized double unparkVehicle(String ticketId, PaymentMode mode) {
    Ticket t = activeTickets.remove(ticketId);
    if (t == null)
      throw new RuntimeException("Invalid ticket");
    double fee = feeStrategy.calculate(t);
    Payment p = new Payment(UUID.randomUUID().toString(), fee, mode);
    if (!p.process())
      throw new RuntimeException("Payment failed");
    t.close(fee);
    t.getSpot().release();
    return fee;
  }
}

// ===== Demo =====
public class parking {
  public static void main(String[] args) {
    List<ParkingSpot> spots = new ArrayList<>();
    spots.add(new ParkingSpot("F1-S1", SpotType.SMALL));
    spots.add(new ParkingSpot("F1-M1", SpotType.MEDIUM));
    spots.add(new ParkingSpot("F1-L1", SpotType.LARGE));

    ParkingLevel level1 = new ParkingLevel(1, spots);
    ParkingLot lot = ParkingLot.init(List.of(level1), new HourlyFeeStrategy());

    Vehicle car = new Car("KA-01-1234");
    Ticket ticket = lot.parkVehicle(car);
    System.out.println("Parked at: " + ticket.getSpot().getId());

    double fee = lot.unparkVehicle(ticket.getTicketId(), PaymentMode.UPI);
    System.out.println("Fee: " + fee);
  }
}

// Entity -> Person, Place and Things any Noun
/*
Trick:
while doing system design questions - think Top-Down approach [big to small] while crreating the Entity classes
and Down-Top approach while implementing them (code)
*/
