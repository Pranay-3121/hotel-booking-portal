import { useEffect, useState } from 'react';
import { getAlternatives } from '../api';
import { formatDate, formatINR } from '../utils';

function shiftLabel(days) {
  const n = Math.abs(days);
  return `${n} day${n > 1 ? 's' : ''} ${days > 0 ? 'later' : 'earlier'}`;
}

export default function AlternativeDates({ hotelId, checkIn, checkOut, onPick }) {
  const key = `${hotelId}|${checkIn}|${checkOut}`;
  const [result, setResult] = useState({ key: '', options: [], failed: false });

  useEffect(() => {
    let cancelled = false;
    getAlternatives(hotelId, checkIn, checkOut)
      .then((options) => { if (!cancelled) setResult({ key, options, failed: false }); })
      .catch(() => { if (!cancelled) setResult({ key, options: [], failed: true }); });
    return () => { cancelled = true; };
  }, [hotelId, checkIn, checkOut, key]);

  if (result.key !== key) return <p className="muted small">Looking for nearby dates...</p>;
  if (result.failed) return null; // suggestions are a bonus, never block the page
  if (result.options.length === 0) {
    return <p className="muted small">No rooms are free within a week of these dates. Try another hotel.</p>;
  }

  return (
    <div className="alternatives">
      <div className="alt-title">Rooms are free on nearby dates</div>
      {result.options.map((o) => (
        <button
          key={o.checkIn}
          type="button"
          className="alt-option"
          onClick={() => onPick(o.checkIn, o.checkOut)}
        >
          <span>
            <strong>{formatDate(o.checkIn)} to {formatDate(o.checkOut)}</strong>
            <span className="muted small"> &middot; {shiftLabel(o.shiftDays)} &middot; {o.nights} night{o.nights > 1 ? 's' : ''}</span>
          </span>
          <span className="alt-price">{formatINR(o.totalAmount)}</span>
        </button>
      ))}
    </div>
  );
}