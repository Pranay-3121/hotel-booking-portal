import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { cancelBooking, getBooking } from '../api';
import BookingDetails from '../components/BookingDetails';

export default function ManageBookingPage() {
  const [params] = useSearchParams();
  const [reference, setReference] = useState(params.get('reference') || '');
  const [email, setEmail] = useState('');
  const [booking, setBooking] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  async function onFind(e) {
    e.preventDefault();
    setError('');
    setMessage('');
    setBooking(null);
    if (!reference.trim() || !email.trim()) {
      setError('Please enter both your booking reference and email.');
      return;
    }
    setLoading(true);
    try {
      setBooking(await getBooking(reference.trim(), email.trim()));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function onCancel() {
    if (!window.confirm('Cancel this booking? This cannot be undone.')) return;
    setError('');
    setMessage('');
    setLoading(true);
    try {
      setBooking(await cancelBooking(booking.reference, email.trim()));
      setMessage('Your booking has been cancelled and the room has been released.');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      <h1>Manage your booking</h1>
      <form className="panel narrow" onSubmit={onFind} noValidate>
        <label>
          Booking reference
          <input type="text" placeholder="BK-XXXXXXXX" value={reference} onChange={(e) => setReference(e.target.value)} />
        </label>
        <label>
          Email used for booking
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
        </label>
        <button className="btn" type="submit" disabled={loading}>{loading ? 'Please wait...' : 'Find booking'}</button>
      </form>

      {error && <p className="notice notice-error">{error}</p>}
      {message && <p className="notice notice-ok">{message}</p>}

      {booking && (
        <>
          <BookingDetails booking={booking} />
          {booking.status === 'CONFIRMED' && (
            <button className="btn btn-danger" onClick={onCancel} disabled={loading}>Cancel booking</button>
          )}
        </>
      )}
    </>
  );
}