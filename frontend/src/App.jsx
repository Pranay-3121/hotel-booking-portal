import { Link, NavLink, Route, Routes } from 'react-router-dom';
import HomePage from './pages/HomePage';
import HotelPage from './pages/HotelPage';
import ConfirmationPage from './pages/ConfirmationPage';
import ManageBookingPage from './pages/ManageBookingPage';

export default function App() {
  return (
    <>
      <header className="topbar">
        <div className="container topbar-inner">
          <Link to="/" className="brand">StayEasy</Link>
          <nav>
            <NavLink to="/" end>Hotels</NavLink>
            <NavLink to="/manage">Manage booking</NavLink>
          </nav>
        </div>
      </header>
      <main className="container">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/hotels/:id" element={<HotelPage />} />
          <Route path="/confirmation/:reference" element={<ConfirmationPage />} />
          <Route path="/manage" element={<ManageBookingPage />} />
          <Route path="*" element={<p className="notice">Page not found. <Link to="/">Go home</Link></p>} />
        </Routes>
      </main>
    </>
  );
}