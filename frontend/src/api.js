export class ApiError extends Error {
  constructor(message, status, code, fieldErrors) {
    super(message);
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors || {};
  }
}

async function request(path, options = {}) {
  let res;
  try {
    res = await fetch(`/api${path}`, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    });
  } catch {
    throw new ApiError('Cannot reach the server. Please check your connection and try again.', 0);
  }

  let data = null;
  try {
    data = await res.json();
  } catch {
    /* empty or non-JSON body */
  }

  if (!res.ok) {
    throw new ApiError(
      data?.message || 'Something went wrong. Please try again.',
      res.status, data?.code, data?.fieldErrors,
    );
  }
  return data;
}

function query(params) {
  const q = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v) q.set(k, v);
  });
  const s = q.toString();
  return s ? `?${s}` : '';
}

export const searchHotels = ({ location, checkIn, checkOut } = {}) =>
  request(`/hotels${query({ location, checkIn, checkOut })}`);

export const getHotel = (id, checkIn, checkOut) =>
  request(`/hotels/${id}${query({ checkIn, checkOut })}`);

export const createBooking = (payload) =>
  request('/bookings', { method: 'POST', body: JSON.stringify(payload) });

export const getBooking = (reference, email) =>
  request(`/bookings/${encodeURIComponent(reference)}${query({ email })}`);

export const cancelBooking = (reference, email) =>
  request(`/bookings/${encodeURIComponent(reference)}/cancel`, {
    method: 'POST', body: JSON.stringify({ email }),
  });