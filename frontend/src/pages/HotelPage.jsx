import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { createBooking, getHotel } from '../api';
import { addDays, formatDate, formatINR, todayISO } from '../utils';
import AlternativeDates from '../components/AlternativeDates';

export default function HotelPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const checkIn = params.get('checkIn') || '';
  const checkOut = params.get('checkOut') || '';
  const today = todayISO();

  const [hotel, setHotel] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [dateError, setDateError] = useState('');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});

  const load = useCallback(async () => {
    setLoadError('');
    setDateError('');
    try {
      setHotel(await getHotel(id, checkIn, checkOut));
    } catch (e) {
      if (e.status === 400 && (checkIn || checkOut)) {
        // Dates are invalid: still show the hotel so the guest can fix the dates
        setDateError(e.message);
        try {
          setHotel(await getHotel(id));
        } catch (e2) {
          setLoadError(e2.message);
        }
      } else {
        setLoadError(e.message);
      }
    } finally {
      setLoading(false);
    }
  }, [id, checkIn, checkOut]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  function setDates(inDate, outDate) {
    const next = {};
    if (inDate) next.checkIn = inDate;
    if (outDate) next.checkOut = outDate;
    setParams(next, { replace: true });
  }

  function onCheckIn(value) {
    const out = value ? (checkOut && checkOut > value ? checkOut : addDays(value, 1)) : '';
    setDates(value, out);
  }

  async function onSubmit(e) {
    e.preventDefault();
    setFormError('');
    setFieldErrors({});
    setSubmitting(true);
    try {
      const booking = await createBooking({
        hotelId: Number(id), guestName: name, guestEmail: email, checkIn, checkOut,
      });
      navigate(`/confirmation/${booking.reference}`, { state: { booking } });
    } catch (err) {
      setFormError(err.message);
      setFieldErrors(err.fieldErrors || {});
      if (err.code === 'SOLD_OUT') load(); // refresh availability shown on screen
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <p className="muted">Loading...</p>;
  if (loadError) return <p className="notice notice-error">{loadError} <Link to="/">Back to hotels</Link></p>;
  if (!hotel) return null;

  const hasQuote = hotel.nights != null && !dateError;
  const soldOut = hasQuote && hotel.availableRooms === 0;
  const canBook = hasQuote && !soldOut;

  return (
    <>
      <Link to="/" className="back">&larr; All hotels</Link>
      <div className="two-col">
        <section>
          <div className="card-banner large">{hotel.location}</div>
          <h1>{hotel.name}</h1>
          <p>{hotel.description}</p>
          <div className="chips">
            {hotel.amenities.map((a) => <span key={a} className="chip">{a}</span>)}
          </div>
          <p className="price big">
            {formatINR(hotel.pricePerNight)} <span className="muted small">/ night</span>
          </p>
        </section>

        <aside className="panel">
          <h2>Book your stay</h2>
          <div className="date-row">
            <label>
              Check-in
              <input type="date" min={today} value={checkIn} onChange={(e) => onCheckIn(e.target.value)} />
            </label>
            <label>
              Check-out
              <input
                type="date" min={checkIn ? addDays(checkIn, 1) : addDays(today, 1)}
                value={checkOut} onChange={(e) => setDates(checkIn, e.target.value)}
              />
            </label>
          </div>

          {dateError && <p className="notice notice-error">{dateError}</p>}
          {!checkIn && !checkOut && <p className="muted small">Select your dates to see availability and price.</p>}

          {soldOut && (
            <>
              <p className="notice notice-error">No rooms available for these dates.</p>
              <AlternativeDates
                hotelId={hotel.id}
                checkIn={checkIn}
                checkOut={checkOut}
                onPick={(inDate, outDate) => setDates(inDate, outDate)}
              />
            </>
          )}

          {canBook && hotel.availableRooms <= 2 && (
            <p className="notice notice-warn">Hurry: only {hotel.availableRooms} room{hotel.availableRooms > 1 ? 's' : ''} left!</p>
          )}

          {canBook && (
            <div className="summary">
              <div className="row-between">
                <span>{formatINR(hotel.pricePerNight)} x {hotel.nights} night{hotel.nights > 1 ? 's' : ''}</span>
                <span>{formatINR(hotel.totalAmount)}</span>
              </div>
              <div className="row-between total">
                <span>Total</span><span>{formatINR(hotel.totalAmount)}</span>
              </div>
              <div className="muted small">{formatDate(checkIn)} to {formatDate(checkOut)}</div>
            </div>
          )}

          <form onSubmit={onSubmit} noValidate>
            <label>
              Full name
              <input type="text" value={name} onChange={(e) => setName(e.target.value)} />
              {fieldErrors.guestName && <span className="field-error">{fieldErrors.guestName}</span>}
            </label>
            <label>
              Email
              <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
              {fieldErrors.guestEmail && <span className="field-error">{fieldErrors.guestEmail}</span>}
            </label>
            {formError && <p className="notice notice-error">{formError}</p>}
            <button className="btn btn-block" type="submit" disabled={!canBook || submitting}>
              {submitting ? 'Booking...' : 'Confirm booking'}
            </button>
          </form>
        </aside>
      </div>
    </>
  );
}