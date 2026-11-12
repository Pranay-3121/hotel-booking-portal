import { formatDate, formatINR } from '../utils';

export default function BookingDetails({ booking }) {
  const cancelled = booking.status === 'CANCELLED';
  return (
    <div className="panel">
      <div className="row-between">
        <div>
          <div className="muted small">Booking reference</div>
          <div className="reference">{booking.reference}</div>
        </div>
        <span className={`badge ${cancelled ? 'badge-red' : 'badge-green'}`}>{booking.status}</span>
      </div>
      <dl className="details">
        <dt>Hotel</dt><dd>{booking.hotelName}, {booking.location}</dd>
        <dt>Guest</dt><dd>{booking.guestName} ({booking.guestEmail})</dd>
        <dt>Check-in</dt><dd>{formatDate(booking.checkIn)}</dd>
        <dt>Check-out</dt><dd>{formatDate(booking.checkOut)}</dd>
        <dt>Stay</dt>
        <dd>{booking.nights} night{booking.nights > 1 ? 's' : ''} x {formatINR(booking.pricePerNight)}</dd>
        <dt>Total</dt><dd><strong>{formatINR(booking.totalAmount)}</strong></dd>
      </dl>
    </div>
  );
}