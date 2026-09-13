// Convenience redirect: the Android app just opens GET /mp-authorize and
// lands on Mercado Pago's real consent screen. Keeps the Client ID and
// redirect_uri configured only here, so they can change without an app
// update.
//
// Mercado Pago requires PKCE on the authorization request. Since this
// bridge is stateless (no session/database), the code_verifier is carried
// through as the `state` param — Mercado Pago echoes it back unchanged on
// the callback, where it's used to complete the PKCE exchange.

function base64url(bytes: Uint8Array): string {
  return btoa(String.fromCharCode(...bytes))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

async function sha256Base64Url(input: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(input));
  return base64url(new Uint8Array(digest));
}

Deno.serve(async (_req: Request) => {
  const codeVerifier = base64url(crypto.getRandomValues(new Uint8Array(64)));
  const codeChallenge = await sha256Base64Url(codeVerifier);

  const params = new URLSearchParams({
    client_id: Deno.env.get("MP_CLIENT_ID") ?? "",
    response_type: "code",
    redirect_uri: Deno.env.get("MP_REDIRECT_URI") ?? "",
    state: codeVerifier,
    code_challenge: codeChallenge,
    code_challenge_method: "S256",
  });

  return Response.redirect(
    `https://auth.mercadopago.com/authorization?${params.toString()}`,
    302,
  );
});
