import { Navigate, Route, Routes } from "react-router-dom";
import { AppLayout } from "./components/layout/AppLayout";
import { DashboardPage } from "./pages/Dashboard/DashboardPage";
import { LoginPage } from "./pages/Login/LoginPage";
import { ProfilePage } from "./pages/Profile/ProfilePage";
import { RegisterPage } from "./pages/Register/RegisterPage";
import { StockDetailsPage } from "./pages/StockDetails/StockDetailsPage";
import { StocksPage } from "./pages/Stocks/StocksPage";
import { WatchlistPage } from "./pages/Watchlist/WatchlistPage";
import { ProtectedRoute } from "./routes/ProtectedRoute";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/watchlists" element={<WatchlistPage />} />
          <Route path="/watchlists/:id" element={<WatchlistPage />} />
          <Route path="/stocks" element={<StocksPage />} />
          <Route path="/stocks/:symbol" element={<StockDetailsPage />} />
          <Route path="/profile" element={<ProfilePage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
