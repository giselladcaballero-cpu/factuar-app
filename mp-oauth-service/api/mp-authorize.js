// Convenience redirect: the Android app just opens GET /api/mp-authorize and
// lands on Mercado Pago's real consent screen. Keeps the Client ID and
// redirect_uri configured only here, so they can change without an app
// update.
//
// Mercado Pago now requires PKCE on the authorization request. Since this
// bridge is stateless (no session/database), the code_verifier is carried
// through as the `state` param — Mercado Pago echoes it back unchanged on
// the callback, where it's used to complete the PKCE exchange.

import crypto from "crypto";

export default function handler(req, res) {
  const codeVerifier = crypto.randomBytes(64).toString("base64url");
  const codeChallenge = crypto.createHash("sha256").update(codeVerifier).digest("base64url");

  const params = new URLSearchParams({
    client_id: process.env.MP_CLIENT_ID,
    response_type: "code",
    redirect_uri: process.env.MP_REDIRECT_URI,
    state: codeVerifier,
    code_challenge: codeChallenge,
    code_challenge_method: "S256",
  });
  res.redirect(302, `https://auth.mercadopago.com/authorization?${params.toString()}`);
}
