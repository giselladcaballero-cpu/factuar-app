// Convenience redirect: the Android app just opens
// GET /api/mp-authorize?state=<random> and lands on Mercado Pago's real
// consent screen. Keeps the Client ID and redirect_uri configured only here,
// so they can change without an app update.

export default function handler(req, res) {
  const { state } = req.query;
  const params = new URLSearchParams({
    client_id: process.env.MP_CLIENT_ID,
    response_type: "code",
    platform_id: "mp",
    redirect_uri: process.env.MP_REDIRECT_URI,
    state: state || "",
  });
  res.redirect(302, `https://auth.mercadopago.com.ar/authorization?${params.toString()}`);
}
