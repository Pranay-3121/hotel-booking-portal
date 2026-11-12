import { useEffect, useState } from 'react';
import { searchHotels } from '../api';
import { addDays, todayISO } from '../utils';
import HotelCard from '../components/HotelCard';

export default function HomePage() {
  const today = todayISO();
  const [location, setLocation] = useState('');
  const [checkIn, setCheckIn] = useState('');
  const [checkOut, setCheckOut] = useState('');
  const [hotels, setHotels] = useState([]);
  const [searchedDates, setSearchedDates] = useState({ checkIn: '', checkOut: '' });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function runSearch(params) {
    setLoading(true);
    setError('');
    try {
      setHotels(await searchHotels(params));
      setSearchedDates({ checkIn: params.checkIn || '', checkOut: params.checkOut || '' });
    } catch (e) {
      setError(e.message);
      setHotels([]);
    } finally {
      setLoading(false);
    }
  }

    useEffect(() => {
    let cancelled = false;
    searchHotels({})
      .then((data) => { if (!cancelled) setHotels(data); })
      .catch((e) => { if (!cancelled) setError(e.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, []);

  function onCheckIn(value) {
    setCheckIn(value);
    if (value && (!checkOut || checkOut <= value)) setCheckOut(addDays(value, 1));
  }

  function onSubmit(e) {
    e.preventDefault();
    if ((checkIn && !checkOut) || (!checkIn && checkOut)) {
      setError('Please select both check-in and check-out dates.');
      return;
    }
    if (checkIn && checkOut <= checkIn) {
      setError('Check-out must be after check-in.');
      return;
    }
    runSearch({ location, checkIn, checkOut });
  }

  return (
    <>
      <section className="hero">
        <h1>Find your perfect stay</h1>
        <form className="search" onSubmit={onSubmit}>
          <label>
            Location
            <input
              type="text" placeholder="e.g. Goa" value={location}
              onChange={(e) => setLocation(e.target.value)}
            />
          </label>
          <label>
            Check-in
            <input type="date" min={today} value={checkIn} onChange={(e) => onCheckIn(e.target.value)} />
          </label>
          <label>
            Check-out
            <input
              type="date" min={checkIn ? addDays(checkIn, 1) : addDays(today, 1)}
              value={checkOut} onChange={(e) => setCheckOut(e.target.value)}
            />
          </label>
          <button className="btn" type="submit">Search</button>
        </form>
      </section>

      {error && <p className="notice notice-error">{error}</p>}
      {loading && <p className="muted">Loading hotels...</p>}
      {!loading && !error && hotels.length === 0 && (
        <p className="notice">No hotels found for this search. Try another location.</p>
      )}

      <section className="grid">
        {hotels.map((h) => (
          <HotelCard key={h.id} hotel={h} checkIn={searchedDates.checkIn} checkOut={searchedDates.checkOut} />
        ))}
      </section>
    </>
  );
}