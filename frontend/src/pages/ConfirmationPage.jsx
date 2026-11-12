import { Link, useLocation, useParams } from 'react-router-dom';
import BookingDetails from '../components/BookingDetails';

export default function ConfirmationPage() {
  const { reference } = useParams();
  const { state } = useLocation();
  const booking = state?.booking;

  if (!booking) {
    return (
      <div className="notice">
        <p>We can only show the confirmation right after booking.</p>
        <Link to={`/manage?reference=${reference}`}>Look up booking {reference}</Link>
      </div>
    );
  }

  return (
    <>
      <h1>Booking confirmed</h1>
      <p>Thank you, {booking.guestName}! Please save your reference. You will need it, along with your email, to view or cancel this booking.</p>
      <BookingDetails booking={booking} />
      <p>
        <Link className="btn" to="/">Book another stay</Link>{' '}
        <Link className="btn btn-outline" to={`/manage?reference=${booking.reference}`}>Manage this booking</Link>
      </p>
    </>
  );
}