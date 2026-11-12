import { Link } from 'react-router-dom';
import { formatINR } from '../utils';

export default function HotelCard({ hotel, checkIn, checkOut }) {
  const hasDates = hotel.availableRooms != null;
  const soldOut = hasDates && hotel.availableRooms === 0;
  const fewLeft = hasDates && hotel.availableRooms > 0 && hotel.availableRooms <= 2;
  const query = checkIn && checkOut ? `?checkIn=${checkIn}&checkOut=${checkOut}` : '';

  return (
    <article className="card">
      <div className="card-banner">{hotel.location}</div>
      <div className="card-body">
        <h3>{hotel.name}</h3>
        <p className="muted">{hotel.description}</p>
        <div className="chips">
          {hotel.amenities.map((a) => <span key={a} className="chip">{a}</span>)}
        </div>
        <div className="card-footer">
          <div>
            <div className="price">
              {formatINR(hotel.pricePerNight)} <span className="muted small">/ night</span>
            </div>
            {hasDates && !soldOut && (
              <div className="muted small">
                {hotel.nights} night{hotel.nights > 1 ? 's' : ''}: {formatINR(hotel.totalAmount)}
              </div>
            )}
            {soldOut && <span className="badge badge-red">Sold out for these dates</span>}
            {fewLeft && <span className="badge badge-amber">Only {hotel.availableRooms} left</span>}
          </div>
          <Link className="btn" to={`/hotels/${hotel.id}${query}`}>View</Link>
        </div>
      </div>
    </article>
  );
}